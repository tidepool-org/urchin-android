package io.tidepool.urchin;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmResults;
import io.tidepool.urchin.data.Hashtag;
import io.tidepool.urchin.ui.HashtagAdapter;
import io.tidepool.urchin.util.HashtagUtils;

public class NewNoteActivity extends AppCompatActivity {
    private static final String LOG_TAG = "NewNote";

    private static final int MAX_TAGS = 50;         // Most tags we will show in the scrolling list
    private static final int FORMAT_TIMEOUT = 1000; // Delay we wait to see if the user has stopped typing

    private EditText _noteEditText;
    private TextView _dateTimeTextView;
    private Button _postButton;
    private RecyclerView _hashtagView;

    private Date _noteTime;
    private Handler _formatTextHandler;

    private boolean _updatingText;                  // So we don't update text recursively

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_note);

        _noteEditText = (EditText)findViewById(R.id.note_edit_text);
        _dateTimeTextView = (TextView)findViewById(R.id.date_time);
        _postButton = (Button)findViewById(R.id.post_button);
        _hashtagView = (RecyclerView)findViewById(R.id.hashtag_recyclerview);
        _formatTextHandler = new Handler();

        // Show a context menu for the date / time bar
        View dateTimeLayout = findViewById(R.id.date_time_layout);
        registerForContextMenu(dateTimeLayout);

        // Make it work on a tap instead of just a long press
        dateTimeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performLongClick();
            }
        });

        // Respond to the button
        findViewById(R.id.post_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postClicked();
            }
        });

        // Set the time to now
        _noteTime = new Date();
        setDateTimeText(_noteTime);

        // Populate the hashtags
        setupHashtags();

        // Make the hashtags look good in the note
        // TODO: This seems kind of clunky having the delay. Without the delay it's almost unusable,
        // though, as formatting the tags seems to take a long time. There's probably a more efficient
        // way to make this work...
        _noteEditText.addTextChangedListener(new TextWatcher() {
            boolean _isChanging;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                final String text = _noteEditText.getText().toString();
                final int start = _noteEditText.getSelectionStart();
                final int end = _noteEditText.getSelectionEnd();

                if ( !_updatingText ) {
                    // Format the text once the user has stopped typing
                    _formatTextHandler.removeCallbacksAndMessages(null);
                    _formatTextHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            formatText(text, start, end);
                        }
                    }, FORMAT_TIMEOUT);
                }
            }
        });
    }

    private void formatText(String text, int selectionStart, int selectionEnd) {
        _updatingText = true;
        SpannableString ss = new SpannableString(text);
        Log.d(LOG_TAG, "Text: " + text);
        HashtagUtils.formatHashtags(ss, getResources().getColor(R.color.hashtag_text), true);
        _noteEditText.setText(ss);
        _noteEditText.setSelection(selectionStart, selectionEnd);
        _updatingText = false;
    }

    private void postClicked() {
        Log.d(LOG_TAG, "POST");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        // No menu for us.
        menu.clear();
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_date_time, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_change_date:
                changeDate();
                break;

            case R.id.action_change_time:
                changeTime();
                break;

            default:
                return super.onContextItemSelected(item);
        }
        return true;
    }

    private void setDateTimeText(Date when) {
        DateFormat df = new SimpleDateFormat("EEEE MM/dd/yy h:mm a", Locale.getDefault());
        _dateTimeTextView.setText(df.format(when));
    }

    private void changeDate() {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(_noteTime);

        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                cal.set(Calendar.YEAR, year);
                cal.set(Calendar.MONTH, monthOfYear);
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                _noteTime = cal.getTime();
                setDateTimeText(_noteTime);
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void changeTime() {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(_noteTime);

       new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
           @Override
           public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
               cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
               cal.set(Calendar.MINUTE, minute);
               _noteTime = cal.getTime();
               setDateTimeText(_noteTime);
           }
       }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
    }

    private void setupHashtags() {
        // TESTING

        // Get the tags from the database
        Realm realm = Realm.getInstance(this);
        RealmResults<Hashtag> allTags = realm.where(Hashtag.class).findAllSorted("tag");
        Set<String> uniqueTags = new HashSet<>();
        for ( Hashtag tag : allTags ) {
            uniqueTags.add(tag.getTag());
        }

        // Get the counts of each of the hashtags
        final Map<String, Long> tagCounts = new HashMap<>();
        for ( String tag : uniqueTags ) {
            tagCounts.put(tag, realm.where(Hashtag.class).equalTo("tag", tag).count());
            // Log.d(LOG_TAG, "Tag: " + tag + " Count: " + tagCounts.get(tag));
        }

        // Sort the tags by count
        List<String> sortedTags = new ArrayList<String>(uniqueTags);
        Collections.sort(sortedTags, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                Long l = tagCounts.get(lhs);
                Long r = tagCounts.get(rhs);
                // Sort by name if the counts are equal
                if ( l.equals(r) ) {
                    return lhs.compareTo(rhs);
                }
                // Reverse sort here- highest counts come first
                return r.compareTo(l);
            }
        });

        // Add the defaults to the end of the sorted tag list, just in case there aren't any
        // defined yet
        String[] defaultTags = getResources().getStringArray(R.array.default_hashtags);
        sortedTags.addAll(Arrays.asList(defaultTags));

        // Create the list of hashtags for the adapter in the same order as sortedTags
        List<Hashtag> hashtagList = new ArrayList<>();
        for ( String tagName : sortedTags ) {
            Hashtag tag = realm.where(Hashtag.class).equalTo("tag", tagName).findAll().first();
            if ( tag == null ) {
                // Probably one of our default tags- they're not in the database.
                tag = new Hashtag(tagName);
            }
            hashtagList.add(tag);
            if ( hashtagList.size() >= MAX_TAGS ) {
                break;
            }
        }

        _hashtagView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        _hashtagView.setAdapter(new HashtagAdapter(hashtagList, new HashtagAdapter.OnTagTappedListener() {
            @Override
            public void tagTapped(String tag) {
                addHashtag(tag);
            }
        }));
    }

    private void addHashtag(String tag) {
        // Add the text to the editor at the current position
        int start = Math.max(_noteEditText.getSelectionStart(), 0);
        int end = Math.max(_noteEditText.getSelectionEnd(), 0);

        // Find out if there is a space behind the insertion point, and add one if not
        Editable text = _noteEditText.getText();
        if ( end > 0 && !Character.isWhitespace(text.charAt(end - 1)) ) {
            tag = " " + tag;
        }

        _noteEditText.getText().replace(Math.min(start, end), Math.max(start, end),
                tag, 0, tag.length());
    }
}
