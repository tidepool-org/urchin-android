package io.tidepool.urchin;

import android.animation.Animator;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import io.tidepool.urchin.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
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
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.tidepool.urchin.api.APIClient;
import io.tidepool.urchin.data.CurrentUser;
import io.tidepool.urchin.data.Hashtag;
import io.tidepool.urchin.data.Note;
import io.tidepool.urchin.data.User;
import io.tidepool.urchin.ui.HashtagAdapter;
import io.tidepool.urchin.ui.UserFilterAdapter;
import io.tidepool.urchin.util.HashtagUtils;
import io.tidepool.urchin.util.MiscUtils;

public class NewNoteActivity extends AppCompatActivity implements RealmChangeListener {
    private static final String LOG_TAG = "NewNote";

    // Arguments we can take to edit instead of create a new note
    public static final String ARG_EDIT_NOTE_ID = "EditNoteId";         // The ID of the note to edit

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

    private Note _editingNote;                      // Null for a new note

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_note);

        _noteEditText = (EditText)findViewById(R.id.note_edit_text);
        _dateTimeTextView = (TextView)findViewById(R.id.date_time);
        _hashtagView = (RecyclerView)findViewById(R.id.hashtag_recyclerview);
        _dropDownLayout = (LinearLayout)findViewById(R.id.layout_drop_down);
        _dropDownListView = (ListView)findViewById(R.id.listview_filter);

        _dropDownLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDropDownMenu(false);
            }
        });

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
                if ( !_updatingText ) {
                    // Format the text once the user has stopped typing
                    _formatTextHandler.removeCallbacksAndMessages(null);
                    _formatTextHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            formatText();
                        }
                    }, FORMAT_TIMEOUT);
                }
            }
        });

        // Set our current user to whatever is in the database, or the first user found if none exists
        Realm realm = Realm.getInstance(this);
        realm.addChangeListener(this);

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
        realm.close();

        // See if we were launched to create a new note, or to edit an existing one
        Bundle args = getIntent().getExtras();
        if ( args != null ) {
            String messageId = args.getString(ARG_EDIT_NOTE_ID);
            if (messageId != null) {
                setEditing(messageId);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Realm realm = Realm.getInstance(this);
        realm.removeChangeListener(this);
    }

    private void setEditing(String messageId) {
        Realm realm = Realm.getInstance(this);
        Note note = realm.where(Note.class).equalTo("id", messageId).findFirst();
        User author = realm.where(User.class).equalTo("userid", note.getUserid()).findFirst();
        setCurrentUser(author);
        _noteTime = note.getTimestamp();
        setDateTimeText(_noteTime);
        SpannableString ss = new SpannableString(note.getMessagetext());
        HashtagUtils.formatHashtags(ss, getResources().getColor(R.color.hashtag_text), true);
        _noteEditText.setText(ss);
        _editingNote = note;
    }

    private void setCurrentUser(User user) {
        _currentUser = user;
        if ( user != null ) {
            setTitle(MiscUtils.getPrintableNameForUser(_currentUser));
            Realm realm = Realm.getInstance(this);
            realm.beginTransaction();
            RealmResults<CurrentUser> results = realm.where(CurrentUser.class).findAll();
            if (results.size() > 0) {
                results.clear();
            }

            CurrentUser u = realm.createObject(CurrentUser.class);
            u.setCurrentUser(user);
            realm.commitTransaction();
            realm.close();
        }
    }

    private void formatText() {
        long startTime = System.nanoTime();

        _updatingText = true;
        int selectionStart = _noteEditText.getSelectionStart();
        int selectionEnd = _noteEditText.getSelectionEnd();
        String text = _noteEditText.getText().toString();
        SpannableString ss = new SpannableString(text);
        Log.d(LOG_TAG, "Text: " + text);
        HashtagUtils.formatHashtags(ss, getResources().getColor(R.color.hashtag_text), true);
        _noteEditText.setText(ss, EditText.BufferType.SPANNABLE);
        _noteEditText.setSelection(selectionStart, selectionEnd);
        _updatingText = false;

        long totalTime = System.nanoTime() - startTime;
        Log.d(LOG_TAG, "formatText took " + totalTime / 1000000L + "ms");
    }

    private void postOrUpdate() {
        Log.d(LOG_TAG, "POST");

        if ( !wasEdited() ) {
            // Treat like a back button
            onBackPressed();
            return;
        }

        // Put up a wait dialog while we post the message
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle(_editingNote != null ? R.string.updating_note : R.string.posting_note);
        pd.setIcon(getResources().getDrawable(R.mipmap.ic_launcher));
        pd.show();

        APIClient api = MainActivity.getInstance().getAPIClient();

        Note note = new Note();
        note.setMessagetext(_noteEditText.getText().toString());
        note.setTimestamp(_noteTime);

        if ( _editingNote == null ) {
            // We are creating a new note
            note.setGroupid(_currentUser.getUserid());
            note.setUserid(api.getUser().getUserid());
            note.setAuthorFullName(MiscUtils.getPrintableNameForUser(_currentUser));
            note.setGuid(UUID.randomUUID().toString());

            api.postNote(note, new APIClient.PostNoteListener() {
                @Override
                public void notePosted(Note note, Exception error) {
                    pd.dismiss();
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
        } else {
            // We are updating an existing note. We only care about the ID, messagetext and timestamp.
            note.setId(_editingNote.getId());

            api.updateNote(note, new APIClient.UpdateNoteListener() {
                @Override
                public void noteUpdated(Note note, Exception error) {
                    pd.dismiss();
                    if (error == null) {
                        // Note was posted. Update the note in the database.
                        Toast.makeText(NewNoteActivity.this, R.string.note_updated, Toast.LENGTH_LONG).show();
                        Realm realm = Realm.getInstance(NewNoteActivity.this);
                        realm.beginTransaction();
                        _editingNote.setMessagetext(note.getMessagetext());
                        _editingNote.setTimestamp(note.getTimestamp());
                        realm.commitTransaction();
                        finish();
                    } else {
                        String errorMessage = getResources().getString(R.string.error_updating, error.getMessage());
                        Toast.makeText(NewNoteActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
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

    @Override
    public void onBackPressed() {
        // Let the back button dismiss the drop-down menu if present
        if ( _dropDownLayout.getVisibility() == View.VISIBLE ) {
            showDropDownMenu(false);
        } else {
            if ( wasEdited() ) {
                confirmExit();
            } else {
                super.onBackPressed();
            }
        }
    }

    private void confirmExit() {
        boolean editing = (_editingNote != null);
        int title = editing ? R.string.save_changes_title : R.string.discard_note_title;
        int message = editing ? R.string.save_changes_message : R.string.discard_note_message;
        int ok = editing ? R.string.button_save : android.R.string.ok;
        int cancel = editing ? R.string.button_discard : android.R.string.cancel;

        DialogInterface.OnClickListener okListener;
        DialogInterface.OnClickListener cancelListener;

        if ( editing ) {
            okListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Save the changes and exit. Just click Post and we're done.
                    postOrUpdate();
                }
            };
            cancelListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Discard the changes and exit.
                    NewNoteActivity.super.onBackPressed();
                }
            };
        } else {
            okListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Discard the changes and exit
                    NewNoteActivity.super.onBackPressed();
                }
            };

            // Don't exit
            cancelListener = null;
        }

        // Build and show the dialog
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(ok, okListener)
                .setNegativeButton(cancel, cancelListener)
                .create().show();
    }

    private boolean wasEdited() {
        if ( _editingNote == null ) {
            return _noteEditText.getText().length() > 0;
        }

        // We are editing a note. Return true if the text or date has changed
        return !(_noteEditText.getText().toString().equals(_editingNote.getMessagetext()) &&
                 _noteTime.equals(_editingNote.getTimestamp()));
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
        if ( _editingNote == null ) {
            inflater.inflate(R.menu.menu_new_note, menu);
        } else {
            inflater.inflate(R.menu.menu_edit_note, menu);
        }
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

        if ( id == R.id.action_delete_note ) {
            Log.d(LOG_TAG, "Delete Note");
            new AlertDialog.Builder(this)
                    .setTitle(R.string.action_delete_note)
                    .setMessage(R.string.delete_note_confirm)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteNote();
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(getResources().getDrawable(R.mipmap.ic_launcher))
                    .show();
            return true;
        }

        if ( id == R.id.action_save_note ) {
            postOrUpdate();
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

    private void deleteNote() {
        // We're deleting the note we are currently editing.
        MainActivity.getInstance().getAPIClient().deleteNote(_editingNote, new APIClient.DeleteNoteListener() {
            @Override
            public void noteDeleted(Exception error) {
                if ( error == null ) {
                    Toast.makeText(NewNoteActivity.this, R.string.note_deleted, Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(NewNoteActivity.this, R.string.note_deleted_error, Toast.LENGTH_LONG).show();
                }
            }
        });
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

        // Add the defaults to unique tag list, just in case there aren't any tags
        // defined yet.
        String[] defaultTags = getResources().getStringArray(R.array.default_hashtags);
        uniqueTags.addAll(Arrays.asList(defaultTags));

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
                if (l.equals(r)) {
                    return lhs.compareTo(rhs);
                }
                // Reverse sort here- highest counts come first
                return r.compareTo(l);
            }
        });

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

        // https://trello.com/c/RRylANWR - Add Space after Hashtag
        tag += " ";

        _noteEditText.getText().replace(Math.min(start, end), Math.max(start, end),
                tag, 0, tag.length());
    }

    @Override
    public void onChange() {
        // Realm database has changed
        Realm realm = Realm.getInstance(this);
        realm.removeChangeListener(this);
        Log.d(LOG_TAG, "Realm database has changed- repopulating drop-down list and hashtag view");
        populateDropDownList();
        setupHashtags();
        setCurrentUser(_currentUser);
        realm.addChangeListener(this);
    }
}
