package io.tidepool.urchin.util;

import com.twitter.Extractor;

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

        Extractor extractor = new Extractor();
        List<String> extractedHashtags = extractor.extractHashtags(message);
        for (String hashtag : extractedHashtags) {
            tags.add(new Hashtag(hashtag));
        }

        return tags;
    }

    /**
     * Formats the hashtags in a SpannableString with the given color, and bold if specified.
     *
     * @param text  SpannableString with the text to format hashtags in
     * @param color Color to color the hashtags
     * @param bold  Set to true to make the hashtags bold
     */
    public static void formatHashtags(SpannableString text, int color, boolean bold) {
        List<Hashtag> tags = new ArrayList<>();

        Extractor extractor = new Extractor();
        List<Extractor.Entity> extractedHashtagWithIndices = extractor.extractHashtagsWithIndices(text.toString());
        for (Extractor.Entity entity : extractedHashtagWithIndices) {
            text.setSpan(new ForegroundColorSpan(color), entity.getStart(), entity.getEnd(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (bold) {
                text.setSpan(new StyleSpan(Typeface.BOLD), entity.getStart(), entity.getEnd(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }
}
