/*
 * This file is part of CodeOps Studio.
 * Copyright (C) 2024-2026 Etido Peter
 *
 * JNI bridge to libcooda_native.so (C++23 backend).
 */

package com.eup.codeopsstudio.nativebridge;

import androidx.annotation.NonNull;

/**
 * Loads {@code cooda_native} and exposes native entry points.
 * Keep this class thin — heavy logic lives in C++ modules.
 */
public final class NativeBackend {

    private static final boolean LOADED;

    static {
        boolean ok = false;
        try {
            System.loadLibrary("cooda_native");
            ok = true;
        } catch (UnsatisfiedLinkError e) {
            // Library missing on hosts without NDK build; callers check isAvailable().
        }
        LOADED = ok;
    }

    private NativeBackend() {}

    public static boolean isAvailable() {
        return LOADED;
    }

    /**
     * Smoke-test entry: returns native library version string.
     */
    @NonNull
    public static native String nativeVersion();
}
