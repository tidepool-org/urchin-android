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
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Brian King on 8/25/15.
 */
public class APIClient {

    private static final String LOG_TAG = "APIClient";

    private static final String HEADER_SESSION_ID = "x-tidepool-session-token";

    // Map of server names to base URLs
    private static final Map<String, URL> __servers;
    static {
        __servers = new HashMap<>();
        try {
            __servers.put("Production", new URL("https://api.tidepool.io"));
            __servers.put("Development", new URL("https://devel-api.tidepool.io"));
            __servers.put("Staging", new URL("https://staging-api.tidepool.io"));

        } catch (MalformedURLException e) {
            // Should never happen
        }
    }

    // RequestQueue our requests will be made on
    private RequestQueue _requestQueue;

    // Base URL for network requests
    private URL _baseURL;

    // Context used to create us
    private Context _context;

    // Session ID, set after login
    private String _sessionId;


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

    public static abstract class SignInListener {
        public abstract void signInComplete(User user, Exception exception);
    }

    public Request signIn(String username, String password, final SignInListener listener) {
        // Our session ID is no longer valid. Get rid of it.
        _sessionId = null;

        final Map<String, String> headers = getHeaders();
        String authString = username + ":" + password;
        String base64string = Base64.encodeToString(authString.getBytes(), Base64.NO_WRAP);
        headers.put("Authorization", "Basic " + base64string);

        String url = null;
        try {
            url = new URL(_baseURL, "/auth/login").toString();
        } catch (MalformedURLException e) {
            listener.signInComplete(null, e);
            return null;
        }

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
