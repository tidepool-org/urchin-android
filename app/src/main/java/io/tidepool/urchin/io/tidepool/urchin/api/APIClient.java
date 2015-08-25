package io.tidepool.urchin.io.tidepool.urchin.api;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Brian King on 8/25/15.
 */
public class APIClient {

    public static final String PRODUCTION = "Production";
    public static final String DEVELOPMENT = "Development";
    public static final String STAGING = "Staging";

    private static final String LOG_TAG = "APIClient";

    // Header label for the session token
    private static final String HEADER_SESSION_ID = "x-tidepool-session-token";

    // Key into the shared preferences database for our own preferences
    private static final String PREFS_KEY = "APIClient";

    // Key into the shared preferences to store our session ID (so we don't need to log in each time)
    private static final String KEY_SESSION_ID = "sessionID";

    // Map of server names to base URLs
    private static final Map<String, URL> __servers;

    // RequestQueue our requests will be made on
    private RequestQueue _requestQueue;

    // Base URL for network requests
    private URL _baseURL;

    // Context used to create us
    private Context _context;

    // Session ID, set after login
    private String _sessionId;

    // Static initialization
    static {
        __servers = new HashMap<>();
        try {
            __servers.put(PRODUCTION, new URL("https://api.tidepool.io"));
            __servers.put(DEVELOPMENT, new URL("https://devel-api.tidepool.io"));
            __servers.put(STAGING, new URL("https://staging-api.tidepool.io"));

        } catch (MalformedURLException e) {
            // Should never happen
        }
    }

    /**
     * Constructor
     *
     * @param context Context
     * @param server Server to connect to, one of Production, Development or Staging
     */
    public APIClient(Context context, String server) {
        _context = context;
        _baseURL = __servers.get(server);

        // Set up the disk cache for caching responses
        Cache cache = new DiskBasedCache(context.getCacheDir(), 1024*1024);

        // Set up the HTTPURLConnection network stack
        Network network = new BasicNetwork(new HurlStack());

        // Create the request queue using the cache and network we just created
        _requestQueue = new RequestQueue(cache, network);
        _requestQueue.start();
    }

    /**
     * Sets the server the API client will connect to. Valid servers are:
     * <ul>
     *     <li>Production</li>
     *     <li>Development</li>
     *     <li>Staging</li>
     * </ul>
     * @param serverType String with one of the above values used to set the server
     */
    public void setServer(String serverType) {
        URL url = __servers.get(serverType);
        if ( url == null ) {
            Log.e(LOG_TAG, "No server called " + serverType + " found in map");
        } else {
            _baseURL = url;
        }
    }

    /**
     * Returns the session ID used for this client.
     * @return the session ID, or null if not authenticated
     */
    public String getSessionId() {
        return _sessionId;
    }

    /**
     * Sets the session ID to be used for all subsequent requests. Setting the session ID will
     * cancel any outstanding requests.
     *
     * @param sessionId new session ID to use
     */
    public void setSessionId(String sessionId) {
        _requestQueue.cancelAll(_context);

        _sessionId = sessionId;

        // Save this to the shared preferences
        if ( sessionId == null ) {
            _context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE).edit()
                    .remove(KEY_SESSION_ID);
        } else {
            _context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE).edit()
                    .putString(KEY_SESSION_ID, sessionId).apply();
        }
    }

    public static abstract class SignInListener {
        /**
         * Called when the sign-in request returns. This method will capture the session ID
         * from the headers returned in the sign-in request, and use it in all subsequent
         * requests.
         *
         * @param user User object, if the sign-in was successful
         * @param exception Exception if the sign-in was not successful
         */
        public abstract void signInComplete(User user, Exception exception);
    }

    /**
     * Signs in a user. The listener will be called with the user object, or an error if the sign
     * in failed.
     *
     * @param username Username
     * @param password Password
     * @param listener Listener to receive the result
     * @return a Request object, which may be canceled.
     */
    public Request signIn(String username, String password, final SignInListener listener) {
        // Our session ID is no longer valid. Get rid of it.
        _sessionId = null;

        // Create the authorization header with base64-encoded username:password
        final Map<String, String> headers = getHeaders();
        String authString = username + ":" + password;
        String base64string = Base64.encodeToString(authString.getBytes(), Base64.NO_WRAP);
        headers.put("Authorization", "Basic " + base64string);

        // Build the URL for login
        String url = null;
        try {
            url = new URL(getBaseURL(), "/auth/login").toString();
        } catch (MalformedURLException e) {
            listener.signInComplete(null, e);
            return null;
        }

        // Create the request. We want to set and get the headers, so need to override
        // parseNetworkResponse and getHeaders in the request object.
        StringRequest req = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            // Listener overrides
            @Override
            public void onResponse(String response) {
                Log.d(LOG_TAG, "Login success: " + response);
                User user = new Gson().fromJson(response, User.class);
                Exception e = null;
                if ( _sessionId == null ) {
                    // No session ID returned in the headers!
                    e = new Exception("No session ID returned in headers");
                }
                listener.signInComplete(user, e);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(LOG_TAG, "Login failure: " + error);
                listener.signInComplete(null, error);
            }
        }) {
            // Request overrides
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                String sessionId = response.headers.get(HEADER_SESSION_ID);
                if ( sessionId != null ) {
                    Log.d(LOG_TAG, "Found session ID: " + sessionId);
                    _sessionId = sessionId;
                }
                return super.parseNetworkResponse(response);
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return headers;
            }
        };

        // Tag the request with our context so they all can be removed if the activity goes away
        req.setTag(_context);
        _requestQueue.add(req);
        return req;
    }

    protected URL getBaseURL() {
        return _baseURL;
    }

    /**
     * Returns a map with the HTTP headers. This will include the session ID if present.
     *
     * @return A map with the HTTP headers for a request.
     */
    protected Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        if ( _sessionId != null ) {
            headers.put(HEADER_SESSION_ID, _sessionId);
        }
        return headers;
    }
}
