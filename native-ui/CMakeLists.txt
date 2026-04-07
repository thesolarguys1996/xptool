cmake_minimum_required(VERSION 3.26)
project(xptool_native_ui LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_executable(xptool_native_ui
  src/ui_config.cpp
  src/telemetry_parser.cpp
  src/status_overlay.cpp
  src/main.cpp
)

target_include_directories(xptool_native_ui PRIVATE include)
