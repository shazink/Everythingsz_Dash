package com.example.everythingsz_dash.data;

import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Handles exporting and importing bookmarks as JSON files.
 * All methods here run synchronously — call them from a background thread.
 */
public class BackupManager {

    private final FolderDao folderDao;
    private final BookmarkDao bookmarkDao;

    public BackupManager(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.folderDao = db.folderDao();
        this.bookmarkDao = db.bookmarkDao();
    }

    /**
     * Exports all folders and bookmarks to a JSON file at the given URI.
     *
     * @return true if successful
     */
    public boolean exportToUri(Context context, Uri uri) {
        try {
            JSONObject root = new JSONObject();
            root.put("app", "Everythingsz Dash");
            root.put("version", 1);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            root.put("exportedAt", sdf.format(new Date()));

            List<Folder> folders = folderDao.getAllFolders();
            JSONArray foldersArray = new JSONArray();

            for (Folder folder : folders) {
                JSONObject folderJson = new JSONObject();
                folderJson.put("name", folder.getName());
                folderJson.put("createdAt", folder.getCreatedAt());

                List<Bookmark> bookmarks = bookmarkDao.getBookmarksByFolder(folder.getId());
                JSONArray bookmarksArray = new JSONArray();

                for (Bookmark bookmark : bookmarks) {
                    JSONObject bookmarkJson = new JSONObject();
                    bookmarkJson.put("title", bookmark.getTitle());
                    bookmarkJson.put("url", bookmark.getUrl());
                    bookmarkJson.put("createdAt", bookmark.getCreatedAt());
                    bookmarksArray.put(bookmarkJson);
                }

                folderJson.put("bookmarks", bookmarksArray);
                foldersArray.put(folderJson);
            }

            root.put("folders", foldersArray);

            OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
            if (outputStream == null) return false;

            outputStream.write(root.toString(2).getBytes("UTF-8"));
            outputStream.flush();
            outputStream.close();

            return true;
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Imports folders and bookmarks from a JSON file at the given URI.
     * Merges into existing data: if a folder with the same name exists,
     * bookmarks are added to it; otherwise a new folder is created.
     *
     * @return the number of bookmarks imported, or -1 on error
     */
    public int importFromUri(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return -1;

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            JSONObject root = new JSONObject(sb.toString());

            // Validate format
            if (!root.has("folders")) return -1;

            JSONArray foldersArray = root.getJSONArray("folders");
            int totalImported = 0;

            for (int i = 0; i < foldersArray.length(); i++) {
                JSONObject folderJson = foldersArray.getJSONObject(i);
                String folderName = folderJson.getString("name");

                // Check if folder already exists
                Folder existingFolder = folderDao.getFolderByName(folderName);
                long folderId;

                if (existingFolder != null) {
                    folderId = existingFolder.getId();
                } else {
                    Folder newFolder = new Folder(folderName);
                    if (folderJson.has("createdAt")) {
                        newFolder.setCreatedAt(folderJson.getLong("createdAt"));
                    }
                    folderId = folderDao.insert(newFolder);
                }

                // Import bookmarks into this folder
                if (folderJson.has("bookmarks")) {
                    JSONArray bookmarksArray = folderJson.getJSONArray("bookmarks");
                    for (int j = 0; j < bookmarksArray.length(); j++) {
                        JSONObject bookmarkJson = bookmarksArray.getJSONObject(j);

                        Bookmark bookmark = new Bookmark(
                                bookmarkJson.getString("title"),
                                bookmarkJson.getString("url"),
                                folderId
                        );
                        if (bookmarkJson.has("createdAt")) {
                            bookmark.setCreatedAt(bookmarkJson.getLong("createdAt"));
                        }

                        bookmarkDao.insert(bookmark);
                        totalImported++;
                    }
                }
            }

            return totalImported;
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            return -1;
        }
    }
}
