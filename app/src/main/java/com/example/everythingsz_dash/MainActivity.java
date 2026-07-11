package com.example.everythingsz_dash;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load the folder list fragment on first launch
        if (savedInstanceState == null) {
            loadFragment(new FolderListFragment(), false);
        }
    }

    /**
     * Loads a fragment into the container.
     *
     * @param fragment     The fragment to load
     * @param addToBackStack Whether to add to the back stack for navigation
     */
    public void loadFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                )
                .replace(R.id.fragment_container, fragment);

        if (addToBackStack) {
            transaction.addToBackStack(null);
        }

        transaction.commit();
    }

    /**
     * Navigate to the bookmark list for a given folder.
     */
    public void openFolder(long folderId, String folderName) {
        BookmarkListFragment fragment = BookmarkListFragment.newInstance(folderId, folderName);
        loadFragment(fragment, true);
    }
}
