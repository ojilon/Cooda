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

package com.eup.codeopsstudio.logger.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable data model representing a single log entry.
 * <p>
 * Fields are {@link CharSequence} so both plain strings and styled
 * (e.g. {@link android.text.SpannableString}) content are accepted.
 */
public final class LogModel {

  private final int mIcon;
  private final CharSequence mTag;
  private final CharSequence mLevel;
  private final CharSequence mDateFormat;
  private final CharSequence mMessage;
  private final UUID id = UUID.randomUUID();

  /** Convenience constructor: basic log with just a message. */
  public LogModel(CharSequence message) {
    this(0, null, null, null, message);
  }

  /** Convenience constructor: diagnostics log with icon and message. */
  public LogModel(int icon, CharSequence message) {
    this(icon, null, null, null, message);
  }

  /** Convenience constructor: log with tag and level. */
  public LogModel(CharSequence tag, CharSequence level, CharSequence message) {
    this(0, tag, level, null, message);
  }

  /** Convenience constructor: log with date, tag, and level. */
  public LogModel(CharSequence date, CharSequence tag, CharSequence level, CharSequence message) {
    this(0, tag, level, date, message);
  }

  /** Full constructor: icon, tag, level, date, message. */
  public LogModel(int icon, CharSequence tag, CharSequence level, CharSequence date,
      CharSequence message) {
    this.mIcon = icon;
    this.mTag = tag;
    this.mLevel = level == null ? "INFO" : level;
    this.mDateFormat = date;
    this.mMessage = message;
  }

  /** Get the log icon resource ID. */
  public int getIcon() {
    return mIcon;
  }

  /** Get the log tag. */
  public CharSequence getTag() {
    return mTag;
  }

  /** Get the log level. */
  public CharSequence getLevel() {
    return mLevel;
  }

  /** Get the date format. */
  public CharSequence getDateFormat() {
    return mDateFormat;
  }

  /** Get the log message. */
  public CharSequence getMessage() {
    return mMessage;
  }

  /** Get the stable unique ID of this log entry. */
  public UUID getID() {
    return id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(mMessage, mTag, mDateFormat, mLevel);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LogModel that = (LogModel) o;
    return Objects.equals(mMessage, that.mMessage)
        && Objects.equals(mTag, that.mTag)
        && Objects.equals(mDateFormat, that.mDateFormat)
        && Objects.equals(mLevel, that.mLevel);
  }
}
