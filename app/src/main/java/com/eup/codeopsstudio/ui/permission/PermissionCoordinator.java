/*
 * This file is part of CodeOps Studio.
 * Copyright (C) 2024-2026 Etido Peter
 *
 * Extracted from MainFragment: storage + notification permission flows.
 */

package com.eup.codeopsstudio.ui.permission;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.eup.codeopsstudio.R;
import com.eup.codeopsstudio.common.ILog;
import com.eup.codeopsstudio.util.BaseUtil;
import com.eup.codeopsstudio.util.Wizard;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Owns runtime storage and notification permission requests/dialogs so
 * {@link com.eup.codeopsstudio.MainFragment} stays focused on IDE coordination.
 *
 * <p>Call {@link #attach(Fragment)} from {@code onCreate} so launchers are registered
 * before the fragment reaches STARTED.
 */
public final class PermissionCoordinator {

    private static final String TAG = "PermissionCoordinator";

    /** Fragment surface needed for context, activity, and rationale checks. */
    public interface Host {
        @NonNull
        FragmentActivity requireActivity();

        @NonNull
        android.content.Context requireContext();

        boolean shouldShowRequestPermissionRationale(@NonNull String permission);

        /** Invoked when storage is already granted (or becomes usable). */
        void onStorageReady();

        /** User declined storage after denial dialog (exit path). */
        void onStorageDeniedExit();
    }

    private final Host host;

    private ActivityResultLauncher<Intent> storageLauncherApi30;
    private ActivityResultLauncher<String[]> storageLauncherApi19;
    private ActivityResultLauncher<String> notificationLauncherApi33;

    public PermissionCoordinator(@NonNull Host host) {
        this.host = host;
    }

    /**
     * Registers activity-result launchers. Must be called from the fragment's {@code onCreate}.
     */
    public void attach(@NonNull Fragment fragment) {
        storageLauncherApi30 =
                fragment.registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result != null
                                    && !Wizard.isStoragePermissionGranted(host.requireActivity())) {
                                showStoragePermissionDeniedDialog(
                                        this::requestStoragePermission, host::onStorageDeniedExit);
                            } else if (Wizard.isStoragePermissionGranted(host.requireActivity())) {
                                host.onStorageReady();
                            }
                        });

        storageLauncherApi19 =
                fragment.registerForActivityResult(
                        new ActivityResultContracts.RequestMultiplePermissions(),
                        isGranted -> {
                            if (isGranted.containsValue(false)) {
                                showStoragePermissionDeniedDialog(
                                        this::requestStoragePermission, host::onStorageDeniedExit);
                            } else {
                                host.onStorageReady();
                            }
                        });

        notificationLauncherApi33 =
                fragment.registerForActivityResult(
                        new ActivityResultContracts.RequestPermission(),
                        isGranted -> {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                return;
                            }
                            if (Boolean.TRUE.equals(isGranted)) {
                                BaseUtil.toastLong(R.string.msg_notification_permission_granted);
                            } else {
                                if (host.shouldShowRequestPermissionRationale(
                                        Manifest.permission.POST_NOTIFICATIONS)) {
                                    showNotificationPermissionRationale();
                                } else {
                                    showNotificationSettingsRationale();
                                }
                            }
                        });
    }

    /** Request storage if missing; otherwise notify host that storage is ready. */
    public void ensureStoragePermission() {
        if (Wizard.isStoragePermissionGranted(host.requireContext())) {
            host.onStorageReady();
        } else {
            requestStoragePermission();
        }
    }

    public void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Wizard.requestStoragePermissionApi30(host.requireContext(), storageLauncherApi30);
        } else {
            Wizard.requestStoragePermissionApi19(storageLauncherApi19);
        }
    }

    public void ensureNotificationPermissionGranted() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (Wizard.isNotificationPermissionGranted(host.requireActivity())) {
            showNotificationSettingsRationaleIfAllowed();
        } else {
            requestNotificationPermission();
        }
    }

    private void showStoragePermissionDeniedDialog(
            @NonNull Runnable positiveAction, @NonNull Runnable negativeAction) {
        new MaterialAlertDialogBuilder(host.requireContext())
                .setTitle(R.string.storage_permission_denied)
                .setMessage(
                        host.requireContext()
                                .getString(
                                        R.string.storage_permission_denial_prompt,
                                        host.requireContext().getString(R.string.app_name)))
                .setPositiveButton(
                        R.string.storage_permission_request_again,
                        (d, which) -> positiveAction.run())
                .setNegativeButton(R.string.exit, (d, which) -> negativeAction.run())
                .setCancelable(false)
                .show();
    }

    private void showNotificationSettingsRationaleIfAllowed() {
        if (Wizard.areNotificationsAllowed(host.requireActivity())) {
            ILog.debug(TAG, "Notifications allowed");
        } else {
            showNotificationSettingsRationale();
        }
    }

    private void showNotificationSettingsRationale() {
        new MaterialAlertDialogBuilder(host.requireContext())
                .setTitle(R.string.msg_grant_notification_permission)
                .setMessage(R.string.msg_request_notification_rationale)
                .setPositiveButton(
                        R.string.ok_turn_on,
                        (d, which) ->
                                Wizard.launchDeviceSettingsActivity(
                                        host.requireActivity(),
                                        Settings.ACTION_APP_NOTIFICATION_SETTINGS))
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(false)
                .show();
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void showNotificationPermissionRationale() {
        new MaterialAlertDialogBuilder(host.requireContext())
                .setTitle(R.string.msg_grant_notification_permission)
                .setMessage(R.string.msg_request_notification_rationale)
                .setPositiveButton(R.string.ok, (d, which) -> requestNotificationPermission())
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(false)
                .show();
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void requestNotificationPermission() {
        try {
            notificationLauncherApi33.launch(Manifest.permission.POST_NOTIFICATIONS);
        } catch (ActivityNotFoundException e) {
            ILog.error(TAG, "requestNotificationPermission failed", e);
            BaseUtil.toastLong(R.string.msg_no_handle_activity_found);
        }
    }
}
