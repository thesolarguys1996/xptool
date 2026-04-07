#include "xptool/ui/ui_config.hpp"

#include <algorithm>
#include <cctype>
#include <fstream>
#include <string>

namespace xptool::ui {
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

std::string to_lower(std::string value) {
  std::transform(value.begin(), value.end(), value.begin(), [](unsigned char c) {
    return static_cast<char>(std::tolower(c));
  });
  return value;
}

bool parse_bool(const std::string& value, bool fallback) {
  const std::string lowered = to_lower(trim(value));
  if (lowered == "true" || lowered == "1" || lowered == "yes") {
    return true;
  }
  if (lowered == "false" || lowered == "0" || lowered == "no") {
    return false;
  }
  return fallback;
}

} // namespace

UiConfig load_ui_config(const std::string& path) {
  UiConfig config;
  if (path.empty()) {
    return config;
  }

  std::ifstream file(path);
  if (!file.is_open()) {
    return config;
  }

  std::string line;
  while (std::getline(file, line)) {
    const std::string raw = trim(line);
    if (raw.empty() || raw[0] == '#') {
      continue;
    }
    const auto pos = raw.find('=');
    if (pos == std::string::npos) {
      continue;
    }
    const std::string key = to_lower(trim(raw.substr(0, pos)));
    const std::string value = trim(raw.substr(pos + 1));
    if (key == "focus_activity") {
      config.focus_activity = to_lower(value);
    } else if (key == "show_rejected") {
      config.show_rejected = parse_bool(value, config.show_rejected);
    } else if (key == "max_recent_events") {
      try {
        config.max_recent_events = std::max(1, std::stoi(value));
      } catch (...) {
      }
    } else if (key == "compact") {
      config.compact = parse_bool(value, config.compact);
    }
  }
  return config;
}

} // namespace xptool::ui
