#pragma once

#include <map>
#include <string>
#include <vector>

namespace xptool::core {

struct RuntimeTelemetryEvent {
  std::string event_type;
  std::string reason_code;
  std::map<std::string, std::string> data;
};

class RuntimeTelemetry {
public:
  void record(const RuntimeTelemetryEvent& event);
  [[nodiscard]] const std::vector<RuntimeTelemetryEvent>& events() const;
  void clear();

private:
  std::vector<RuntimeTelemetryEvent> events_;
};

} // namespace xptool::core
