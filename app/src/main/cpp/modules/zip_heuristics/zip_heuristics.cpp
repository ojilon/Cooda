#include "zip_heuristics.hpp"

#include <cctype>
#include <string>

namespace cooda::zip {
namespace {

[[nodiscard]] char ascii_lower(char c) noexcept {
    return static_cast<char>(std::tolower(static_cast<unsigned char>(c)));
}

[[nodiscard]] bool iequals(std::string_view a, std::string_view b) noexcept {
    if (a.size() != b.size()) {
        return false;
    }
    for (std::size_t i = 0; i < a.size(); ++i) {
        if (ascii_lower(a[i]) != ascii_lower(b[i])) {
            return false;
        }
    }
    return true;
}

[[nodiscard]] bool ends_with_ci(std::string_view s, std::string_view suffix) noexcept {
    if (s.size() < suffix.size()) {
        return false;
    }
    return iequals(s.substr(s.size() - suffix.size()), suffix);
}

}  // namespace

bool is_zip_candidate(std::string_view path_or_name, std::string_view mime) noexcept {
    // MIME first (matches former Java MetaDocument.MimeType.ZIP)
    if (!mime.empty() && iequals(mime, "application/zip")) {
        return true;
    }
    // Extension heuristic (path or bare name)
    if (path_or_name.empty()) {
        return false;
    }
    // Prefer last path segment if separators present
    auto name = path_or_name;
    if (const auto pos = path_or_name.find_last_of("/\\"); pos != std::string_view::npos) {
        name = path_or_name.substr(pos + 1);
    }
    return ends_with_ci(name, ".zip");
}

}  // namespace cooda::zip
