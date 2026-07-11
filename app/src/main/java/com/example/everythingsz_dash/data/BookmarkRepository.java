package com.example.everythingsz_dash.data;

import android.content.Context;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository that wraps DAOs and runs all database operations on a background thread.
 * UI code should call these methods and provide callbacks to receive results.
 */
public class BookmarkRepository {

    private final FolderDao folderDao;
    private final BookmarkDao bookmarkDao;
    private final ExecutorService executor;

    public interface DataCallback<T> {
        void onResult(T result);
    }

    public BookmarkRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.folderDao = db.folderDao();
        this.bookmarkDao = db.bookmarkDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    // ==================== Folder Operations ====================

    public void getAllFolders(DataCallback<List<Folder>> callback) {
        executor.execute(() -> {
            List<Folder> folders = folderDao.getAllFolders();
            callback.onResult(folders);
        });
    }

    public void searchFolders(String query, DataCallback<List<Folder>> callback) {
        executor.execute(() -> {
            List<Folder> folders = folderDao.searchFolders(query);
            callback.onResult(folders);
        });
    }

    public void getFolderById(long id, DataCallback<Folder> callback) {
        executor.execute(() -> {
            Folder folder = folderDao.getFolderById(id);
            callback.onResult(folder);
        });
    }

    public void insertFolder(Folder folder, DataCallback<Long> callback) {
        executor.execute(() -> {
            long id = folderDao.insert(folder);
            if (callback != null) callback.onResult(id);
        });
    }

    public void updateFolder(Folder folder, Runnable onComplete) {
        executor.execute(() -> {
            folderDao.update(folder);
            if (onComplete != null) onComplete.run();
        });
    }

    public void deleteFolder(Folder folder, Runnable onComplete) {
        executor.execute(() -> {
            folderDao.delete(folder);
            if (onComplete != null) onComplete.run();
        });
    }

    // ==================== Bookmark Operations ====================

    public void getBookmarksByFolder(long folderId, DataCallback<List<Bookmark>> callback) {
        executor.execute(() -> {
            List<Bookmark> bookmarks = bookmarkDao.getBookmarksByFolder(folderId);
            callback.onResult(bookmarks);
        });
    }

    public void searchBookmarksInFolder(long folderId, String query, DataCallback<List<Bookmark>> callback) {
        executor.execute(() -> {
            List<Bookmark> bookmarks = bookmarkDao.searchBookmarksInFolder(folderId, query);
            callback.onResult(bookmarks);
        });
    }

    public void getBookmarkCount(long folderId, DataCallback<Integer> callback) {
        executor.execute(() -> {
            int count = bookmarkDao.getBookmarkCount(folderId);
            callback.onResult(count);
        });
    }

    public void insertBookmark(Bookmark bookmark, DataCallback<Long> callback) {
        executor.execute(() -> {
            long id = bookmarkDao.insert(bookmark);
            if (callback != null) callback.onResult(id);
        });
    }

    public void updateBookmark(Bookmark bookmark, Runnable onComplete) {
        executor.execute(() -> {
            bookmarkDao.update(bookmark);
            if (onComplete != null) onComplete.run();
        });
    }

    public void deleteBookmark(Bookmark bookmark, Runnable onComplete) {
        executor.execute(() -> {
            bookmarkDao.delete(bookmark);
            if (onComplete != null) onComplete.run();
        });
    }

    public void moveBookmark(long bookmarkId, long newFolderId, Runnable onComplete) {
        executor.execute(() -> {
            bookmarkDao.moveBookmark(bookmarkId, newFolderId);
            if (onComplete != null) onComplete.run();
        });
    }

    // ==================== Backup / Restore ====================

    public void getAllBookmarks(DataCallback<List<Bookmark>> callback) {
        executor.execute(() -> {
            List<Bookmark> bookmarks = bookmarkDao.getAllBookmarks();
            callback.onResult(bookmarks);
        });
    }

    public void getFolderByName(String name, DataCallback<Folder> callback) {
        executor.execute(() -> {
            Folder folder = folderDao.getFolderByName(name);
            callback.onResult(folder);
        });
    }

    /**
     * Synchronous access to DAOs for use by BackupManager on a background thread.
     */
    public FolderDao folderDao() {
        return folderDao;
    }

    public BookmarkDao bookmarkDao() {
        return bookmarkDao;
    }

    public ExecutorService getExecutor() {
        return executor;
    }
}
