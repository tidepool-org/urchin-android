package io.tidepool.urchin;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmList;
import io.realm.RealmResults;
import io.tidepool.urchin.api.APIClient;
import io.tidepool.urchin.data.Note;
import io.tidepool.urchin.data.Profile;
import io.tidepool.urchin.data.EmailAddress;
import io.tidepool.urchin.data.SharedUserId;
import io.tidepool.urchin.data.User;

public class MainActivity extends AppCompatActivity implements RealmChangeListener, SwipeRefreshLayout.OnRefreshListener {
    private static final String LOG_TAG = "MainActivity";

    // What server we will connect to
    public static final String SERVER = APIClient.PRODUCTION;

    // Activity request codes
    private static final int REQ_LOGIN = 1;

    private APIClient _apiClient;

    // UI stuff
    private RecyclerView _recyclerView;
    private ImageButton _addButton;
    private RealmResults<Note> _notesResultSet;
    private SwipeRefreshLayout _swipeRefreshLayout;

    private DateFormat _cardDateFormat = new SimpleDateFormat("EEEE MM/dd/yy h:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _recyclerView = (RecyclerView)findViewById(R.id.recycler_view);
        _recyclerView.setLayoutManager(new LinearLayoutManager(this));

        _swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_refresh);
        _swipeRefreshLayout.setOnRefreshListener(this);

        _addButton = (ImageButton)findViewById(R.id.add_button);
        _addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addButtonTapped();
            }
        });

        Realm realm = Realm.getInstance(this);

        // Create our API client on the appropriate service
        _apiClient = new APIClient(this, SERVER);
        String sessionId = _apiClient.getSessionId();
        User user = _apiClient.getUser();
        if ( sessionId == null || user == null ) {
            // We need to sign in
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivityForResult(loginIntent, REQ_LOGIN);
        } else {
            updateUser();
        }

        setTitle(R.string.all_notes);
    }

    protected void startQuery() {
        Realm realm = Realm.getInstance(this);

        // Set up our query
        _notesResultSet = realm.where(Note.class).findAllSorted("timestamp", false);
        _recyclerView.setAdapter(new NotesAdapter());
    }

    @Override
    protected void onStart() {
        super.onStart();
        startQuery();
        Realm realm = Realm.getInstance(this);
        realm.addChangeListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        _swipeRefreshLayout.setRefreshing(false);
        Realm realm = Realm.getInstance(this);
        realm.removeChangeListener(this);
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_sign_out) {
            _apiClient.signOut(new APIClient.SignOutListener() {
                @Override
                public void signedOut(int responseCode, Exception error) {
                    Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivityForResult(loginIntent, REQ_LOGIN);
                }
            });
            return true;
        }

        return super.onOptionsItemSelected(item);
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
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    protected void addButtonTapped() {
        Log.d(LOG_TAG, "Add Tapped");
        Intent intent = new Intent(this, NewNoteActivity.class);
        startActivity(intent);
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
        }
    }

    @Override
    public void onRefresh() {
        // Swipe view refresh
        Log.d(LOG_TAG, "OnRefresh");
        updateUser();
    }

    public class NotesViewHolder extends RecyclerView.ViewHolder {
        public TextView _author;
        public TextView _date;
        public TextView _body;

        public NotesViewHolder(View itemView) {
            super(itemView);
            _author = (TextView)itemView.findViewById(R.id.note_author);
            _date = (TextView)itemView.findViewById(R.id.note_date);
            _body = (TextView)itemView.findViewById(R.id.note_body);
        }
    }

    public class NotesAdapter extends RecyclerView.Adapter<NotesViewHolder> {

        public String getPrintableNameForUser(User user) {
            String name = null;
            if ( user != null ) {
                Profile profile = user.getProfile();
                if (profile != null) {
                    name = profile.getFullName();
                    if (!TextUtils.isEmpty(name)) {
                        return name;
                    }
                    name = profile.getFirstName() + " " + profile.getLastName();
                } else {
                    name = user.getFullName();
                    if (TextUtils.isEmpty(name)) {
                        name = user.getUsername();
                    }
                }
                if ( TextUtils.isEmpty(name) ) {
                    name = "[" + user.getUserid() + "]";
                }
            }

            if ( TextUtils.isEmpty(name) ) {
                name = "UNKNOWN";
            }

            return name;
        }

        @Override
        public NotesViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cardview_note, viewGroup, false);
            return new NotesViewHolder(v);
        }

        @Override
        public void onBindViewHolder(NotesViewHolder notesViewHolder, int i) {
            Note note = _notesResultSet.get(i);
            SpannableString bodyText = new SpannableString(note.getMessagetext());
            formatHashtags(bodyText);
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
                notesViewHolder._author.setText(getPrintableNameForUser(user) + " to " + getPrintableNameForUser(group));
            } else {
                notesViewHolder._author.setText(getPrintableNameForUser(user));
            }

            notesViewHolder._date.setText(_cardDateFormat.format(note.getTimestamp()));

            int colorId = (i % 2 == 0) ? R.color.card_bg_even : R.color.card_bg_odd;
            notesViewHolder.itemView.setBackgroundColor(notesViewHolder.itemView.getContext().getResources().getColor(colorId));
        }

        @Override
        public int getItemCount() {
            return _notesResultSet.size();
        }

        void formatHashtags(SpannableString text) {
            int color = getResources().getColor(R.color.hashtag_text);
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
                        text.setSpan(new StyleSpan(Typeface.BOLD), startSpan, i, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        startSpan = -1;
                    }
                }
            }
            if ( startSpan != -1 ) {
                // Hashtag was last
                text.setSpan(new ForegroundColorSpan(color), startSpan, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                text.setSpan(new StyleSpan(Typeface.BOLD), startSpan, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    @Override
    public void onChange() {
        // Realm dataset has changed. Refresh our data
        _recyclerView.getAdapter().notifyDataSetChanged();
    }
}
