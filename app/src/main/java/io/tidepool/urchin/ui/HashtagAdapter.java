package io.tidepool.urchin.ui;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.tidepool.urchin.R;
import io.tidepool.urchin.data.Hashtag;
import io.tidepool.urchin.util.HashtagUtils;

/**
 * Created by Brian King on 9/1/15.
 */
public class HashtagAdapter extends ArrayAdapter<Hashtag> {
    private final Context _context;
    private OnStarTappedListener _onStarTappedListener;
    private List<Hashtag> _hashtags;

    public HashtagAdapter(Context context, List<Hashtag> hashtags, OnStarTappedListener starTappedListener) {
        super(context, -1, hashtags);
        _hashtags = hashtags;
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
                if (_onStarTappedListener != null) {
                    _onStarTappedListener.onStarTapped(hashtag);
                }
            }
        });

        // Set the hashtag label text
        tv.setText(hashtag.getTag());

        // Only show the star if somebody is listening
        if ( _onStarTappedListener != null ) {
            starButton.setVisibility(View.VISIBLE);
            // Set the appropriate star icon and color
            boolean isStarred = HashtagUtils.isHashtagStarred(_context, hashtag.getTag());
            int starId = isStarred ? R.drawable.ic_star_white_24dp : R.drawable.ic_star_border_white_24dp;
            int colorId = isStarred ? R.color.starred : R.color.unstarred;

            starButton.setImageDrawable(ContextCompat.getDrawable(_context, starId));
            starButton.setColorFilter(ContextCompat.getColor(_context, colorId), PorterDuff.Mode.SRC_ATOP);
        } else {
            starButton.setVisibility(View.GONE);
        }
        return rowView;
    }

    public class HashtagFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if ( constraint == null || constraint.length() == 0 ) {
                // Return everything
                results.values = _hashtags;
                results.count = _hashtags.size();
            } else {
                // Filter the list
                List<Hashtag> filteredTags = new ArrayList<>();
                for ( Hashtag tag : _hashtags ) {
                    if ( tag.getTag().toUpperCase().startsWith(constraint.toString().toUpperCase())) {
                        filteredTags.add(tag);
                    }
                }
                results.values = filteredTags;
                results.count = filteredTags.size();
            }

            return results;
        }

        @Override
        @SuppressWarnings("unchecked")  // Cast of results.values: We know what it is.
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if ( results.count == 0 ) {
                notifyDataSetInvalidated();
            } else {
                _hashtags = (List<Hashtag>)results.values;
            }
        }

        @Override
        public CharSequence convertResultToString(Object resultValue) {
            Hashtag tag = (Hashtag)resultValue;
            if ( tag != null ) {
                return tag.getTag();
            }
            return null;
        }
    }

    private Filter _hashtagFilter;
    @Override
    public Filter getFilter() {
        if ( _hashtagFilter == null ) {
            _hashtagFilter = new HashtagFilter();
        }
        return _hashtagFilter;
    }
}
