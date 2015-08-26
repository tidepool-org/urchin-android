package io.tidepool.urchin.data;

import java.util.Date;

import io.realm.RealmObject;

/**
 * Created by Brian King on 8/26/15.
 */
public class Message extends RealmObject {
    private Date createdtime;
    private String groupid;
    private String guid;
    private String id;
    private String messagetext;
    private String parentmessage;
    private Date timestamp;
    private String userid;

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
}
