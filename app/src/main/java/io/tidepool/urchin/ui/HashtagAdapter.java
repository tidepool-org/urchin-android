package io.tidepool.urchin.ui;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import io.tidepool.urchin.R;
import io.tidepool.urchin.data.Hashtag;

/**
 * Created by Brian King on 9/1/15.
 */
public class HashtagAdapter extends RecyclerView.Adapter<HashtagAdapter.HashtagViewHolder> {
    private List<Hashtag> _hashtags;
    private OnTagTappedListener _listener;

    public HashtagAdapter(List<Hashtag> hashtags, OnTagTappedListener listener) {
        _hashtags = hashtags;
        _listener = listener;
    }

    @Override
    public HashtagViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cardview_hashtag, viewGroup, false);
        return new HashtagViewHolder(v);
    }

    @Override
    public void onBindViewHolder(HashtagViewHolder holder, int position) {
        Hashtag tag = _hashtags.get(position);
        final String tagText = tag.getTag();
        holder.textView.setText(tagText);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (_listener != null) {
                    _listener.tagTapped(tagText);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return _hashtags.size();
    }

    public static abstract class OnTagTappedListener {
        public abstract void tagTapped(String tag);
    }

    public static class HashtagViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;

        public HashtagViewHolder(View v) {
            super(v);
            textView = (TextView) v.findViewById(R.id.hashtag_textview);
        }
    }
}
