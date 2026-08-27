package com.frostbyte.launcher.modloaders.modpacks.api;

import com.frostbyte.launcher.mcgui.ProgressLayout;

import com.frostbyte.launcher.R;
import com.frostbyte.launcher.Tools;
import com.frostbyte.launcher.instances.InstanceInstaller;
import com.frostbyte.launcher.instances.Instances;
import com.frostbyte.launcher.instances.Instance;
import com.frostbyte.launcher.modloaders.modpacks.imagecache.ModIconCache;
import com.frostbyte.launcher.modloaders.modpacks.models.ModDetail;
import com.frostbyte.launcher.progresskeeper.DownloaderProgressWrapper;
import com.frostbyte.launcher.utils.DownloadUtils;
import com.frostbyte.launcher.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.Callable;

public class ModpackInstaller {

    public static ModLoader installModpack(String modpackName, String title, File modpackFile, String icon, InstallFunction installFunction) throws IOException {
        // Build a new minecraft instance, folder first
        ModLoader modLoaderInfo;
        Instance instance = Instances.createInstance(i-> i.name = title, modpackName.substring(0, Math.min(16,modpackName.length())));
        try {
            // Install the modpack
            modLoaderInfo = installFunction.installModpack(modpackFile, instance.getGameDirectory());

            if(modLoaderInfo == null) throw new IOException("Unknown modpack mod loader information");

            if(modLoaderInfo.requiresGuiInstallation()) {
                InstanceInstaller instanceInstaller = modLoaderInfo.createInstaller();
                if(instanceInstaller == null) throw new IOException("Failed to prepare data for instance installation");
                instance.installer = instanceInstaller;
            } else {
                String versionId = modLoaderInfo.installHeadlessly();
                if(versionId == null) throw new IOException("Unknown mod loader version");
                instance.versionId = versionId;
            }
            instance.write();
            ModIconCache.writeInstanceImage(instance, icon);

            Instances.setSelectedInstance(instance);
            if(modLoaderInfo.requiresGuiInstallation()) {
                instance.installer.start();
            }
        } catch (IOException e) {
            Instances.removeInstance(instance);
            throw e;
        } finally {
            modpackFile.delete();
            ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK);
        }

        return modLoaderInfo;
    }

    public static ModLoader downloadModpack(ModDetail modDetail, int selectedVersion, InstallFunction installFunction) throws IOException {
        String versionUrl = modDetail.versionUrls[selectedVersion];
        String versionHash = modDetail.versionHashes[selectedVersion];
        String modpackName = FileUtils.escapeFileName(modDetail.title.toLowerCase(Locale.ROOT) + " " + modDetail.versionNames[selectedVersion]);
        String name = modDetail.title;
        String icon = modDetail.getIconCacheTag();

        if (versionHash != null) {
            modpackName += "_" + versionHash;
        }

        if (modpackName.length() > 255){
            modpackName = modpackName.substring(0,255);
        }

        File modpackFile = new File(Tools.DIR_CACHE, modpackName + ".cf");

        byte[] downloadBuffer = new byte[8192];
        try {
            DownloadUtils.ensureSha1(modpackFile, versionHash, (Callable<Void>) () -> {
                DownloadUtils.downloadFileMonitored(versionUrl, modpackFile, downloadBuffer,
                        new DownloaderProgressWrapper(R.string.modpack_download_downloading_metadata,
                                ProgressLayout.INSTALL_MODPACK
                        )
                );
                return null;
            });
        } catch (IOException e) {
            modpackFile.delete();
            throw e;
        }

        return installModpack(modpackName, name, modpackFile, icon, installFunction);
    }

    public interface InstallFunction {
        ModLoader installModpack(File modpackFile, File instanceDestination) throws IOException;
    }
}
