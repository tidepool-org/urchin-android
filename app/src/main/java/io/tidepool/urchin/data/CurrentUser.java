package io.tidepool.urchin.data;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by Brian King on 9/2/15.
 */
public class CurrentUser extends RealmObject {
    private User currentUser;

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }
}
