package io.tidepool.urchin.io.tidepool.urchin.api;

import io.realm.RealmObject;

/**
 * Created by Brian King on 8/26/15.
 */
public class Patient extends RealmObject {
    private String birthday;
    private String diagnosisDate;
    private String aboutMe;

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getDiagnosisDate() {
        return diagnosisDate;
    }

    public void setDiagnosisDate(String diagnosisDate) {
        this.diagnosisDate = diagnosisDate;
    }

    public String getAboutMe() {
        return aboutMe;
    }

    public void setAboutMe(String aboutMe) {
        this.aboutMe = aboutMe;
    }
}
