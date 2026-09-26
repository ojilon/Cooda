/**
 * ZIP / archive path heuristics for Cooda (C++23).
 * Pure string checks — no I/O. Used from JNI and host builds.
 */
#pragma once

#include <string_view>

namespace cooda::zip {

/** True if path/name ends with .zip (case-insensitive) or mime is application/zip. */
[[nodiscard]] bool is_zip_candidate(std::string_view path_or_name,
                                    std::string_view mime) noexcept;

}  // namespace cooda::zip
