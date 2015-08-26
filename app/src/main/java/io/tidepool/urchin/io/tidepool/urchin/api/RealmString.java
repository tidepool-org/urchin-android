package io.tidepool.urchin.io.tidepool.urchin.api;

import io.realm.RealmObject;

/**
 * Created by Brian King on 8/26/15.
 */
public class RealmString extends RealmObject {
    private String val;

    public RealmString() {}
    public RealmString(String val) { this.val = val; }
    public String getVal() {
        return val;
    }

    public void setVal(String val) {
        this.val = val;
    }
}
