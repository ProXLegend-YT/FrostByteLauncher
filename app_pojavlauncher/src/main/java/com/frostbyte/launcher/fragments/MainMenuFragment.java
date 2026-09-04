package com.frostbyte.launcher.fragments;

import static com.frostbyte.launcher.Tools.openPath;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.frostbyte.launcher.mcgui.mcVersionSpinner;

import com.frostbyte.launcher.CustomControlsActivity;
import com.frostbyte.launcher.LogViewerActivity;
import com.frostbyte.launcher.PojavApplication;
import com.frostbyte.launcher.instances.ShortcutHelper;
import com.frostbyte.launcher.ScreenshotGalleryActivity;
import com.frostbyte.launcher.R;

import com.frostbyte.launcher.Tools;
import com.frostbyte.launcher.contracts.OpenDocumentWithExtension;
import com.frostbyte.launcher.extra.ExtraConstants;
import com.frostbyte.launcher.extra.ExtraCore;
import com.frostbyte.launcher.instances.Instance;
import com.frostbyte.launcher.instances.Instances;
import com.frostbyte.launcher.progresskeeper.ProgressKeeper;
import com.frostbyte.launcher.utils.FileUtils;

import java.io.File;

public class MainMenuFragment extends Fragment {
    public static final String TAG = "MainMenuFragment";

    private mcVersionSpinner mVersionSpinner;

    private final ActivityResultLauncher<Object> mModInstallerLauncher =
            registerForActivityResult(new OpenDocumentWithExtension("jar"), (data)->{
                if(data != null) Tools.launchModInstaller(requireContext(), data);
            });

    public MainMenuFragment(){
        super(R.layout.fragment_launcher);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button mDiscordButton = view.findViewById(R.id.social_media_button);
        Button mCustomControlButton = view.findViewById(R.id.custom_control_button);
        Button mInstallJarButton = view.findViewById(R.id.install_jar_button);
        Button mShareLogsButton = view.findViewById(R.id.share_logs_button);
        Button mScreenshotsButton = view.findViewById(R.id.screenshots_button);
        Button mOpenDirectoryButton = view.findViewById(R.id.open_files_button);

        ImageButton mEditProfileButton = view.findViewById(R.id.edit_profile_button);
        Button mPlayButton = view.findViewById(R.id.play_button);
        mVersionSpinner = view.findViewById(R.id.mc_version_spinner);

        mDiscordButton.setOnClickListener(v -> Tools.openURL(requireActivity(), getString(R.string.social_media_invite)));
        mDiscordButton.setOnLongClickListener((v)->{
            Tools.swapFragment(requireActivity(), GamepadMapperFragment.class, GamepadMapperFragment.TAG, null);
            return true;
        });
        mCustomControlButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), CustomControlsActivity.class)));
        mInstallJarButton.setOnClickListener(v -> runInstallerWithConfirmation());
        mEditProfileButton.setOnClickListener(v -> mVersionSpinner.openProfileEditor(requireActivity()));

        mPlayButton.setOnClickListener(v -> ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true));

        mShareLogsButton.setOnClickListener((v) -> startActivity(new Intent(requireContext(), LogViewerActivity.class)));
        mScreenshotsButton.setOnClickListener((v) -> startActivity(new Intent(requireContext(), ScreenshotGalleryActivity.class)));

        mOpenDirectoryButton.setOnClickListener((v)-> openGameDirectory(v.getContext()));


    }

    private void openGameDirectory(Context context) {
        Instance instance = Instances.loadSelectedInstance();
        if(instance == null) {
            Toast.makeText(context, R.string.no_instance, Toast.LENGTH_LONG).show();
            return;
        }
        File gameDirectory = instance.getGameDirectory();
        if(FileUtils.ensureDirectorySilently(gameDirectory)) {
            openPath(context, gameDirectory, false);
        }else {
            Toast.makeText(context, R.string.gamedir_open_failed, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ExtraCore.setValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, true);
        PojavApplication.sExecutorService.submit(() -> ShortcutHelper.syncShortcuts(requireContext().getApplicationContext()));
    }

    private void runInstallerWithConfirmation() {
        if (ProgressKeeper.getTaskCount() == 0) {
            mModInstallerLauncher.launch(null);
        } else Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
    }
}
