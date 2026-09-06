package com.frostbyte.launcher;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.frostbyte.launcher.worlds.WorldBackupManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A genuinely new capability, not a reskin: zip world backups you can restore later, entirely
 * offline and local. Neither PojavLauncher nor MojoLauncher provide this. Kept deliberately
 * simple — two lists (worlds you can back up, backups you can restore or delete) — rather than
 * a bigger redesign, matching this session's approach of shipping one complete, verified thing
 * at a time instead of broad, half-finished surface area.
 */
public class WorldBackupsActivity extends BaseActivity {

    @Override
    public boolean setFullscreen() {
        return false;
    }

    private WorldBackupManager mManager;
    private ListView mWorldsList;
    private ListView mBackupsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_world_backups);

        findViewById(R.id.backups_back_button).setOnClickListener(v -> finish());

        File savesDir = new File(Tools.DIR_GAME_NEW, "saves");
        File backupsDir = new File(Tools.DIR_GAME_HOME, "world_backups");
        mManager = new WorldBackupManager(backupsDir);

        mWorldsList = findViewById(R.id.backups_worlds_list);
        mBackupsList = findViewById(R.id.backups_existing_list);

        refreshWorlds(savesDir);
        refreshBackups();
    }

    private void refreshWorlds(File savesDir) {
        List<String> names = new ArrayList<>();
        File[] worldDirs = savesDir.listFiles(File::isDirectory);
        if (worldDirs != null) {
            for (File dir : worldDirs) names.add(dir.getName());
        }

        if (names.isEmpty()) {
            names.add(getString(R.string.world_backups_no_worlds));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
        mWorldsList.setAdapter(adapter);
        if (worldDirs != null) {
            mWorldsList.setOnItemClickListener((parent, view, position, id) -> {
                File worldDir = worldDirs[position];
                confirmBackup(worldDir, savesDir);
            });
        }
    }

    private void confirmBackup(File worldDir, File savesDir) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.world_backups_confirm_title))
                .setMessage(getString(R.string.world_backups_confirm_message, worldDir.getName()))
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    WorldBackupManager.BackupResult result = mManager.backup(worldDir);
                    if (result instanceof WorldBackupManager.BackupResult.Success) {
                        Toast.makeText(this, R.string.world_backups_backup_success, Toast.LENGTH_SHORT).show();
                        refreshBackups();
                    } else {
                        String reason = ((WorldBackupManager.BackupResult.Failure) result).reason;
                        Toast.makeText(this, getString(R.string.world_backups_backup_failed, reason), Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void refreshBackups() {
        List<WorldBackupManager.WorldBackupInfo> backups = mManager.listBackups();
        List<String> labels = new ArrayList<>();
        for (WorldBackupManager.WorldBackupInfo info : backups) {
            String date = DateFormat.getDateFormat(this).format(info.createdAtEpochMillis) + " " +
                    DateFormat.getTimeFormat(this).format(info.createdAtEpochMillis);
            String size = Formatter.formatShortFileSize(this, info.sizeBytes);
            labels.add(info.worldName + "  ·  " + date + "  ·  " + size);
        }
        if (labels.isEmpty()) {
            labels.add(getString(R.string.world_backups_no_backups));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
        mBackupsList.setAdapter(adapter);
        if (!backups.isEmpty()) {
            mBackupsList.setOnItemClickListener((parent, view, position, id) ->
                    showBackupActions(backups.get(position)));
        }
    }

    private void showBackupActions(WorldBackupManager.WorldBackupInfo info) {
        new AlertDialog.Builder(this)
                .setTitle(info.worldName)
                .setItems(new CharSequence[]{
                        getString(R.string.world_backups_action_restore),
                        getString(R.string.world_backups_action_delete)
                }, (dialog, which) -> {
                    if (which == 0) confirmRestore(info);
                    else confirmDelete(info);
                })
                .show();
    }

    private void confirmRestore(WorldBackupManager.WorldBackupInfo info) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.world_backups_confirm_title))
                .setMessage(getString(R.string.world_backups_restore_warning, info.worldName))
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    File destination = new File(new File(Tools.DIR_GAME_NEW, "saves"), info.worldName);
                    WorldBackupManager.RestoreResult result = mManager.restore(info.file, destination);
                    if (result instanceof WorldBackupManager.RestoreResult.Success) {
                        Toast.makeText(this, R.string.world_backups_restore_success, Toast.LENGTH_SHORT).show();
                        refreshWorlds(new File(Tools.DIR_GAME_NEW, "saves"));
                    } else {
                        String reason = ((WorldBackupManager.RestoreResult.Failure) result).reason;
                        Toast.makeText(this, getString(R.string.world_backups_restore_failed, reason), Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void confirmDelete(WorldBackupManager.WorldBackupInfo info) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.world_backups_confirm_title))
                .setMessage(getString(R.string.world_backups_delete_warning, info.worldName))
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    mManager.deleteBackup(info.file);
                    refreshBackups();
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }
}
