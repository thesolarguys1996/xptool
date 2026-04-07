#pragma once

#include <string>

namespace xptool::ui {

struct UiConfig {
  std::string focus_activity = "all";
  bool show_rejected = true;
  int max_recent_events = 8;
  bool compact = false;
};

UiConfig load_ui_config(const std::string& path);

} // namespace xptool::ui
