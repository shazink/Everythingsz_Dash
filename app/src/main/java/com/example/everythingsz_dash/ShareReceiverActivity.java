package com.example.everythingsz_dash;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingsz_dash.data.Bookmark;
import com.example.everythingsz_dash.data.BookmarkRepository;
import com.example.everythingsz_dash.data.Folder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Activity that receives shared text/URLs from other apps via the Android share sheet.
 * Shows a dialog-like UI to pick a folder and save the bookmark.
 */
public class ShareReceiverActivity extends AppCompatActivity {

    private BookmarkRepository repository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private TextView textSharedUrl;
    private TextInputEditText editShareTitle;
    private TextInputLayout tilShareTitle;
    private RecyclerView recyclerFolderPicker;
    private FolderPickerAdapter folderPickerAdapter;

    private String sharedUrl = "";
    private long selectedFolderId = -1;

    // Pattern to extract URLs from text
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_receiver);

        repository = new BookmarkRepository(this);

        // Get shared content
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null && type.equals("text/plain")) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            String sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT);

            if (sharedText != null) {
                // Extract URL from shared text
                sharedUrl = extractUrl(sharedText);

                if (sharedUrl.isEmpty()) {
                    Toast.makeText(this, R.string.toast_share_no_url, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                // Determine title
                String title = "";
                if (sharedSubject != null && !sharedSubject.isEmpty()) {
                    title = sharedSubject;
                } else {
                    title = extractDomain(sharedUrl);
                }

                setupUI(title);
            } else {
                Toast.makeText(this, R.string.toast_share_no_url, Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            finish();
        }
    }

    private void setupUI(String defaultTitle) {
        textSharedUrl = findViewById(R.id.text_shared_url);
        editShareTitle = findViewById(R.id.edit_share_title);
        tilShareTitle = findViewById(R.id.til_share_title);
        recyclerFolderPicker = findViewById(R.id.recycler_folder_picker);
        MaterialButton btnSave = findViewById(R.id.btn_share_save);
        MaterialButton btnCancel = findViewById(R.id.btn_share_cancel);
        TextView textCreateFolder = findViewById(R.id.text_create_folder);

        textSharedUrl.setText(sharedUrl);
        editShareTitle.setText(defaultTitle);

        // Setup folder picker
        recyclerFolderPicker.setLayoutManager(new LinearLayoutManager(this));
        folderPickerAdapter = new FolderPickerAdapter(folderId -> {
            selectedFolderId = folderId;
        });
        recyclerFolderPicker.setAdapter(folderPickerAdapter);

        loadFolders();

        // Create new folder
        textCreateFolder.setOnClickListener(v -> showCreateFolderDialog());

        // Save button
        btnSave.setOnClickListener(v -> saveBookmark());

        // Cancel button
        btnCancel.setOnClickListener(v -> finish());
    }

    private void loadFolders() {
        repository.getAllFolders(folders -> mainHandler.post(() -> {
            folderPickerAdapter.setFolders(folders);
            // Auto-select first folder if available
            if (!folders.isEmpty() && selectedFolderId == -1) {
                selectedFolderId = folders.get(0).getId();
                folderPickerAdapter.setSelectedFolderId(selectedFolderId);
            }
        }));
    }

    private void saveBookmark() {
        String title = editShareTitle.getText() != null
                ? editShareTitle.getText().toString().trim() : "";

        if (title.isEmpty()) {
            tilShareTitle.setError(getString(R.string.toast_share_enter_title));
            return;
        }
        tilShareTitle.setError(null);

        if (selectedFolderId == -1) {
            Toast.makeText(this, R.string.toast_share_select_folder, Toast.LENGTH_SHORT).show();
            return;
        }

        Bookmark bookmark = new Bookmark(title, sharedUrl, selectedFolderId);
        repository.insertBookmark(bookmark, id -> mainHandler.post(() -> {
            Toast.makeText(ShareReceiverActivity.this,
                    R.string.toast_share_saved, Toast.LENGTH_SHORT).show();
            finish();
        }));
    }

    private void showCreateFolderDialog() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_add_folder, null);

        TextInputEditText editName = dialogView.findViewById(R.id.edit_folder_name);
        TextInputLayout tilName = dialogView.findViewById(R.id.til_folder_name);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_new_folder)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_create, null)
                .setNegativeButton(R.string.btn_cancel, null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = editName.getText() != null
                        ? editName.getText().toString().trim() : "";
                if (name.isEmpty()) {
                    tilName.setError(getString(R.string.error_empty_name));
                    return;
                }

                Folder folder = new Folder(name);
                repository.insertFolder(folder, id -> mainHandler.post(() -> {
                    selectedFolderId = id;
                    loadFolders();
                    Toast.makeText(ShareReceiverActivity.this,
                            R.string.toast_folder_created, Toast.LENGTH_SHORT).show();
                }));
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    /**
     * Extracts the first URL from the given text.
     */
    private String extractUrl(String text) {
        if (text == null) return "";

        Matcher matcher = URL_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // If no http/https URL found, check if the whole text looks like a domain
        String trimmed = text.trim();
        if (trimmed.contains(".") && !trimmed.contains(" ")) {
            return "https://" + trimmed;
        }

        return trimmed;
    }

    /**
     * Extracts a user-friendly domain name from a URL.
     */
    private String extractDomain(String url) {
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (host != null) {
                // Remove www. prefix
                if (host.startsWith("www.")) {
                    host = host.substring(4);
                }
                // Capitalize first letter
                return host.substring(0, 1).toUpperCase() + host.substring(1);
            }
        } catch (Exception ignored) {}
        return url;
    }

    // ==================== Folder Picker Adapter ====================

    interface OnFolderSelectedListener {
        void onFolderSelected(long folderId);
    }

    static class FolderPickerAdapter extends RecyclerView.Adapter<FolderPickerAdapter.ViewHolder> {
        private List<Folder> folders = new ArrayList<>();
        private long selectedFolderId = -1;
        private final OnFolderSelectedListener listener;

        FolderPickerAdapter(OnFolderSelectedListener listener) {
            this.listener = listener;
        }

        void setFolders(List<Folder> folders) {
            this.folders = folders;
            notifyDataSetChanged();
        }

        void setSelectedFolderId(long id) {
            this.selectedFolderId = id;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_folder_picker, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Folder folder = folders.get(position);
            holder.textName.setText(folder.getName());

            boolean isSelected = folder.getId() == selectedFolderId;
            holder.iconSelected.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            // Highlight selected folder's icon background
            holder.iconBg.setBackgroundResource(isSelected
                    ? R.drawable.bg_folder_icon_gradient
                    : R.drawable.bg_folder_icon);

            holder.itemView.setOnClickListener(v -> {
                selectedFolderId = folder.getId();
                notifyDataSetChanged();
                if (listener != null) {
                    listener.onFolderSelected(folder.getId());
                }
            });
        }

        @Override
        public int getItemCount() {
            return folders.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView textName;
            final ImageView iconSelected;
            final View iconBg;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                textName = itemView.findViewById(R.id.text_folder_picker_name);
                iconSelected = itemView.findViewById(R.id.icon_folder_selected);
                iconBg = itemView.findViewById(R.id.folder_picker_icon_bg);
            }
        }
    }
}
