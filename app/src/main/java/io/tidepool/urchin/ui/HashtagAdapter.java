package io.tidepool.urchin.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import io.tidepool.urchin.R;
import io.tidepool.urchin.data.Hashtag;

/**
 * Created by Brian King on 9/1/15.
 */
public class HashtagAdapter extends ArrayAdapter<Hashtag> {
    private final Context _context;
    private OnStarTappedListener _onStarTappedListener;

    public HashtagAdapter(Context context, List<Hashtag> hashtags, OnStarTappedListener starTappedListener) {
        super(context, -1, hashtags);
        _context = context;
        _onStarTappedListener = starTappedListener;
    }

    public static abstract class OnStarTappedListener {
        public abstract void onStarTapped(Hashtag hashtag);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Hashtag hashtag = getItem(position);

        LayoutInflater inflater = (LayoutInflater)_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = convertView != null ? convertView : inflater.inflate(R.layout.list_item_hashtag, parent, false);
        TextView tv = (TextView)rowView.findViewById(R.id.hashtag_textview);
        ImageButton starButton = (ImageButton)rowView.findViewById(R.id.star_button);
        starButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ( _onStarTappedListener != null ) {
                    _onStarTappedListener.onStarTapped(hashtag);
                }
            }
        });

        tv.setText(hashtag.getTag());

        return rowView;
    }
}
