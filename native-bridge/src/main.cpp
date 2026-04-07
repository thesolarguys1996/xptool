#include "xptool/bridge/bridge_service.hpp"

#include <cstdlib>
#include <iostream>
#include <string>

namespace {

void print_help() {
  std::cout
      << "xptool_native_bridge\n"
      << "Usage:\n"
      << "  xptool_native_bridge [options]\n\n"
      << "Options:\n"
      << "  --bind-address <addr>           Bind address (default: 127.0.0.1)\n"
      << "  --port <port>                  Bind port (default: 7611)\n"
      << "  --auth-token-env <env>         Auth token env var (default: XPTOOL_NATIVE_BRIDGE_TOKEN)\n"
      << "  --command-ingest-path <path>   NDJSON command envelope input path\n"
      << "  --telemetry-out-path <path>    NDJSON telemetry output path\n"
      << "  --enable-verifier              Enable replay/timestamp verifier\n"
      << "  --max-clock-skew-seconds <n>   Allowed issuedAtUtc drift when verifier is enabled (default: 300)\n"
      << "  --no-stdout-telemetry          Disable telemetry stdout emission\n"
      << "  --help                         Show this help\n";
}

bool require_value(int argc, char** argv, int index, const std::string& flag_name, std::string* out) {
  if (out == nullptr || index + 1 >= argc) {
    std::cerr << "missing value for " << flag_name << "\n";
    return false;
  }
  *out = argv[index + 1];
  return true;
}

} // namespace

int main(int argc, char** argv) {
  xptool::bridge::BridgeConfig config;

  for (int i = 1; i < argc; ++i) {
    const std::string arg = argv[i];
    if (arg == "--help") {
      print_help();
      return 0;
    }
    if (arg == "--bind-address") {
      std::string value;
      if (!require_value(argc, argv, i, arg, &value)) {
        return 1;
      }
      config.bind_address = value;
      ++i;
      continue;
    }
    if (arg == "--port") {
      std::string value;
      if (!require_value(argc, argv, i, arg, &value)) {
        return 1;
      }
      config.port = std::atoi(value.c_str());
      ++i;
      continue;
    }
    if (arg == "--auth-token-env") {
      std::string value;
      if (!require_value(argc, argv, i, arg, &value)) {
        return 1;
      }
      config.auth_token_env = value;
      ++i;
      continue;
    }
    if (arg == "--command-ingest-path") {
      std::string value;
      if (!require_value(argc, argv, i, arg, &value)) {
        return 1;
      }
      config.command_ingest_path = value;
      ++i;
      continue;
    }
    if (arg == "--telemetry-out-path") {
      std::string value;
      if (!require_value(argc, argv, i, arg, &value)) {
        return 1;
      }
      config.telemetry_out_path = value;
      ++i;
      continue;
    }
    if (arg == "--max-clock-skew-seconds") {
      std::string value;
      if (!require_value(argc, argv, i, arg, &value)) {
        return 1;
      }
      config.max_clock_skew_seconds = std::atoi(value.c_str());
      ++i;
      continue;
    }
    if (arg == "--enable-verifier") {
      config.enable_verifier = true;
      continue;
    }
    if (arg == "--no-stdout-telemetry") {
      config.emit_telemetry_to_stdout = false;
      continue;
    }
    std::cerr << "unknown option: " << arg << "\n";
    print_help();
    return 1;
  }

  return xptool::bridge::run_bridge_service(config);
}
