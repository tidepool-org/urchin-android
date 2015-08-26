package io.tidepool.urchin.io.tidepool.urchin.api;

import java.util.Date;

/**
 * Created by Brian King on 8/25/15.
 */
public class Profile {
    private String firstName;
    private String lastName;
    private String fullName;
    private String shortName;
    private Patient patient;

    public static class Patient {
        Date birthday;
        Date diagnosisDate;
        String aboutMe;
    }
}
