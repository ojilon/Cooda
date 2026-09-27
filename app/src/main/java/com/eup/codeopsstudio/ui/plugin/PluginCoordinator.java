/*
 * This file is part of CodeOps Studio.
 * Copyright (C) 2024-2026 Etido Peter
 *
 * Extracted from MainFragment: bundled plugin discovery/install.
 * NATIVE_CANDIDATE: unzip + plugin layout is a strong JNI target later.
 */

package com.eup.codeopsstudio.ui.plugin;

import android.content.Context;

import androidx.annotation.NonNull;

import com.eup.codeopsstudio.R;
import com.eup.codeopsstudio.common.ILog;
import com.eup.codeopsstudio.common.archive.ZIPArchive;
import com.eup.codeopsstudio.common.util.FileUtil;
import com.eup.codeopsstudio.common.util.PreferencesUtils;

import java.io.File;
import java.io.IOException;

/**
 * Owns bundled plugin install (currently Eruda) so
 * {@link com.eup.codeopsstudio.MainFragment} only triggers it after storage is ready.
 */
public final class PluginCoordinator {

    private static final String TAG = "PluginCoordinator";
    private static final String ERUDA_ASSET = "plugins/eruda.min.zip";

    /**
     * Discover/install bundled plugins. Safe to call after storage permission is granted.
     */
    public void checkAndInstall(@NonNull Context context) {
        ILog.debug(TAG, context.getString(R.string.msg_checking_plugins));
        installErudaConsole(context);
    }

    // NATIVE_CANDIDATE: asset unzip / plugin tree may move behind JNI
    private void installErudaConsole(@NonNull Context context) {
        try {
            ILog.debug(TAG, context.getString(R.string.msg_installing_js_console_plugins));
            int bufferSize = PreferencesUtils.getCurrentBufferSize();
            File destDir = FileUtil.Path.PLUGINS_FOLDER;
            var archive = ZIPArchive.fromAssets(context, ERUDA_ASSET, destDir, bufferSize);
            archive.unzip();
        } catch (IOException e) {
            ILog.error(TAG, "Plugin installation failed: " + e.getMessage());
        }
    }
}
