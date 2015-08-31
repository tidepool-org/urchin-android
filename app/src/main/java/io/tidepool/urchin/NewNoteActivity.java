package io.tidepool.urchin;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NewNoteActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String LOG_TAG = "NewNote";

    private EditText _noteEditText;
    private TextView _dateTimeTextView;
    private Button _postButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_note);

        _noteEditText = (EditText)findViewById(R.id.note_edit_text);
        _dateTimeTextView = (TextView)findViewById(R.id.date_time);
        _postButton = (Button)findViewById(R.id.post_button);

        // Respond to taps on the date / time bar
        findViewById(R.id.date_time_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dateTimeClicked();
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
        setDateTimeText(new Date());
    }

    private void postClicked() {
        Log.d(LOG_TAG, "POST");
    }

    private void dateTimeClicked() {
        Log.d(LOG_TAG, "DATE");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        // No menu for us.
        menu.clear();
        return true;
    }

    private void setDateTimeText(Date when) {
        DateFormat df = new SimpleDateFormat("EEEE MM/dd/yy h:mm a", Locale.getDefault());
        _dateTimeTextView.setText(df.format(when));
    }

    @Override
    public void onClick(View v) {

    }
}
