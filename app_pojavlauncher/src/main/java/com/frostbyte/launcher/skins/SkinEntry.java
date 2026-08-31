package com.frostbyte.launcher.skins;

/**
 * A single skin the user can preview and apply.
 * - Bundled presets come from assets/frostbyte_skins/*.png (see SkinManager.BUNDLED_PRESETS)
 * - Looked-up entries come from a Mojang username search and carry a remote texture URL
 */
public class SkinEntry {
    public enum Source { BUNDLED_ASSET, REMOTE_URL, LOCAL_FILE }

    public final Source source;
    public final String displayName;
    /** For BUNDLED_ASSET: path under assets/. For REMOTE_URL: the texture URL. For LOCAL_FILE: absolute path. */
    public final String reference;
    /** Only set for REMOTE_URL entries resolved from a username lookup */
    public final boolean isSlimModel;

    public SkinEntry(Source source, String displayName, String reference, boolean isSlimModel) {
        this.source = source;
        this.displayName = displayName;
        this.reference = reference;
        this.isSlimModel = isSlimModel;
    }
}
