package io.tidepool.urchin.io.tidepool.urchin.api;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by Brian King on 8/26/15.
 */
public class Session extends RealmObject {
    // This object is a singleton, always use the same key
    public static final String SESSION_KEY = "SessionKey";

    @PrimaryKey
    private String key;
    private String sessionId;
    private User user;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
