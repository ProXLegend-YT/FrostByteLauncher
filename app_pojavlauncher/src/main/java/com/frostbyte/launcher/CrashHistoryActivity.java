package com.frostbyte.launcher;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Real crash history, not just the single most-recent one: PojavApplication's crash handler
 * previously only ever wrote "latestcrash.txt", so a second crash silently overwrote the first
 * with no way to look back. That single-file write is untouched — this only adds a parallel
 * timestamped copy per crash into a "crash_history" folder, and this screen lists them.
 */
public class CrashHistoryActivity extends BaseActivity {

    @Override
    public boolean setFullscreen() {
        return false;
    }

    private File[] mReportFiles = new File[0];
    private ListView mListView;
    private View mEmptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash_history);

        findViewById(R.id.crash_history_back_button).setOnClickListener(v -> finish());
        mListView = findViewById(R.id.crash_history_list);
        mEmptyText = findViewById(R.id.crash_history_empty_text);

        refresh();
    }

    private File historyDir() {
        return new File(Tools.DIR_GAME_HOME, "crash_history");
    }

    private void refresh() {
        File dir = historyDir();
        File[] files = dir.exists() ? dir.listFiles((d, name) -> name.endsWith(".txt")) : null;
        mReportFiles = files != null ? files : new File[0];
        // Newest first, matching how the world backups list orders entries.
        Arrays.sort(mReportFiles, Comparator.comparingLong(File::lastModified).reversed());

        boolean isEmpty = mReportFiles.length == 0;
        mEmptyText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        mListView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        List<String> labels = new ArrayList<>();
        for (File f : mReportFiles) {
            String time = DateFormat.getDateFormat(this).format(f.lastModified()) + " " +
                    DateFormat.getTimeFormat(this).format(f.lastModified());
            labels.add(time);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_world_backup_row, labels);
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener((parent, view, position, id) -> showReportActions(mReportFiles[position]));
    }

    private void showReportActions(File reportFile) {
        new AlertDialog.Builder(this)
                .setTitle(reportFile.getName())
                .setItems(new CharSequence[]{
                        getString(R.string.crash_history_action_view),
                        getString(R.string.crash_history_action_share),
                        getString(R.string.crash_history_action_delete)
                }, (dialog, which) -> {
                    if (which == 0) viewReport(reportFile);
                    else if (which == 1) shareReport(reportFile);
                    else confirmDelete(reportFile);
                })
                .show();
    }

    private void viewReport(File reportFile) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(reportFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.crash_history_read_failed, e.getMessage()), Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(reportFile.getName())
                .setMessage(content.toString())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void shareReport(File reportFile) {
        try {
            Tools.openPath(this, reportFile, true);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.crash_history_share_failed, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDelete(File reportFile) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.world_backups_confirm_title))
                .setMessage(getString(R.string.crash_history_delete_warning))
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    //noinspection ResultOfMethodCallIgnored
                    reportFile.delete();
                    refresh();
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }
}
