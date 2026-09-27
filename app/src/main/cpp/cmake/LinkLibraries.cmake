# LinkLibraries.cmake — central C++ link dependencies across modules
# (Wayer-style dedicated cmake for links)

cmake_minimum_required(VERSION 3.22.1)

if(NOT TARGET cooda_link_libs)
    add_library(cooda_link_libs INTERFACE)

    if(ANDROID)
        find_library(log-lib log)
        find_library(android-lib android)
        target_link_libraries(cooda_link_libs INTERFACE
            ${log-lib}
            ${android-lib}
            c++_shared
        )
    endif()
endif()
