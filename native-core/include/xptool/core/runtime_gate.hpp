#pragma once

#include <string>

namespace xptool::core {

class StateSnapshot;

struct GateDecision {
  bool allowed = false;
  std::string reason_code;
};

class RuntimeGate {
public:
  explicit RuntimeGate(int min_dispatch_interval_ticks = 2);

  [[nodiscard]] GateDecision evaluate(const StateSnapshot& snapshot, bool has_intent) const;
  void record_dispatch(int tick);

private:
  int min_dispatch_interval_ticks_;
  int last_dispatch_tick_;
};

} // namespace xptool::core
