package com.frostbyte.launcher.instances;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.frostbyte.launcher.R;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class InstanceIconProvider {
    public static final String FALLBACK_ICON_NAME = "default";
    private static final Map<String, Drawable> sIconCache = new ConcurrentHashMap<>();
    private static final Map<String, Drawable> sStaticIconCache = new ConcurrentHashMap<>();
    private static final Map<String, Integer> sStaticIcons = new ConcurrentHashMap<>();

    static {
        sStaticIcons.put("default", R.drawable.ic_frostbyte_full);
        sStaticIcons.put("fabric", R.drawable.ic_fabric);
        sStaticIcons.put("quilt", R.drawable.ic_quilt);
        sStaticIcons.put("forge", R.drawable.ic_forge);
        sStaticIcons.put("neoforge", R.drawable.ic_neoforge);
    }

    /**
     * Fetch an icon from the cache, or load it if it's not cached.
     * @param resources the Resources object, used for creating drawables
     * @param instance the instance
     * @return an icon drawable
     */
    public static @NonNull Drawable fetchIcon(Resources resources, @NonNull DisplayInstance instance) {
        String cacheKey = instanceCacheKey(instance);

        Drawable cachedIcon = sIconCache.get(cacheKey);
        if(cachedIcon != null) return cachedIcon;

        Drawable instanceIcon = fetchInstanceFileIcon(resources, cacheKey, instance.getInstanceIconLocation());
        if(instanceIcon != null) return instanceIcon;

        return fetchStaticIcon(resources, cacheKey, instance.icon);
    }

    /**
     * Drop an icon from the icon cache. When dropped, it's Drawable will be re-read from the
     * instance icon file (or re-fetched from the static cache)
     * @param key the instance
     */
    public static void dropIcon(@NonNull Instance key) {
        sIconCache.remove(instanceCacheKey(key));
    }

    private static String instanceCacheKey(DisplayInstance instance) {
        // Instances are frequently reloaded fresh from disk as new objects, so a stable
        // identifier (name) is used here rather than System.identityHashCode, which could
        // collide or get reused across garbage collections and cause icons to intermittently
        // fail to show.
        return instance.name != null ? instance.name : ("unnamed_" + System.identityHashCode(instance));
    }

    private static Drawable fetchInstanceFileIcon(Resources resources, String cacheKey, File iconLocation) {
        if(!iconLocation.isFile() || !iconLocation.canRead()) return null;
        Bitmap iconBitmap = BitmapFactory.decodeFile(iconLocation.getAbsolutePath());
        if(iconBitmap == null) return null;
        Drawable iconDrawable = new BitmapDrawable(resources, iconBitmap);
        sIconCache.put(cacheKey, iconDrawable);
        return iconDrawable;
    }

    private static Drawable fetchStaticIcon(Resources resources, String cacheKey, String icon) {
        String safeIcon = (icon != null) ? icon : FALLBACK_ICON_NAME;
        Drawable staticIcon = sStaticIconCache.get(safeIcon);
        if(staticIcon == null) {
            staticIcon = getStaticIcon(resources, safeIcon);
            if(staticIcon == null) staticIcon = fetchFallbackIcon(resources);
            sStaticIconCache.put(safeIcon, staticIcon);
        }
        sIconCache.put(cacheKey, staticIcon);
        return staticIcon;
    }

    private static @NonNull Drawable fetchFallbackIcon(Resources resources) {
        Drawable fallbackIcon = sStaticIconCache.get(FALLBACK_ICON_NAME);
        if(fallbackIcon == null) {
            fallbackIcon = Objects.requireNonNull(getStaticIcon(resources, FALLBACK_ICON_NAME));
            sStaticIconCache.put(FALLBACK_ICON_NAME, fallbackIcon);
        }
        return fallbackIcon;
    }

    private static Drawable getStaticIcon(Resources resources, @NonNull String icon) {
        int staticIconResource = getStaticIconResource(icon);
        if(staticIconResource == -1) return null;
        return ResourcesCompat.getDrawable(resources, staticIconResource, null);
    }

    private static int getStaticIconResource(String icon) {
        Integer iconResource = sStaticIcons.get(icon);
        if(iconResource == null) return -1;
        return iconResource;
    }

    /**
     * Check whether the icon under the specified name is a static icon available in the provider.
     * @param name static icon name to check
     * @return whether the icon is available or not
     */
    public static boolean hasStaticIcon(String name) {
        return sStaticIcons.containsKey(name);
    }
}
