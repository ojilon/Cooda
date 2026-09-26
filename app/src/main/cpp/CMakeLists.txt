# Root CMake for Cooda native backend — multi-module aggregator (C++23)
cmake_minimum_required(VERSION 3.22.1)

project(cooda_native LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/cmake")

include(CompilerFlags)
include(LinkLibraries)

# Future native module trees (ZIP heuristics, plugins, …)
add_subdirectory(modules)

set(COODA_NATIVE_SOURCES
    cooda/cooda_native.cpp
    ${COODA_MODULE_SOURCES}
)

add_library(cooda_native SHARED ${COODA_NATIVE_SOURCES})

target_include_directories(cooda_native
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_SOURCE_DIR}/cooda
        ${COODA_MODULE_INCLUDE_DIRS}
)

target_link_libraries(cooda_native
    PRIVATE
        project_warnings
        cooda_link_libs
)

if(TARGET cooda_modules)
    target_link_libraries(cooda_native PRIVATE cooda_modules)
endif()
