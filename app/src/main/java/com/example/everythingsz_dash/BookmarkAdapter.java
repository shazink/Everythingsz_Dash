package com.example.everythingsz_dash;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingsz_dash.data.Bookmark;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.BookmarkViewHolder> {

    public interface OnBookmarkClickListener {
        void onBookmarkClick(Bookmark bookmark);
    }

    public interface OnBookmarkLongClickListener {
        void onBookmarkLongClick(Bookmark bookmark, View anchorView);
    }

    private List<Bookmark> bookmarks = new ArrayList<>();
    private final OnBookmarkClickListener clickListener;
    private final OnBookmarkLongClickListener longClickListener;

    public BookmarkAdapter(OnBookmarkClickListener clickListener, OnBookmarkLongClickListener longClickListener) {
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    public void setBookmarks(List<Bookmark> bookmarks) {
        this.bookmarks = bookmarks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookmarkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bookmark, parent, false);
        return new BookmarkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookmarkViewHolder holder, int position) {
        Bookmark bookmark = bookmarks.get(position);

        holder.textTitle.setText(bookmark.getTitle());
        holder.textUrl.setText(bookmark.getUrl());

        holder.cardBookmark.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onBookmarkClick(bookmark);
            }
        });

        holder.cardBookmark.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onBookmarkLongClick(bookmark, v);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return bookmarks.size();
    }

    static class BookmarkViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardBookmark;
        final TextView textTitle;
        final TextView textUrl;

        BookmarkViewHolder(@NonNull View itemView) {
            super(itemView);
            cardBookmark = itemView.findViewById(R.id.card_bookmark);
            textTitle = itemView.findViewById(R.id.text_bookmark_title);
            textUrl = itemView.findViewById(R.id.text_bookmark_url);
        }
    }
}
