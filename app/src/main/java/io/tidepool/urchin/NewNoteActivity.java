package io.tidepool.urchin;

import android.animation.Animator;
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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

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
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmResults;
import io.tidepool.urchin.api.APIClient;
import io.tidepool.urchin.data.CurrentUser;
import io.tidepool.urchin.data.Hashtag;
import io.tidepool.urchin.data.Note;
import io.tidepool.urchin.data.User;
import io.tidepool.urchin.ui.HashtagAdapter;
import io.tidepool.urchin.ui.UserFilterAdapter;
import io.tidepool.urchin.util.HashtagUtils;

public class NewNoteActivity extends AppCompatActivity {
    private static final String LOG_TAG = "NewNote";

    private static final int MAX_TAGS = 50;         // Most tags we will show in the scrolling list
    private static final int FORMAT_TIMEOUT = 1000; // Delay we wait to see if the user has stopped typing

    private EditText _noteEditText;
    private TextView _dateTimeTextView;
    private RecyclerView _hashtagView;
    private LinearLayout _dropDownLayout;
    private ListView _dropDownListView;

    private User _currentUser;

    private Date _noteTime;
    private Handler _formatTextHandler;

    private boolean _updatingText;                  // So we don't update text recursively

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_note);

        _noteEditText = (EditText)findViewById(R.id.note_edit_text);
        _dateTimeTextView = (TextView)findViewById(R.id.date_time);
        _hashtagView = (RecyclerView)findViewById(R.id.hashtag_recyclerview);
        _dropDownLayout = (LinearLayout)findViewById(R.id.layout_drop_down);
        _dropDownListView = (ListView)findViewById(R.id.listview_filter);

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
        // TODO: Better hashtag formatting in edit text.
        // This seems kind of clunky having the delay. Without the delay it's almost unusable,
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

        // Set our current user to whatever is in the database, or the first user found if none exists
        Realm realm = Realm.getInstance(this);
        CurrentUser currentUser = realm.where(CurrentUser.class).findFirst();
        if ( currentUser != null ) {
            setCurrentUser(currentUser.getCurrentUser());
        } else {
            // Find a user that has a profile
            RealmResults<User> users = realm.where(User.class).findAllSorted("fullName");
            for ( User user : users ) {
                if ( user.getProfile() != null && user.getProfile().getPatient() != null ) {
                    setCurrentUser(user);
                    break;
                }
            }
        }
    }

    private void setCurrentUser(User user) {
        _currentUser = user;
        setTitle(user.getProfile().getFullName());
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
        APIClient api = MainActivity.getInstance().getAPIClient();
        Note note = new Note();
        note.setGroupid(_currentUser.getUserid());
        note.setMessagetext(_noteEditText.getText().toString());
        note.setTimestamp(_noteTime);
        note.setUserid(api.getUser().getUserid());
        note.setGuid(UUID.randomUUID().toString());

        api.postNote(note, new APIClient.PostNoteListener() {
            @Override
            public void notePosted(Note note, Exception error) {
                if (error == null) {
                    // Note was posted.
                    Toast.makeText(NewNoteActivity.this, R.string.note_posted, Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    String errorMessage = getResources().getString(R.string.error_posting, error.getMessage());
                    Toast.makeText(NewNoteActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void populateDropDownList() {

        // Make an adapter with the "extras": "sign out" and "all users".
        List<User> users = UserFilterAdapter.createUserList(this);
        _dropDownListView.setAdapter(new UserFilterAdapter(this, R.layout.list_item_user, users, false));
        _dropDownListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                User user = (User) _dropDownListView.getAdapter().getItem(position);
                setCurrentUser(user);
                showDropDownMenu(false);
            }
        });

    }
    private void showDropDownMenu(boolean show) {
        if ( show ) {
            setTitle(R.string.note_for);
            _dropDownLayout.setTranslationY(-_dropDownLayout.getHeight());
            _dropDownLayout.requestLayout();

            _dropDownLayout.animate()
                    .translationY(0)
                    .setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            _dropDownLayout.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {

                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    });
        } else {
            setTitle(_currentUser.getProfile().getFullName());
            _dropDownLayout.animate()
                    .translationY(-_dropDownLayout.getHeight())
                    .setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            _dropDownLayout.setVisibility(View.INVISIBLE);
                            _dropDownLayout.setTranslationY(0);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            _dropDownLayout.setVisibility(View.INVISIBLE);
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        // No menu for us.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if ( id == R.id.action_filter_notes ) {
            if ( _dropDownLayout.getVisibility() == View.INVISIBLE ) {
                populateDropDownList();
                showDropDownMenu(true);
            } else {
                showDropDownMenu(false);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
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
            RealmResults<Hashtag> results = realm.where(Hashtag.class).equalTo("tag", tagName).findAll();
            Hashtag tag = null;
            if ( results.size() > 0 ) {
                tag = results.first();
            }
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

        realm.close();
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
