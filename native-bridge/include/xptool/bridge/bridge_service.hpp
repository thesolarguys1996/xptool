#pragma once

#include <string>

namespace xptool::bridge {

struct BridgeConfig {
  std::string bind_address = "127.0.0.1";
  int port = 7611;
  std::string auth_token_env = "XPTOOL_NATIVE_BRIDGE_TOKEN";
  std::string command_ingest_path;
  std::string telemetry_out_path;
  bool enable_verifier = false;
  int max_clock_skew_seconds = 300;
  bool emit_telemetry_to_stdout = true;
};

int run_bridge_service(const BridgeConfig& config);
bool is_loopback_address(const std::string& address);

} // namespace xptool::bridge
