package io.tidepool.urchin.data;

import io.realm.RealmObject;

/**
 * Created by Brian King on 8/26/15.
 */
public class EmailAddress extends RealmObject {
    private String val;
    private String ownerId;

    public EmailAddress() {}
    public EmailAddress(String val) { this.val = val; }
    public String getVal() {
        return val;
    }
    public void setVal(String val) {
        this.val = val;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
}
