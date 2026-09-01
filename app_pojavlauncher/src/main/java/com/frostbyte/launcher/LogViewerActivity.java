package com.frostbyte.launcher;

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Shows the current game/launcher log file in-app, instead of only being able to
 * share it out to another app. Supports a simple case-insensitive search that
 * highlights matches, useful for quickly spotting "FATAL", "Exception", "zink", etc
 * after a crash without leaving the launcher.
 */
public class LogViewerActivity extends BaseActivity {

    @Override
    public boolean setFullscreen() {
        return false;
    }

    private TextView mLogText;
    private String mRawLogContent = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_viewer);

        mLogText = findViewById(R.id.log_viewer_text);
        mLogText.setTextIsSelectable(true);
        findViewById(R.id.log_viewer_back_button).setOnClickListener(v -> finish());

        loadLog();

        findViewById(R.id.log_viewer_share_button).setOnClickListener(v ->
                Tools.shareLog(this));

        findViewById(R.id.log_viewer_refresh_button).setOnClickListener(v -> loadLog());
    }

    private void loadLog() {
        File logFile = new File(Tools.DIR_GAME_HOME, "latestlog.txt");
        if (!logFile.isFile()) {
            mRawLogContent = getString(R.string.log_viewer_no_log);
            mLogText.setText(mRawLogContent);
            return;
        }
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            mRawLogContent = sb.toString();
            mLogText.setText(mRawLogContent);
        } catch (IOException e) {
            mRawLogContent = "";
            mLogText.setText(getString(R.string.log_viewer_read_error, e.getMessage()));
        }
    }

    private void highlightMatches(String query) {
        if (mRawLogContent.isEmpty()) return;
        if (query == null || query.isEmpty()) {
            mLogText.setText(mRawLogContent);
            return;
        }
        SpannableString spannable = new SpannableString(mRawLogContent);
        String lowerContent = mRawLogContent.toLowerCase();
        String lowerQuery = query.toLowerCase();
        int index = lowerContent.indexOf(lowerQuery);
        int firstMatch = -1;
        int highlightColor = androidx.core.content.ContextCompat.getColor(this, R.color.frostbyte_violet);
        while (index >= 0) {
            if (firstMatch == -1) firstMatch = index;
            spannable.setSpan(new BackgroundColorSpan(highlightColor), index, index + query.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            index = lowerContent.indexOf(lowerQuery, index + query.length());
        }
        mLogText.setText(spannable);
        if (firstMatch == -1) {
            Toast.makeText(this, R.string.log_viewer_no_matches, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_log_viewer, menu);
        MenuItem searchItem = menu.findItem(R.id.log_viewer_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                highlightMatches(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                highlightMatches(newText);
                return true;
            }
        });
        return true;
    }
}
