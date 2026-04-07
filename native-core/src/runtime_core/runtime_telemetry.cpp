#include "xptool/core/runtime_telemetry.hpp"

namespace xptool::core {

void RuntimeTelemetry::record(const RuntimeTelemetryEvent& event) { events_.push_back(event); }

const std::vector<RuntimeTelemetryEvent>& RuntimeTelemetry::events() const { return events_; }

void RuntimeTelemetry::clear() { events_.clear(); }

} // namespace xptool::core
