#include "xptool/ui/status_overlay.hpp"

#include <algorithm>
#include <map>
#include <sstream>
#include <string>
#include <vector>

namespace xptool::ui {
namespace {

bool contains(const std::string& value, const std::string& token) { return value.find(token) != std::string::npos; }

bool is_dispatched(const TelemetryEvent& event) {
  if (contains(event.event_type, "dispatched")) {
    return true;
  }
  return contains(event.reason_code, "_dispatch_");
}

bool is_rejected(const TelemetryEvent& event) {
  if (contains(event.event_type, "rejected")) {
    return true;
  }
  return contains(event.reason_code, "unsupported") || contains(event.reason_code, "invalid") || contains(event.reason_code, "replay_rejected");
}

bool is_no_progress(const TelemetryEvent& event) {
  return contains(event.event_type, "no_progress") || contains(event.reason_code, "no_intent");
}

std::string event_line(const TelemetryEvent& event) {
  std::ostringstream line;
  line << event.event_time_utc << " event=" << event.event_type << " reason=" << event.reason_code;
  if (!event.activity.empty()) {
    line << " activity=" << event.activity;
  }
  if (!event.command_type.empty()) {
    line << " command=" << event.command_type;
  }
  if (!event.target_id.empty()) {
    line << " target=" << event.target_id;
  }
  return line.str();
}

} // namespace

OverlaySummary build_overlay_summary(const std::vector<TelemetryEvent>& events, const UiConfig& config) {
  OverlaySummary summary;
  std::map<std::string, int> reason_counts;
  std::vector<std::string> recent;

  for (const auto& event : events) {
    if (config.focus_activity != "all") {
      if (event.activity.empty() || event.activity != config.focus_activity) {
        continue;
      }
    }
    if (!config.show_rejected && is_rejected(event)) {
      continue;
    }
    ++summary.total_events;
    if (is_dispatched(event)) {
      ++summary.dispatched_events;
    }
    if (is_rejected(event)) {
      ++summary.rejected_events;
    }
    if (is_no_progress(event)) {
      ++summary.no_progress_events;
    }
    reason_counts[event.reason_code] += 1;
    recent.push_back(event_line(event));
  }

  std::vector<std::pair<std::string, int>> reasons(reason_counts.begin(), reason_counts.end());
  std::sort(reasons.begin(), reasons.end(), [](const auto& a, const auto& b) {
    if (a.second == b.second) {
      return a.first < b.first;
    }
    return a.second > b.second;
  });

  const int max_reasons = 6;
  for (int i = 0; i < static_cast<int>(reasons.size()) && i < max_reasons; ++i) {
    summary.top_reason_lines.push_back(reasons[i].first + "=" + std::to_string(reasons[i].second));
  }

  const int recent_keep = std::max(1, config.max_recent_events);
  const int start = static_cast<int>(recent.size()) > recent_keep ? static_cast<int>(recent.size()) - recent_keep : 0;
  for (int i = start; i < static_cast<int>(recent.size()); ++i) {
    summary.recent_event_lines.push_back(recent[i]);
  }
  return summary;
}

std::string render_overlay_text(const OverlaySummary& summary, const UiConfig& config, const std::string& telemetry_path) {
  std::ostringstream out;
  out << "Native UI Overlay\n";
  out << "telemetry_path=" << telemetry_path << "\n";
  out << "focus_activity=" << config.focus_activity << " compact=" << (config.compact ? "true" : "false") << "\n";
  out << "total_events=" << summary.total_events << " dispatched=" << summary.dispatched_events
      << " rejected=" << summary.rejected_events << " no_progress=" << summary.no_progress_events << "\n";

  if (!summary.top_reason_lines.empty()) {
    out << "top_reasons:";
    for (const auto& line : summary.top_reason_lines) {
      out << " " << line;
    }
    out << "\n";
  }

  if (!config.compact) {
    out << "recent_events:\n";
    for (const auto& line : summary.recent_event_lines) {
      out << "  " << line << "\n";
    }
  }

  return out.str();
}

} // namespace xptool::ui
