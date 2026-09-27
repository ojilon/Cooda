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
package com.eup.codeopsstudio.plugin;

import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import java.util.Objects;

/**
 * Java port of PluginItem (was Kotlin). keeps the exact field/method surface
 * so Java call sites are unaffected.
 */
public class PluginItem {

  @NonNull public final String id;
  @Nullable public final String name;
  @Nullable public final String author;
  @Nullable public final Drawable icon;
  @Nullable public final String description;
  @Nullable public final String downloadUrl;

  public PluginItem(
      @NonNull String id,
      @Nullable String name,
      @Nullable String author,
      @Nullable Drawable icon,
      @Nullable String description) {
    this(id, name, author, icon, description, null);
  }

  public PluginItem(
      @NonNull String id,
      @Nullable String name,
      @Nullable String author,
      @Nullable Drawable icon,
      @Nullable String description,
      @Nullable String downloadUrl) {
    this.id = id;
    this.name = name;
    this.author = author;
    this.icon = icon;
    this.description = description;
    this.downloadUrl = downloadUrl;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PluginItem that = (PluginItem) o;
    return id.equals(that.id)
        && Objects.equals(name, that.name)
        && Objects.equals(author, that.author)
        && Objects.equals(description, that.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, description);
  }

  public static final DiffUtil.ItemCallback<PluginItem> DIFF_CALLBACK =
      new DiffUtil.ItemCallback<PluginItem>() {
        @Override
        public boolean areItemsTheSame(
            @NonNull PluginItem oldItem, @NonNull PluginItem newItem) {
          return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(
            @NonNull PluginItem oldItem, @NonNull PluginItem newItem) {
          return oldItem.equals(newItem);
        }
      };
}
