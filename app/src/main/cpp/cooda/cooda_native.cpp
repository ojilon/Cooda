/**
 * Cooda native backend entry — C++23, STL, RAII.
 * JNI surface for NativeBackend.
 */

#include <jni.h>

#include <string>
#include <string_view>

#include "zip_heuristics.hpp"

namespace cooda {
namespace {

[[nodiscard]] std::string make_version() {
    constexpr std::string_view kVersion = "cooda_native/0.2.0+cxx23+zip";
    return std::string{kVersion};
}

}  // namespace
}  // namespace cooda

extern "C" JNIEXPORT jstring JNICALL
Java_com_eup_codeopsstudio_nativebridge_NativeBackend_nativeVersion(JNIEnv* env, jclass /*clazz*/) {
    const auto version = cooda::make_version();
    return env->NewStringUTF(version.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_eup_codeopsstudio_nativebridge_NativeBackend_isZipCandidateNative(
    JNIEnv* env, jclass /*clazz*/, jstring path_or_name, jstring mime) {
    const char* path_c = path_or_name != nullptr ? env->GetStringUTFChars(path_or_name, nullptr) : nullptr;
    const char* mime_c = mime != nullptr ? env->GetStringUTFChars(mime, nullptr) : nullptr;

    const std::string_view path_sv = path_c != nullptr ? std::string_view{path_c} : std::string_view{};
    const std::string_view mime_sv = mime_c != nullptr ? std::string_view{mime_c} : std::string_view{};

    const bool result = cooda::zip::is_zip_candidate(path_sv, mime_sv);

    if (path_c != nullptr) {
        env->ReleaseStringUTFChars(path_or_name, path_c);
    }
    if (mime_c != nullptr) {
        env->ReleaseStringUTFChars(mime, mime_c);
    }
    return result ? JNI_TRUE : JNI_FALSE;
}
