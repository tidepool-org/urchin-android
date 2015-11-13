package io.tidepool.urchin.util;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.widget.MultiAutoCompleteTextView;

/**
 * Created by Brian King on 10/28/15.
 */
public class StringTokenizer implements MultiAutoCompleteTextView.Tokenizer {

    @Override
    public int findTokenStart(CharSequence text, int cursor) {
        int i = cursor;
        while (i > 0 && (text.charAt(i - 1) != ' ' && text.charAt(i - 1) != '\n')) {
            i--;
        }
        while (i < cursor && (text.charAt(i) == ' ' || text.charAt(i) == '\n')) {
            i++;
        }
        return i;
    }

    @Override
    public int findTokenEnd(CharSequence text, int cursor) {
        int i = cursor;
        int len = text.length();

        while (i < len) {
            if (text.charAt(i) == ' ' || text.charAt(i) == '\n') {
                return i;
            } else {
                i++;
            }
        }
        return len;
    }

    @Override
    public CharSequence terminateToken(CharSequence text) {
        int i = text.length();

        while (i > 0 && (text.charAt(i - 1) == ' ' || text.charAt(i - 1) == '\n')) {
            i--;
        }
        if (i > 0 && (text.charAt(i - 1) == ' ' || text.charAt(i - 1) == '\n')) {
            return text;
        } else {
            if (text instanceof Spanned) {
                SpannableString sp = new SpannableString(text + " ");
                TextUtils.copySpansFrom((Spanned) text, 0, text.length(),
                        Object.class, sp, 0);
                return sp;
            } else {
                return text + " ";
            }
        }
    }
}
