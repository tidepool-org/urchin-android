package io.tidepool.urchin.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmResults;
import io.tidepool.urchin.R;
import io.tidepool.urchin.data.Profile;
import io.tidepool.urchin.data.User;

/**
 * Created by Brian King on 9/2/15.
 */
public class UserFilterAdapter extends ArrayAdapter<User> {
    private static final String LOG_TAG = "UserFilterAdapter";

    // Special user IDs used for populating the filter menu
    private static final String TAG_ALL_USERS = "AllUsers";
    private static final String TAG_SIGNOUT = "SignOut";

    private boolean _addExtras;

    /**
     * List adapter for showing the list of users.
     * @param context Context, we always need one
     * @param resource Resource ID of the elements in the list
     * @param objects List of objects we will be presenting
     * @param addExtras Set to true to add "All Users" and "Sign Out" to the list (for main view)
     */
    public UserFilterAdapter(Context context, int resource, List<User> objects, boolean addExtras) {
        super(context, resource, objects);
        _addExtras = addExtras;
    }

    /**
     * Helper method to create the list of valid users for both the main view and "add note" view
     * @return a list of User objects to be passed to this adapter's constructor
     */
    public static List<User> createUserList(Context context) {
        Realm realm = Realm.getInstance(context);

        // Unfortunately Realm does not support nested or joined queries, so we'll have to a bit
        // of manual labor here.

        // Get all of the profiles that do not have a null patient field. These are the only users
        // who can post a message.
        RealmResults<Profile> profiles = realm.where(Profile.class).isNotNull("patient").findAll();

        // Create a set of user IDs from these profiles
        Set<String> userIds = new HashSet<>();
        for ( Profile profile : profiles ) {
            userIds.add(profile.getUserId());
        }

        // Now get our list of users
        List<User> users = new ArrayList<>();
        for ( String userId : userIds ) {
            User user = realm.where(User.class).equalTo("userid", userId).findFirst();
            if ( user != null ) {
                users.add(user);
            } else {
                Log.e(LOG_TAG, "Failed to find user with ID " + userId);
            }
        }

        // Sort alphabetically by fullname, as that seems to be the only field guaranteed to be set
        Collections.sort(users, new Comparator<User>() {
            @Override
            public int compare(User lhs, User rhs) {
                return lhs.getProfile().getFullName().compareTo(rhs.getProfile().getFullName());
            }
        });

        realm.close();

        return users;
    }

    @Override
    public int getCount() {
        int additionals = _addExtras ? 2 : 0;
        return super.getCount() + additionals;
    }

    @Override
    public User getItem(int index) {
        if ( _addExtras ) {
            if (index == 0) {
                User user = new User();
                user.setUserid(TAG_ALL_USERS);
                user.setFullName(getContext().getString(R.string.all_notes));
                return user;
            }

            if (index == getCount() - 1) {
                User user = new User();
                user.setUserid(TAG_SIGNOUT);
                user.setFullName(getContext().getString(R.string.action_sign_out));
                return user;
            }

            return super.getItem(index - 1);
        } else {
            return super.getItem(index);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if ( v == null ) {
            v = LayoutInflater.from(getContext()).inflate(R.layout.list_item_user, parent, false);
        }

        User user = getItem(position);
        TextView nameText = (TextView)v.findViewById(R.id.name_textview);
        String name = user.getFullName();
        if (TextUtils.isEmpty(name)){
            name = user.getProfile().getFullName();
        }
        nameText.setText(name);

        View indent = v.findViewById(R.id.indent);
        if ( user.getUserid().equals(TAG_ALL_USERS) || user.getUserid().equals(TAG_SIGNOUT) ) {
            indent.setVisibility(View.GONE);
            nameText.setTypeface(null, Typeface.BOLD);
        } else {
            indent.setVisibility(View.VISIBLE);
            nameText.setTypeface(null, Typeface.NORMAL);
        }

        return v;
    }
}
