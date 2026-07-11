package com.example.everythingsz_dash;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingsz_dash.data.Folder;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {

    public interface OnFolderClickListener {
        void onFolderClick(Folder folder);
    }

    public interface OnFolderLongClickListener {
        void onFolderLongClick(Folder folder, View anchorView);
    }

    private List<Folder> folders = new ArrayList<>();
    private final OnFolderClickListener clickListener;
    private final OnFolderLongClickListener longClickListener;
    // Map of folder id -> bookmark count, loaded asynchronously
    private final java.util.Map<Long, Integer> bookmarkCounts = new java.util.HashMap<>();

    public FolderAdapter(OnFolderClickListener clickListener, OnFolderLongClickListener longClickListener) {
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    public void setFolders(List<Folder> folders) {
        this.folders = folders;
        notifyDataSetChanged();
    }

    public void setBookmarkCount(long folderId, int count) {
        bookmarkCounts.put(folderId, count);
        // Find the position for this folder and update
        for (int i = 0; i < folders.size(); i++) {
            if (folders.get(i).getId() == folderId) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_folder, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        Folder folder = folders.get(position);
        Context context = holder.itemView.getContext();

        holder.textFolderName.setText(folder.getName());

        // Show bookmark count
        Integer count = bookmarkCounts.get(folder.getId());
        if (count != null) {
            if (count == 0) {
                holder.textBookmarkCount.setText(context.getString(R.string.bookmark_count_zero));
            } else if (count == 1) {
                holder.textBookmarkCount.setText(context.getString(R.string.bookmark_count_one));
            } else {
                holder.textBookmarkCount.setText(context.getString(R.string.bookmark_count, count));
            }
        } else {
            holder.textBookmarkCount.setText(context.getString(R.string.bookmark_count_zero));
        }

        holder.cardFolder.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onFolderClick(folder);
            }
        });

        holder.cardFolder.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onFolderLongClick(folder, v);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    static class FolderViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardFolder;
        final TextView textFolderName;
        final TextView textBookmarkCount;

        FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            cardFolder = itemView.findViewById(R.id.card_folder);
            textFolderName = itemView.findViewById(R.id.text_folder_name);
            textBookmarkCount = itemView.findViewById(R.id.text_bookmark_count);
        }
    }
}
