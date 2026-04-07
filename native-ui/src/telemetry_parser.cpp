#include "xptool/ui/telemetry_parser.hpp"

#include <deque>
#include <fstream>
#include <regex>
#include <string>

namespace xptool::ui {
namespace {

std::string extract_string_field(const std::string& line, const std::string& key) {
  const std::regex pattern("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
  std::smatch match;
  if (!std::regex_search(line, match, pattern) || match.size() < 2) {
    return {};
  }
  return match[1].str();
}

bool is_relevant_schema(const std::string& schema_version) {
  return schema_version == "1.0";
}

} // namespace

std::optional<TelemetryEvent> parse_telemetry_line(const std::string& line) {
  if (line.empty()) {
    return std::nullopt;
  }

  TelemetryEvent event;
  event.schema_version = extract_string_field(line, "schemaVersion");
  if (!is_relevant_schema(event.schema_version)) {
    return std::nullopt;
  }
  event.event_id = extract_string_field(line, "eventId");
  event.event_type = extract_string_field(line, "eventType");
  event.event_time_utc = extract_string_field(line, "eventTimeUtc");
  event.source = extract_string_field(line, "source");
  event.reason_code = extract_string_field(line, "reasonCode");
  event.activity = extract_string_field(line, "activity");
  event.command_type = extract_string_field(line, "commandType");
  event.target_id = extract_string_field(line, "targetId");

  if (event.event_type.empty() && event.reason_code.empty()) {
    return std::nullopt;
  }
  return event;
}

std::vector<TelemetryEvent> read_telemetry_file(const std::string& path, int tail_lines) {
  std::vector<TelemetryEvent> events;
  std::ifstream file(path);
  if (!file.is_open()) {
    return events;
  }

  const bool tail_only = tail_lines > 0;
  std::deque<std::string> lines;
  std::string line;
  while (std::getline(file, line)) {
    if (tail_only) {
      lines.push_back(line);
      if (static_cast<int>(lines.size()) > tail_lines) {
        lines.pop_front();
      }
    } else {
      lines.push_back(line);
    }
  }

  events.reserve(lines.size());
  for (const auto& raw : lines) {
    const auto parsed = parse_telemetry_line(raw);
    if (parsed.has_value()) {
      events.push_back(parsed.value());
    }
  }
  return events;
}

} // namespace xptool::ui
