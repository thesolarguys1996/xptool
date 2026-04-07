#pragma once

#include "xptool/ui/telemetry_parser.hpp"
#include "xptool/ui/ui_config.hpp"

#include <string>
#include <vector>

namespace xptool::ui {

struct OverlaySummary {
  int total_events = 0;
  int dispatched_events = 0;
  int rejected_events = 0;
  int no_progress_events = 0;
  std::vector<std::string> top_reason_lines;
  std::vector<std::string> recent_event_lines;
};

OverlaySummary build_overlay_summary(const std::vector<TelemetryEvent>& events, const UiConfig& config);
std::string render_overlay_text(const OverlaySummary& summary, const UiConfig& config, const std::string& telemetry_path);

} // namespace xptool::ui
