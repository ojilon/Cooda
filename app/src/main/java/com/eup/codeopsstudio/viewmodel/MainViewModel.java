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

package com.eup.codeopsstudio.viewmodel;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

/**
 * View Model managing the central state for CodeOps Studio.
 * <p>
 * This ViewModel uses LiveData to expose observable state to the UI.
 * All state changes go through posted values, ensuring thread safety.
 * <p>
 * Functional style: Methods that modify state return fluent results
 * where appropriate, and observables use LiveData's built-in
 * lifecycle-aware subscription model.
 *
 * @author Etido Peter
 */
public class MainViewModel extends ViewModel {

  /** Observable toolbar title. */
  private final MutableLiveData<String> mToolbarTitle = new MutableLiveData<>();

  /** Observable toolbar subtitle. */
  private final MutableLiveData<String> mToolbarSubTitle = new MutableLiveData<>();

  /** Observable drawer instance state. */
  private final MutableLiveData<Boolean> mDrawerInstance = new MutableLiveData<>(false);

  /** Observable bottom sheet expanded state. */
  private final MutableLiveData<Boolean> bottomSheetExpanded = new MutableLiveData<>(false);

  /** Observable bottom sheet state (collapsed/expanded). */
  private final MutableLiveData<Integer> mBottomSheetState =
      new MutableLiveData<>(BottomSheetBehavior.STATE_COLLAPSED);

  /** Observable menu update trigger. */
  private final MutableLiveData<Boolean> shouldUpdateMenu = new MutableLiveData<>();

  /** Observable add pane request. */
  private final MutableLiveData<Boolean> addPane = new MutableLiveData<>(false);

  /** Observable exit request event. */
  private final MutableLiveData<Event<Boolean>> exitRequest = new MutableLiveData<>();

  /** Observable drawer state event. */
  private final MutableLiveData<Event<Boolean>> mDrawerState = new MutableLiveData<>();

  /** IDE logs observable. */
  private MutableLiveData<ArrayList<LogModel>> mIDELogs;

  /** BUILD logs observable. */
  private MutableLiveData<ArrayList<LogModel>> mBUILDLogs;

  /** Main progress model observable. */
  private MutableLiveData<ProgressModel> main_progress_model = new MutableLiveData<>();

  /** Observable intent bundle event. */
  private final MutableLiveData<Event<Bundle>> intentBundle = new MutableLiveData<>();

  /** Observable permission event. */
  private final MutableLiveData<PermissionEvent> permissionEvent = new MutableLiveData<>();

  /**
   * Default constructor. Initializes the toolbar title from the application context.
   * <p>
   * Called once during ViewModel creation. Uses {@link IdeApplication#getGlobalContext()}
   * to retrieve the string resource.
   */
  public MainViewModel() {
    setToolbarTitle(IdeApplication.getGlobalContext().getString(R.string.app_name));
  }

  /**
   * Request to add a new pane (e.g., side panel).
   * <p>
   * Posts a true value to the {@code addPane} LiveData, which observers
   * can watch to react to the request (typically showing a bottom sheet
   * or drawer).
   *
   * @return {@code true} if the request was posted successfully
   */
  public boolean addPane() {
    this.addPane.postValue(true);
    return true;
  }

  /** Get the observable for addSettingsPane. */
  public LiveData<Boolean> addSettingsPane() {
    return this.addSettingsPane;
  }

  /** Set the addSettingsPane observable value. */
  public void addSettingsPane(boolean enabled) {
    this.addSettingsPane.setValue(enabled);
  }

  /** Clear the exit request event. */
  public void clearExitRequest() {
    exitRequest.setValue(new Event<>(false));
  }

  /** Close the navigation drawer. */
  public void closeDrawer() {
    mDrawerState.postValue(new Event<>(false));
  }

  /** Get the observable for addPane. */
  public LiveData<Boolean> getAddPane() {
    return this.addPane;
  }

  /** Get the BUILD logs observable (lazily initialized). */
  public LiveData<ArrayList<LogModel>> getBUILDLogs() {
    if (mBUILDLogs == null) {
      mBUILDLogs = new MutableLiveData<>();
    }
    return mBUILDLogs;
  }

  /** Get the bottom sheet expanded state. */
  public LiveData<Boolean> getBottomSheetExpanded() {
    return bottomSheetExpanded;
  }

  /** Set the bottom sheet expanded state. */
  public void setBottomSheetExpanded(boolean expand) {
    bottomSheetExpanded.setValue(expand);
  }

  /** Get the bottom sheet state integer (STATE_COLLAPSED, STATE_EXPANDED, etc.). */
  public LiveData<Integer> getBottomSheetState() {
    return mBottomSheetState;
  }

  /** Set the bottom sheet state. */
  public void setBottomSheetState(@BottomSheetBehavior.State int bottomSheetState) {
    mBottomSheetState.setValue(bottomSheetState);
  }

  /** Get the drawer instance state. */
  public LiveData<Boolean> getDrawerInstance() {
    return mDrawerInstance;
  }

  /** Set the drawer instance state. */
  public void setDrawerInstance(boolean isDrawerLayout) {
    mDrawerInstance.setValue(isDrawerLayout);
  }

  /** Get the drawer state event observable. */
  public LiveData<Event<Boolean>> getDrawerState() {
    return mDrawerState;
  }

  /**
   * Check if the drawer is currently open.
   * <p>
   * Retrieves the current drawer state and returns {@code true} if open,
   * {@code false} otherwise. Handles the case where the value is null.
   *
   * @return {@code true} if drawer is open, {@code false} otherwise
   */
  public boolean isDrawerOpen() {
    Boolean state = mDrawerState.getValue().getContentIfNotHandled();
    return state != null && state;
  }

  /** Request to open the navigation drawer. */
  public void requestOpenDrawer() {
    mDrawerState.postValue(new Event<>(true));
  }

  /** Request to close the navigation drawer. */
  public void requestCloseDrawer() {
    mDrawerState.postValue(new Event<>(false));
  }

  /** Get the exit request event observable. */
  public LiveData<Event<Boolean>> getExitRequest() {
    return exitRequest;
  }

  /** Get the IDE logs observable (lazily initialized). */
  public LiveData<ArrayList<LogModel>> getIDELogs() {
    if (mIDELogs == null) {
      mIDELogs = new MutableLiveData<>();
    }
    return mIDELogs;
  }

  /** Get the shouldUpdateMenu observable. */
  public LiveData<Boolean> getShouldUpdateMenu() {
    return shouldUpdateMenu;
  }

  /** Set the shouldUpdateMenu observable value. */
  public void setShouldUpdateMenu(boolean update) {
    shouldUpdateMenu.setValue(update);
  }

  /** Get the toolbar subtitle observable. */
  public LiveData<String> getToolbarSubTitle() {
    return mToolbarSubTitle;
  }

  /** Set the toolbar subtitle observable value. */
  public void setToolbarSubTitle(@Nullable String title) {
    mToolbarSubTitle.setValue(title);
  }

  /** Get the toolbar title observable. */
  public LiveData<String> getToolbarTitle() {
    return mToolbarTitle;
  }

  /** Set the toolbar title observable value. */
  public void setToolbarTitle(@Nullable String title) {
    mToolbarTitle.setValue(title);
  }

  /** Get the web view pane file observable. */
  public MutableLiveData<File> getWebViewPaneFile() {
    return mWebViewPaneFile;
  }

  /** Set the web view pane file. */
  public void setWebViewPaneFile(File file) {
    mWebViewPaneFile.setValue(file);
  }

  /** Get the pick ZIP file observable. */
  public LiveData<File> getZipFile() {
    return this.pickZipFile;
  }

  /** Set the pick ZIP file observable value. */
  public void setZipFile(File file) {
    this.pickZipFile.setValue(file);
  }

  /**
   * Observe an editor file being opened.
   * <p>
   * Subscribes the given observer to the {@code mOpenEditorFile} LiveData,
   * tied to the provided {@link LifecycleOwner} for lifecycle awareness.
   *
   * @param lifecycleOwner the owner whose lifecycle controls observation
   * @param observer the observer to receive file updates
   */
  public void observeEditorFileOpening(LifecycleOwner lifecycleOwner, Observer<File> observer) {
    mOpenEditorFile.observe(lifecycleOwner, observer);
  }

  /**
   * Observe the tree view fragment's view file.
   * <p>
   * Subscribes the given observer to the {@code mTreeFragmentViewFile} LiveData,
   * tied to the provided {@link LifecycleOwner}.
   *
   * @param lifecycleOwner the owner whose lifecycle controls observation
   * @param observer the observer to receive file updates
   */
  public void observeSetTreeViewFragmentFile(
      LifecycleOwner lifecycleOwner, Observer<File> observer) {
    this.mTreeFragmentViewFile.observe(lifecycleOwner, observer);
  }

  /** Open a file in the editor. */
  public void openEditorFile(File file) {
    mOpenEditorFile.setValue(file);
  }

  /** Request an exit confirmation. */
  public void requestExit() {
    exitRequest.setValue(new Event<>(true));
  }

  /** Set the tree view fragment's directory tree file. */
  public void setTreeViewFragmentTreeDir(File file) {
    mTreeFragmentViewFile.setValue(file);
  }

  /** Get the main progress model observable. */
  public LiveData<ProgressModel> getMainProgress() {
    return this.main_progress_model;
  }

  /**
   * Update the main progress model.
   * <p>
   * Updates the progress with determinate state, a progress value (0-100),
   * and a completion flag.
   *
   * @param isInDeterminate true for determinate progress, false for indeterminate
   * @param progressValue   progress percentage (0-100)
   * @param isComplete      whether the task is complete
   */
  public void updateMainProgress(boolean isInDeterminate, int progressValue, boolean isComplete) {
    this.main_progress_model.setValue(
        new ProgressModel(isInDeterminate, progressValue, isComplete));
  }

  /**
   * Observe the main progress model.
   * <p>
   * Subscribes the given observer to the {@code main_progress_model} LiveData,
   * tied to the provided {@link LifecycleOwner}.
   *
   * @param lifecycleOwner the owner whose lifecycle controls observation
   * @param observer the observer to receive progress updates
   */
  public void observeMainProgress(LifecycleOwner lifecycleOwner, Observer<ProgressModel> observer) {
    this.main_progress_model.observe(lifecycleOwner, observer);
  }

  /**
   * Set an intent bundle event.
   * <p>
   * Wraps the given intent into a bundle and posts it as an event.
   * Observers can use {@link Event#getContentIfNotHandled()} to retrieve
   * the bundle.
   *
   * @param intent the intent to bundle and observe
   */
  public void setIntentBundle(Intent intent) {
    intentBundle.setValue(new Event<>(toBundle(intent)));
  }

  /**
   * Convert an Android Intent to a Bundle.
   * <p>
   * Utility method to convert an Intent's extras into a Bundle.
   * Used internally by {@link #setIntentBundle(Intent)}.
   *
   * @param intent the intent to convert
   * @return a Bundle containing the intent's extras, or an empty bundle
   */
  private static Bundle toBundle(Intent intent) {
    if (intent == null) {
      return new Bundle();
    }
    Bundle bundle = new Bundle();
    intent.extras().keySet().forEach(key -> bundle.putString(key, intent.getStringExtra(key)));
    return bundle;
  }

  /** Observe the intent bundle event. */
  public void observeIntentBundle(LifecycleOwner lifecycleOwner, Observer<Event<Bundle>> observer) {
    intentBundle.observe(lifecycleOwner, observer);
  }

  /** Get the intent bundle observable. */
  public LiveData<Event<Bundle>> getIntentBundle() {
    return intentBundle;
  }

  /** Observe permission events. */
  public void observePermissionEvents(
      LifecycleOwner lifecycleOwner, Observer<PermissionEvent> observer) {
    permissionEvent.observe(lifecycleOwner, observer);
  }

  /** Request storage permission. */
  public void requestStoragePermission() {
    permissionEvent.setValue(
        new PermissionEvent(PermissionEvent.Type.STORAGE, PermissionEvent.Action.REQUEST));
  }

  /** Request notification permission. */
  public void requestNotificationPermission() {
    permissionEvent.setValue(
        new PermissionEvent(PermissionEvent.Type.NOTIFICATION, PermissionEvent.Action.REQUEST));
  }
}