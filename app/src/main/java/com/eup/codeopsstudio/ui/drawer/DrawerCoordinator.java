/*
 * This file is part of CodeOps Studio.
 * Copyright (C) 2024-2026 Etido Peter
 *
 * Extracted from MainFragment: primary drawer open/close, slide, and state.
 */

package com.eup.codeopsstudio.ui.drawer;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.lifecycle.LifecycleOwner;

import com.eup.codeopsstudio.databinding.FragmentMainBinding;
import com.eup.codeopsstudio.ui.PrimaryDrawerLayout;
import com.eup.codeopsstudio.viewmodel.MainViewModel;

/**
 * Owns primary side-drawer wiring so {@link com.eup.codeopsstudio.MainFragment}
 * only registers back-press and lifecycle around it.
 */
public final class DrawerCoordinator {

    private static final String KEY_START_DRAWER_STATE = "start_drawer_state";

    public interface Host {
        @NonNull
        View rootView();

        @NonNull
        FragmentMainBinding binding();

        @NonNull
        MainViewModel mainViewModel();

        @NonNull
        LifecycleOwner viewLifecycleOwner();

        void setBackPressEnabled(boolean enabled);
    }

    private final Host host;

    public DrawerCoordinator(@NonNull Host host) {
        this.host = host;
    }

    public void setup() {
        View root = host.rootView();
        FragmentMainBinding binding = host.binding();
        MainViewModel mainViewModel = host.mainViewModel();

        if (root instanceof PrimaryDrawerLayout drawerLayout) {
            mainViewModel.setDrawerInstance(true);
            mainViewModel
                    .getDrawerState()
                    .observe(
                            host.viewLifecycleOwner(),
                            event -> {
                                Boolean shouldOpenDrawer = event.getContentIfNotHandled();
                                if (Boolean.TRUE.equals(shouldOpenDrawer)) {
                                    drawerLayout.openDrawer(binding.navPrimarySideBar);
                                } else {
                                    drawerLayout.closeDrawer(binding.navPrimarySideBar);
                                }
                            });

            binding.fragmentMainContent.toolbar.setNavigationOnClickListener(
                    v -> {
                        if (drawerLayout.isDrawerOpen(binding.navPrimarySideBar)) {
                            mainViewModel.requestCloseDrawer();
                        } else if (!drawerLayout.isDrawerOpen(binding.navPrimarySideBar)) {
                            mainViewModel.requestOpenDrawer();
                        }
                    });

            drawerLayout.addDrawerListener(
                    new PrimaryDrawerLayout.SimpleDrawerListener() {
                        @Override
                        public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                            float factor = 1f;
                            float translation = drawerView.getWidth() * slideOffset * factor;
                            binding.fragmentMainContent.mainContentLayout.setTranslationX(
                                    translation);
                        }

                        @Override
                        public void onDrawerOpened(@NonNull View drawerView) {
                            host.setBackPressEnabled(true);
                        }

                        @Override
                        public void onDrawerClosed(@NonNull View drawerView) {
                            host.setBackPressEnabled(false);
                        }
                    });
        } else {
            // Large screens may not use PrimaryDrawerLayout
            mainViewModel.setDrawerInstance(false);
            binding.fragmentMainContent.toolbar.setNavigationIcon(null);
        }
    }

    /** @return true if back was consumed (drawer closed or exit requested). */
    public boolean handleBackPress() {
        View root = host.rootView();
        if (!(root instanceof PrimaryDrawerLayout)) {
            return false;
        }
        MainViewModel mainViewModel = host.mainViewModel();
        if (mainViewModel.isDrawerOpen()) {
            mainViewModel.requestCloseDrawer();
        } else {
            mainViewModel.requestExit();
        }
        return true;
    }

    public void saveState(@NonNull Bundle outState) {
        View root = host.rootView();
        if (root instanceof PrimaryDrawerLayout drawer) {
            outState.putBoolean(KEY_START_DRAWER_STATE, drawer.isDrawerOpen(GravityCompat.START));
        }
    }

    public void restoreState(@Nullable Bundle state) {
        if (state == null) {
            return;
        }
        View root = host.rootView();
        if (root instanceof PrimaryDrawerLayout drawer) {
            if (state.getBoolean(KEY_START_DRAWER_STATE, false)) {
                drawer.openDrawer(GravityCompat.START);
            }
        }
    }
}
