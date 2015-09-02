package io.tidepool.urchin.data;

import io.realm.RealmObject;
import io.realm.annotations.Index;

/**
 * Created by Brian King on 8/31/15.
 */
public class Hashtag extends RealmObject {
    @Index
    private String ownerId;

    @Index
    private String tag;

    public Hashtag() {
        super();
    }

    public Hashtag(String tag) {
        this.tag = tag;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
