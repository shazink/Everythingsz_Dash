package com.example.everythingsz_dash.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "folders")
public class Folder {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String name;

    private long createdAt;

    public Folder() {
        this.createdAt = System.currentTimeMillis();
    }

    public Folder(String name) {
        this.name = name;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
