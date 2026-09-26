# Aggregates native code from project modules.
# Subdirs may set COODA_MODULE_SOURCES / COODA_MODULE_INCLUDE_DIRS in parent scope.

set(COODA_MODULE_SOURCES "" PARENT_SCOPE)
set(COODA_MODULE_INCLUDE_DIRS "" PARENT_SCOPE)

# Example for later (NATIVE_CANDIDATE migrations):
# add_subdirectory(zip_heuristics)
# add_subdirectory(plugins)
