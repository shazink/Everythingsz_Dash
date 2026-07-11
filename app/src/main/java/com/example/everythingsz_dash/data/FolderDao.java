package com.example.everythingsz_dash.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY name ASC")
    List<Folder> getAllFolders();

    @Query("SELECT * FROM folders WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    List<Folder> searchFolders(String query);

    @Query("SELECT * FROM folders WHERE id = :id")
    Folder getFolderById(long id);

    @Query("SELECT * FROM folders WHERE name = :name LIMIT 1")
    Folder getFolderByName(String name);

    @Insert
    long insert(Folder folder);

    @Update
    void update(Folder folder);

    @Delete
    void delete(Folder folder);
}
