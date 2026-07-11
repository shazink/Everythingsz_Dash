package com.example.everythingsz_dash;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingsz_dash.data.BackupManager;
import com.example.everythingsz_dash.data.BookmarkRepository;
import com.example.everythingsz_dash.data.Folder;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FolderListFragment extends Fragment {

    private RecyclerView recyclerView;
    private FolderAdapter adapter;
    private BookmarkRepository repository;
    private LinearLayout emptyState;
    private EditText searchField;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // SAF launchers for export/import
    private ActivityResultLauncher<Intent> exportLauncher;
    private ActivityResultLauncher<Intent> importLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register SAF launchers
        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            performExport(uri);
                        }
                    }
                }
        );

        importLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            performImport(uri);
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_folder_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new BookmarkRepository(requireContext());

        // Setup toolbar with menu
        Toolbar toolbar = view.findViewById(R.id.toolbar_folders);
        toolbar.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_export) {
                launchExport();
                return true;
            } else if (itemId == R.id.action_import) {
                launchImport();
                return true;
            }
            return false;
        });

        // Setup RecyclerView with 2-column grid
        recyclerView = view.findViewById(R.id.recycler_folders);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        adapter = new FolderAdapter(
                // Click: open the folder
                folder -> {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).openFolder(folder.getId(), folder.getName());
                    }
                },
                // Long click: show context menu
                this::showFolderContextMenu
        );
        recyclerView.setAdapter(adapter);

        emptyState = view.findViewById(R.id.empty_state_folders);

        // FAB: add new folder
        FloatingActionButton fab = view.findViewById(R.id.fab_add_folder);
        fab.setOnClickListener(v -> showAddFolderDialog());

        // Search
        searchField = view.findViewById(R.id.search_folders);
        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFolders(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Load folders
        loadFolders();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFolders();
    }

    private void loadFolders() {
        String query = searchField != null ? searchField.getText().toString().trim() : "";
        if (query.isEmpty()) {
            repository.getAllFolders(folders -> mainHandler.post(() -> displayFolders(folders)));
        } else {
            repository.searchFolders(query, folders -> mainHandler.post(() -> displayFolders(folders)));
        }
    }

    private void filterFolders(String query) {
        if (query.isEmpty()) {
            repository.getAllFolders(folders -> mainHandler.post(() -> displayFolders(folders)));
        } else {
            repository.searchFolders(query, folders -> mainHandler.post(() -> displayFolders(folders)));
        }
    }

    private void displayFolders(List<Folder> folders) {
        if (!isAdded()) return;

        adapter.setFolders(folders);

        if (folders.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }

        // Load bookmark counts for each folder
        for (Folder folder : folders) {
            repository.getBookmarkCount(folder.getId(), count ->
                    mainHandler.post(() -> adapter.setBookmarkCount(folder.getId(), count))
            );
        }
    }

    // ==================== Export / Import ====================

    private void launchExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.export_file_name));
        exportLauncher.launch(intent);
    }

    private void launchImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        // Also accept any file type in case JSON isn't recognized
        String[] mimeTypes = {"application/json", "text/plain", "*/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        importLauncher.launch(intent);
    }

    private void performExport(Uri uri) {
        executor.execute(() -> {
            BackupManager backupManager = new BackupManager(requireContext());
            boolean success = backupManager.exportToUri(requireContext(), uri);
            mainHandler.post(() -> {
                if (!isAdded()) return;
                if (success) {
                    Toast.makeText(requireContext(), R.string.toast_export_success, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), R.string.toast_export_failed, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void performImport(Uri uri) {
        executor.execute(() -> {
            BackupManager backupManager = new BackupManager(requireContext());
            int count = backupManager.importFromUri(requireContext(), uri);
            mainHandler.post(() -> {
                if (!isAdded()) return;
                if (count > 0) {
                    Toast.makeText(requireContext(), R.string.toast_import_success, Toast.LENGTH_SHORT).show();
                    loadFolders();
                } else if (count == 0) {
                    Toast.makeText(requireContext(), R.string.toast_import_no_data, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), R.string.toast_import_failed, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // ==================== Folder Dialogs ====================

    private void showAddFolderDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_folder, null);

        TextInputEditText editName = dialogView.findViewById(R.id.edit_folder_name);
        TextInputLayout tilName = dialogView.findViewById(R.id.til_folder_name);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_new_folder)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_create, null) // Set below to prevent auto-dismiss
                .setNegativeButton(R.string.btn_cancel, null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = editName.getText() != null ? editName.getText().toString().trim() : "";
                if (name.isEmpty()) {
                    tilName.setError(getString(R.string.error_empty_name));
                    return;
                }

                Folder folder = new Folder(name);
                repository.insertFolder(folder, id -> mainHandler.post(() -> {
                    Toast.makeText(requireContext(), R.string.toast_folder_created, Toast.LENGTH_SHORT).show();
                    loadFolders();
                }));
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void showFolderContextMenu(Folder folder, View anchorView) {
        PopupMenu popup = new PopupMenu(requireContext(), anchorView);
        popup.inflate(R.menu.menu_folder_context);

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_rename) {
                showRenameFolderDialog(folder);
                return true;
            } else if (itemId == R.id.action_delete) {
                showDeleteFolderConfirmation(folder);
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void showRenameFolderDialog(Folder folder) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_folder, null);

        TextInputEditText editName = dialogView.findViewById(R.id.edit_folder_name);
        TextInputLayout tilName = dialogView.findViewById(R.id.til_folder_name);
        editName.setText(folder.getName());
        editName.selectAll();

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_rename_folder)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_save, null)
                .setNegativeButton(R.string.btn_cancel, null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = editName.getText() != null ? editName.getText().toString().trim() : "";
                if (name.isEmpty()) {
                    tilName.setError(getString(R.string.error_empty_name));
                    return;
                }

                folder.setName(name);
                repository.updateFolder(folder, () -> mainHandler.post(() -> {
                    Toast.makeText(requireContext(), R.string.toast_folder_renamed, Toast.LENGTH_SHORT).show();
                    loadFolders();
                }));
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void showDeleteFolderConfirmation(Folder folder) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_delete_folder)
                .setMessage(R.string.confirm_delete_folder)
                .setPositiveButton(R.string.btn_delete, (d, which) -> {
                    repository.deleteFolder(folder, () -> mainHandler.post(() -> {
                        Toast.makeText(requireContext(), R.string.toast_folder_deleted, Toast.LENGTH_SHORT).show();
                        loadFolders();
                    }));
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }
}
