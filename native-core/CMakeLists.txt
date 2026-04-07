cmake_minimum_required(VERSION 3.26)
project(xptool_native_core LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_library(xptool_native_core STATIC
  src/runtime_core/runtime_runner.cpp
  src/runtime_core/runtime_coordinator.cpp
  src/runtime_core/runtime_gate.cpp
  src/runtime_core/repeat_target_selector.cpp
  src/runtime_core/runtime_telemetry.cpp
  src/motor/motor_runtime.cpp
  src/state/state_acquisition_service.cpp
  src/state/state_snapshot.cpp
  src/state/state_snapshot_translator.cpp
  src/activities/mining/mining_runtime.cpp
  src/activities/fishing/fishing_runtime.cpp
  src/activities/combat/combat_runtime.cpp
  src/activities/banking/banking_runtime.cpp
  src/activities/woodcutting/woodcutting_runtime.cpp
  src/activities/woodcutting/woodcutting_target_selector.cpp
)

target_include_directories(xptool_native_core PUBLIC include)

add_executable(xptool_native_core_smoke
  src/runtime_core/runtime_smoke_main.cpp
)
target_link_libraries(xptool_native_core_smoke PRIVATE xptool_native_core)

add_executable(xptool_native_core_woodcutting_parity
  src/activities/woodcutting/woodcutting_parity_main.cpp
)
target_link_libraries(xptool_native_core_woodcutting_parity PRIVATE xptool_native_core)

add_executable(xptool_native_core_activity_parity
  src/activities/activity_parity_main.cpp
)
target_link_libraries(xptool_native_core_activity_parity PRIVATE xptool_native_core)

add_executable(xptool_native_core_state_acquisition_smoke
  src/state/state_acquisition_smoke_main.cpp
)
target_link_libraries(xptool_native_core_state_acquisition_smoke PRIVATE xptool_native_core)
