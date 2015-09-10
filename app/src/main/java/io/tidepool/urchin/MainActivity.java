package io.tidepool.urchin;

import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmList;
import io.realm.RealmResults;
import io.tidepool.urchin.api.APIClient;
import io.tidepool.urchin.data.CurrentUser;
import io.tidepool.urchin.data.Note;
import io.tidepool.urchin.data.Profile;
import io.tidepool.urchin.data.SharedUserId;
import io.tidepool.urchin.data.User;
import io.tidepool.urchin.ui.UserFilterAdapter;
import io.tidepool.urchin.util.HashtagUtils;
import io.tidepool.urchin.util.MiscUtils;

public class MainActivity extends AppCompatActivity implements RealmChangeListener, SwipeRefreshLayout.OnRefreshListener {
    private static final String LOG_TAG = "MainActivity";

    // What server we will connect to
    public static final String SERVER = APIClient.PRODUCTION;

    // Activity request codes
    private static final int REQ_LOGIN = 1;
    private static final int REQ_NOTE = 2;

    // Key into preferences for the ID of the user filter
    private static final String PREFS_KEY_USERID = "PrefsUserId";

    private APIClient _apiClient;

    // User to filter messages on, or null for all messages
    private User _userFilter;

    // UI stuff
    private RecyclerView _recyclerView;
    private ImageButton _addButton;
    private RealmResults<Note> _notesResultSet;
    private SwipeRefreshLayout _swipeRefreshLayout;
    private LinearLayout _dropDownLayout;
    private DateFormat _cardDateFormat = new SimpleDateFormat("EEEE MM/dd/yy h:mm a", Locale.getDefault());
    private ListView _dropDownListView;

    // State stuff
    private boolean _justAdded;

    // Access to our instance
    private static MainActivity __thisInstance;
    public static MainActivity getInstance() {
        return __thisInstance;
    }
    // Provide access to our APIClient
    public  APIClient getAPIClient() {
        return _apiClient;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        __thisInstance = this;

        setContentView(R.layout.activity_main);

        _recyclerView = (RecyclerView)findViewById(R.id.recycler_view);
        _recyclerView.setLayoutManager(new LinearLayoutManager(this));

        _swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_refresh);
        _swipeRefreshLayout.setOnRefreshListener(this);

        _dropDownLayout = (LinearLayout)findViewById(R.id.layout_drop_down);
        _dropDownLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDropDownMenu(false);
            }
        });
        _dropDownListView = (ListView)findViewById(R.id.listview_filter);

        // Add a footer with the version / server
        LinearLayout footerLayout = (LinearLayout)getLayoutInflater().inflate(R.layout.version, null);
        TextView footer = (TextView)footerLayout.findViewById(R.id.version_textview);
        footer.setText(MiscUtils.getAppInfoString(this));

        _dropDownListView.addFooterView(footerLayout);

        _addButton = (ImageButton)findViewById(R.id.add_button);
        _addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addButtonTapped();
            }
        });

        // For now, we are going to blow away our database on an update
        Realm realm = null;
        try {
            realm = Realm.getInstance(this);
        } catch (RuntimeException e) {
            Log.e(LOG_TAG, "Failed to load realm database. Blowing away and trying anew.");
            File dbPath = getFilesDir();
            File dbFile = new File(dbPath, "default.realm");
            boolean deleted = dbFile.delete();
            Log.e(LOG_TAG, "dbFile: " + dbFile.getPath() + " deleted: " + deleted);

            // Try again, this time we'll just blow up if it doesn't work
            try {
                realm = Realm.getInstance(this);
            } catch (RuntimeException eInner) {
                Log.e(LOG_TAG, "Failed to open / update the Realm database. Re-throwing.");
                e.printStackTrace();
                throw(eInner);
            }
        }


        // Create our API client on the appropriate service
        _apiClient = new APIClient(this, SERVER);

        // BSK: We will leave the realm instance open for the lifetime of this activity,
        // and will perform an extra close() in onDestroy().
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close out the dangling getInstance() from onCreate()
        Realm realm = Realm.getInstance(this);
        realm.close();
        realm.close();
    }

    @Override
    public void onBackPressed() {
        // Let the back button dismiss the drop-down menu if present
        if ( _dropDownLayout.getVisibility() == View.VISIBLE ) {
            showDropDownMenu(false);
        } else {
            super.onBackPressed();
        }
    }

    protected void populateNotes() {
        Realm realm = Realm.getInstance(this);

        // Set up our query
        if ( _userFilter == null ) {
            _notesResultSet = realm.where(Note.class).findAllSorted("timestamp", false);
            String title = getResources().getString(R.string.all_notes);
            setTitle(title);

        } else {
            _notesResultSet = realm.where(Note.class).equalTo("userid", _userFilter.getUserid())
                    .findAllSorted("timestamp", false);
            setTitle(_userFilter.getProfile().getFullName());
        }

        _recyclerView.setAdapter(new NotesAdapter());
        realm.close();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(LOG_TAG, "onStart");

        String sessionId = _apiClient.getSessionId();
        User user = _apiClient.getUser();
        if ( sessionId == null || user == null ) {
            // We need to sign in
            showLogin();
        } else {
            // Refresh the session token
            _apiClient.refreshToken(new APIClient.RefreshTokenListener() {
                @Override
                public void tokenRefreshed(Exception error) {
                    Log.d(LOG_TAG, "tokenRefreshed: " + error);

                    if (error != null) {
                        // We could not refresh. Need to log in.
                        showLogin();
                    } else {
                        updateUser();
                        // Select the current user, if present in the db
                        restoreUserFilter();

                        // Launch the "Add Note" unless we're returning from that activity
                        if ( !_justAdded ) {
                            addButtonTapped();
                        }
                        _justAdded = false;
                    }
                }
            });
        }

        Realm realm = Realm.getInstance(this);
        realm.addChangeListener(this);
        realm.close();
    }

    @Override
    protected void onStop() {
        super.onStop();
        _swipeRefreshLayout.setRefreshing(false);
        Realm realm = Realm.getInstance(this);
        realm.removeChangeListener(this);
        realm.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
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

    private void showLogin() {
        Intent loginIntent = new Intent(this, LoginActivity.class);
        startActivityForResult(loginIntent, REQ_LOGIN);
    }

    private void showDropDownMenu(boolean show) {
        if ( show ) {
            _addButton.setVisibility(View.INVISIBLE);
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
            _addButton.setVisibility(View.VISIBLE);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch ( requestCode ) {
            case REQ_LOGIN:
                // Sent from the LoginActivity.
                // Get the auth token and user from the intent
                if ( resultCode == Activity.RESULT_OK ) {
                    Log.d(LOG_TAG, "Sign-In success");
                    if ( data.getBooleanExtra(LoginActivity.BUNDLE_REMEMBER_ME, false) ) {
                        Log.d(LOG_TAG, "Remember Me!");
                    }
                    updateUser();
                } else {
                    finish();
                }
                break;

            case REQ_NOTE:
                // Got back from "Add Note" screen
                Log.d(LOG_TAG, "ADD returned");
                _justAdded = true;
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    protected void addButtonTapped() {
        Log.d(LOG_TAG, "Add Tapped");
        Intent intent = new Intent(this, NewNoteActivity.class);
        startActivityForResult(intent, REQ_NOTE);
    }

    /**
     * Gets information about the current user
     */
    private void updateUser() {
        _apiClient.getViewableUserIds(new APIClient.ViewableUserIdsListener() {
            @Override
            public void fetchComplete(RealmList<SharedUserId> userIds, Exception error) {
                Log.d(LOG_TAG, "Viewable IDs received: " + userIds + "Error: " + error);
                updateProfilesAndNotes(userIds);
            }
        });
    }

    private void updateProfilesAndNotes(RealmList<SharedUserId> userIds) {
        final Date to = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(to);
        c.add(Calendar.MONTH, -3);
        final Date from = c.getTime();

        if ( userIds != null ) {
            for (SharedUserId userId : userIds) {
                _apiClient.getProfileForUserId(userId.getVal(), new APIClient.ProfileListener() {
                    @Override
                    public void profileReceived(Profile profile, Exception error) {
                        Log.d(LOG_TAG, "Profile updated: " + profile + " error: " + error);
                    }
                });
                _apiClient.getNotes(userId.getVal(), from, to, new APIClient.NotesListener() {
                    @Override
                    public void notesReceived(RealmList<Note> notes, Exception error) {
                        Log.d(LOG_TAG, "Notes received: " + notes + " error: " + error);
                        _swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        } else {
            _swipeRefreshLayout.setRefreshing(false);
            restoreUserFilter();
        }
    }

    private void populateDropDownList() {

        // Make an adapter with the "extras": "sign out" and "all users".
        List<User> users = UserFilterAdapter.createUserList(this);
        _dropDownListView.setAdapter(new UserFilterAdapter(this, R.layout.list_item_user, users, true));
        _dropDownListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    // All users
                    setUserFilter(null);
                } else if (position == parent.getAdapter().getCount() - 1) {
                    signOut();
                } else {
                    User user = (User) _dropDownListView.getAdapter().getItem(position);
                    setUserFilter(user);
                }
                showDropDownMenu(false);
            }
        });
    }

    private void setUserFilter(User user) {
        // Set the current user in the database
        Realm realm = Realm.getInstance(this);
        realm.beginTransaction();
        realm.where(CurrentUser.class).findAll().clear();
        CurrentUser newCurrentUser = realm.createObject(CurrentUser.class);
        if ( user == null ) {
            // We want a real user object in here- it's the logged-in user.
            newCurrentUser.setCurrentUser(_apiClient.getUser());
        } else {
            newCurrentUser.setCurrentUser(user);
        }
        realm.commitTransaction();
        realm.close();

        // Set our local copy and update the list of notes
        _userFilter = user;
        populateNotes();

        // Save the last user in preferences
        if ( user == null ) {
            getPreferences(Context.MODE_PRIVATE).edit()
                    .remove(PREFS_KEY_USERID).apply();
        } else {
            getPreferences(Context.MODE_PRIVATE).edit()
                    .putString(PREFS_KEY_USERID, user.getUserid()).apply();
        }
    }

    private void restoreUserFilter() {
        String userId = getPreferences(Context.MODE_PRIVATE).getString(PREFS_KEY_USERID, null);
        User user = null;
        if ( userId != null ) {
            Realm realm = Realm.getInstance(this);
            user = realm.where(User.class).equalTo("userid", userId).findFirst();
            realm.close();
        }
        setUserFilter(user);
    }

    private void signOut() {
        _userFilter = null;
        _dropDownListView.setAdapter(null);
        _recyclerView.setAdapter(null);
        _apiClient.signOut(new APIClient.SignOutListener() {
            @Override
            public void signedOut(int responseCode, Exception error) {
                showLogin();
            }
        });
    }

    @Override
    public void onRefresh() {
        // Swipe view refresh
        Log.d(LOG_TAG, "OnRefresh");
        updateUser();
    }

    public String getSelectedServer() {
        return SERVER;
    }

    public class NotesViewHolder extends RecyclerView.ViewHolder {
        public TextView _author;
        public TextView _date;
        public TextView _body;
        public ImageView _editImageView;

        public NotesViewHolder(View itemView) {
            super(itemView);
            _author = (TextView)itemView.findViewById(R.id.note_author);
            _date = (TextView)itemView.findViewById(R.id.note_date);
            _body = (TextView)itemView.findViewById(R.id.note_body);
            _editImageView = (ImageView)itemView.findViewById(R.id.edit_note_button);
            _editImageView.setColorFilter(getResources().getColor(R.color.edit_button_tint));
        }
    }

    public class NotesAdapter extends RecyclerView.Adapter<NotesViewHolder> {

        @Override
        public NotesViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cardview_note, viewGroup, false);
            return new NotesViewHolder(v);
        }

        @Override
        public void onBindViewHolder(NotesViewHolder notesViewHolder, int i) {
            final Note note = _notesResultSet.get(i);
            SpannableString bodyText = new SpannableString(note.getMessagetext());
            int color = getResources().getColor(R.color.hashtag_text);
            HashtagUtils.formatHashtags(bodyText, color, true);
            notesViewHolder._body.setText(bodyText, TextView.BufferType.SPANNABLE);

            Realm realm = Realm.getInstance(MainActivity.this);

            User user = realm.where(User.class).equalTo("userid", note.getUserid()).findFirst();
            User group = null;
            String groupId = note.getGroupid();
            String userId = note.getUserid();
            if ( groupId != null && !groupId.equals(userId) ) {
                group = realm.where(User.class).equalTo("userid", groupId).findFirst();
            }

            if ( group != null ) {
                notesViewHolder._author.setText(MiscUtils.getPrintableNameForUser(user) + " to " + MiscUtils.getPrintableNameForUser(group));
            } else {
                notesViewHolder._author.setText(MiscUtils.getPrintableNameForUser(user));
            }

            notesViewHolder._date.setText(_cardDateFormat.format(note.getTimestamp()));

            int colorId = (i % 2 == 0) ? R.color.card_bg_even : R.color.card_bg_odd;
            CardView cardView = (CardView)notesViewHolder.itemView;
            cardView.setCardBackgroundColor(notesViewHolder.itemView.getContext().getResources().getColor(colorId));

            if ( note.getUserid().equals(_apiClient.getUser().getUserid())) {
                notesViewHolder._editImageView.setVisibility(View.VISIBLE);
                notesViewHolder._editImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        editNoteClicked(note);
                    }
                });
            } else {
                notesViewHolder._editImageView.setVisibility(View.GONE);
            }

            realm.close();
        }

        @Override
        public int getItemCount() {
            return _notesResultSet.size();
        }

    }

    private void editNoteClicked(Note note) {
        Log.d(LOG_TAG, "Edit note: " + note.getMessagetext());
        Intent intent = new Intent(this, NewNoteActivity.class);
        intent.putExtra(NewNoteActivity.ARG_EDIT_NOTE_ID, note.getId());
        startActivityForResult(intent, REQ_NOTE);
    }


    @Override
    public void onChange() {
        // Realm dataset has changed. Refresh our data
        if ( _recyclerView.getAdapter() != null ) {
            _recyclerView.getAdapter().notifyDataSetChanged();
        }
    }
}
