/**
 * Cooda native backend entry — C++23, STL, RAII.
 * Scaffold: version string for JNI smoke test.
 */

#include <jni.h>

#include <string>
#include <string_view>

namespace cooda {
namespace {

[[nodiscard]] std::string make_version() {
    // RAII: std::string owns the buffer; no manual new/delete.
    constexpr std::string_view kVersion = "cooda_native/0.1.0+cxx23";
    return std::string{kVersion};
}

}  // namespace
}  // namespace cooda

extern "C" JNIEXPORT jstring JNICALL
Java_com_eup_codeopsstudio_nativebridge_NativeBackend_nativeVersion(JNIEnv* env, jclass /*clazz*/) {
    const auto version = cooda::make_version();
    return env->NewStringUTF(version.c_str());
}
