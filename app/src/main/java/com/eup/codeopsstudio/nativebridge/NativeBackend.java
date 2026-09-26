/*
 * This file is part of CodeOps Studio.
 * Copyright (C) 2024-2026 Etido Peter
 *
 * JNI bridge to libcooda_native.so (C++23 backend).
 */

package com.eup.codeopsstudio.nativebridge;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Loads {@code cooda_native} and exposes native entry points.
 * Keep this class thin — heavy logic lives in C++ modules.
 */
public final class NativeBackend {

    private static final String ZIP_MIME = "application/zip";
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

    /**
     * Whether {@code pathOrName} / {@code mimeType} looks like a ZIP archive.
     * Uses C++ heuristic when the library is loaded; otherwise pure Java fallback.
     */
    public static boolean isZipCandidate(@Nullable String pathOrName, @Nullable String mimeType) {
        final String path = pathOrName != null ? pathOrName : "";
        final String mime = mimeType != null ? mimeType : "";
        if (LOADED) {
            return isZipCandidateNative(path, mime);
        }
        return isZipCandidateJava(path, mime);
    }

    /** JNI: implemented in modules/zip_heuristics via cooda_native.cpp */
    private static native boolean isZipCandidateNative(@NonNull String pathOrName,
                                                       @NonNull String mimeType);

    /** Fallback when libcooda_native is not packaged (e.g. unit tests). */
    static boolean isZipCandidateJava(@NonNull String pathOrName, @NonNull String mimeType) {
        if (!mimeType.isEmpty() && ZIP_MIME.equalsIgnoreCase(mimeType)) {
            return true;
        }
        if (pathOrName.isEmpty()) {
            return false;
        }
        int slash = Math.max(pathOrName.lastIndexOf('/'), pathOrName.lastIndexOf('\\'));
        String name = slash >= 0 ? pathOrName.substring(slash + 1) : pathOrName;
        return name.toLowerCase().endsWith(".zip");
    }
}
