package io.tidepool.urchin;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class NewNoteActivity extends AppCompatActivity {
    private static final String LOG_TAG = "NewNote";

    private EditText _noteEditText;
    private TextView _dateTimeTextView;
    private Button _postButton;

    private Date _noteTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_note);

        _noteEditText = (EditText)findViewById(R.id.note_edit_text);
        _dateTimeTextView = (TextView)findViewById(R.id.date_time);
        _postButton = (Button)findViewById(R.id.post_button);

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
}
