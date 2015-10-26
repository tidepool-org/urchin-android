package io.tidepool.urchin.data;

import io.realm.RealmObject;
import io.realm.annotations.Index;

/**
 * Created by Brian King on 10/26/15.
 */
public class StarredTag extends RealmObject {
    @Index
    private String tagName;

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }
}
