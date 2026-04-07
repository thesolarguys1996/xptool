#include "xptool/bridge/bridge_service.hpp"

#include "xptool/bridge/command_envelope_validator.hpp"
#include "xptool/bridge/telemetry_event.hpp"

#include <cstdlib>
#include <fstream>
#include <map>
#include <string>

namespace xptool::bridge {
namespace {

std::string trim(const std::string& value) {
  const std::string whitespace = " \t\r\n";
  const auto start = value.find_first_not_of(whitespace);
  if (start == std::string::npos) {
    return {};
  }
  const auto end = value.find_last_not_of(whitespace);
  return value.substr(start, end - start + 1);
}

std::string load_env_value(const std::string& name) {
  if (name.empty()) {
    return {};
  }
  const char* raw = std::getenv(name.c_str());
  if (raw == nullptr) {
    return {};
  }
  return trim(raw);
}

} // namespace

bool is_loopback_address(const std::string& address) {
  const std::string normalized = trim(address);
  return normalized == "127.0.0.1" || normalized == "::1" || normalized == "localhost";
}

int run_bridge_service(const BridgeConfig& config) {
  TelemetrySink telemetry(config.telemetry_out_path, config.emit_telemetry_to_stdout);

  if (!is_loopback_address(config.bind_address)) {
    telemetry.emit(
        "bridge_startup_rejected",
        "local_only_bind_required",
        {
            {"bindAddress", config.bind_address},
            {"port", std::to_string(config.port)},
        });
    return 2;
  }

  const std::string token = load_env_value(config.auth_token_env);
  if (token.empty()) {
    telemetry.emit(
        "bridge_startup_rejected",
        "missing_token",
        {
            {"tokenEnv", config.auth_token_env},
            {"bindAddress", config.bind_address},
            {"port", std::to_string(config.port)},
        });
    return 3;
  }

  telemetry.emit(
      "bridge_started",
      "accepted",
      {
          {"bindAddress", config.bind_address},
          {"port", std::to_string(config.port)},
          {"tokenEnv", config.auth_token_env},
          {"verifierEnabled", config.enable_verifier ? "true" : "false"},
      });

  if (trim(config.command_ingest_path).empty()) {
    telemetry.emit(
        "bridge_idle",
        "no_command_ingest_path",
        {
            {"hint", "set --command-ingest-path to process envelope records"},
        });
    return 0;
  }

  std::ifstream input(config.command_ingest_path);
  if (!input.is_open()) {
    telemetry.emit(
        "command_ingest_rejected",
        "ingest_open_failed",
        {
            {"path", config.command_ingest_path},
        });
    return 4;
  }

  CommandEnvelopeValidator validator(config.enable_verifier, config.max_clock_skew_seconds);
  std::string line;
  unsigned long long accepted = 0;
  unsigned long long rejected = 0;
  unsigned long long processed = 0;
  while (std::getline(input, line)) {
    const std::string stripped = trim(line);
    if (stripped.empty()) {
      continue;
    }
    ++processed;
    const auto result = validator.validate_line(stripped);
    if (result.accepted) {
      ++accepted;
      telemetry.emit(
          "command_accepted",
          "accepted",
          {
              {"commandId", result.command_id},
              {"commandType", result.command_type},
          });
      continue;
    }

    ++rejected;
    telemetry.emit(
        "command_rejected",
        result.reason_code,
        {
            {"commandId", result.command_id},
            {"commandType", result.command_type},
        });
  }

  telemetry.emit(
      "command_ingest_complete",
      "accepted",
      {
          {"processed", std::to_string(processed)},
          {"accepted", std::to_string(accepted)},
          {"rejected", std::to_string(rejected)},
      });
  return 0;
}

} // namespace xptool::bridge
