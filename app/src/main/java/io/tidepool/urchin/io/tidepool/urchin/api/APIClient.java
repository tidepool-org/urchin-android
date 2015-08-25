package io.tidepool.urchin.io.tidepool.urchin.api;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Brian King on 8/25/15.
 */
public class APIClient {

    private static final String LOG_TAG = "APIClient";

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
        abstract void signInComplete(User user, Exception exception);
    }

    public Request signIn(String username, String password, final SignInListener listener) {
        // Our session ID is no longer valid. Get rid of it.
        _sessionId = null;
        Map<String, String> headers = getHeaders();
        String authString = username + ":" + password;
        String base64string = Base64.encodeToString(authString.getBytes(), 0);
        headers.put("Authorization", "Basic " + base64string);

        String url = null;
        try {
            url = new URL(_baseURL, "/auth/login").toString();
        } catch (MalformedURLException e) {
            listener.signInComplete(null, e);
            return null;
        }

        GsonRequest<User> req = new GsonRequest<>(url, User.class, headers, new Response.Listener<User>() {
            @Override
            public void onResponse(User response) {
                listener.signInComplete(response, null);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.signInComplete(null, error);
            }
        });

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
            headers.put("x-tidepool-session-id", _sessionId);
        }
        return headers;
    }

}
