# CompilerFlags.cmake — C++23, STL, RAII-friendly strict warnings
# Android NDK builds use Clang. Host presets may use GCC 15.

cmake_minimum_required(VERSION 3.22.1)

if(NOT TARGET project_warnings)
    add_library(project_warnings INTERFACE)
    target_compile_features(project_warnings INTERFACE cxx_std_23)

    if(CMAKE_CXX_COMPILER_ID MATCHES "Clang")
        target_compile_options(project_warnings INTERFACE
            -Wall -Wextra -Wpedantic -Wshadow -Wnon-virtual-dtor
            -Wcast-align -Wunused -Woverloaded-virtual
            -Wconversion -Wsign-conversion -Wnull-dereference
            -Wdouble-promotion -Wformat=2 -Werror=return-type
        )
    elseif(CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
        target_compile_options(project_warnings INTERFACE
            -Wall -Wextra -Wpedantic -Wshadow -Wnon-virtual-dtor
            -Wcast-align -Wunused -Woverloaded-virtual
            -Wconversion -Wsign-conversion -Wnull-dereference
            -Wdouble-promotion -Wformat=2 -Werror=return-type
        )
        # GCC 13+ static analyzer (host-gcc-15 preset)
        if(CMAKE_CXX_COMPILER_VERSION VERSION_GREATER_EQUAL 13)
            target_compile_options(project_warnings INTERFACE -fanalyzer)
        endif()
    endif()
endif()
