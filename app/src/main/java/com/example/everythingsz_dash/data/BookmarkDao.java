package com.example.everythingsz_dash.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface BookmarkDao {

    @Query("SELECT * FROM bookmarks WHERE folderId = :folderId ORDER BY createdAt DESC")
    List<Bookmark> getBookmarksByFolder(long folderId);

    @Query("SELECT * FROM bookmarks WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    List<Bookmark> searchBookmarks(String query);

    @Query("SELECT * FROM bookmarks WHERE folderId = :folderId AND (title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%') ORDER BY createdAt DESC")
    List<Bookmark> searchBookmarksInFolder(long folderId, String query);

    @Query("SELECT COUNT(*) FROM bookmarks WHERE folderId = :folderId")
    int getBookmarkCount(long folderId);

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    List<Bookmark> getAllBookmarks();

    @Insert
    long insert(Bookmark bookmark);

    @Update
    void update(Bookmark bookmark);

    @Delete
    void delete(Bookmark bookmark);

    @Query("UPDATE bookmarks SET folderId = :newFolderId WHERE id = :bookmarkId")
    void moveBookmark(long bookmarkId, long newFolderId);
}
