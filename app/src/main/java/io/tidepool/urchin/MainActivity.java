package io.tidepool.urchin;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Calendar;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmList;
import io.tidepool.urchin.api.APIClient;
import io.tidepool.urchin.data.Note;
import io.tidepool.urchin.data.Profile;
import io.tidepool.urchin.data.EmailAddress;
import io.tidepool.urchin.data.SharedUserId;
import io.tidepool.urchin.data.User;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "MainActivity";

    // Activity request codes
    private static final int REQ_LOGIN = 1;

    private APIClient _apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Realm realm = Realm.getInstance(this);
        Log.d(LOG_TAG, "Realm path: " + realm.getPath());

        // Create our API client on the appropriate service
        _apiClient = new APIClient(this, APIClient.PRODUCTION);
        String sessionId = _apiClient.getSessionId();
        User user = _apiClient.getUser();
        if ( sessionId == null || user == null ) {
            // We need to sign in
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivityForResult(loginIntent, REQ_LOGIN);
        } else {
            updateUser();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch ( requestCode ) {
            case REQ_LOGIN:
                // Sent from the LoginActivity.
                // Get the auth token and user from the intent
                if ( resultCode == Activity.RESULT_OK ) {
                    Log.d(LOG_TAG, "Sign-In success");
                    updateUser();
                }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Gets information about the current user
     */
    private void updateUser() {
        final Date to = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(to);
        c.add(Calendar.MONTH, -3);
        final Date from = c.getTime();
        _apiClient.getViewableUserIds(new APIClient.ViewableUserIdsListener() {
            @Override
            public void fetchComplete(RealmList<SharedUserId> userIds, Exception error) {
                Log.d(LOG_TAG, "Viewable IDs: " + userIds + "Error: " + error);
                if ( userIds != null ) {
                    for (SharedUserId userId : userIds) {
                        _apiClient.getProfileForUserId(userId.getVal(), new APIClient.ProfileListener() {
                            @Override
                            public void profileReceived(Profile profile, Exception error) {
                                Log.d(LOG_TAG, "Profile: " + profile);
                            }
                        });
                        _apiClient.getNotes(userId.getVal(), from, to, new APIClient.NotesListener() {
                            @Override
                            public void notesReceived(RealmList<Note> notes, Exception error) {
                                Log.d(LOG_TAG, "Notes received: " + notes + " error: " + error);
                            }
                        });
                    }
                }
            }
        });
    }
}
