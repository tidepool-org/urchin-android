package io.tidepool.urchin.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import io.tidepool.urchin.R;
import io.tidepool.urchin.data.User;

/**
 * Created by Brian King on 9/2/15.
 */
public class UserFilterAdapter extends ArrayAdapter<User> {
    // Special user IDs used for populating the filter menu
    private static final String TAG_ALL_USERS = "AllUsers";
    private static final String TAG_SIGNOUT = "SignOut";

    public UserFilterAdapter(Context context, int resource, List<User> objects) {
        super(context, resource, objects);
    }

    @Override
    public int getCount() {
        // We have 2 additional rows to show, "All Users" and "Sign Out".
        return super.getCount() + 2;
    }

    @Override
    public User getItem(int index) {
        if ( index == 0 ) {
            User user = new User();
            user.setUserid(TAG_ALL_USERS);
            user.setFullName(getContext().getString(R.string.all_notes));
            return user;
        }

        if ( index == getCount() - 1 ) {
            User user = new User();
            user.setUserid(TAG_SIGNOUT);
            user.setFullName(getContext().getString(R.string.action_sign_out));
            return user;
        }

        return super.getItem(index - 1);
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
