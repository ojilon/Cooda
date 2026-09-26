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

package com.eup.codeopsstudio.palette;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.eup.codeopsstudio.databinding.ItemCommandBinding;
import com.eup.codeopsstudio.databinding.ItemCommandHeaderBinding;


/**
 * A specialized {@link androidx.recyclerview.widget.ListAdapter} for rendering palette items.
 *
 * @author Etido Peter
 */
public class PaletteAdapter extends ListAdapter<PaletteItem, RecyclerView.ViewHolder> {

  private int selectedPosition = 0;

  public PaletteAdapter() {
    super(PaletteItem.DIFF_CALLBACK);
  }

  @NonNull
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    if (viewType == PaletteItem.TYPE_HEADER) {
      return new HeaderViewHolder(ItemCommandHeaderBinding.inflate(inflater, parent, false));
    }
    return new CommandViewHolder(ItemCommandBinding.inflate(inflater, parent, false));
  }

  @Override
  public int getItemViewType(int position) {
    return getItem(position).getType();
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    PaletteItem item = getItem(position);

    if (holder instanceof HeaderViewHolder) {
      ((HeaderViewHolder) holder).tvHeader.setText(item.getTitle());
    } else if (holder instanceof CommandViewHolder) {
      CommandViewHolder cmd = (CommandViewHolder) holder;
      cmd.tvTitle.setText(item.getTitle());

      bindText(cmd.tvSubtitle, item.getSubtitle());
      bindText(cmd.tvTag, item.getTag());
      bindText(cmd.tvShortcut, item.getShortcut());

      cmd.itemView.setOnClickListener(
          v -> {
            if (item.getAction() != null) item.getAction().run();
          });

      cmd.itemView.setActivated(position == selectedPosition);
    }
  }

  private void bindText(TextView tv, String text) {
    if (text != null && !text.isEmpty()) {
      tv.setText(text);
      tv.setVisibility(View.VISIBLE);
    } else {
      tv.setVisibility(View.GONE);
    }
  }

  public int getSelectedPosition() {
    return selectedPosition;
  }

  public void setSelectedPosition(int position) {
    int oldPosition = selectedPosition;
    selectedPosition = position;
    // Refresh only the two rows affected to avoid flickering
    notifyItemChanged(oldPosition);
    notifyItemChanged(selectedPosition);
  }

  static class HeaderViewHolder extends RecyclerView.ViewHolder {
    final TextView tvHeader;

    HeaderViewHolder(ItemCommandHeaderBinding binding) {
      super(binding.getRoot());
      tvHeader = binding.tvHeader;
    }
  }

  static class CommandViewHolder extends RecyclerView.ViewHolder {
    final TextView tvTitle, tvSubtitle, tvTag, tvShortcut;

    CommandViewHolder(ItemCommandBinding binding) {
      super(binding.getRoot());
      tvTitle = binding.tvTitle;
      tvSubtitle = binding.tvSubtitle;
      tvTag = binding.tvTag;
      tvShortcut = binding.tvShortcut;
    }
  }
}
