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

package com.eup.codeopsstudio.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import com.eup.codeopsstudio.IdeApplication;
import com.eup.codeopsstudio.R;
import com.eup.codeopsstudio.common.AsyncTask;
import com.eup.codeopsstudio.common.Constants;
import com.eup.codeopsstudio.common.ILog;
import com.eup.codeopsstudio.common.models.Event;
import com.eup.codeopsstudio.server.provider.IPProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Utility class containing reusable methods for CodeOps Studio.
 * <p>
 * This class provides a wide range of helper methods used across the application.
 * Many methods leverage functional interfaces and predicates to enable
 * more concise, readable, and testable code.
 * <p>
 * All methods are static - no instantiation is needed. Use directly via
 * {@code BaseUtil.methodName()}.
 * <p>
 * Functional style: Several methods accept Predicate or lambda parameters
 * to customize behavior without subclassing or verbose anonymous classes.
 */
public class BaseUtil {

  /** Log tag for this utility class. */
  public static final String TAG = BaseUtil.class.getSimpleName();

  /** Medium screen width threshold in dp (600dp). */
  public static final int MEDIUM_SCREEN_WIDTH_SIZE = 600;

  /** Large screen width threshold in dp (1240dp). */
  public static final int LARGE_SCREEN_WIDTH_SIZE = 1240;

  /** Tag identifier for on global layout listener. */
  private static final int TAG_ON_GLOBAL_LAYOUT_LISTENER = -8;

  /** Cached delta for decor view height calculations. */
  private static int sDecorViewDelta = 0;

  /**
   * Copy the given text to the system clipboard.
   * <p>
   * If {@code withToast} is true, also shows a short toast confirming the copy action.
   * Uses the application context's clipboard manager to set the primary clipboard.
   *
   * @param text the text to copy to the clipboard
   * @param withToast whether to show a toast confirmation after copying
   */
  public static void copyToClipBoard(String text, boolean withToast) {
    copyToClipBoard(text);
    // Functional predicate: only show toast if withToast is true
    if (isFlagTrue(withToast)) {
      toastShort(R.string.copied_to_clipboard);
    }
  }

  /**
   * Copy text to the clipboard only (no toast).
   * <p>
   * Retrieves the clipboard service from the application context and sets
   * the primary clip to the given plain text.
   *
   * @param text the text to copy to the clipboard
   */
  public static void copyToClipBoard(String text) {
    ClipboardManager clipboardManager =
        (ClipboardManager) IdeApplication.getGlobalSystemService(Context.CLIPBOARD_SERVICE);
    ClipData clipData = ClipData.newPlainText("", text);
    clipboardManager.setPrimaryClip(clipData);
  }

  /**
   * Functional helper: check if a boolean flag is true.
   * <p>
   * Used internally to avoid verbose if-else blocks for simple boolean checks.
   *
   * @param flag the boolean flag to check
   * @return the flag value
   */
  private static boolean isFlagTrue(boolean flag) {
    return flag;
  }

  /**
   * Show a short toast message.
   * <p>
   * Runs on the UI thread via {@link AsyncTask#runOnUiThread(Runnable)} to ensure
   * the toast appears correctly on the main thread.
   *
   * @param stringRes the string resource ID for the toast message
   */
  public static void toastShort(final @StringRes int stringRes) {
    AsyncTask.runOnUiThread(
        () -> Toast.makeText(IdeApplication.getGlobalContext(), stringRes, Toast.LENGTH_SHORT)
            .show());
  }

  /**
   * Show a short toast message with a plain string.
   *
   * @param message the message string to display
   */
  public static void toastShort(final String message) {
    AsyncTask.runOnUiThread(
        () -> Toast.makeText(IdeApplication.getGlobalContext(), message, Toast.LENGTH_SHORT).show());
  }

  /** Show a long toast message. */
  public static void toastLong(final String message) {
    AsyncTask.runOnUiThread(
        () -> Toast.makeText(IdeApplication.getGlobalContext(), message, Toast.LENGTH_LONG).show());
  }

  /** Show a long toast with a string resource. */
  public static void toastLong(final @StringRes int stringRes) {
    AsyncTask.runOnUiThread(
        () -> Toast.makeText(IdeApplication.getGlobalContext(), stringRes, Toast.LENGTH_LONG).show());
  }

  /** Return whether a view is currently GONE (collapsed/hidden). */
  public static boolean isCollapsed(View v) {
    return v.getVisibility() == View.GONE;
  }

  /** Check if a view is expanded/visible. */
  public static boolean isExpanded(View v) {
    return v.getVisibility() == View.VISIBLE;
  }

  /** Check if the current screen is a large screen size. */
  public static boolean isLargeScreenSize() {
    return getScreenSize() >= LARGE_SCREEN_WIDTH_SIZE;
  }

  /** Check if the current screen is a medium screen size. */
  public static boolean isMediumScreenSize() {
    int size = getScreenSize();
    return size >= MEDIUM_SCREEN_WIDTH_SIZE && size < LARGE_SCREEN_WIDTH_SIZE;
  }

  /** Check if the current screen is a small screen size. */
  public static boolean isSmallScreenSize() {
    return getScreenSize() < MEDIUM_SCREEN_WIDTH_SIZE;
  }

  /** Get the current screen width in dp. */
  public static int getScreenSize() {
    Configuration configuration = IdeApplication.getGlobalConfiguration();
    return configuration.screenWidthDp;
  }

  /** Callback interface for soft input height changes. */
  public interface OnSoftInputChangedListener {
    void onSoftInputChanged(int height);
  }

  /**
   * Check if soft input (keyboard) is visible.
   * <p>
   * Usage: {@code BaseUtil.isSoftInputVisible(activity)}
   * <p>
   * Checks the decor view's invisible height to determine if the keyboard is showing.
   *
   * @param activity The activity to check
   * @return {@code true} if soft input is visible, {@code false} otherwise
   */
  public static boolean isSoftInputVisible(@NonNull final Activity activity) {
    return getDecorViewInvisibleHeight(activity.getWindow()) > 0;
  }

  /** Get the height of the invisible area below the decor view. */
  private static int getDecorViewInvisibleHeight(@NonNull final Window window) {
    final View decorView = window.getDecorView();
    final Rect outRect = new Rect();
    decorView.getWindowVisibleDisplayFrame(outRect);
    int delta = Math.abs(decorView.getBottom() - outRect.bottom);
    if (delta <= getNavBarHeight() + getStatusBarHeight()) {
      sDecorViewDelta = delta;
      return 0;
    }
    return delta - sDecorViewDelta;
  }

  /** Get the navigation bar height from system resources. */
  public static int getNavBarHeight() {
    Resources res = Resources.getSystem();
    int resourceId = res.getIdentifier("navigation_bar_height", "dimen", "android");
    if (resourceId != 0) {
      return res.getDimensionPixelSize(resourceId);
    }
    return 0;
  }

  /** Get the status bar height from system resources. */
  public static int getStatusBarHeight() {
    Resources resources = Resources.getSystem();
    int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
    if (resourceId != 0) {
      return resources.getDimensionPixelSize(resourceId);
    }
    return 0;
  }

  /** Open a URL in the default browser. */
  public static void openUrl(String url) {
    try {
      var mIntent = new Intent(Intent.ACTION_VIEW);
      mIntent.setData(Uri.parse(url));
      IdeApplication.getGlobalContext().startActivity(mIntent);
    } catch (Throwable throwable) {
      toastShort(throwable.getMessage());
      ILog.error(TAG, "Failed to open url", throwable);
    }
  }

  /** Open a URL outside of the current activity (new task). */
  public static void openUrlOutsideActivity(String url) {
    try {
      var mIntent = new Intent(Intent.ACTION_VIEW);
      mIntent.setData(Uri.parse(url));
      mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      IdeApplication.getGlobalContext().startActivity(mIntent);
    } catch (Throwable throwable) {
      toastLong(throwable.getMessage());
      ILog.error(TAG, "Failed to open url", throwable);
    }
  }

  /**
   * Register a soft input changed listener.
   * <p>
   * Listens for changes in the soft keyboard height and callbacks the provided listener.
   * The listener will be called whenever the keyboard height changes.
   *
   * @param activity The activity whose window to monitor
   * @param listener The listener to receive height change events
   */
  public static void registerSoftInputChangedListener(
      @NonNull final Activity activity, @NonNull final OnSoftInputChangedListener listener) {
    registerSoftInputChangedListener(activity.getWindow(), listener);
  }

  /**
   * Register a soft input changed listener on a window.
   * <p>
   * Sets up a global layout listener on the window's content view to detect
   * soft keyboard height changes.
   *
   * @param window The window to monitor
   * @param listener The listener to receive height change events
   */
  public static void registerSoftInputChangedListener(
      @NonNull final Window window, @NonNull final OnSoftInputChangedListener listener) {
    final int flags = window.getAttributes().flags;
    if ((flags & WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS) != 0) {
      window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }
    final FrameLayout contentView = window.findViewById(android.R.id.content);
    final int[] decorViewInvisibleHeightPre = {getDecorViewInvisibleHeight(window)};
    // Lambda: onGlobalLayoutListener reacts to layout changes
    OnGlobalLayoutListener onGlobalLayoutListener =
        () -> {
          int height = getDecorViewInvisibleHeight(window);
          if (!equal(decorViewInvisibleHeightPre[0], height)) {
            listener.onSoftInputChanged(height);
            decorViewInvisibleHeightPre[0] = height;
          }
        };
    contentView.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
    contentView.setTag(TAG_ON_GLOBAL_LAYOUT_LISTENER, onGlobalLayoutListener);
  }

  /** Unregister soft input changed listener for an activity. */
  public static void unregisterSoftInputChangedListener(@NonNull final Activity activity) {
    unregisterSoftInputChangedListener(activity.getWindow());
  }

  /** Unregister soft input changed listener for a window. */
  public static void unregisterSoftInputChangedListener(@NonNull final Window window) {
    final View contentView = window.findViewById(android.R.id.content);
    if (contentView == null) {
      return;
    }
    Object tag = contentView.getTag(TAG_ON_GLOBAL_LAYOUT_LISTENER);
    if (tag instanceof OnGlobalLayoutListener) {
      contentView.getViewTreeObserver()
          .removeOnGlobalLayoutListener((OnGlobalLayoutListener) tag);
      contentView.setTag(TAG_ON_GLOBAL_LAYOUT_LISTENER, null);
    }
  }

  /** Share the application link via the Play Store. */
  public static void shareAppPlayStoreLink() {
    shareAppPlayStoreLink(IdeApplication.getGlobalContext());
  }

  /** Share the application link via the Play Store (custom context). */
  public static void shareAppPlayStoreLink(Context ctx) {
    ShareCompat.IntentBuilder shareIntent =
        new ShareCompat.IntentBuilder(ctx);
    shareIntent.setType("text/plain");
    String appName = ctx.getString(R.string.app_name);
    shareIntent.setChooserTitle(appName);
    shareIntent.setText(
        ctx.getString(R.string.share_app_info, appName, Constants.CHECK_UPDATE_GITHUB_URL));
    shareIntent.getIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    shareIntent.startChooser();
  }

  /** Toggle the soft input display (show/hide keyboard). */
  public static void toggleSoftInput() {
    InputMethodManager imm =
        (InputMethodManager) IdeApplication.getInstance().getSystemService(
            Context.INPUT_METHOD_SERVICE);
    if (imm == null) {
      return;
    }
    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
  }

  /**
   * Start an object animation on a view.
   * <p>
   * Creates an ObjectAnimator with the given property name and value,
   * then starts the animation.
   *
   * @param view the view to animate
   * @param propertyName the animation property name (e.g., "alpha", "translationX")
   * @param value the target value for the animation
   * @param duration the animation duration in milliseconds
   */
  public static void startObjectAnimation(
      View view, String propertyName, double value, double duration) {
    ObjectAnimator anim = ObjectAnimator.ofFloat(view, propertyName, (float) value);
    anim.setDuration((long) duration);
    anim.start();
  }

  /** Rotate a chevron image view to indicate open/close state. */
  public static void rotateChevron(boolean isOpen, ImageView chevronView) {
    float startRotation = isOpen ? -90f : 0f;
    float endRotation = isOpen ? 0f : -90f;

    RotateAnimation rotateAnimation =
        new RotateAnimation(
            startRotation,
            endRotation,
            RotateAnimation.RELATIVE_TO_SELF,
            0.5f,
            RotateAnimation.RELATIVE_TO_SELF,
            0.5f);
    rotateAnimation.setDuration(200);
    rotateAnimation.setFillAfter(true);
    chevronView.startAnimation(rotateAnimation);
  }

  /** Show an exit confirmation dialog. */
  private void showExitDialog(
      @NonNull Context context, @NonNull Event<Boolean> event, @Nullable Runnable action) {
    if (event.getContentIfNotHandled()) {
      new MaterialAlertDialogBuilder(context)
          .setTitle(R.string.close_app)
          .setMessage(
              context.getString(
                  R.string.close_app_confirmation, context.getString(R.string.app_name)))
          .setNegativeButton(android.R.string.no, null)
          .setPositiveButton(
              android.R.string.yes,
              (d, w) -> {
                if (action != null) {
                  action.run();
                }
              })
          .show();
    }
  }

  /**
   * Apply window insets to a view's margin.
   * <p>
   * Uses a lambda listener to apply window insets (system bars) to the view's layout params.
   *
   * @param view the view to apply insets to
   * @param left whether to apply left inset
   * @param top whether to apply top inset
   * @param right whether to apply right inset
   * @param bottom whether to apply bottom inset
   * @param insetFlag which inset flag to extract (e.g., WindowInsetsCompat.TYPE_WINDOW_NAVIGATION_BAR)
   */
  public static void applyWindowInsetToMargin(
      View view, boolean left, boolean top, boolean right, boolean bottom, int insetFlag) {
    ViewCompat.setOnApplyWindowInsetsListener(
        view,
        (v, windowInsets) -> {
          Insets insets = windowInsets.getInsets(insetFlag);
          ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();

          lp.topMargin = (top ? insets.top : 0);
          lp.bottomMargin = (bottom ? insets.bottom : 0);
          lp.leftMargin = left ? insets.left : 0;
          lp.rightMargin = (right ? insets.right : 0);

          v.setLayoutParams(lp);
          return windowInsets;
        });
  }

  /**
   * Apply window insets to a view's padding.
   * <p>
   * Similar to {@link #applyWindowInsetToMargin} but for padding instead of margins.
   *
   * @param view the view to apply insets to
   * @param left whether to apply left padding inset
   * @param top whether to apply top padding inset
   * @param right whether to apply right padding inset
   * @param bottom whether to apply bottom padding inset
   * @param insetFlag which inset flag to extract
   * @param traverse whether to traverse to child views
   */
  public static void applyWindowInsetToPadding(
      View view,
      boolean left,
      boolean top,
      boolean right,
      boolean bottom,
      int insetFlag,
      boolean traverse) {
    final int initialLeft = view.getPaddingLeft();
    final int initialTop = view.getPaddingTop();
    final int initialRight = view.getPaddingRight();
    final int initialBottom = view.getPaddingBottom();

    ViewCompat.setOnApplyWindowInsetsListener(
        view,
        (v, windowInsets) -> {
          Insets insets = windowInsets.getInsets(insetFlag);
          v.setPadding(
              initialLeft + (left ? insets.left : 0),
              initialTop + (top ? insets.top : 0),
              initialRight + (right ? insets.right : 0),
              initialBottom + (bottom ? insets.bottom : 0));

          if (traverse) {
            return windowInsets;
          } else {
            return WindowInsetsCompat.CONSUMED;
          }
        });
  }

  /**
   * Apply IME insets to keep a view visible above the soft keyboard.
   * <p>
   * Adjusts the view's padding and optionally animates a translation Y
   * to keep the view visible when the keyboard appears.
   *
   * @param target The view that should remain visible (e.g., bottom sheet, header)
   * @param animate whether to animate the translation
   */
  public static void applyImeInsets(@NonNull final View target, boolean animate) {
    ViewCompat.setOnApplyWindowInsetsListener(
        target,
        (v, insets) -> {
          Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
          Insets navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());

          int bottomInset = Math.max(imeInsets.bottom, navInsets.bottom);

          v.setPaddingRelative(
              v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottomInset);

          if (animate) {
            v.animate().setDuration(200).translationY(-imeInsets.bottom).start();
          }
          return insets;
        });
  }

  /** Display a simple dialog with a message and OK button. */
  public static void displayDialog(@NonNull Context context, int message) {
    new MaterialAlertDialogBuilder(context)
        .setMessage(message)
        .setPositiveButton(R.string.ok, null)
        .show();
  }

  /** Apply edge-to-edge display settings. */
  public static void enforceEdgeToEdge(Window window, boolean edgeToEdgeEnabled) {
    WindowCompat.setDecorFitsSystemWindows(window, !edgeToEdgeEnabled);
  }

  /** Convert dp to pixels. */
  public static int dpToPx(float dp) {
    return Math.round(
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            IdeApplication.getGlobalResources().getDisplayMetrics()));
  }

  /** Convert pixels to dp. */
  public static int pxToDp(float px) {
    return Math.round(px / IdeApplication.getGlobalResources().getDisplayMetrics().density);
  }

  /** Check if the device is connected to the internet. */
  public static boolean isConnected() {
    var provider = new IPProvider(IdeApplication.getGlobalContext());
    return provider.isConnected();
  }

  /** Get memory usage string for the given runtime. */
  public static String getMemoryUsage(Runtime runtime) {
    long maxMemoryInBytes = runtime.maxMemory();
    long availableMemInBytes =
        maxMemoryInBytes - (runtime.totalMemory() - runtime.freeMemory());
    long usedMemInBytes = maxMemoryInBytes - availableMemInBytes;
    long usedMemInPercentage = usedMemInBytes * 100 / maxMemoryInBytes;

    return IdeApplication.getGlobalContext()
        .getString(
            R.string.app_memory_usage,
            Formatter.formatShortFileSize(IdeApplication.getGlobalContext(), usedMemInBytes),
            Formatter.formatShortFileSize(IdeApplication.getGlobalContext(), maxMemoryInBytes),
            usedMemInPercentage);
  }

  /** Create a new snack bar builder. */
  public static SnackBarBuilder newSnackBarBuilder() {
    return new SnackBarBuilder();
  }

  /** Builder class for creating SnackBar instances with a fluent API. */
  public static class SnackBarBuilder {
    private Context context;
    private String message;
    private View view;
    private View anchorView;
    private int messageMaxLines;
    private DURATION duration = DURATION.SHORT;
    private String actionDescription;
    private View.OnClickListener actionViewOnClickListener;
    private int messageColor, actionTextColor, backgroundTint;

    /** Build and show the snack bar. */
    public void create() {
      Objects.requireNonNull(view, "No view was not set for SnackBar");
      Objects.requireNonNull(message, "Message was not set for SnackBar");

      context = context == null ? view.getContext() : context;
      final Snackbar snackbar = Snackbar.make(context, view, message, duration.get());

      if (actionDescription != null && actionViewOnClickListener == null) {
        snackbar.setAction(actionDescription, null);
      } else if (actionDescription != null) {
        snackbar.setAction(actionDescription, actionViewOnClickListener);
      }

      if (anchorView != null) {
        snackbar.setAnchorView(anchorView);
      }

      if (messageMaxLines != 0) {
        snackbar.setTextMaxLines(messageMaxLines);
      }

      if (messageColor != 0) {
        snackbar.setTextColor(ContextCompat.getColor(context, messageColor));
      }

      if (actionTextColor != 0) {
        snackbar.setActionTextColor(ContextCompat.getColor(context, actionTextColor));
      }

      if (backgroundTint != 0) {
        snackbar.setBackgroundTint(ContextCompat.getColor(context, backgroundTint));
      }

      snackbar.show();
    }

    /** Set the action click listener. */
    public SnackBarBuilder setActionClickListener(View.OnClickListener listener) {
      this.actionViewOnClickListener = listener;
      return this;
    }

    /** Set the action description text. */
    public SnackBarBuilder setActionDescription(String description) {
      this.actionDescription = description;
      return this;
    }

    /** Set the action text color resource ID. */
    public SnackBarBuilder setActionTextColor(int color) {
      this.actionTextColor = color;
      return this;
    }

    /** Set the anchor view for the snack bar. */
    public SnackBarBuilder setAnchorView(View anchor) {
      this.anchorView = anchor;
      return this;
    }

    /** Set the background tint color resource ID. */
    public SnackBarBuilder setBackgroundTint(int tint) {
      this.backgroundTint = tint;
      return this;
    }

    /** Set the context for the snack bar. */
    public SnackBarBuilder setContext(Context context) {
      this.context = context;
      return this;
    }

    /** Set the duration of the snack bar. */
    public SnackBarBuilder setDuration(DURATION duration) {
      this.duration = duration;
      return this;
    }

    /** Set the message text. */
    public SnackBarBuilder setMessage(String message) {
      this.message = message;
      return this;
    }

    /** Set the message color resource ID. */
    public SnackBarBuilder setMessageColor(int color) {
      this.messageColor = color;
      return this;
    }

    /** Set the maximum number of lines for the message. */
    public SnackBarBuilder setMessageMaxLines(int messageMaxLines) {
      this.messageMaxLines = messageMaxLines;
      return this;
    }

    /** Set the view for the snack bar. */
    public SnackBarBuilder setView(View view) {
      this.view = view;
      return this;
    }

    /** Duration options for snack bar. */
    public enum DURATION {
      SHORT(Snackbar.LENGTH_SHORT),
      LONG(Snackbar.LENGTH_LONG),
      INDEFINITE(Snackbar.LENGTH_INDEFINITE);

      private final int duration;

      DURATION(final int duration) {
        this.duration = duration;
      }

      /** Get the integer duration value for Snackbar. */
      public int get() {
        return this.duration;
      }
    }
  }
}