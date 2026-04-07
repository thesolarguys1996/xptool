#include "xptool/core/runtime_runner.hpp"
#include "xptool/core/state_frame.hpp"

#include <iostream>
#include <vector>

int main() {
  xptool::core::RuntimeRunner runner;
  std::vector<xptool::core::StateFrame> frames;
  frames.resize(8);

  frames[0].schema_version = "1.1";
  frames[0].tick = 100;
  frames[0].logged_in = true;
  frames[0].in_focus = true;
  frames[0].has_target_candidate = true;
  frames[0].woodcutting_candidates.push_back(xptool::core::WoodcuttingTargetCandidate{2001, 2, true});

  frames[1].schema_version = "1.1";
  frames[1].tick = 101;
  frames[1].in_focus = true;
  frames[1].has_target_candidate = true;
  frames[1].woodcutting_candidates.push_back(xptool::core::WoodcuttingTargetCandidate{2002, 1, true});

  frames[2].schema_version = "2.0";
  frames[2].tick = 102;
  frames[2].in_focus = true;
  frames[2].has_target_candidate = true;
  frames[2].woodcutting_candidates.push_back(xptool::core::WoodcuttingTargetCandidate{2003, 1, true});

  frames[3].schema_version = "1.0";
  frames[3].tick = 103;
  frames[3].logged_in = true;
  frames[3].in_focus = true;
  frames[3].legacy_nearest_tree_id = 2004;
  frames[3].legacy_nearest_tree_distance = 5;
  frames[3].legacy_nearest_tree_interactable = true;
  frames[3].has_target_candidate = true;

  frames[4].schema_version = "bad";
  frames[4].tick = 104;

  frames[5].schema_version = "bad";
  frames[5].tick = 105;

  frames[6].schema_version = "bad";
  frames[6].tick = 106;

  frames[7].schema_version = "bad";
  frames[7].tick = 107;

  for (const auto& frame : frames) {
    const auto result = runner.tick_frame(frame);
    std::cout << "frameTick=" << (frame.tick.has_value() ? std::to_string(frame.tick.value()) : std::string("-1"))
              << " dispatched=" << (result.dispatched ? "true" : "false") << " reason=" << result.reason_code
              << " command=" << result.command_type << " target=" << result.target_id << "\n";
  }

  const auto& events = runner.telemetry().events();
  int acquisition_events = 0;
  int deferred_events = 0;
  int hard_reject_events = 0;
  for (const auto& event : events) {
    if (event.event_type == "state_acquisition") {
      ++acquisition_events;
      if (event.reason_code.rfind("deferred_", 0) == 0) {
        ++deferred_events;
      }
      if (event.data.find("accepted") != event.data.end() && event.data.at("accepted") == "false") {
        ++hard_reject_events;
      }
      std::cout << "acquisition reason=" << event.reason_code << " accepted=" << event.data.at("accepted")
                << " usedLastGood=" << event.data.at("usedLastGood")
                << " failures=" << event.data.at("consecutiveFailures") << "\n";
    }
  }
  std::cout << "telemetry_events=" << events.size() << " state_acquisition_events=" << acquisition_events
            << " deferred_acquisition_events=" << deferred_events
            << " hard_reject_acquisition_events=" << hard_reject_events << "\n";
  return 0;
}
