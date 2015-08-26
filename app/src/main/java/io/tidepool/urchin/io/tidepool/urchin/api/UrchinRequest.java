package io.tidepool.urchin.io.tidepool.urchin.api;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmObject;

/**
 * Created by Brian King on 8/26/15.
 */
public class UrchinRequest<T extends RealmObject> extends Request<T> {
    private final Realm realm;
    private final Class<T> clazz;
    private final Map<String, String> headers;
    private final Response.Listener<T> listener;

    public UrchinRequest(Realm realm, int method, String url, Class<T> clazz, Map<String, String> headers,
                         Response.Listener<T> listener, Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.realm = realm;
        this.clazz = clazz;
        this.headers = headers;
        this.listener = listener;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return headers != null ? headers : super.getHeaders();
    }

    @Override
    protected void deliverResponse(T response) {
        listener.onResponse(response);
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        try {
            String json = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers));
            realm.beginTransaction();
            Response<T> r = Response.success(realm.createOrUpdateObjectFromJson(this.clazz, json),
                    HttpHeaderParser.parseCacheHeaders(response));
            realm.commitTransaction();
            return r;
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        }
    }
}
