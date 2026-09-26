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

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.util.Pair;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import com.eup.codeopsstudio.common.Constants;
import com.eup.codeopsstudio.common.ILog;
import com.eup.codeopsstudio.common.util.PreferencesUtils;
import com.eup.codeopsstudio.databinding.FragmentMainBinding;
import com.eup.codeopsstudio.databinding.LayoutDialogTextInputBinding;
import com.eup.codeopsstudio.domain.events.CurrentPaneEvent;
import com.eup.codeopsstudio.domain.events.EditorModificationEvent;
import com.eup.codeopsstudio.logger.Logger;
import com.eup.codeopsstudio.models.user.User;
import com.eup.codeopsstudio.nativebridge.NativeBackend;
import com.eup.codeopsstudio.observers.ContextualObserver;
import com.eup.codeopsstudio.palette.providers.WindowProvider;
import com.eup.codeopsstudio.pane.Pane;
import com.eup.codeopsstudio.ui.drawer.DrawerCoordinator;
import com.eup.codeopsstudio.ui.editor.panes.WebViewPane;
import com.eup.codeopsstudio.ui.fcm.AppUpdateCoordinator;
import com.eup.codeopsstudio.ui.plugin.PluginCoordinator;
import com.eup.codeopsstudio.ui.menu.ToolbarMenuController;
import com.eup.codeopsstudio.ui.permission.PermissionCoordinator;
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
 * Main IDE surface: lifecycle and ViewModel observers.
 * Menu → {@link ToolbarMenuController}; permissions → {@link PermissionCoordinator};
 * updates → {@link AppUpdateCoordinator}; plugins → {@link PluginCoordinator};
 * drawer → {@link DrawerCoordinator}.
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
    private PermissionCoordinator permissionCoordinator;
    private AppUpdateCoordinator appUpdateCoordinator;
    private PluginCoordinator pluginCoordinator;
    private DrawerCoordinator drawerCoordinator;

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

        pluginCoordinator = new PluginCoordinator();

        permissionCoordinator =
                new PermissionCoordinator(
                        new PermissionCoordinator.Host() {
                            @NonNull
                            @Override
                            public FragmentActivity requireActivity() {
                                return MainFragment.this.requireActivity();
                            }

                            @NonNull
                            @Override
                            public android.content.Context requireContext() {
                                return MainFragment.this.requireContext();
                            }

                            @Override
                            public boolean shouldShowRequestPermissionRationale(
                                    @NonNull String permission) {
                                return MainFragment.this.shouldShowRequestPermissionRationale(
                                        permission);
                            }

                            @Override
                            public void onStorageReady() {
                                pluginCoordinator.checkAndInstall(requireContext());
                            }

                            @Override
                            public void onStorageDeniedExit() {
                                requireActivity().finishAffinity();
                                System.exit(0);
                            }
                        });
        permissionCoordinator.attach(this);

        appUpdateCoordinator =
                new AppUpdateCoordinator(
                        new AppUpdateCoordinator.Host() {
                            @Override
                            public androidx.fragment.app.FragmentManager childFragmentManager() {
                                return getChildFragmentManager();
                            }

                            @Override
                            public boolean canShowUi() {
                                return isAdded() && !isRemoving() && !isDetached();
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
        onBackPressedCallback =
                new OnBackPressedCallback(false) {
                    @Override
                    public void handleOnBackPressed() {
                        var webViewPane = selected(WebViewPane.class);
                        if (webViewPane != null && webViewPane.getWebView().canGoBack()) {
                            webViewPane.getWebView().goBack();
                            return;
                        }
                        if (drawerCoordinator != null) {
                            drawerCoordinator.handleBackPress();
                        }
                    }
                };

        activity.getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), onBackPressedCallback);

        drawerCoordinator =
                new DrawerCoordinator(
                        new DrawerCoordinator.Host() {
                            @NonNull
                            @Override
                            public View rootView() {
                                return rootView;
                            }

                            @NonNull
                            @Override
                            public FragmentMainBinding binding() {
                                return binding;
                            }

                            @NonNull
                            @Override
                            public MainViewModel mainViewModel() {
                                return mainViewModel;
                            }

                            @NonNull
                            @Override
                            public androidx.lifecycle.LifecycleOwner viewLifecycleOwner() {
                                return getViewLifecycleOwner();
                            }

                            @Override
                            public void setBackPressEnabled(boolean enabled) {
                                if (onBackPressedCallback != null) {
                                    onBackPressedCallback.setEnabled(enabled);
                                }
                            }
                        });
        drawerCoordinator.setup();

        permissionCoordinator.ensureStoragePermission();
        permissionCoordinator.ensureNotificationPermissionGranted();
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
                        appUpdateCoordinator.handleIntentBundle(bundle);
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
                    // ZIP heuristic: native (C++23) when available, Java fallback otherwise
                    final String mime = Wizard.getMimeType(requireContext(), file);
                    if (NativeBackend.isZipCandidate(file.getName(), mime)) {
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

        appUpdateCoordinator.checkForStoredAppUpdates();
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
        if (drawerCoordinator != null) {
            drawerCoordinator.saveState(outState);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (drawerCoordinator != null) {
            drawerCoordinator.restoreState(savedInstanceState);
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

    public void openFileInPane(File file) {
        mainViewModel.openEditorFile(file);
        invalidateMenu();
    }

    private void restoreLastProject() {
        if (!PreferencesUtils.openLastOpenedProject()) {
            return;
        }
        try {
            String lastProjectPath =
                    PreferencesUtils.getLastOpenedProjectPreferences()
                            .getString(Constants.SharedPreferenceKeys.KEY_LAST_OPENED_PROJECT, "");
            if (!Wizard.isEmpty(lastProjectPath)) {
                var projectFile = new File(lastProjectPath);
                if (projectFile.exists() && projectFile.isDirectory()) {
                    mainViewModel.setTreeViewFragmentTreeDir(projectFile);
                }
            }
        } catch (Throwable e) {
            PreferencesUtils.clearPreference(
                    PreferencesUtils.getLastOpenedProjectPreferences(),
                    Constants.SharedPreferenceKeys.KEY_LAST_OPENED_PROJECT);
            logger.e(TAG, "Failed to reopen last opened project: " + e);
        }
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
}
