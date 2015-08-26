package io.tidepool.urchin;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.gson.Gson;

import java.util.List;

import io.tidepool.urchin.io.tidepool.urchin.api.APIClient;
import io.tidepool.urchin.io.tidepool.urchin.api.Profile;
import io.tidepool.urchin.io.tidepool.urchin.api.User;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "MainActivity";

    // Activity request codes
    private static final int REQ_LOGIN = 1;

    private APIClient _apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create our API client on the appropriate service
        _apiClient = new APIClient(this, APIClient.PRODUCTION);
        if ( _apiClient.getSessionId() == null ) {
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
                    String token = data.getStringExtra(LoginActivity.SESSION_ID);
                    String json = data.getStringExtra(LoginActivity.USER_JSON);
                    Log.d(LOG_TAG, "Sign-In success: " + json);
                    if ( token != null ) {
                        _apiClient.setSessionId(token);
                        _apiClient.setUser(new Gson().fromJson(json, User.class));
                        updateUser();
                    }
                }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Gets information about the current user
     */
    private void updateUser() {
        _apiClient.getViewableUserIds(new APIClient.ViewableUserIdsListener() {
            @Override
            public void fetchComplete(List<String> userIds, Exception error) {
                Log.d(LOG_TAG, "Viewable IDs: " + userIds + "Error: " + error);
                for ( String userId : userIds ) {
                    _apiClient.getProfileForUserId(userId, new APIClient.ProfileListener() {
                        @Override
                        public void profileReceived(Profile profile, Exception error) {
                            Log.d(LOG_TAG, "Profile: " + profile);
                        }
                    });
                }
            }
        });
    }
}
