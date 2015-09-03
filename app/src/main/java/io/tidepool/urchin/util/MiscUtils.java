package io.tidepool.urchin.util;

import android.text.TextUtils;

import io.tidepool.urchin.data.Profile;
import io.tidepool.urchin.data.User;

/**
 * Created by Brian King on 9/3/15.
 */
public class MiscUtils {
    public static String getPrintableNameForUser(User user) {
        String name = null;
        if ( user != null ) {
            Profile profile = user.getProfile();
            if (profile != null) {
                name = profile.getFullName();
                if (!TextUtils.isEmpty(name)) {
                    return name;
                }
                name = profile.getFirstName() + " " + profile.getLastName();
            } else {
                name = user.getFullName();
                if (TextUtils.isEmpty(name)) {
                    name = user.getUsername();
                }
            }
            if ( TextUtils.isEmpty(name) ) {
                name = "[" + user.getUserid() + "]";
            }
        }

        if ( TextUtils.isEmpty(name) ) {
            name = "UNKNOWN";
        }

        return name;
    }

}
