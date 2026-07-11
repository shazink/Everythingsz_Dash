package com.example.everythingsz_dash;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingsz_dash.data.Bookmark;
import com.example.everythingsz_dash.data.BookmarkRepository;
import com.example.everythingsz_dash.data.Folder;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class BookmarkListFragment extends Fragment {

    private static final String ARG_FOLDER_ID = "folder_id";
    private static final String ARG_FOLDER_NAME = "folder_name";

    private long folderId;
    private String folderName;

    private RecyclerView recyclerView;
    private BookmarkAdapter adapter;
    private BookmarkRepository repository;
    private LinearLayout emptyState;
    private EditText searchField;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static BookmarkListFragment newInstance(long folderId, String folderName) {
        BookmarkListFragment fragment = new BookmarkListFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_FOLDER_ID, folderId);
        args.putString(ARG_FOLDER_NAME, folderName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            folderId = getArguments().getLong(ARG_FOLDER_ID);
            folderName = getArguments().getString(ARG_FOLDER_NAME, "Bookmarks");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bookmark_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new BookmarkRepository(requireContext());

        // Setup toolbar with folder name and back navigation
        Toolbar toolbar = view.findViewById(R.id.toolbar_bookmarks);
        toolbar.setTitle(folderName);
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // Setup RecyclerView
        recyclerView = view.findViewById(R.id.recycler_bookmarks);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new BookmarkAdapter(
                // Click: open in browser
                this::openBookmarkInBrowser,
                // Long click: show context menu
                this::showBookmarkContextMenu
        );
        recyclerView.setAdapter(adapter);

        emptyState = view.findViewById(R.id.empty_state_bookmarks);

        // FAB: add new bookmark
        FloatingActionButton fab = view.findViewById(R.id.fab_add_bookmark);
        fab.setOnClickListener(v -> showAddBookmarkDialog());

        // Search
        searchField = view.findViewById(R.id.search_bookmarks);
        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBookmarks(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadBookmarks();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadBookmarks();
    }

    private void loadBookmarks() {
        String query = searchField != null ? searchField.getText().toString().trim() : "";
        if (query.isEmpty()) {
            repository.getBookmarksByFolder(folderId, bookmarks ->
                    mainHandler.post(() -> displayBookmarks(bookmarks)));
        } else {
            repository.searchBookmarksInFolder(folderId, query, bookmarks ->
                    mainHandler.post(() -> displayBookmarks(bookmarks)));
        }
    }

    private void filterBookmarks(String query) {
        if (query.isEmpty()) {
            repository.getBookmarksByFolder(folderId, bookmarks ->
                    mainHandler.post(() -> displayBookmarks(bookmarks)));
        } else {
            repository.searchBookmarksInFolder(folderId, query, bookmarks ->
                    mainHandler.post(() -> displayBookmarks(bookmarks)));
        }
    }

    private void displayBookmarks(List<Bookmark> bookmarks) {
        if (!isAdded()) return;

        adapter.setBookmarks(bookmarks);

        if (bookmarks.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void openBookmarkInBrowser(Bookmark bookmark) {
        try {
            String url = bookmark.getUrl();
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.toast_cannot_open_url, Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddBookmarkDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_bookmark, null);

        TextInputEditText editTitle = dialogView.findViewById(R.id.edit_bookmark_title);
        TextInputEditText editUrl = dialogView.findViewById(R.id.edit_bookmark_url);
        TextInputLayout tilTitle = dialogView.findViewById(R.id.til_bookmark_title);
        TextInputLayout tilUrl = dialogView.findViewById(R.id.til_bookmark_url);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_new_bookmark)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_create, null)
                .setNegativeButton(R.string.btn_cancel, null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String title = editTitle.getText() != null ? editTitle.getText().toString().trim() : "";
                String url = editUrl.getText() != null ? editUrl.getText().toString().trim() : "";

                // Validate
                boolean valid = true;
                if (title.isEmpty()) {
                    tilTitle.setError(getString(R.string.error_empty_name));
                    valid = false;
                } else {
                    tilTitle.setError(null);
                }

                if (url.isEmpty()) {
                    tilUrl.setError(getString(R.string.error_empty_url));
                    valid = false;
                } else {
                    tilUrl.setError(null);
                }

                if (!valid) return;

                Bookmark bookmark = new Bookmark(title, url, folderId);
                repository.insertBookmark(bookmark, id -> mainHandler.post(() -> {
                    Toast.makeText(requireContext(), R.string.toast_bookmark_added, Toast.LENGTH_SHORT).show();
                    loadBookmarks();
                }));
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void showBookmarkContextMenu(Bookmark bookmark, View anchorView) {
        PopupMenu popup = new PopupMenu(requireContext(), anchorView);
        popup.inflate(R.menu.menu_bookmark_context);

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_edit) {
                showEditBookmarkDialog(bookmark);
                return true;
            } else if (itemId == R.id.action_move) {
                showMoveBookmarkDialog(bookmark);
                return true;
            } else if (itemId == R.id.action_delete) {
                showDeleteBookmarkConfirmation(bookmark);
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void showEditBookmarkDialog(Bookmark bookmark) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_bookmark, null);

        TextInputEditText editTitle = dialogView.findViewById(R.id.edit_bookmark_title);
        TextInputEditText editUrl = dialogView.findViewById(R.id.edit_bookmark_url);
        TextInputLayout tilTitle = dialogView.findViewById(R.id.til_bookmark_title);
        TextInputLayout tilUrl = dialogView.findViewById(R.id.til_bookmark_url);

        editTitle.setText(bookmark.getTitle());
        editUrl.setText(bookmark.getUrl());

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_edit_bookmark)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_save, null)
                .setNegativeButton(R.string.btn_cancel, null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String title = editTitle.getText() != null ? editTitle.getText().toString().trim() : "";
                String url = editUrl.getText() != null ? editUrl.getText().toString().trim() : "";

                boolean valid = true;
                if (title.isEmpty()) {
                    tilTitle.setError(getString(R.string.error_empty_name));
                    valid = false;
                } else {
                    tilTitle.setError(null);
                }

                if (url.isEmpty()) {
                    tilUrl.setError(getString(R.string.error_empty_url));
                    valid = false;
                } else {
                    tilUrl.setError(null);
                }

                if (!valid) return;

                bookmark.setTitle(title);
                bookmark.setUrl(url);
                repository.updateBookmark(bookmark, () -> mainHandler.post(() -> {
                    Toast.makeText(requireContext(), R.string.toast_bookmark_updated, Toast.LENGTH_SHORT).show();
                    loadBookmarks();
                }));
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void showMoveBookmarkDialog(Bookmark bookmark) {
        // Load all folders to populate the picker
        repository.getAllFolders(folders -> mainHandler.post(() -> {
            if (!isAdded()) return;

            // Remove current folder from the list
            List<Folder> otherFolders = new ArrayList<>();
            for (Folder f : folders) {
                if (f.getId() != folderId) {
                    otherFolders.add(f);
                }
            }

            if (otherFolders.isEmpty()) {
                Toast.makeText(requireContext(), "No other folders to move to", Toast.LENGTH_SHORT).show();
                return;
            }

            // Build names array for the dialog
            String[] folderNames = new String[otherFolders.size()];
            for (int i = 0; i < otherFolders.size(); i++) {
                folderNames[i] = otherFolders.get(i).getName();
            }

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.dialog_move_bookmark)
                    .setItems(folderNames, (d, which) -> {
                        Folder targetFolder = otherFolders.get(which);
                        repository.moveBookmark(bookmark.getId(), targetFolder.getId(), () ->
                                mainHandler.post(() -> {
                                    Toast.makeText(requireContext(), R.string.toast_bookmark_moved, Toast.LENGTH_SHORT).show();
                                    loadBookmarks();
                                })
                        );
                    })
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show();
        }));
    }

    private void showDeleteBookmarkConfirmation(Bookmark bookmark) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_delete_bookmark)
                .setMessage(R.string.confirm_delete_bookmark)
                .setPositiveButton(R.string.btn_delete, (d, which) -> {
                    repository.deleteBookmark(bookmark, () -> mainHandler.post(() -> {
                        Toast.makeText(requireContext(), R.string.toast_bookmark_deleted, Toast.LENGTH_SHORT).show();
                        loadBookmarks();
                    }));
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }
}
