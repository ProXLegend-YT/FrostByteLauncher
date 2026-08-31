package com.frostbyte.launcher;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.frostbyte.launcher.instances.Instance;
import com.frostbyte.launcher.instances.Instances;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Browse, share, and delete screenshots taken in the currently selected instance.
 * Screenshots live under that instance's own game directory, since each instance
 * (or the shared data folder, if the instance uses shared data) keeps its own set.
 */
public class ScreenshotGalleryActivity extends BaseActivity {

    private GridView mGrid;
    private ScreenshotAdapter mAdapter;
    private File mScreenshotsDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screenshot_gallery);

        mGrid = findViewById(R.id.screenshot_grid);
        TextView emptyText = findViewById(R.id.screenshot_gallery_empty);

        Instance instance = Instances.loadSelectedInstance();
        if (instance == null) {
            emptyText.setText(R.string.no_instance);
            emptyText.setVisibility(View.VISIBLE);
            return;
        }

        mScreenshotsDir = new File(instance.getGameDirectory(), "screenshots");
        mAdapter = new ScreenshotAdapter();
        mGrid.setAdapter(mAdapter);
        refresh(emptyText);
    }

    private void refresh(TextView emptyText) {
        List<File> screenshots = new ArrayList<>();
        File[] files = mScreenshotsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (files != null) {
            screenshots.addAll(Arrays.asList(files));
        }
        screenshots.sort(Comparator.comparingLong(File::lastModified).reversed());

        mAdapter.setFiles(screenshots);
        emptyText.setVisibility(screenshots.isEmpty() ? View.VISIBLE : View.GONE);
        if (screenshots.isEmpty()) emptyText.setText(R.string.screenshot_gallery_empty);
    }

    private void openFullSize(File file) {
        new AlertDialog.Builder(this)
                .setView(buildFullSizeView(file))
                .setPositiveButton(R.string.log_viewer_share, (dialog, which) -> shareScreenshot(file))
                .setNegativeButton(R.string.screenshot_delete, (dialog, which) -> confirmDelete(file))
                .setNeutralButton(android.R.string.cancel, null)
                .show();
    }

    private View buildFullSizeView(File file) {
        ImageView imageView = new ImageView(this);
        imageView.setAdjustViewBounds(true);
        imageView.setImageBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));
        return imageView;
    }

    private void shareScreenshot(File file) {
        Tools.openPath(this, file, true);
    }

    private void confirmDelete(File file) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.screenshot_delete)
                .setMessage(R.string.screenshot_delete_confirm)
                .setPositiveButton(R.string.screenshot_delete, (dialog, which) -> {
                    if (file.delete()) {
                        refresh(findViewById(R.id.screenshot_gallery_empty));
                    } else {
                        Toast.makeText(this, R.string.screenshot_delete_failed, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private class ScreenshotAdapter extends BaseAdapter {
        private final List<File> mFiles = new ArrayList<>();

        void setFiles(List<File> files) {
            mFiles.clear();
            mFiles.addAll(files);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mFiles.size();
        }

        @Override
        public File getItem(int position) {
            return mFiles.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView instanceof ImageView) {
                imageView = (ImageView) convertView;
            } else {
                imageView = new ImageView(ScreenshotGalleryActivity.this);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setLayoutParams(new GridView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 300));
                int pad = 4;
                imageView.setPadding(pad, pad, pad, pad);
            }
            File file = mFiles.get(position);
            imageView.setImageBitmap(decodeSampled(file));
            imageView.setOnClickListener(v -> openFullSize(file));
            return imageView;
        }

        private Bitmap decodeSampled(File file) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4; // thumbnail-sized, avoid loading full-res screenshots into a grid
            return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        }
    }
}
