package com.frostbyte.launcher.fragments;

import com.frostbyte.launcher.modloaders.FabriclikeUtils;
import com.frostbyte.launcher.modloaders.ModloaderListenerProxy;

public class FabricInstallFragment extends FabriclikeInstallFragment {

    public static final String TAG = "FabricInstallFragment";

    public FabricInstallFragment() {
        super(FabriclikeUtils.FABRIC_UTILS, TAG);
    }
}
