package io.tidepool.urchin.data;

import io.realm.RealmObject;

/**
 * Created by Brian King on 8/26/15.
 */
public class SharedUserId extends RealmObject {
    private String val;

    public SharedUserId() {
    }

    public SharedUserId(String val) {
        this.val = val;
    }

    public String getVal() {
        return val;
    }

    public void setVal(String val) {
        this.val = val;
    }
}
