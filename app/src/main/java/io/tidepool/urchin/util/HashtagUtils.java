package io.tidepool.urchin.util;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import io.tidepool.urchin.R;
import io.tidepool.urchin.data.Hashtag;
import io.tidepool.urchin.data.StarredTag;

/**
 * Created by Brian King on 8/31/15.
 */
public class HashtagUtils {
    /**
     * Given a string, returns a list of Hashtags found in the string. The objects have not been
     * inserted into Realm.
     *
     * @param message Message containing the hashtags
     * @return a list of Hashtag objects ready to be inserted into Realm
     */
    public static List<Hashtag> parseHashtags(String message) {
        List<Hashtag> tags = new ArrayList<>();
        String[] words = message.split("\\s+");
        for ( String word : words ) {
            if ( word.startsWith("#") ) {
                tags.add(new Hashtag(word));
            }
        }
        return tags;
    }

    /**
     * Formats the hashtags in a SpannableString with the given color, and bold if specified.
     * @param text SpannableString with the text to format hashtags in
     * @param color Color to color the hashtags
     * @param bold Set to true to make the hashtags bold
     */
    public static void formatHashtags(SpannableString text, int color, boolean bold) {
        int startSpan = -1;
        for ( int i = 0; i < text.length(); i++ ) {
            Character c = text.charAt(i);
            if ( startSpan == -1 ) {
                // We're looking for a hashtag
                if ( c.equals('#') ) {
                    startSpan = i;
                }
            } else {
                // We're looking for whitespace
                if ( Character.isWhitespace(c) ) {
                    // Found it. Add the span.
                    text.setSpan(new ForegroundColorSpan(color), startSpan, i, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    if ( bold ) {
                        text.setSpan(new StyleSpan(Typeface.BOLD), startSpan, i, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    startSpan = -1;
                }
            }
        }
        if ( startSpan != -1 ) {
            // Hashtag was last
            text.setSpan(new ForegroundColorSpan(color), startSpan, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if ( bold ) {
                text.setSpan(new StyleSpan(Typeface.BOLD), startSpan, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    /**
     * Returns true if the hashtag with the given name is starred by the user
     * @param context Context to get the realm database from
     * @param hashtagName Name of the hashtag to check
     * @return the StarredTag object if the tag is starred, null otherwise
     */
    @Nullable
    public static StarredTag getStarredTag(Context context, String hashtagName) {
        Realm realm = Realm.getInstance(context);
        RealmResults<StarredTag> results = realm.where(StarredTag.class).equalTo("tagName", hashtagName).findAll();
        StarredTag result = results.size() == 0 ? null : results.first();
        realm.close();
        return result;
    }

    public static boolean isHashtagStarred(Context context, String hashtagName) {
        return (getStarredTag(context, hashtagName) != null);
    }

    /**
     * Sets or clears a "star" on a hashtag name.
     * @param context Context to get the realm database from
     * @param hashtagName Name of the hashtag to star or un-star
     * @param starred True to star, false to un-star
     */
    public static void setHashtagStar(Context context, String hashtagName, boolean starred) {
        Realm realm = Realm.getInstance(context);
        if ( starred ) {
            // We are setting the star.
            realm.beginTransaction();
            StarredTag tag = realm.createObject(StarredTag.class);
            tag.setTagName(hashtagName);
            tag.setTimestamp(new Date());
            realm.commitTransaction();
        } else {
            // We are removing the star
            realm.beginTransaction();
            realm.where(StarredTag.class).equalTo("tagName", hashtagName).findAll().clear();
            realm.commitTransaction();
        }
        realm.close();
    }
}
