#pragma once

#include <optional>
#include <string>
#include <vector>

namespace xptool::ui {

struct TelemetryEvent {
  std::string schema_version;
  std::string event_id;
  std::string event_type;
  std::string event_time_utc;
  std::string source;
  std::string reason_code;
  std::string activity;
  std::string command_type;
  std::string target_id;
};

std::optional<TelemetryEvent> parse_telemetry_line(const std::string& line);
std::vector<TelemetryEvent> read_telemetry_file(const std::string& path, int tail_lines);

} // namespace xptool::ui
