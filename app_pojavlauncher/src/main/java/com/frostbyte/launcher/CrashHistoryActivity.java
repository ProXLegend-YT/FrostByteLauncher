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

    /**
     * Parses the timestamp out of "crash_yyyy-MM-dd_HH-mm-ss-SSS.txt" (the exact format
     * PojavApplication's crash handler writes). Falls back to the file's own lastModified() if
     * the name doesn't match — e.g. a file moved or renamed outside the app — so a single
     * unexpected filename can't crash the whole list, just lose precise ordering for that entry.
     */
    private long parseCrashFileTimestamp(File file) {
        String name = file.getName();
        try {
            String stamp = name.substring("crash_".length(), name.length() - ".txt".length());
            java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS", java.util.Locale.US);
            return format.parse(stamp).getTime();
        } catch (Exception e) {
            return file.lastModified();
        }
    }

    private void refresh() {
        File dir = historyDir();
        File[] files = dir.exists() ? dir.listFiles((d, name) -> name.endsWith(".txt")) : null;
        mReportFiles = files != null ? files : new File[0];
        // Sort and display using the timestamp encoded in the filename (crash_yyyy-MM-dd_HH-mm-ss-SSS.txt,
        // written in PojavApplication's crash handler) rather than File.lastModified() — several
        // crashes in quick succession (e.g. a crash loop right after relaunching) can round to
        // the same displayed minute under lastModified()'s coarser resolution, making genuinely
        // different crashes look like duplicates in the list.
        Arrays.sort(mReportFiles, Comparator.comparingLong(this::parseCrashFileTimestamp).reversed());

        boolean isEmpty = mReportFiles.length == 0;
        mEmptyText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        mListView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        List<String> labels = new ArrayList<>();
        for (File f : mReportFiles) {
            long timestamp = parseCrashFileTimestamp(f);
            String time = DateFormat.getDateFormat(this).format(timestamp) + " " +
                    DateFormat.getTimeFormat(this).format(timestamp);
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
