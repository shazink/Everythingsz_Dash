package com.example.everythingsz_dash.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "bookmarks",
        foreignKeys = @ForeignKey(
                entity = Folder.class,
                parentColumns = "id",
                childColumns = "folderId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index(value = "folderId"))
public class Bookmark {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String title;

    private String url;

    private long folderId;

    private long createdAt;

    public Bookmark() {
        this.createdAt = System.currentTimeMillis();
    }

    public Bookmark(String title, String url, long folderId) {
        this.title = title;
        this.url = url;
        this.folderId = folderId;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getFolderId() {
        return folderId;
    }

    public void setFolderId(long folderId) {
        this.folderId = folderId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
