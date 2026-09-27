/*
 * This file is part of CodeOps Studio.
 * Copyright (C) 2024-2026 Etido Peter
 *
 * Extracted from MainFragment to keep the fragment focused on lifecycle
 * and coordination. Menu selection/prepare logic is pure UI orchestration.
 */

package com.eup.codeopsstudio.ui.menu;

import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eup.codeopsstudio.R;
import com.eup.codeopsstudio.pane.Pane;
import com.eup.codeopsstudio.ui.editor.code.CodeEditorPane;
import com.eup.codeopsstudio.ui.editor.panes.WebViewPane;
import com.eup.codeopsstudio.util.BaseUtil;
import com.eup.codeopsstudio.util.Wizard;
import com.eup.codeopsstudio.viewmodel.MainViewModel;

/**
 * Owns toolbar menu selection and preparation for the main IDE surface.
 * Keeps {@link com.eup.codeopsstudio.MainFragment} thinner.
 */
public final class ToolbarMenuController {

    /** Resolves the currently selected pane of a given type (or null). */
    public interface PaneSelector {
        @Nullable
        <T extends Pane> T selected(@NonNull Class<T> type);
    }

    private final MainViewModel mainViewModel;
    private final PaneSelector paneSelector;
    private final Host host;

    /** Minimal host surface for activity-level actions (new/close window). */
    public interface Host {
        void newWindow();
        void closeThisWindow();
    }

    public ToolbarMenuController(
            @NonNull MainViewModel mainViewModel,
            @NonNull PaneSelector paneSelector,
            @NonNull Host host) {
        this.mainViewModel = mainViewModel;
        this.paneSelector = paneSelector;
        this.host = host;
    }

    public boolean onMenuItemSelected(@NonNull MenuItem item) {
        final int id = item.getItemId();
        final CodeEditorPane editorPane = paneSelector.selected(CodeEditorPane.class);
        final WebViewPane webViewPane = paneSelector.selected(WebViewPane.class);

        if (editorPane != null && editorPane.getEditor() != null) {
            return handleCodeEditorActions(item, id, editorPane);
        }
        if (webViewPane != null) {
            return handleWebViewActions(item, id, webViewPane);
        }

        if (id == R.id.menu_new_window) {
            host.newWindow();
            return true;
        }
        if (id == R.id.menu_close_window) {
            host.closeThisWindow();
            return true;
        }
        return false;
    }

    public void onPrepareMenu(@NonNull Menu menu) {
        CodeEditorPane editorPane = paneSelector.selected(CodeEditorPane.class);
        WebViewPane webViewPane = paneSelector.selected(WebViewPane.class);

        if (editorPane != null) {
            configureEditorMenu(menu, editorPane);
        } else if (webViewPane != null) {
            configureWebViewMenu(menu, webViewPane);
        } else {
            hideAllMenuGroups(menu);
        }
    }

    private boolean handleCodeEditorActions(
            @NonNull MenuItem item, int id, @NonNull CodeEditorPane editorPane) {
        if (id == R.id.menu_run) {
            editorPane.saveEditor();
            mainViewModel.setWebViewPaneFile(editorPane.getFile());
            return true;
        } else if (id == R.id.menu_undo) {
            editorPane.undo();
            return true;
        } else if (id == R.id.menu_redo) {
            editorPane.redo();
            return true;
        } else if (id == R.id.menu_save_file) {
            editorPane.saveEditor();
            return true;
        } else if (id == R.id.menu_save_as) {
            editorPane.saveAs();
            return true;
        } else if (id == R.id.menu_reload_file) {
            editorPane.reloadFile();
            return true;
        } else if (id == R.id.menu_reload_with_charset) {
            editorPane.showCharsetSelectionDialog();
            return true;
        } else if (id == R.id.menu_file_statistics) {
            editorPane.showStatistics();
            return true;
        } else if (id == R.id.menu_findFile) {
            editorPane.getSearchManager().openSearchPanel(true);
            return true;
        } else if (id == R.id.menu_jump_to_line) {
            editorPane.doJumpToLine();
            return true;
        } else if (id == R.id.menu_read_only_mode) {
            final boolean newState = !item.isChecked();
            editorPane.makeReadOnly(newState);
            item.setChecked(newState);
            return true;
        } else if (id == R.id.menu_copy_line) {
            editorPane.getEditor().copyText();
            return true;
        } else if (id == R.id.menu_delete_line) {
            editorPane.getEditor().deleteLine();
            return true;
        } else if (id == R.id.menu_replace_line) {
            editorPane.getEditor().replaceCurrLine();
            return true;
        } else if (id == R.id.menu_duplicate_line) {
            editorPane.getEditor().duplicateLine();
            return true;
        } else if (id == R.id.menu_convert_to_lowercase) {
            editorPane.getEditor().convertSelectionToLowerCase();
            return true;
        } else if (id == R.id.menu_convert_to_uppercase) {
            editorPane.getEditor().convertSelectionToUpperCase();
            return true;
        } else if (id == R.id.menu_reset_color_schemes) {
            editorPane.refreshEditorLanguageSyntax();
            return true;
        } else if (id == R.id.menu_cut_line) {
            editorPane.getEditor().cutLine();
            return true;
        } else if (id == R.id.menu_previous_cursor_position) {
            editorPane.navigateToPreviousCursorPosition();
            return true;
        } else if (id == R.id.menu_next_cursor_position) {
            editorPane.navigateToNextCursorPosition();
            return true;
        } else if (id == R.id.menu_increase_indent) {
            editorPane.increaseIndent();
            return true;
        } else if (id == R.id.menu_decrease_indent) {
            editorPane.decreaseIndent();
            return true;
        } else if (id == R.id.menu_soft_wrap) {
            final boolean newState = !item.isChecked();
            editorPane.setSoftWrapEnabled(newState);
            item.setChecked(newState);
            return true;
        } else if (id == R.id.menu_lite_mode) {
            final boolean newState = !item.isChecked();
            editorPane.setSmoothModeEnabled(newState);
            item.setChecked(newState);
            return true;
        } else if (id == R.id.menu_syntax_highlight) {
            editorPane.showLanguagePicker();
            return true;
        }
        return false;
    }

    private boolean handleWebViewActions(
            @NonNull MenuItem item, int id, @NonNull WebViewPane webViewPane) {
        final WebView webView = webViewPane.getWebView();
        final boolean newCheckedState = !item.isChecked();

        if (id == R.id.menu_undo) {
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                BaseUtil.toastShort(R.string.alrt_cannot_go_back);
            }
            return true;
        } else if (id == R.id.menu_redo) {
            if (webView.canGoForward()) {
                webView.goForward();
            } else {
                BaseUtil.toastShort(R.string.alrt_cannot_go_forward);
            }
            return true;
        } else if (id == R.id.menu_zoom) {
            webViewPane.setZoomable(newCheckedState);
            item.setChecked(newCheckedState);
            return true;
        } else if (id == R.id.menu_desktop_mode) {
            webViewPane.enableDeskTopMode(newCheckedState);
            item.setChecked(newCheckedState);
            return true;
        } else if (id == R.id.menu_refresh) {
            webViewPane.refresh();
            return true;
        } else if (id == R.id.menu_open_in_browser) {
            webViewPane.openInDeviceBrowser();
            return true;
        } else if (id == R.id.menu_copy_url) {
            final String url = webView.getOriginalUrl();
            if (!Wizard.isEmpty(url)) {
                BaseUtil.copyToClipBoard(url, true);
            }
            return true;
        }
        return false;
    }

    private void configureEditorMenu(@NonNull Menu menu, @NonNull CodeEditorPane editorPane) {
        menu.setGroupVisible(R.id.group_file_operations, true);
        menu.setGroupVisible(R.id.group_editor_actions, true);
        menu.setGroupVisible(R.id.group_unredo, true);

        MenuItem prev = menu.findItem(R.id.menu_previous_cursor_position);
        if (prev != null) {
            prev.setEnabled(editorPane.canNavigateToPrevious());
        }
        MenuItem next = menu.findItem(R.id.menu_next_cursor_position);
        if (next != null) {
            next.setEnabled(editorPane.canNavigateToNext());
        }
        MenuItem softWrap = menu.findItem(R.id.menu_soft_wrap);
        if (softWrap != null) {
            softWrap.setChecked(editorPane.isSoftWrapEnabled());
        }
        MenuItem liteMode = menu.findItem(R.id.menu_lite_mode);
        if (liteMode != null) {
            liteMode.setChecked(editorPane.isSmoothModeEnabled());
        }

        if (editorPane.isReadOnlyMode()) {
            disableEditorMenuItems(menu);
        } else {
            enableEditorMenuItems(menu, editorPane);
        }

        MenuItem run = menu.findItem(R.id.menu_run);
        if (run != null) {
            // NATIVE_CANDIDATE: markup detection could move to native later
            run.setVisible(com.eup.codeopsstudio.common.Constants.isMarkUp(editorPane.getFile()));
        }
        MenuItem save = menu.findItem(R.id.menu_save_file);
        if (save != null) {
            save.setEnabled(editorPane.isModified());
        }
        MenuItem readOnly = menu.findItem(R.id.menu_read_only_mode);
        if (readOnly != null) {
            readOnly.setChecked(editorPane.isReadOnlyMode());
        }
    }

    private void enableEditorMenuItems(@NonNull Menu menu, @NonNull CodeEditorPane editorPane) {
        MenuItem undoItem = menu.findItem(R.id.menu_undo);
        MenuItem redoItem = menu.findItem(R.id.menu_redo);

        if (undoItem != null && redoItem != null) {
            if (undoItem.getActionView() != null && redoItem.getActionView() != null) {
                undoItem.setEnabled(editorPane.canUndo());
                redoItem.setEnabled(editorPane.canRedo());
            } else {
                menu.setGroupEnabled(R.id.group_unredo, true);
                undoItem.setEnabled(editorPane.canUndo());
                redoItem.setEnabled(editorPane.canRedo());
            }
        }

        menu.setGroupVisible(R.id.group_content_edit, true);
        menu.setGroupEnabled(R.id.group_file_operations, true);
        MenuItem inc = menu.findItem(R.id.menu_increase_indent);
        MenuItem dec = menu.findItem(R.id.menu_decrease_indent);
        if (inc != null) {
            inc.setEnabled(true);
        }
        if (dec != null) {
            dec.setEnabled(true);
        }
    }

    private void disableEditorMenuItems(@NonNull Menu menu) {
        MenuItem undoItem = menu.findItem(R.id.menu_undo);
        MenuItem redoItem = menu.findItem(R.id.menu_redo);

        if (undoItem != null && redoItem != null) {
            if (undoItem.getActionView() != null && redoItem.getActionView() != null) {
                undoItem.setEnabled(false);
                redoItem.setEnabled(false);
            } else {
                menu.setGroupEnabled(R.id.group_unredo, false);
            }
        }

        menu.setGroupEnabled(R.id.group_content_edit, false);
        menu.setGroupEnabled(R.id.group_file_operations, false);
        MenuItem inc = menu.findItem(R.id.menu_increase_indent);
        MenuItem dec = menu.findItem(R.id.menu_decrease_indent);
        if (inc != null) {
            inc.setEnabled(false);
        }
        if (dec != null) {
            dec.setEnabled(false);
        }
    }

    private void configureWebViewMenu(@NonNull Menu menu, @NonNull WebViewPane webViewPane) {
        hideEditorMenuGroups(menu);
        menu.setGroupVisible(R.id.group_unredo, true);
        MenuItem live = menu.findItem(R.id.menu_liveserver);
        if (live != null) {
            live.setVisible(true);
        }
        MenuItem zoom = menu.findItem(R.id.menu_zoom);
        if (zoom != null) {
            zoom.setChecked(webViewPane.isZoomable());
        }
        MenuItem desk = menu.findItem(R.id.menu_desktop_mode);
        if (desk != null) {
            desk.setChecked(webViewPane.isDeskTopMode());
        }
        MenuItem redo = menu.findItem(R.id.menu_redo);
        if (redo != null) {
            redo.setEnabled(webViewPane.canRedo());
        }
        MenuItem undo = menu.findItem(R.id.menu_undo);
        if (undo != null) {
            undo.setEnabled(webViewPane.canUndo());
        }
    }

    private void hideAllMenuGroups(@NonNull Menu menu) {
        menu.setGroupVisible(R.id.group_file_operations, false);
        menu.setGroupVisible(R.id.group_content_edit, false);
        menu.setGroupVisible(R.id.group_editor_actions, false);
        menu.setGroupVisible(R.id.group_unredo, false);
        MenuItem live = menu.findItem(R.id.menu_liveserver);
        if (live != null) {
            live.setVisible(false);
        }
    }

    private void hideEditorMenuGroups(@NonNull Menu menu) {
        menu.setGroupVisible(R.id.group_file_operations, false);
        menu.setGroupVisible(R.id.group_content_edit, false);
        menu.setGroupVisible(R.id.group_editor_actions, false);
    }
}
