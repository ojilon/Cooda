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

package com.eup.codeopsstudio.logger;

/**
 * Enum representing the different log levels used throughout CodeOps Studio.
 * <p>
 * Each level has an associated capitalized string representation
 * used for display in the UI and log output.
 * <p>
 * Functional style: Use {@link #valueOfLevel(String)} to look up a level
 * by its string name, or iterate {@link #values()} for all levels.
 */
public enum LogLevel {

  WARN("WARNING"),
  INFO("INFO"),
  DEBUG("DEBUG"),
  ERROR("ERROR");

  /** The display string for this log level. */
  private final String level;

  LogLevel(String level) {
	this.level = level;
  }

  /** Get the display string for this log level. */
  public String getLevel() {
	return level;
  }

  /**
   * Look up a LogLevel by its display string.
   * <p>
   * Functional approach: returns the matching level or {@code null} if not found.
   * Use this instead of the old iterative {@code getLevel()} method.
   *
   * @param level the log level string (e.g., "DEBUG", "ERROR")
   * @return the matching LogLevel, or {@code null} if not found
   */
  public static LogLevel valueOfLevel(String level) {
	for (LogLevel logLevel : values()) {
	  if (logLevel.level.equals(level)) {
		return logLevel;
	  }
	}
	return null;
  }
}