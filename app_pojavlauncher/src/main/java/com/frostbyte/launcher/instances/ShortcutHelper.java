package com.frostbyte.launcher.instances;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.frostbyte.launcher.LauncherActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds Android home-screen shortcuts ("long-press the app icon") that jump straight
 * into launching a specific Minecraft instance, skipping the version picker.
 */
public class ShortcutHelper {

    public static final String EXTRA_LAUNCH_INSTANCE_ID = "frostbyte_launch_instance_id";

    /** Android allows a limited number of dynamic shortcuts; keep this comfortably under the cap. */
    private static final int MAX_DYNAMIC_SHORTCUTS = 4;

    /**
     * Refreshes the app's dynamic shortcut list to match the most recently used instances.
     * Safe to call often (e.g. whenever the instance list changes); it's cheap and idempotent.
     */
    public static void syncShortcuts(Context context) {
        try {
            List<Instance> instances = Instances.loadAllInstances();
            List<ShortcutInfoCompat> shortcuts = new ArrayList<>();

            int limit = Math.min(instances.size(), MAX_DYNAMIC_SHORTCUTS);
            for (int i = 0; i < limit; i++) {
                Instance instance = instances.get(i);
                shortcuts.add(buildShortcut(context, instance, i));
            }

            ShortcutManagerCompat.removeAllDynamicShortcuts(context);
            if (!shortcuts.isEmpty()) {
                ShortcutManagerCompat.addDynamicShortcuts(context, shortcuts);
            }
        } catch (IOException ignored) {
            // If instances fail to load, just leave shortcuts as they were; not worth crashing over.
        }
    }

    private static ShortcutInfoCompat buildShortcut(Context context, Instance instance, int rank) {
        Intent intent = new Intent(context, LauncherActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(EXTRA_LAUNCH_INSTANCE_ID, instance.name);

        Drawable iconDrawable = InstanceIconProvider.fetchIcon(context.getResources(), instance);
        IconCompat icon = drawableToIconCompat(iconDrawable);

        ShortcutInfoCompat.Builder builder = new ShortcutInfoCompat.Builder(context, "instance_" + instance.name)
                .setShortLabel(instance.name)
                .setLongLabel("Launch " + instance.name)
                .setIntent(intent)
                .setRank(rank);

        if (icon != null) builder.setIcon(icon);

        return builder.build();
    }

    private static IconCompat drawableToIconCompat(Drawable drawable) {
        if (drawable == null) return null;
        int size = Math.max(1, Math.max(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight()));
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        drawable.setBounds(0, 0, size, size);
        drawable.draw(canvas);
        return IconCompat.createWithBitmap(bitmap);
    }
}
