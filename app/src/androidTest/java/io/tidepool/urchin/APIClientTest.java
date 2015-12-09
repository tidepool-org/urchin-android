package io.tidepool.urchin;

import com.android.volley.AuthFailureError;

import android.app.Instrumentation;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.InstrumentationTestCase;
import android.widget.Toast;

import static com.jayway.awaitility.Awaitility.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import io.tidepool.urchin.api.APIClient;
import io.tidepool.urchin.data.Note;
import io.tidepool.urchin.data.User;
import io.tidepool.urchin.util.HashtagUtils;
import io.tidepool.urchin.util.Log;
import io.tidepool.urchin.util.MiscUtils;

@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class APIClientTest extends InstrumentationTestCase {
    @Override
    @Before
    public void setUp() throws Exception {
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        mTargetContext = getInstrumentation().getTargetContext();

        _setUpAPIClient(APIClient.DEVELOPMENT);
    }

    @Test
    public void testAllOnStaging() throws AssertionError {
        Log.v(this.getClass().getName(), "testAllOnStaging");
        _testAllOn(APIClient.STAGING);
    }

    @Test
    public void testAllOnProduction() throws AssertionError {
        Log.v(this.getClass().getName(), "testAllOnProduction");
        _testAllOn(APIClient.PRODUCTION);
    }

    @Test
    public void testSignInSuccess() throws AssertionError {
        mAwaitDone = new AtomicBoolean(false);
        mAPIClient.signIn("ethan+urchintests@tidepool.org", "urchintests", new APIClient.SignInListener() {
            @Override
            public void signInComplete(User user, Exception exception) {
                mAWaitHashMap = new HashMap<String, Object>();
                mAWaitHashMap.put("user", user);
                mAWaitHashMap.put("exception", exception);
                mAwaitDone.set(true);
            }
        });
        await().atMost(10, TimeUnit.SECONDS).untilTrue(mAwaitDone);

        User user = (User) mAWaitHashMap.get("user");
        Exception exception = (Exception) mAWaitHashMap.get("exception");
        assertThat(user, notNullValue());
        assertThat(exception, nullValue());
    }

    @Test
    public void testSignInFailure() throws AssertionError {
        mAwaitDone = new AtomicBoolean(false);
        mAPIClient.signIn("ethan+urchintests@tidepool.org", "wrong-password", new APIClient.SignInListener() {
            @Override
            public void signInComplete(User user, Exception error) {
                mAWaitHashMap = new HashMap<String, Object>();
                mAWaitHashMap.put("user", user);
                mAWaitHashMap.put("error", error);
                mAwaitDone.set(true);
            }
        });
        await().atMost(10, TimeUnit.SECONDS).untilTrue(mAwaitDone);

        User user = (User) mAWaitHashMap.get("user");
        Exception error = (Exception) mAWaitHashMap.get("error");
        assertThat(user, nullValue());
        assertThat(error, instanceOf(AuthFailureError.class));
    }

    @Test
    public void testSignOut() throws AssertionError {
        testSignInSuccess();

        mAwaitDone = new AtomicBoolean(false);
        mAPIClient.signOut(new APIClient.SignOutListener() {
            @Override
            public void signedOut(int responseCode, Exception error) {
                mAWaitHashMap = new HashMap<String, Object>();
                mAWaitHashMap.put("responseCode", responseCode);
                mAWaitHashMap.put("error", error);
                mAwaitDone.set(true);
            }
        });
        await().atMost(10, TimeUnit.SECONDS).untilTrue(mAwaitDone);

        int responseCode = (int) mAWaitHashMap.get("responseCode");
        Exception error = (Exception) mAWaitHashMap.get("error");
        assertThat(responseCode, is(200));
        assertThat(error, nullValue());
    }

// TODO: my - This is failing due to issues with Realm. The session ID is failing to be stored and later retrieved in the Realm it seems.
//    @Test
//    public void testRefreshToken() throws AssertionError {
//        testSignInSuccess(); // TODO: my - After this, getSessionId is still null
//
//        mAwaitDone = new AtomicBoolean(false);
//        mAPIClient.refreshToken(new APIClient.RefreshTokenListener() {
//            @Override
//            public void tokenRefreshed(Exception error) {
//                mAWaitHashMap = new HashMap<String, Object>();
//                mAWaitHashMap.put("error", error);
//                mAwaitDone.set(true);
//            }
//        });
//        await().atMost(10, TimeUnit.SECONDS).untilTrue(mAwaitDone);
//
//        String sessionId = mAPIClient.getSessionId();
//        Exception error = (Exception) mAWaitHashMap.get("error");
//        assertThat(sessionId, notNullValue());
//        assertThat(error, nullValue());
//    }

// TODO: my - This is failing due to issues with Realm. The session ID is failing to be stored and later retrieved in the Realm it seems.
//    @Test
//    public void testPostNote() {
//        testSignInSuccess();
//
//        Note note = new Note();
//        note.setMessagetext("New note added from test.");
//        note.setTimestamp(new Date());
//        note.setGroupid(mAPIClient.getUser().getUserid());
//        note.setUserid(mAPIClient.getUser().getUserid());
//        note.setAuthorFullName(MiscUtils.getPrintableNameForUser(mAPIClient.getUser()));
//        note.setGuid(UUID.randomUUID().toString());
//
//        mAwaitDone = new AtomicBoolean(false);
//        mAPIClient.postNote(note, new APIClient.PostNoteListener() {
//            @Override
//            public void notePosted(Note note, Exception error) {
//                mAWaitHashMap = new HashMap<String, Object>();
//                mAWaitHashMap.put("error", error);
//                mAwaitDone.set(true);
//            }
//        });
//        await().atMost(10, TimeUnit.SECONDS).untilTrue(mAwaitDone);
//
//        Exception error = (Exception) mAWaitHashMap.get("error");
//        assertThat(error, nullValue());
//    }

    private void _testAllOn(String environment) throws AssertionError {
        _setUpAPIClient(environment);

        Pattern allOnPattern = Pattern.compile("^.*AllOn.*$");
        for (Method method : getClass().getDeclaredMethods()) {
            String methodName = method.getName();
            if (method.isAnnotationPresent(Test.class) &&
                    !allOnPattern.matcher(methodName).matches()) {
                Log.v(this.getClass().getName(), "invoking method: " + methodName);
                try {
                    method.invoke(this);
                } catch (IllegalAccessException e) {
                } catch (InvocationTargetException e) {
                    if (e.getTargetException() instanceof AssertionError) {
                        throw (AssertionError) e.getTargetException();
                    }
                }
            }
        }
    }

    private void _setUpAPIClient(String environment) {
        mAPIClient = new APIClient(getInstrumentation().getTargetContext(), environment);
        Log.v(this.getClass().getName(), "APIClient set up for environment: " + environment);
    }

    private Context mTargetContext;
    private APIClient mAPIClient;
    private AtomicBoolean mAwaitDone;
    private HashMap<String, Object> mAWaitHashMap;
}
