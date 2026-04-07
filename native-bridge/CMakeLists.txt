cmake_minimum_required(VERSION 3.26)
project(xptool_native_bridge LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_executable(xptool_native_bridge
  src/bridge_service.cpp
  src/command_envelope_validator.cpp
  src/command_policy.cpp
  src/telemetry_event.cpp
  src/main.cpp
)

target_include_directories(xptool_native_bridge PRIVATE include)
