#include "xptool/core/runtime_runner.hpp"
#include "xptool/core/state_snapshot.hpp"

#include <iostream>

int main() {
  xptool::core::RuntimeRunner runner;

  xptool::core::StateSnapshot cases[7];
  cases[0].tick = 10;
  cases[0].logged_in = true;
  cases[0].in_focus = true;
  cases[0].has_target_candidate = true;
  cases[0].woodcutting_candidates.push_back(xptool::core::WoodcuttingTargetCandidate{1001, 2, true});
  cases[0].woodcutting_candidates.push_back(xptool::core::WoodcuttingTargetCandidate{1002, 4, true});
  cases[1].tick = 11;
  cases[1].logged_in = true;
  cases[1].in_focus = true;
  cases[1].has_target_candidate = true;
  cases[1].woodcutting_candidates.push_back(xptool::core::WoodcuttingTargetCandidate{1001, 1, true});
  cases[1].woodcutting_candidates.push_back(xptool::core::WoodcuttingTargetCandidate{1002, 3, true});
  cases[2].tick = 12;
  cases[2].logged_in = true;
  cases[2].in_focus = true;
  cases[2].has_target_candidate = true;
  cases[2].woodcutting_candidates.push_back(xptool::core::WoodcuttingTargetCandidate{1001, 1, true});
  cases[2].woodcutting_candidates.push_back(xptool::core::WoodcuttingTargetCandidate{1002, 2, true});
  cases[3].tick = 13;
  cases[3].logged_in = true;
  cases[3].in_focus = true;
  cases[3].has_target_candidate = true;
  cases[3].woodcutting_candidates.push_back(xptool::core::WoodcuttingTargetCandidate{1002, 1, true});
  cases[4].tick = 14;
  cases[4].logged_in = true;
  cases[4].in_focus = true;
  cases[4].has_target_candidate = true;
  cases[4].woodcutting_candidates.push_back(xptool::core::WoodcuttingTargetCandidate{1002, 1, true});
  cases[5].tick = 15;
  cases[5].logged_in = true;
  cases[5].in_focus = true;
  cases[5].has_target_candidate = false;
  cases[6].tick = 16;
  cases[6].logged_in = true;
  cases[6].in_focus = true;
  cases[6].has_target_candidate = true;
  cases[6].nearest_target_id = 1003;
  cases[6].nearest_target_distance = 6;
  cases[6].nearest_target_interactable = true;

  for (const auto& snapshot : cases) {
    const auto result = runner.tick(snapshot);
    std::cout << "tick=" << snapshot.tick << " dispatched=" << (result.dispatched ? "true" : "false")
              << " reason=" << result.reason_code << " command=" << result.command_type << " target=" << result.target_id << "\n";
  }

  const auto& telemetry = runner.telemetry().events();
  std::cout << "telemetry_events=" << telemetry.size() << "\n";
  return 0;
}
