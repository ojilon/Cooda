# Aggregates native code from project modules.
# Subdirs append to COODA_MODULE_SOURCES / COODA_MODULE_INCLUDE_DIRS (parent scope).

set(COODA_MODULE_SOURCES "")
set(COODA_MODULE_INCLUDE_DIRS "")

add_subdirectory(zip_heuristics)
# add_subdirectory(plugins)  # next NATIVE_CANDIDATE

set(COODA_MODULE_SOURCES "${COODA_MODULE_SOURCES}" PARENT_SCOPE)
set(COODA_MODULE_INCLUDE_DIRS "${COODA_MODULE_INCLUDE_DIRS}" PARENT_SCOPE)
