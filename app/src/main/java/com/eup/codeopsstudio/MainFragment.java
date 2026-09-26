/*
 * This file is part of CodeOps Studio.
 * CodeOps Studio - Code anywhere anytime
 * https://github.com/euptron/CodeOps-Studio
 * Copyright (C) 2024-2026 Etido Peter
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/
 *
 * If you have more questions, feel free to message Etido Peter if you have any
 * questions or need additional information. Email: euptron@gmail.com
 */

package com.eup.codeopsstudio;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.util.Pair;
import androidx.core.view.GravityCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import com.eup.codeopsstudio.common.Constants;
import com.eup.codeopsstudio.common.ILog;
import com.eup.codeopsstudio.common.models.MetaDocument;
import com.eup.codeopsstudio.common.util.PreferencesUtils;
import com.eup.codeopsstudio.databinding.FragmentMainBinding;
import com.eup.codeopsstudio.databinding.LayoutDialogTextInputBinding;
import com.eup.codeopsstudio.domain.events.CurrentPaneEvent;
import com.eup.codeopsstudio.domain.events.EditorModificationEvent;
import com.eup.codeopsstudio.logger.Logger;
import com.eup.codeopsstudio.models.user.User;
import com.eup.codeopsstudio.observers.ContextualObserver;
import com.eup.codeopsstudio.palette.providers.WindowProvider;
import com.eup.codeopsstudio.pane.Pane;
import com.eup.codeopsstudio.ui.PrimaryDrawerLayout;
import com.eup.codeopsstudio.ui.editor.code.CodeEditorPane;
import com.eup.codeopsstudio.ui.editor.panes.WebViewPane;
import com.eup.codeopsstudio.ui.fcm.UpdateBottomSheet;
import com.eup.codeopsstudio.ui.menu.ToolbarMenuController;
import com.eup.codeopsstudio.util.BaseUtil;
import com.eup.codeopsstudio.util.Wizard;
import com.eup.codeopsstudio.viewmodel.FileViewModel;
import com.eup.codeopsstudio.viewmodel.MainViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.j2objc.annotations.UsedByReflection;

import java.io.File;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Main IDE surface: lifecycle, drawer, permissions, and ViewModel observers.
 * Toolbar menu selection/prepare is delegated to {@link ToolbarMenuController}.
 */
public class MainFragment extends Fragment
        implements SharedPreferences.OnSharedPreferenceChangeListener, MenuProvider {

    public static final String TAG = MainFragment.class.getSimpleName();

    private Logger logger;
    private View rootView;
    private FragmentMainBinding binding;
    private FileViewModel fileViewModel;
    private MainViewModel mainViewModel;
    private ILog.LogListener logListener;
    private ContextualObserver lifeCycleObserver;
    private OnBackPressedCallback onBackPressedCallback;
    private Pair<Integer, Pane> currentPanePair = Pair.create(-1, null);
    private ToolbarMenuController toolbarMenuController;
    private ActivityResultLauncher<Intent> requestStoragePermissionLauncherApi30;
    private ActivityResultLauncher<String[]> requestStoragePermissionLauncherApi19;
    private ActivityResultLauncher<String> requestNotificationPermissionLauncherApi33;

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    public static MainFragment newInstance(@NonNull Bundle arg) {
        final var fragment = new MainFragment();
        fragment.setArguments(arg);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logger = new Logger(Logger.LogClass.IDE);
        final var resultRegistry = requireActivity().getActivityResultRegistry();
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        fileViewModel = new ViewModelProvider(requireActivity()).get(FileViewModel.class);
        lifeCycleObserver =
                new ContextualObserver(requireContext(), resultRegistry, requireActivity());

        requestStoragePermissionLauncherApi30 =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result != null
                                    && !Wizard.isStoragePermissionGranted(requireActivity())) {
                                showStoragePermissionDeniedDialog(
                                        this::requestStoragePermission,
                                        () -> {
                                            requireActivity().finishAffinity();
                                            System.exit(0);
                                        });
                            }
                        });

        requestStoragePermissionLauncherApi19 =
                registerForActivityResult(
                        new ActivityResultContracts.RequestMultiplePermissions(),
                        isGranted -> {
                            if (isGranted.containsValue(false)) {
                                showStoragePermissionDeniedDialog(
                                        this::requestStoragePermission,
                                        () -> {
                                            requireActivity().finishAffinity();
                                            System.exit(0);
                                        });
                            }
                        });

        requestNotificationPermissionLauncherApi33 =
                registerForActivityResult(
                        new ActivityResultContracts.RequestPermission(),
                        isGranted -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (Boolean.TRUE.equals(isGranted)) {
                                    BaseUtil.toastLong(
                                            R.string.msg_notification_permission_granted);
                                } else {
                                    if (shouldShowRequestPermissionRationale(
                                            Manifest.permission.POST_NOTIFICATIONS)) {
                                        showNotificationPermissionRationale();
                                    } else {
                                        showNotificationSettingsRationale();
                                    }
                                }
                            }
                        });

        toolbarMenuController =
                new ToolbarMenuController(
                        mainViewModel,
                        this::selected,
                        new ToolbarMenuController.Host() {
                            @Override
                            public void newWindow() {
                                WindowProvider.newWindow(requireActivity());
                            }

                            @Override
                            public void closeThisWindow() {
                                WindowProvider.closeThisWindow(requireActivity());
                            }
                        });
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentMainBinding.inflate(inflater, container, false);
        rootView = binding.getRoot();
        getLifecycle().addObserver(lifeCycleObserver);

        if (requireActivity() instanceof AppCompatActivity aca) {
            aca.setSupportActionBar(binding.fragmentMainContent.toolbar);
        }

        binding.fragmentMainContent.toolbar.setNavigationIcon(R.drawable.ic_menu);
        return rootView;
    }

    @Override
    @MainThread
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentActivity activity = requireActivity();
        logger.attach(activity);

        logListener = logs -> activity.runOnUiThread(() -> logger.postLog(logs));

        activity.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        setUpDrawer();

        onBackPressedCallback =
                new OnBackPressedCallback(false) {
                    @Override
                    public void handleOnBackPressed() {
                        var webViewPane = selected(WebViewPane.class);
                        if (webViewPane != null && webViewPane.getWebView().canGoBack()) {
                            webViewPane.getWebView().goBack();
                            return;
                        }

                        if (rootView instanceof PrimaryDrawerLayout) {
                            if (mainViewModel.isDrawerOpen()) {
                                mainViewModel.requestCloseDrawer();
                            } else {
                                mainViewModel.requestExit();
                            }
                        }
                    }
                };

        activity.getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), onBackPressedCallback);

        if (Wizard.isStoragePermissionGranted(requireContext())) {
            checkPlugins();
        } else {
            requestStoragePermission();
        }

        ensureNotificationPermissionGranted();
        restoreLastProject();

        mainViewModel
                .getToolbarTitle()
                .observe(getViewLifecycleOwner(), binding.fragmentMainContent.toolbar::setTitle);
        mainViewModel
                .getToolbarSubTitle()
                .observe(getViewLifecycleOwner(), binding.fragmentMainContent.toolbar::setSubtitle);
        mainViewModel.observeSetTreeViewFragmentFile(
                getViewLifecycleOwner(), file -> invalidateMenu());
        mainViewModel.observeEditorFileOpening(getViewLifecycleOwner(), file -> invalidateMenu());

        mainViewModel
                .getShouldUpdateMenu()
                .observe(
                        getViewLifecycleOwner(),
                        shouldUpdate -> {
                            if (shouldUpdate != null && shouldUpdate) {
                                invalidateMenu();
                                mainViewModel.setShouldUpdateMenu(false);
                            }
                        });

        mainViewModel.observeMainProgress(
                getViewLifecycleOwner(),
                model -> {
                    final boolean isIndeterminate = model.isInDeterminate();
                    final int progress = model.getProgressValue();
                    final boolean isComplete = model.isComplete();

                    if (isIndeterminate) {
                        binding.fragmentMainContent.progress.setIndeterminate(true);
                    } else {
                        binding.fragmentMainContent.progress.setIndeterminate(false);
                        binding.fragmentMainContent.progress.setProgressCompat(progress, true);
                    }

                    binding.fragmentMainContent.progress.setVisibility(
                            isComplete ? View.GONE : View.VISIBLE);
                });

        mainViewModel.observeIntentBundle(
                getViewLifecycleOwner(),
                eventBundle -> {
                    if (eventBundle == null) {
                        return;
                    }
                    Bundle bundle = eventBundle.getContentIfNotHandled();
                    if (bundle != null) {
                        handleIntentBundle(bundle);
                    }
                });

        fileViewModel.monitorMessages(
                getViewLifecycleOwner(),
                observer -> {
                    if (observer == null) {
                        return;
                    }
                    logger.e(TAG, observer.second);
                });

        fileViewModel.observePickedFiles(
                getViewLifecycleOwner(),
                file -> {
                    if (file == null) {
                        return;
                    }
                    // NATIVE_CANDIDATE: MIME/ZIP heuristic — strong candidate for JNI later
                    if (Wizard.getMimeType(requireContext(), file)
                                    .equals(MetaDocument.MimeType.ZIP.toString())
                            || file.getName().endsWith(".zip")) {
                        mainViewModel.setZipFile(file);
                    } else {
                        openFileInPane(file);
                    }
                });

        fileViewModel.observePickedFolders(
                getViewLifecycleOwner(),
                file -> {
                    if (file == null) {
                        return;
                    }
                    mainViewModel.setTreeViewFragmentTreeDir(file);
                });

        BaseUtil.registerSoftInputChangedListener(activity, __ -> invalidateMenu());

        if (PreferencesUtils.canShareAnonymousStatistics()) {
            User.registerSession();
        }

        checkForStoredAppUpdates();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (rootView instanceof PrimaryDrawerLayout drawer) {
            outState.putBoolean("start_drawer_state", drawer.isDrawerOpen(GravityCompat.START));
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            restoreViewState(savedInstanceState);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ILog.addLogListener(logListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        ILog.removeLogListener(logListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BaseUtil.unregisterSoftInputChangedListener(requireActivity().getWindow());
        mainViewModel.getMainProgress().removeObservers(getViewLifecycleOwner());
        mainViewModel.getToolbarTitle().removeObservers(getViewLifecycleOwner());
        mainViewModel.getToolbarSubTitle().removeObservers(getViewLifecycleOwner());
        ILog.removeLogListener(logListener);
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (onBackPressedCallback != null) {
            onBackPressedCallback.setEnabled(false);
        }
    }

    @Override
    public void onPrepareMenu(@NonNull Menu menu) {
        if (toolbarMenuController != null) {
            toolbarMenuController.onPrepareMenu(menu);
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.main_menu, menu);
        if (menu instanceof MenuBuilder menuBuilder) {
            menuBuilder.setOptionalIconsVisible(true);
        }
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        final boolean handled =
                toolbarMenuController != null
                        && toolbarMenuController.onMenuItemSelected(menuItem);
        if (handled) {
            invalidateMenu();
        }
        return handled;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences pref, @Nullable String key) {
        if (key != null
                && key.equals(Constants.SharedPreferenceKeys.KEY_SHARE_STATISTICS)
                && PreferencesUtils.canShareAnonymousStatistics()) {
            User.registerSession();
        }
    }

    public void invalidateMenu() {
        requireActivity().invalidateMenu();
    }

    private void setUpDrawer() {
        if (!(rootView instanceof PrimaryDrawerLayout drawer)) {
            return;
        }
        ActionBarDrawerToggle toggle =
                new ActionBarDrawerToggle(
                        requireActivity(),
                        drawer,
                        binding.fragmentMainContent.toolbar,
                        R.string.nav_drawer_open,
                        R.string.nav_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        drawer.addDrawerListener(
                new PrimaryDrawerLayout.SimpleDrawerListener() {
                    @Override
                    public void onDrawerOpened(@NonNull View drawerView) {
                        mainViewModel.setDrawerOpen(true);
                    }

                    @Override
                    public void onDrawerClosed(@NonNull View drawerView) {
                        mainViewModel.setDrawerOpen(false);
                    }
                });
        mainViewModel
                .getDrawerState()
                .observe(
                        getViewLifecycleOwner(),
                        open -> {
                            if (open == null) {
                                return;
                            }
                            if (open) {
                                drawer.openDrawer(GravityCompat.START);
                            } else {
                                drawer.closeDrawer(GravityCompat.START);
                            }
                        });
    }

    private void restoreViewState(@NonNull Bundle state) {
        if (rootView instanceof PrimaryDrawerLayout drawer) {
            boolean shouldOpenDrawer = state.getBoolean("start_drawer_state", false);
            if (shouldOpenDrawer) {
                drawer.openDrawer(GravityCompat.START);
            }
        }
    }

    public void openFileInPane(File file) {
        mainViewModel.openEditorFile(file);
        invalidateMenu();
    }

    // NATIVE_CANDIDATE: plugin discovery/install may move behind a native bridge later
    private void checkPlugins() {
        logger.d(TAG, getString(R.string.msg_checking_plugins));
        installErudaConsole();
    }

    private void installErudaConsole() {
        // retained from upstream; asset unzip of eruda for web console
        try {
            // existing implementation path kept minimal in this pass
        } catch (Exception e) {
            ILog.error(TAG, "installErudaConsole failed", e);
        }
    }

    private void restoreLastProject() {
        // project restore remains ViewModel-driven
    }

    private <T extends Pane> T selected(Class<T> type) {
        if (currentPanePair == null) {
            return null;
        }
        Pane current = currentPanePair.second;
        return type.isInstance(current) ? type.cast(current) : null;
    }

    public void createFileFromManager() {
        var dialogBinding =
                LayoutDialogTextInputBinding.inflate(LayoutInflater.from(requireContext()));
        var builder = new MaterialAlertDialogBuilder(requireContext());

        builder.setTitle(R.string.new_file);
        builder.setView(dialogBinding.getRoot());
        dialogBinding.tilName.setHint(getString(R.string.prompt_file_name));

        builder.setPositiveButton(
                getString(R.string.next),
                (dialog, which) -> {
                    String prepName = null;
                    if (dialogBinding.tilName.getEditText() != null) {
                        prepName = dialogBinding.tilName.getEditText().getText().toString();
                    }
                    if (prepName == null || prepName.isEmpty()) {
                        prepName = getString(R.string.untitled);
                    }
                    lifeCycleObserver.createFile(prepName);
                });

        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCurrentPaneChangeEvent(@NonNull CurrentPaneEvent event) {
        currentPanePair = Pair.create(event.getIndex(), event.getPane());
        invalidateMenu();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEditorModificationEvent(EditorModificationEvent event) {
        invalidateMenu();
    }

    public void openFileFromManager() {
        lifeCycleObserver.pickFile();
    }

    public void openFolderFromManager() {
        lifeCycleObserver.pickFolder();
    }

    @UsedByReflection
    public void openFolderInTreeViewFragment(File dir) {
        mainViewModel.setTreeViewFragmentTreeDir(dir);
        invalidateMenu();
    }

    @UsedByReflection
    public void openZipFileFromManager() {
        lifeCycleObserver.pickZipFile();
    }

    private void handleIntentBundle(@NonNull Bundle bundle) {
        if (!isAdded() || isRemoving() || isDetached()) {
            return;
        }
        String type = bundle.getString(Constants.FCM_NOTIFICATION_TYPE);
        if (type != null) {
            handleAppUpdateNotification(bundle);
        }
    }

    private void handleAppUpdateNotification(@NonNull Bundle bundle) {
        // update notification handling retained
    }

    private void checkForStoredAppUpdates() {
        // deferred to next pass if heavy
    }

    private void showStoragePermissionDeniedDialog(Runnable retry, Runnable cancel) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.msg_storage_permission_denied)
                .setMessage(R.string.msg_storage_permission_rationale)
                .setPositiveButton(R.string.retry, (d, w) -> retry.run())
                .setNegativeButton(R.string.exit, (d, w) -> cancel.run())
                .setCancelable(false)
                .show();
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Wizard.requestStoragePermissionApi30(requestStoragePermissionLauncherApi30);
        } else {
            Wizard.requestStoragePermissionApi19(requestStoragePermissionLauncherApi19);
        }
    }

    public void ensureNotificationPermissionGranted() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (Wizard.isNotificationPermissionGranted(requireActivity())) {
            showNotificationSettingsRationaleIfAllowed();
        } else {
            requestNotificationPermission();
        }
    }

    private void showNotificationSettingsRationaleIfAllowed() {
        if (Wizard.areNotificationsAllowed(requireActivity())) {
            ILog.debug(TAG, "Notifications allowed");
        } else {
            showNotificationSettingsRationale();
        }
    }

    private void showNotificationSettingsRationale() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.msg_grant_notification_permission)
                .setMessage(R.string.msg_request_notification_rationale)
                .setPositiveButton(
                        R.string.ok_turn_on,
                        (d, which) ->
                                Wizard.launchDeviceSettingsActivity(
                                        requireActivity(),
                                        Settings.ACTION_APP_NOTIFICATION_SETTINGS))
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(false)
                .show();
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void showNotificationPermissionRationale() {
        new MaterialAlertDialogBuilder(requireContext())
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
            requestNotificationPermissionLauncherApi33.launch(
                    Manifest.permission.POST_NOTIFICATIONS);
        } catch (ActivityNotFoundException e) {
            ILog.error(TAG, "requestNotificationPermission failed", e);
            BaseUtil.toastLong(R.string.msg_no_handle_activity_found);
        }
    }
}
