package io.tidepool.urchin.io.tidepool.urchin.api;

/**
 * Created by Brian King on 8/25/15.
 */
public class User {
    private String userid;
    private String username;
    private String[] emails;
    private String fullName;

    public String getUserId() {
        return userid;
    }

    public String getUserName() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String[] getEmails() {
        return emails;
    }
}
