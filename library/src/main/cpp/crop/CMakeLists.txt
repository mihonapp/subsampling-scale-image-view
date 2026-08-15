cmake_minimum_required(VERSION 3.22.1)

project(ssiv_crop)

add_library(ssiv_crop SHARED
    crop.cpp
    borders.cpp
)

target_link_libraries(ssiv_crop android log)
