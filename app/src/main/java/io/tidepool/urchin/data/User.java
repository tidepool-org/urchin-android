package io.tidepool.urchin.data;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by Brian King on 8/25/15.
 */
public class User extends RealmObject {
    @PrimaryKey
    private String userid;
    private String username;
    private RealmList<EmailAddress> emails;
    private String fullName;
    private Profile profile;

    private RealmList<SharedUserId> viewableUserIds;

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public RealmList<EmailAddress> getEmails() {
        return emails;
    }

    public void setEmails(RealmList<EmailAddress> emails) {
        this.emails = emails;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public RealmList<SharedUserId>getViewableUserIds() {
        return viewableUserIds;
    }
    public void setViewableUserIds(RealmList<SharedUserId> ids) {
        viewableUserIds = ids;
    }
    public Profile getProfile() { return profile;}
    public void setProfile(Profile profile) { this.profile = profile; }
}
