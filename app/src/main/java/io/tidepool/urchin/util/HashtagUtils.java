package io.tidepool.urchin.util;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import java.util.ArrayList;
import java.util.List;

import io.tidepool.urchin.R;
import io.tidepool.urchin.data.Hashtag;

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
}
