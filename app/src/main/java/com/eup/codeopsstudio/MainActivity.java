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
 */
public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();

    private String sessionId;
    private MainViewModel mainViewModel;
    private Pane selectedPane;
    private WindowProvider.Session session;
    private ContextualObserver lifecycleObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        BaseUtil.enforceEdgeToEdge(getWindow(), true);
        setContentView(binding.getRoot());

        initializeWindow();

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        lifecycleObserver = new ContextualObserver(this, getActivityResultRegistry(), this);
        getLifecycle().addObserver(lifecycleObserver);

        if (PreferencesUtils.isAppFirstLaunch()) {
            loadFragment(LandingFragment.newInstance(), LandingFragment.TAG);
        } else {
            handleNotificationIntent(getIntent());
            loadFragment(MainFragment.newInstance(), MainFragment.TAG);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean refresh = refreshWindow(result -> {
            ILog.debug(TAG, "invoke#onRefreshWindow:onResume" + result);
            return true;
        });
        ILog.debug(TAG, refresh ? "Window refresh successful" : "Window refresh failed");
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode,
                                         @NonNull Configuration newConfig) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig);
        boolean refresh = refreshWindow(result -> {
            ILog.debug(TAG, "invoke#onRefreshWindow:onMultiWindowModeChanged" + result);
            return true;
        });
        ILog.debug(TAG, refresh ? "Window refresh successful" : "Window refresh failed");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sessionId != null) {
            WindowProvider.Registry.unregister(sessionId);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
            mainViewModel.requestCloseDrawer();
            return true;
        }
        return super.onKeyDown(keyCode, keyEvent);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCurrentPaneChangeEvent(@NonNull CurrentPaneEvent event) {
        selectedPane = event.getPane();
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent == null) {
            ILog.info(TAG, "NotificationIntent is null");
            return;
        }
        mainViewModel.setIntentBundle(intent);
    }

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

    public ContextualObserver getLifecycleObserver() {
        return this.lifecycleObserver;
    }

    public boolean refreshWindow(@NonNull BooleanResult<Integer> result) {
        int totalWindows = WindowProvider.Registry.size();
        int thisWindow = (session != null) ? session.getWindowCount() : -1;
        ILog.debug(TAG, "Window " + thisWindow + " of " + totalWindows);
        return result.process(totalWindows);
    }

    private void initializeWindow() {
        sessionId = getIntent().getStringExtra(WindowProvider.EXTRA_SESSION_ID);
        if (sessionId == null) {
            sessionId = WindowProvider.Registry.generateSessionId();
        }

        session = WindowProvider.Registry.register(sessionId, selectedPane);
        session.setTaskId(getTaskId());

        String label = getString(R.string.app_name);
        String title = label + " #" + session.getWindowCount();
        ActivityManager.TaskDescription td;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            td = new ActivityManager.TaskDescription.Builder().setLabel(title).build();
        } else {
            td = new ActivityManager.TaskDescription(title, null);
        }
        setTaskDescription(td);
    }

    public String getSessionId() {
        return this.sessionId;
    }
}
