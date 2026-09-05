package com.frostbyte.launcher.customcontrols.buttons;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.TypedValue;

import androidx.core.graphics.ColorUtils;

public class BackgroundTint {
    // Slightly more transparent than before across the board, so buttons sit lightly over
    // gameplay instead of feeling like solid opaque blocks; the toggle button (e.g. sneak) stays
    // proportionally brighter than normal buttons so its "on" state is still clearly visible,
    // just not jarringly more opaque than everything else around it.
    public static final int BACKGROUND_DEFAULT_TINT_ALPHA = 110;
    public static final int BACKGROUND_TOGGLE_TINT_ALPHA = 140;

    // FrostByte's accent blue, matching the rest of the app's UI, instead of plain white — same
    // alpha values as before are kept so button opacity/tap visibility in-game is unchanged.
    private static final int FROSTBYTE_CONTROL_TINT = Color.parseColor("#5AD1FF");

    private static int lastTheme = System.identityHashCode(BackgroundTint.class);

    private static final int[][] sState = new int[][] {
            new int[] {android.R.attr.state_activated}
    };
    private static final int[] sDefaultTint = new int[] {
            ColorUtils.setAlphaComponent(FROSTBYTE_CONTROL_TINT, BACKGROUND_DEFAULT_TINT_ALPHA)
    };
    private static final int[] sToggleableTint = new int[] {
            ColorUtils.setAlphaComponent(FROSTBYTE_CONTROL_TINT, BACKGROUND_TOGGLE_TINT_ALPHA)
    };

    public static final ColorStateList DEFAULT_TINT_LIST = new ColorStateList(
            sState, sDefaultTint
    );
    public static final ColorStateList TOGGLE_TINT_LIST = new ColorStateList(
            sState, sToggleableTint
    );

    public static void applyToggleTint(Context context) {
        Resources.Theme theme = context.getTheme();
        int themeHash = theme.hashCode();
        if(themeHash == lastTheme) return;
        final TypedValue value = new TypedValue();
        theme.resolveAttribute(android.R.attr.colorAccent, value, true);
        sToggleableTint[0] = ColorUtils.setAlphaComponent(value.data, BACKGROUND_TOGGLE_TINT_ALPHA);
        lastTheme = themeHash;
    }
}
