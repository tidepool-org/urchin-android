package io.tidepool.urchin.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by Brian King on 8/26/15.
 */
public class Note extends RealmObject {
    private Date createdtime;
    private String groupid;
    private String guid;
    @PrimaryKey private String id;
    private String messagetext;
    private String parentmessage;
    private Date timestamp;
    private String userid;

    // This is manually parsed and set from the User field in the message. We don't want
    // a real user object- just the display name.
    private String authorFullName;

    // Hashtags we parse when we get the Note from the server
    private RealmList<Hashtag> hashtags;

    public Date getCreatedtime() {
        return createdtime;
    }

    public void setCreatedtime(Date createdtime) {
        this.createdtime = createdtime;
    }

    public String getGroupid() {
        return groupid;
    }

    public void setGroupid(String groupid) {
        this.groupid = groupid;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessagetext() {
        return messagetext;
    }

    public void setMessagetext(String messagetext) {
        this.messagetext = messagetext;
    }

    public String getParentmessage() {
        return parentmessage;
    }

    public void setParentmessage(String parentmessage) {
        this.parentmessage = parentmessage;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public RealmList<Hashtag> getHashtags() {
        return hashtags;
    }

    public void setHashtags(RealmList<Hashtag> hashtags) {
        this.hashtags = hashtags;
    }

    public String getAuthorFullName() {
        return authorFullName;
    }

    public void setAuthorFullName(String authorFullName) {
        this.authorFullName = authorFullName;
    }

}
