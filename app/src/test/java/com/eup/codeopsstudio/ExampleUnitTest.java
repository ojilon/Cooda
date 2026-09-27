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

/**
 * Example local unit test for CodeOps Studio, executed on the development machine (host).
 * <p>
 * This test verifies basic Java string operations and is Kotlin-free.
 * Extend these tests to cover utility methods, model validation, or other
 * core logic as the app evolves.
 */
package com.eup.codeopsstudio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Example local unit test, which will execute on the development machine (host).
 * <p>
 * Runs on the host JVM (not on a device), so it has no Android dependencies.
 * Use this to test pure Java logic, model validation, or utility functions
 * that do not require an Android environment.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    /**
     * Verifies that basic string operations work correctly.
     * <p>
     * This is a placeholder test that can be replaced with meaningful tests
     * for BaseUtil, LogModel, or other Kotlin-free utility classes.
     */
    @Test
    public void stringOperations_test() {
        String test = "CodeOps Studio";
        assertNotNull(test);
        assertEquals(16, test.length());
    }

    /**
     * Verifies basic arithmetic, can be used as a smoke test.
     */
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }
}