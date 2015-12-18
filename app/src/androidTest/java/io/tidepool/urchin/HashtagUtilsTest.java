package io.tidepool.urchin;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.widget.TextView;

import android.app.Instrumentation;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.runner.AndroidJUnitRunner;
import android.test.InstrumentationTestCase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.runner.RunWith;
import org.junit.Test;

import static org.junit.Assert.*;

import io.tidepool.urchin.R;
import io.tidepool.urchin.util.HashtagUtils;

@RunWith(AndroidJUnit4.class)
public class HashtagUtilsTest {
    @Test
    public void testEmpty() {
        SpannableString text = new SpannableString("");
        HashtagUtils.formatHashtags(text, 0, true);
        Object[] spans = text.getSpans(0, text.length(), Object.class);

        assertThat(spans.length, is(0));
    }

    @Test
    public void testNoHashtags() {
        SpannableString text = new SpannableString("This is text that does not contain hashtags. No hashtags are present.");
        HashtagUtils.formatHashtags(text, 0, true);
        Object[] spans = text.getSpans(0, text.length(), Object.class);

        assertThat(spans.length, is(0));
    }

    // TODO: my - Need to fix HashtagUtils per comment below and then re-enable this test. Log a Trello bug first.
    @Test
    public void testWithHashtags() {
        SpannableString text = new SpannableString("This #is text #that? does #contain! #hashtags. #hashtags are present.");
        HashtagUtils.formatHashtags(text, 0, true);
        StyleSpan[] spans = text.getSpans(0, text.length(), StyleSpan.class);

        assertThat(spans.length, is(5));

        testStyleSpan(text, spans[0], 5, 8, Typeface.BOLD);
        testStyleSpan(text, spans[1], 14, 19, Typeface.BOLD); // TODO: my - failing, need to fix formatter to stop on puncutation, not just whitespace
        testStyleSpan(text, spans[1], 26, 34, Typeface.BOLD);
        testStyleSpan(text, spans[1], 36, 45, Typeface.BOLD);
        testStyleSpan(text, spans[1], 47, 56, Typeface.BOLD);
    }

    private void testStyleSpan(SpannableString spannableString, StyleSpan styleSpan, int start, int end, int typeface) {
        assertThat(styleSpan.getStyle(), is(typeface));
        assertThat(spannableString.getSpanStart(styleSpan), is(start));
        assertThat(spannableString.getSpanEnd(styleSpan), is(end));
    }
}
