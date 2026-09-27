/*
 * This file is part of CodeOps Studio.
 * Copyright (C) 2024-2026 Etido Peter
 *
 * Extracted from MainFragment: stored + FCM-driven app update prompts.
 */

package com.eup.codeopsstudio.ui.fcm;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import com.eup.codeopsstudio.common.Constants;
import com.eup.codeopsstudio.common.ILog;
import com.eup.codeopsstudio.common.util.PreferencesUtils;
import com.eup.codeopsstudio.util.Wizard;
import com.eup.codeopsstudio.util.versioning.VersionManager;

/**
 * Owns FCM/stored update payloads and {@link UpdateBottomSheet} display so
 * {@link com.eup.codeopsstudio.MainFragment} stays focused on IDE coordination.
 */
public final class AppUpdateCoordinator {

    private static final String TAG = "AppUpdateCoordinator";

    public interface Host {
        @NonNull
        FragmentManager childFragmentManager();

        boolean canShowUi();
    }

    private final Host host;

    public AppUpdateCoordinator(@NonNull Host host) {
        this.host = host;
    }

    public void handleIntentBundle(@NonNull Bundle bundle) {
        if (!host.canShowUi()) {
            return;
        }
        String type = bundle.getString(Constants.FCM_NOTIFICATION_TYPE);
        if (type != null) {
            handleAppUpdateNotification(bundle);
        }
    }

    public void checkForStoredAppUpdates() {
        SharedPreferences prefs = PreferencesUtils.getAppUpdatePreferences();
        ILog.debug(TAG, "#checkForStoredAppUpdates");

        String minVersion = prefs.getString(Constants.PREF_UPDATE_MIN_VERSION, "");
        String latestVersion = prefs.getString(Constants.PREF_UPDATE_VERSION, "");
        String changeLog = prefs.getString(Constants.PREF_UPDATE_CHANGELOG, "");
        String downloadUrl = prefs.getString(Constants.PREF_UPDATE_DOWNLOAD_URL, "");
        String downloadSize = prefs.getString(Constants.PREF_UPDATE_DOWNLOAD_SIZE, "");
        boolean forceUpdate =
                Wizard.toBoolean(prefs.getString(Constants.PREF_UPDATE_FORCED, "false"));

        long lastRemindTime = prefs.getLong(Constants.PREF_LAST_REMIND_TIME, 0L);
        long currentTime = System.currentTimeMillis();
        if (lastRemindTime > 0 && (currentTime - lastRemindTime < Constants.REMIND_INTERVAL_MS)) {
            return;
        }

        maybeShowUpdate(minVersion, downloadUrl, changeLog, latestVersion, forceUpdate, downloadSize);
    }

    private void handleAppUpdateNotification(@NonNull Bundle bundle) {
        ILog.debug(TAG, "#handleAppUpdateNotification");

        String changeLog = bundle.getString(Constants.KEY_CHANGELOG);
        String minVersion = bundle.getString(Constants.KEY_MIN_VERSION);
        String downloadUrl = bundle.getString(Constants.KEY_DOWNLOAD_URL);
        String latestVersion = bundle.getString(Constants.KEY_UPDATE_VERSION);
        String downloadSize = bundle.getString(Constants.KEY_UPDATE_DOWNLOAD_SIZE);
        boolean forceUpdate = Wizard.toBoolean(bundle.getString(Constants.KEY_FORCE_UPDATE));

        maybeShowUpdate(minVersion, downloadUrl, changeLog, latestVersion, forceUpdate, downloadSize);
    }

    private void maybeShowUpdate(
            String minVersion,
            String downloadUrl,
            String changeLog,
            String latestVersion,
            boolean forceUpdate,
            String downloadSize) {
        ILog.debug(TAG, "Update Check: Version=" + latestVersion + ", URL=" + downloadUrl);
        if (Wizard.isEmpty(latestVersion) || Wizard.isEmpty(downloadUrl)) {
            return;
        }
        if (VersionManager.isForceUpdateRequired(minVersion)) {
            showUpdateBottomSheet(
                    minVersion, downloadUrl, changeLog, latestVersion, true, downloadSize);
        } else if (VersionManager.isUpdateAvailable(latestVersion)) {
            showUpdateBottomSheet(
                    minVersion, downloadUrl, changeLog, latestVersion, forceUpdate, downloadSize);
        }
    }

    private void showUpdateBottomSheet(
            String minVersion,
            String downloadUrl,
            String changeLog,
            String version,
            boolean forceUpdate,
            String downloadSize) {
        FragmentManager fragmentManager = host.childFragmentManager();
        var fragment =
                (BottomSheetDialogFragment)
                        fragmentManager.findFragmentByTag(UpdateBottomSheet.TAG);
        if (fragment != null && fragment.isVisible()) {
            ILog.debug(TAG, "Update sheet already visible");
            return;
        }
        var bottomSheet =
                UpdateBottomSheet.newInstance(
                        minVersion, version, changeLog, downloadUrl, forceUpdate, downloadSize);
        bottomSheet.show(fragmentManager, UpdateBottomSheet.TAG);
    }
}
