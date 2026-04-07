#include "xptool/core/runtime_gate.hpp"

#include "xptool/core/state_snapshot.hpp"

namespace xptool::core {

RuntimeGate::RuntimeGate(int min_dispatch_interval_ticks)
    : min_dispatch_interval_ticks_(min_dispatch_interval_ticks <= 0 ? 1 : min_dispatch_interval_ticks), last_dispatch_tick_(-1) {}

GateDecision RuntimeGate::evaluate(const StateSnapshot& snapshot, bool has_intent) const {
  if (!snapshot.is_valid()) {
    GateDecision decision;
    decision.allowed = false;
    decision.reason_code = "invalid_snapshot";
    return decision;
  }
  if (!snapshot.logged_in) {
    GateDecision decision;
    decision.allowed = false;
    decision.reason_code = "not_logged_in";
    return decision;
  }
  if (!snapshot.in_focus) {
    GateDecision decision;
    decision.allowed = false;
    decision.reason_code = "out_of_focus";
    return decision;
  }
  if (!has_intent) {
    GateDecision decision;
    decision.allowed = false;
    decision.reason_code = "no_intent";
    return decision;
  }
  if (last_dispatch_tick_ >= 0 && (snapshot.tick - last_dispatch_tick_) < min_dispatch_interval_ticks_) {
    GateDecision decision;
    decision.allowed = false;
    decision.reason_code = "cooldown_active";
    return decision;
  }
  GateDecision decision;
  decision.allowed = true;
  decision.reason_code = "accepted";
  return decision;
}

void RuntimeGate::record_dispatch(int tick) { last_dispatch_tick_ = tick; }

} // namespace xptool::core
