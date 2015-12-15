package io.tidepool.urchin.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.tidepool.urchin.MainActivity;
import io.tidepool.urchin.api.APIClient;
import io.tidepool.urchin.data.Profile;
import io.tidepool.urchin.data.User;

/**
 * Created by Brian King on 9/3/15.
 */
public class MiscUtils {
    public static String getPrintableNameForUser(User user) {
        String name = null;
        if (user != null) {
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
            if (TextUtils.isEmpty(name)) {
                name = "[" + user.getUserid() + "]";
            }
        }

        if (TextUtils.isEmpty(name)) {
            name = "UNKNOWN";
        }

        return name;
    }

    public static String getAppInfoString(Context context) {
        PackageInfo info = null;
        String ver = "UNKNOWN";
        try {
            info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            ver = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return "v" + ver + " on " + MainActivity.getInstance().getSelectedServer();
    }

    static DateFormat _jsonDateFormat = new SimpleDateFormat(APIClient.MESSAGE_DATE_FORMAT, Locale.US);

    public static String dateToJSONString(Date date) {
        return _jsonDateFormat.format(date);
    }
}
