package com.frostbyte.launcher.fragments;

import com.frostbyte.launcher.modloaders.FabriclikeUtils;
import com.frostbyte.launcher.modloaders.ModloaderListenerProxy;

public class QuiltInstallFragment extends FabriclikeInstallFragment {

    public static final String TAG = "QuiltInstallFragment";
    private static ModloaderListenerProxy sTaskProxy;

    public QuiltInstallFragment() {
        super(FabriclikeUtils.QUILT_UTILS, TAG);
    }
}
