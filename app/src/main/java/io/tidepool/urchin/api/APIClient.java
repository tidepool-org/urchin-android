package io.tidepool.urchin.api;

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
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.tidepool.urchin.data.CurrentUser;
import io.tidepool.urchin.data.Hashtag;
import io.tidepool.urchin.data.Note;
import io.tidepool.urchin.data.Patient;
import io.tidepool.urchin.data.Profile;
import io.tidepool.urchin.data.EmailAddress;
import io.tidepool.urchin.data.Session;
import io.tidepool.urchin.data.SharedUserId;
import io.tidepool.urchin.data.User;
import io.tidepool.urchin.util.HashtagUtils;
import io.tidepool.urchin.util.MiscUtils;

/**
 * Created by Brian King on 8/25/15.
 */
public class APIClient {

    public static final String PRODUCTION = "Production";
    public static final String DEVELOPMENT = "Development";
    public static final String STAGING = "Staging";

    private static final String LOG_TAG = "APIClient";

    // Date format for most things,
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSZ";

    // Date format for messages
    public static final String MESSAGE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

    // Header label for the session token
    private static final String HEADER_SESSION_ID = "x-tidepool-session-token";

    // Key into the shared preferences database for our own preferences
    private static final String PREFS_KEY = "APIClient";

    // Map of server names to base URLs
    private static final Map<String, URL> __servers;

    // RequestQueue our requests will be made on
    private RequestQueue _requestQueue;

    // Base URL for network requests
    private URL _baseURL;

    // Context used to create us
    private Context _context;

    // Static initialization
    static {
        __servers = new HashMap<>();
        try {
            __servers.put(PRODUCTION, new URL("https://api.tidepool.org"));
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
        setServer(server);

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
     * Returns the current user. Only valid if authenticated.
     * @return the current user
     */
    public User getUser() {
        Realm realm = Realm.getInstance(_context);
        RealmResults<Session> results = realm.where(Session.class)
                .findAll();
        if ( results.size() == 0 ) {
            realm.close();
            return null;
        }

        User user = results.first().getUser();
        realm.close();
        return user;
    }

    /**
     * Returns the session ID used for this client.
     * @return the session ID, or null if not authenticated
     */
    public String getSessionId() {
        Realm realm = Realm.getInstance(_context);
        RealmResults<Session> results = realm.where(Session.class)
                .findAll();
        if ( results.size() == 0 ) {
            return null;
        }

        Session session = results.first();
        String sessionId = session.getSessionId();
        realm.close();
        return sessionId;
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
        // Clear out the database, just in case there is anything left over
        Realm realm = Realm.getInstance(_context);
        realm.beginTransaction();
        realm.where(Session.class).findAll().clear();
        realm.commitTransaction();
        realm.close();

        // Create the authorization header with base64-encoded username:password
        final Map<String, String> headers = getHeaders();
        String authString = username + ":" + password;
        String base64string = Base64.encodeToString(authString.getBytes(), Base64.NO_WRAP);
        headers.put("Authorization", "Basic " + base64string);

        // Build the URL
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
                Realm realm = Realm.getInstance(_context);
                Log.d(LOG_TAG, "Login success: " + response);
                RealmResults<Session> sessions = realm.where(Session.class).findAll();
                if ( sessions.size() == 0 ) {
                    // No session ID found
                    listener.signInComplete(null, new Exception("No session ID returned in headers"));
                    return;
                }

                Session s = sessions.first();

                Gson gson = getGson(DEFAULT_DATE_FORMAT);
                User user = gson.fromJson(response, User.class);
                realm.beginTransaction();
                User copiedUser = realm.copyToRealmOrUpdate(user);
                s.setUser(copiedUser);
                realm.commitTransaction();
                realm.close();
                listener.signInComplete(user, null);
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
                    Realm realm = Realm.getInstance(_context);

                    realm.beginTransaction();

                    // Get rid of any old sessions
                    realm.where(Session.class).findAll().clear();

                    // Create the session in the database
                    Session s = realm.createObject(Session.class);
                    s.setSessionId(sessionId);
                    Log.d(LOG_TAG, "Session ID: " + sessionId);
                    s.setKey(Session.SESSION_KEY);

                    realm.commitTransaction();
                    realm.close();
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


    public abstract static class RefreshTokenListener {
        public abstract void tokenRefreshed(Exception error);
    }
    public Request refreshToken(final RefreshTokenListener listener) {
        Log.d(LOG_TAG, "refreshToken");

        String sessionId = getSessionId();
        if ( sessionId == null ) {
            listener.tokenRefreshed(new Exception("No token to refresh"));
            return null;
        }

        // Build the URL
        String url = null;
        try {
            url = new URL(getBaseURL(), "/auth/login").toString();
        } catch (MalformedURLException e) {
            listener.tokenRefreshed(e);
            return null;
        }

        StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                listener.tokenRefreshed(null);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.tokenRefreshed(error);
            }
        }) {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                String sessionId = response.headers.get(HEADER_SESSION_ID);
                if ( sessionId != null ) {
                    Realm realm = Realm.getInstance(_context);

                    realm.beginTransaction();

                    // Get the current session
                    Session s = realm.where(Session.class).findAll().first();

                    // Update the session ID
                    s.setSessionId(sessionId);

                    Log.d(LOG_TAG, "Session ID refreshed: " + sessionId);

                    realm.commitTransaction();
                    realm.close();
                }
                return super.parseNetworkResponse(response);
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return APIClient.this.getHeaders();
            }
        };

        _requestQueue.add(request);
        return request;
    }

    public abstract static class SignOutListener {
        public abstract void signedOut(int responseCode, Exception error);
    }
    public Request signOut(final SignOutListener listener) {
        // Get the headers before we get rid of the session, or we won't have a session ID!
        final Map<String, String> headers = getHeaders();

        clearDatabase();

        String url;
        try {
            url = new URL(getBaseURL(), "/auth/login").toString();
        } catch (MalformedURLException e) {
            listener.signedOut(0, e);
            return null;
        }

        Request<Integer> req = new Request<Integer>(Request.Method.POST, url, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.signedOut(0, error);
            }
        }) {

            @Override
            protected Response<Integer> parseNetworkResponse(NetworkResponse response) {
                return Response.success(response.statusCode, HttpHeaderParser.parseCacheHeaders(response));
            }

            @Override
            protected void deliverResponse(Integer response) {
                listener.signedOut(response, null);
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return headers;
            }
        };

        _requestQueue.add(req);
        return req;
    }

    public static abstract class PostNoteListener {
        public abstract void notePosted(Note note, Exception error);
    }
    public Request postNote(final Note note, final PostNoteListener listener) {
        // Build the URL
        String url = null;
        try {
            url = new URL(getBaseURL(), "/message/send/" + note.getGroupid()).toString();
        } catch (MalformedURLException e) {
            listener.notePosted(null, e);
            return null;
        }

        final String noteJson = getGson(MESSAGE_DATE_FORMAT).toJson(note);

        StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                // Add the message to the database
                Log.d(LOG_TAG, "Post note response data: " + response);

                // The repsonse only contains the ID.
                String noteId = null;
                try {
                    JSONObject noteIdobject = new JSONObject(response);
                    noteId = noteIdobject.getString("id");
                    note.setId(noteId);
                } catch (JSONException e) {
                    e.printStackTrace();
                    listener.notePosted(null, e);
                    return;
                }

                Realm realm = Realm.getInstance(_context);
                realm.beginTransaction();
                Note sentNote = realm.copyToRealmOrUpdate(note);

                // Update the hashtags for this note.
                List<Hashtag> hashtags = HashtagUtils.parseHashtags(sentNote.getMessagetext());
                for ( Hashtag hash : hashtags ) {
                    hash.setOwnerId(sentNote.getUserid());
                    sentNote.getHashtags().add(hash);
                }
                realm.commitTransaction();
                realm.close();
                listener.notePosted(sentNote, null);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "Failed to post message: " + error);
                listener.notePosted(null, error);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return APIClient.this.getHeaders();
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                String bodyText = "{\"message\":" + noteJson + "}";
                Log.d(LOG_TAG, "Message post text: " + bodyText);
                return bodyText.getBytes();
            }

            @Override
            public String getBodyContentType() {
                return "application/json";
            }
        };

        _requestQueue.add(request);
        return request;
    }

    public static abstract class UpdateNoteListener {
        public abstract void noteUpdated(Note note, Exception error);
    }
    public Request updateNote(final Note note, final UpdateNoteListener listener) {
        // Build the URL
        String url = null;
        try {
            url = new URL(getBaseURL(), "/message/edit/" + note.getId()).toString();
        } catch (MalformedURLException e) {
            listener.noteUpdated(null, e);
            return null;
        }

        JSONObject messageObject;
        try {
            messageObject = new JSONObject();
            messageObject.put("messagetext", note.getMessagetext());
            messageObject.put("timestamp", MiscUtils.dateToJSONString(note.getTimestamp()));
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not create edit message JSON: " + e.toString());
            listener.noteUpdated(null, e);
            return null;
        }

        final String noteJson = messageObject.toString();

        StringRequest request = new StringRequest(Request.Method.PUT, url, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                // Update was successful.
                listener.noteUpdated(note, null);
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.noteUpdated(null, error);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return APIClient.this.getHeaders();
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                String bodyText = "{\"message\":" + noteJson + "}";
                return bodyText.getBytes();
            }

            @Override
            public String getBodyContentType() {
                return "application/json";
            }
        };

        _requestQueue.add(request);
        return request;
    }

    public static abstract class DeleteNoteListener {
        public abstract void noteDeleted(Exception error);
    }
    public Request deleteNote(Note note, final DeleteNoteListener listener) {
        String url = null;
        try {
            url = new URL(getBaseURL(), "/message/remove/" + note.getId()).toString();
        } catch (MalformedURLException e) {
            listener.noteDeleted(e);
            return null;
        }

        final String noteId = note.getId();

        StringRequest request = new StringRequest(Request.Method.DELETE, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                // All is well. Delete the note from our database.
                Realm realm = Realm.getInstance(_context);
                realm.beginTransaction();
                realm.where(Note.class).equalTo("id", noteId).findAll().clear();
                realm.commitTransaction();
                realm.close();
                listener.noteDeleted(null);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "Error deleting note: " + error);
                listener.noteDeleted(error);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return APIClient.this.getHeaders();
            }
        };

        _requestQueue.add(request);
        return request;
    }

    public void clearDatabase() {
        // Clean  out the database
        Realm realm = Realm.getInstance(_context);
        realm.beginTransaction();
        realm.where(CurrentUser.class).findAll().clear();
        realm.where(EmailAddress.class).findAll().clear();
        realm.where(Hashtag.class).findAll().clear();
        realm.where(Note.class).findAll().clear();
        realm.where(Patient.class).findAll().clear();
        realm.where(Profile.class).findAll().clear();
        realm.where(Session.class).findAll().clear();
        realm.where(SharedUserId.class).findAll().clear();
        realm.where(User.class).findAll().clear();
        realm.commitTransaction();
        realm.close();
    }

    /**
     * Returns a GSON instance used for working with Realm and GSON together, and a specific
     * date format for date fields.
     *
     * @param dateFormat Date format string to use when parsing dates
     * @return a GSON instance for use with Realm and the specified date format
     */
    public static Gson getGson(String dateFormat) {
        // Make a custom Gson instance, with a custom TypeAdapter for each wrapper object.
        // In this instance we only have RealmList<RealmString> as a a wrapper for RealmList<String>
        Type emailToken = new TypeToken<RealmList<EmailAddress>>(){}.getType();
        Type sharedIdToken = new TypeToken<RealmList<SharedUserId>>(){}.getType();
        Gson gson = new GsonBuilder()
                .setExclusionStrategies(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        return f.getDeclaringClass().equals(RealmObject.class);
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                })
                .registerTypeAdapter(emailToken, new TypeAdapter<RealmList<EmailAddress>>() {

                    @Override
                    public void write(JsonWriter out, RealmList<EmailAddress> value) throws IOException {
                        // Ignore
                    }

                    @Override
                    public RealmList<EmailAddress> read(JsonReader in) throws IOException {
                        RealmList<EmailAddress> list = new RealmList<EmailAddress>();
                        in.beginArray();
                        while (in.hasNext()) {
                            list.add(new EmailAddress(in.nextString()));
                        }
                        in.endArray();
                        return list;
                    }
                })
                .registerTypeAdapter(sharedIdToken, new TypeAdapter<RealmList<SharedUserId>>() {

                    @Override
                    public void write(JsonWriter out, RealmList<SharedUserId> value) throws IOException {
                        // Ignore
                    }

                    @Override
                    public RealmList<SharedUserId> read(JsonReader in) throws IOException {
                        RealmList<SharedUserId> list = new RealmList<SharedUserId>();
                        in.beginArray();
                        while (in.hasNext()) {
                            list.add(new SharedUserId(in.nextString()));
                        }
                        in.endArray();
                        return list;
                    }
                })
                .setDateFormat(dateFormat)
                .create();

        return gson;
    }

    public static abstract class ViewableUserIdsListener {
        public abstract void fetchComplete(RealmList<SharedUserId> userIds, Exception error);
    }
    public Request getViewableUserIds(final ViewableUserIdsListener listener) {
        // Build the URL
        String url = null;
        try {
            url = new URL(getBaseURL(), "/access/groups/" + getUser().getUserid()).toString();
        } catch (MalformedURLException e) {
            listener.fetchComplete(null, e);
            return null;
        }

        StringRequest req = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Realm realm = Realm.getInstance(_context);
                JSONObject jsonObject = null;
                try {
                    Log.d(LOG_TAG, "Groups: " + response);
                    jsonObject = new JSONObject(response);
                } catch (JSONException e) {
                    listener.fetchComplete(null, e);
                    return;
                }

                RealmList<SharedUserId> userIds = new RealmList<>();
                Iterator iter = jsonObject.keys();

                User user = getUser();

                realm.beginTransaction();

                // Out with the old
                realm.where(SharedUserId.class).findAll().clear();

                while ( iter.hasNext() ) {
                    String viewableId = (String)iter.next();
                    userIds.add(new SharedUserId(viewableId));
                }

                // Put the IDs into the database
                user.getViewableUserIds().removeAll(user.getViewableUserIds());
                user.getViewableUserIds().addAll(userIds);

                realm.commitTransaction();

                listener.fetchComplete(userIds, null);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.fetchComplete(null, error);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return APIClient.this.getHeaders();
            }
        };

        _requestQueue.add(req);
        return req;
    }

    public static abstract class ProfileListener {
        public abstract void profileReceived(Profile profile, Exception error);
    }

    public Request getProfileForUserId(final String userId, final ProfileListener listener) {
        // Build the URL
        String url = null;
        try {
            url = new URL(getBaseURL(), "/metadata/" + userId + "/profile").toString();
        } catch (MalformedURLException e) {
            if ( listener != null ) {
                listener.profileReceived(null, e);
            }
            return null;
        }

        StringRequest req = new StringRequest(url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Gson gson = getGson(DEFAULT_DATE_FORMAT);
                Profile fakeProfile = gson.fromJson(response, Profile.class);
                fakeProfile.setUserId(userId);

                Log.d(LOG_TAG, "Profile response: " + response);
                Realm realm = Realm.getInstance(_context);
                realm.beginTransaction();
                Profile profile = realm.copyToRealmOrUpdate(fakeProfile);
                // Create a user with this profile and add / update it
                User user = realm.where(User.class).equalTo("userid", userId).findFirst();
                if ( user == null ) {
                    user = realm.createObject(User.class);
                    user.setUserid(userId);
                }
                user.setProfile(profile);
                realm.commitTransaction();
                if ( listener != null ) {
                    listener.profileReceived(profile, null);
                }
                realm.close();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "Profile error: " + error);
                if ( listener != null ) {
                    listener.profileReceived(null, error);
                }
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return APIClient.this.getHeaders();
            }
        };

        _requestQueue.add(req);
        return req;
    }

    public static abstract class NotesListener {
        public abstract void notesReceived(RealmList<Note> notes, Exception error);
    }
    public Request getNotes(final String userId, final Date fromDate, final Date toDate, final NotesListener listener) {
        String url = null;
        try {
            DateFormat df = new SimpleDateFormat(DEFAULT_DATE_FORMAT, Locale.US);
            String extension = "/message/notes/" + userId + "?starttime=" +
                    URLEncoder.encode(df.format(fromDate), "utf-8") +
                    "&endtime=" +
                    URLEncoder.encode(df.format(toDate), "utf-8");

            url = new URL(getBaseURL(), extension).toString();
        } catch (MalformedURLException e) {
            listener.notesReceived(null, e);
            return null;
        } catch (UnsupportedEncodingException e) {
            listener.notesReceived(null, e);
            return null;
        }

        StringRequest req = new StringRequest(url, new Response.Listener<String>() {
            @Override
            public void onResponse(String json) {
                // Returned JSON is an object array called "messages"
                Log.d(LOG_TAG, "Messages response:" + json);

                Realm realm = Realm.getInstance(_context);
                RealmList<Note> noteList = new RealmList<>();
                realm.beginTransaction();

                // Get rid of all of the hashtags for this user. We'll add them in as we go
                // through the messages
                realm.where(Hashtag.class)
                        .equalTo("ownerId", userId)
                        .findAll().clear();

                // Also get rid of the messages for this user in the specified date range, in case some were deleted.
                realm.where(Note.class)
                        .equalTo("groupid", userId)
                        .greaterThan("timestamp", fromDate)
                        .lessThanOrEqualTo("timestamp", toDate)
                        .findAll().clear();

                // Odd date format in the messages
                Gson gson = getGson(MESSAGE_DATE_FORMAT);
                try {
                    JSONObject obj = new JSONObject(json);
                    JSONArray messages = obj.getJSONArray("messages");

                    for ( int i = 0; i < messages.length(); i++ ) {
                        String msgJson = messages.getString(i);

                        Note note = gson.fromJson(msgJson, Note.class);

                        // Get the fullName field from the "user" property and set it
                        JSONObject jsonObject = new JSONObject(msgJson);
                        JSONObject userObject = jsonObject.getJSONObject("user");
                        note.setAuthorFullName(userObject.getString("fullName"));

                        note = realm.copyToRealmOrUpdate(note);

                        // Update the hashtags for this note.
                        note.getHashtags().clear();
                        List<Hashtag> hashtags = HashtagUtils.parseHashtags(note.getMessagetext());
                        for ( Hashtag hash : hashtags ) {
                            hash.setOwnerId(userId);
                            note.getHashtags().add(hash);
                        }

                        // See if we're missing any users that are mentioned in the note
                        // Check the note author (userid)
                        RealmResults userSearch = realm.where(User.class).equalTo("userid", note.getUserid()).findAll();
                        if ( userSearch.size() == 0 ) {
                            Log.d(LOG_TAG, "Getting profile for user: " + note.getUserid());
                            getProfileForUserId(note.getUserid(), null);
                        }

                        // Also check the group (groupid)
                        userSearch = realm.where(User.class).equalTo("userid", note.getGroupid()).findAll();
                        if ( userSearch.size() == 0 ) {
                            Log.d(LOG_TAG, "Getting profile for group: " + note.getGroupid());
                            getProfileForUserId(note.getGroupid(), null);
                        }
                        noteList.add(note);
                    }
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error parsing notes: " + e);
                    realm.cancelTransaction();
                    listener.notesReceived(null, e);
                    realm.close();
                    return;
                } catch (com.google.gson.JsonSyntaxException e) {
                    Log.e(LOG_TAG, "Error parsing notes: " + e);
                    realm.cancelTransaction();
                    listener.notesReceived(null, e);
                    realm.close();
                }

                realm.commitTransaction();
                listener.notesReceived(noteList, null);
                realm.close();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.notesReceived(null, error);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return APIClient.this.getHeaders();
            }
        };

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
        String sessionId = getSessionId();
        if ( sessionId != null ) {
            headers.put(HEADER_SESSION_ID, sessionId);
        }

        Log.d(LOG_TAG, "Headers: " + headers);
        return headers;
    }
}
