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

/**
 * Immutable data model representing a single log entry.
 * <p>
 * This record holds all the essential properties of a log message:
 * icon, tag, log level, message, and timestamp.
 * <p>
 * Usage: Pass log parameters to the constructor or use the convenience
 * factory methods. The record is immutable and can be used directly
 * in functional pipelines, LiveData observations, or as a diff unit.
 */
public record LogModel(

    /** Optional icon resource ID for diagnostics */
    int mIcon,

    /** Log tag (e.g., "MainActivity", "Network") */
    String mTag,

    /** Log level (e.g., "DEBUG", "ERROR") */
    String mLevel,

    /** Date/time format string or null */
    String mDateFormat,

    /** The actual log message */
    String mMessage

) {

    /** Generate a unique ID for this log entry. */
    public UUID id() {
        return UUID.randomUUID();
    }

    /** Convenience constructor: basic log with just a message. */
    public LogModel(String message) {
        this(0, null, null, null, message);
    }

    /** Convenience constructor: diagnostics log with icon and message. */
    public LogModel(int icon, String message) {
        this(icon, null, null, null, message);
    }

    /** Convenience constructor: log with tag and level. */
    public LogModel(String tag, String level, String message) {
        this(null, tag, level, null, message);
    }

    /** Convenience constructor: log with date, tag, and level. */
    public LogModel(String date, String tag, String level, String message) {
        this(date, 0, tag, level, message);
    }

    /** Convenience constructor: log with date, icon, tag, level, and message. */
    public LogModel(
        String date,
        int icon,
        String tag,
        String level,
        String message
    ) {
        mDateFormat = date;
        mIcon = icon;
        mTag = tag;
        mLogLevel = level != null ? level : "INFO";
        mMessage = message;
    }

    /** Get the log icon resource ID. */
    public int icon() {
        return mIcon;
    }

    /** Get the log tag. */
    public String tag() {
        return mTag;
    }

    /** Get the log level string. */
    public String level() {
        return mLevel;
    }

    /** Get the date format string. */
    public String dateFormat() {
        return mDateFormat;
    }

    /** Get the log message. */
    public String message() {
        return mMessage;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(mMessage, mTag, mDateFormat, mLevel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogModel that = (LogModel) o;
        return java.util.Objects.equals(mMessage, that.message())
            && java.util.Objects.equals(mTag, that.tag())
            && java.util.Objects.equals(mDateFormat, that.dateFormat())
            && java.util.Objects.equals(mLevel, that.level());
    }
}