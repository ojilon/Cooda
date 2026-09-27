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

package com.eup.codeopsstudio.editor.langs.widget.component;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eup.codeopsstudio.editor.databinding.EditorCompletionResultItemBinding;
import com.eup.codeopsstudio.editor.langs.completion.ContextualCompletionItem;

import io.github.rosemoe.sora.lang.completion.CompletionItem;
import io.github.rosemoe.sora.widget.component.EditorCompletionAdapter;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;

/**
 * Completion mAdapter to display results
 *
 * @author Etido Peter
 */
public final class ContextualEditorCompletionAdapter extends EditorCompletionAdapter {

    private int itemHeight = 45; // dp

    @Override
    public int getItemHeight() {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, itemHeight, getContext()
            .getResources().getDisplayMetrics());
    }

    @Override
    public View getView(int pos, View view, ViewGroup parent, boolean isCurrentCursorPosition) {
        final EditorCompletionResultItemBinding binding;
        if (view == null) {
            binding = EditorCompletionResultItemBinding.inflate(
                LayoutInflater.from(getContext()), parent, false);
            view = binding.getRoot();
            view.setTag(binding);
        } else {
            binding = (EditorCompletionResultItemBinding) view.getTag();
        }
        CompletionItem item = getItem(pos);

        binding.resultItemLabel.setText(item.label);
        binding.resultItemDesc.setText(item.desc);

        if (item instanceof ContextualCompletionItem) {
            ContextualCompletionItem comp = (ContextualCompletionItem) getItem(pos);
            if (comp != null) {
                binding.resultItemCompDesc.setText(comp.compDescription);
                binding.resultCompDescHolder.setVisibility(View.VISIBLE);
            } else if (binding.resultCompDescHolder.getVisibility() == View.VISIBLE) {
                binding.resultCompDescHolder.setVisibility(View.GONE);
            }
        }

        if (isCurrentCursorPosition) {
            view.setBackgroundColor(getThemeColor(EditorColorScheme.COMPLETION_WND_ITEM_CURRENT));
        } else {
            view.setBackgroundColor(0);
        }
        binding.resultItemImage.setImageDrawable(item.icon);
        return view;
    }

    public void setItemHeight(final int value) {
        this.itemHeight = value;
    }
}
