/*
 * This file is part of CodeOps Studio (YourForkName).
 * Copyright (C) 2024-2026 Etido Peter
 * Copyright (C) 2026 Your Name <your.email@example.com>
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

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.eup.codeopsstudio.common.ILog;
import com.eup.codeopsstudio.common.models.BooleanResult;
import com.eup.codeopsstudio.common.util.PreferencesUtils;
import com.eup.codeopsstudio.databinding.ActivityMainBinding;
import com.eup.codeopsstudio.domain.events.CurrentPaneEvent;
import com.eup.codeopsstudio.observers.ContextualObserver;
import com.eup.codeopsstudio.palette.providers.WindowProvider;
import com.eup.codeopsstudio.pane.Pane;
import com.eup.codeopsstudio.ui.onboarding.LandingFragment;
import com.eup.codeopsstudio.util.BaseUtil;
import com.eup.codeopsstudio.viewmodel.MainViewModel;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Primary container activity that hosts either MainFragment (regular IDE use) or LandingFragment
 * (first-time onboarding). This is the root of the UI layer.
 * <p>
 * FLOW DIAGRAM (execution order on app start):
 * ------------------------------------------------
 * 1. onCreate(savedInstanceState)
 *    ├─── initializeWindow()          → sets up session ID & task description
 *    ├─── new ViewModelProvider(this)  → creates MainViewModel (survives config changes)
    ├─── new ContextualObserver(...)  → delegates file/folder pickers, permissions
    ├─── getLifecycle().addObserver() → lifecycle-aware observer registration
    └─── branch:
         │       if PreferencesUtils.isAppFirstLaunch():
         │               loadFragment(LandingFragment, TAG)   → onboarding flow
         │       else:
         │               handleNotificationIntent(getIntent())
         │               loadFragment(MainFragment, TAG)      → normal IDE start
 * ------------------------------------------------
 * 2. onResume()
 *    └─── refreshWindow(result)       → syncs window count across all open IDE windows
 * ------------------------------------------------
 * 3. onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
 *    └─── refreshWindow(result)       → same as onResume, for multi-window mode changes
 * ------------------------------------------------
 * 4. onDestroy()
 *    └─── WindowProvider.Registry.unregister(sessionId) → removes this window from registry
 * ------------------------------------------------
 * 5. onNewIntent(intent)
 *    └─── setIntent(intent); handleNotificationIntent(intent) → handles notification taps
 * ------------------------------------------------
 * 6. onKeyDown(keyCode, keyEvent)
 *    └─── if KEYCODE_ESCAPE → mainViewModel.requestCloseDrawer() → closes navigation drawer
 * ------------------------------------------------
 * 7. Event Bus: onCurrentPaneChangeEvent(@NonNull event)
 *    └─── selectedPane = event.getPane() → keeps track of which editor pane is active
 * ------------------------------------------------
 * <p>
 * KEY FIELDS (simplified, minimal OOP):
 * - sessionId: String — unique ID for this window instance
 * - mainViewModel: MainViewModel — central UI state (toolbar, drawer, logs, intents)
 * - selectedPane: Pane — currently selected editor pane (tracked via EventBus)
 * - lifecycleObserver: ContextualObserver — handles Android SAF/picker/permission lifecycle
 * <p>
 * FUNCTIONAL NOTES:
 * - onCreate is the entry point; all other lifecycle methods follow Android contract
 * - refreshWindow uses a BooleanResult lambda to communicate success/failure
 * - WindowProvider.Registry is a static registry tracking ALL open IDE windows
 * - EventBus @Subscribe delivers CurrentPaneEvent from fragments to this activity
 * - Session ID survives configuration changes via MainViewModel (ViewModel survives)
 */

public class MainActivity extends AppCompatActivity {

    /** Log tag, set once at class load. */
    public static final String TAG = MainActivity.class.getSimpleName();

    /** Unique ID for this window instance. Generated if not provided via Intent. */
    private String sessionId;

    /** Central UI state holder — survives configuration changes (rotation, etc.). */
    private MainViewModel mainViewModel;

    /** Currently selected editor pane, updated via EventBus from fragments. */
    private Pane selectedPane;

    /** Lifecycle observer for file pickers, folder selection, save-as, permission launches. */
    private ContextualObserver lifecycleObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* STEP 1: Set up the UI container */
        // Inflate the main layout via data binding, enable edge-to-edge display,
        // and set the content view to the root container.
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        BaseUtil.enforceEdgeToEdge(getWindow(), true);
        setContentView(binding.getRoot());

        /* STEP 2: Initialize multi-window support */
        // Sets up session ID and task description (window title like "CodeOps Studio #3")
        initializeWindow();

        /* STEP 3: Create shared ViewModel — survives config changes */
        // MainViewModel holds toolbar title, drawer state, logs, intent bundles, etc.
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        /* STEP 4: Register lifecycle-aware observer for file operations */
        // ContextualObserver handles: picking files/folders, creating new files,
        // saving documents as, and various permission requests.
        // It observes the Activity lifecycle so callbacks only happen when appropriate.
        lifecycleObserver = new ContextualObserver(this, getActivityResultRegistry(), this);
        getLifecycle().addObserver(lifecycleObserver);

        /* STEP 5: Branch — first launch onboarding vs normal IDE start */
        if (PreferencesUtils.isAppFirstLaunch()) {
            /* FIRST LAUNCH: show onboarding screen */
            loadFragment(LandingFragment.newInstance(), LandingFragment.TAG);
        } else {
            /* NORMAL START: handle pending notification, then load main editor */
            handleNotificationIntent(getIntent());
            loadFragment(MainFragment.newInstance(), MainFragment.TAG);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        /* REFRESH WINDOW COUNT */
        // Invokes refreshWindow with a lambda that logs the result.
        // This keeps the window title (e.g., "CodeOps Studio #2") in sync
        // across all open IDE windows when this one resumes.
        var refresh = refreshWindow(result -> {
            ILog.debug(TAG, "invoke#onRefreshWindow:onResume" + result);
            return true; // handled
        });

        if (refresh) {
            ILog.debug(TAG, "Window refresh successful");
        } else {
            ILog.debug(TAG, "Window refresh failed");
        }
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode,
                                         @NonNull Configuration newConfig) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig);

        /* REFRESH WINDOW COUNT ON MULTI-WINDOW MODE CHANGE */
        // Same as onResume, but triggered when the user switches between
        // multi-window and full-screen mode.
        var refresh = refreshWindow(result -> {
            ILog.debug(TAG, "invoke#onRefreshWindow:onMultiWindowModeChanged" + result);
            return true; // handled
        });

        if (refresh) {
            ILog.debug(TAG, "Window refresh successful");
        } else {
            ILog.debug(TAG, "Window refresh failed");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        /* REMOVE THIS WINDOW FROM REGISTRY */
        // Unregisters this window's session so the WindowProvider.Registry
        // no longer counts it. Important for accurate window counting
        // and task description updates.
        if (sessionId != null) {
            WindowProvider.Registry.unregister(sessionId);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        /* HANDLE NOTIFICATION TAP WHILE APP IS ALREADY RUNNING */
        setIntent(intent); // store the new intent
        handleNotificationIntent(intent); // forward to ViewModel for later processing
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
            /* CLOSE NAVIGATION DRAWER ON ESCAPE */
            // Delegates to MainViewModel.requestCloseDrawer(), which observes
            // the drawer state and closes it if open.
            mainViewModel.requestCloseDrawer();
            return true; // consumed — don't propagate to super
        }
        return super.onKeyDown(keyCode, keyEvent);
    }

    /* EVENT BUS HANDLER */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCurrentPaneChangeEvent(@NonNull CurrentPaneEvent event) {
        /* UPDATE SELECTED PANE FROM EVENT BUS */
        // Fragments (mainly MainFragment) post CurrentPaneEvent when the
        // user switches editor tabs. This keeps selectedPane in sync
        // so other components (like WindowProvider) know which window is active.
        selectedPane = event.getPane();
    }

    /* NOTIFICATION INTENT HANDLER */
    private void handleNotificationIntent(Intent intent) {
        if (intent == null) {
            ILog.info(TAG, "NotificationIntent is null");
            return;
        }
        // Forward the intent bundle to MainViewModel for later processing
        // by fragments or other components that observe intent changes.
        mainViewModel.setIntentBundle(intent);
    }

    /** Load a fragment into the container, optionally with animation. */
    public void loadFragment(Fragment fragment, String tag) {
        loadFragment(fragment, tag, false);
    }

    public void loadFragment(Fragment fragment, String tag, boolean animate) {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(tag) == null) {
            FragmentTransaction transaction = fm.beginTransaction();
            transaction.replace(R.id.fragment_container, fragment, tag);
            if (animate) {
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            }
            transaction.commit();
        }
    }

    /** Return the lifecycle observer for use by other components. */
    public ContextualObserver getLifecycleObserver() {
        return this.lifecycleObserver;
    }

    /**
     * Invokes a refresh of UI elements to keep shared data between windows in sync.
     * <p>Usage: Invoke on #create() or on <ul>{@link Activity#onResume()}</ul>,
     * <ul>{@link Activity#onCreate(Bundle)}</ul>, <ul>
     * {@link Activity#onMultiWindowModeChanged(boolean, Configuration)}</ul>
     *
     * @param result the action to run (typically a lambda that returns true)
     * @return {@code true} on refresh success otherwise {@code false}
     */
    public boolean refreshWindow(@NonNull BooleanResult<Integer> result) {
        int totalWindows = WindowProvider.Registry.size();
        // Log current window count vs total across all registered windows
        ILog.debug(TAG, "Window " + session.getWindowCount() + " of " + totalWindows);
        return result.process(totalWindows);
    }

    /* WINDOW INITIALIZATION */
    private void initializeWindow() {
        /* GET OR GENERATE SESSION ID */
        sessionId = getIntent().getStringExtra(WindowProvider.EXTRA_SESSION_ID);
        if (sessionId == null) {
            sessionId = WindowProvider.Registry.generateSessionId();
        }

        /* REGISTER THIS WINDOW in the global registry */
        // Pass selectedPane so the registry knows which editor pane belongs to this window
        session = WindowProvider.Registry.register(sessionId, selectedPane);
        session.setTaskId(getTaskId());

        /* SET TASK DESCRIPTION (window title) */
        // Shows something like "CodeOps Studio #3" in the recent apps list
        var label = getString(R.string.app_name);
        var title = label + " #" + session.getWindowCount();
        ActivityManager.TaskDescription td;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Newer Android API: use builder pattern
            td = new ActivityManager.TaskDescription.Builder().setLabel(title).build();
        } else {
            // Legacy API: simpler constructor
            td = new ActivityManager.TaskDescription(title, null);
        }
        setTaskDescription(td);
    }

    /** Return this window's session ID. */
    public String getSessionId() {
        return this.sessionId;
    }
}