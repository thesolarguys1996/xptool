#include "xptool/core/runtime_runner.hpp"
#include "xptool/core/state_snapshot.hpp"

#include <fstream>
#include <iostream>
#include <set>
#include <sstream>
#include <string>
#include <vector>

namespace {

struct ParityRecord {
  int tick = -1;
  bool dispatched = false;
  std::string reason_code;
  int target_id = -1;
};

std::vector<xptool::core::StateSnapshot> build_scenario() {
  std::vector<xptool::core::StateSnapshot> snapshots;
  snapshots.resize(7);

  snapshots[0].tick = 10;
  snapshots[0].logged_in = true;
  snapshots[0].in_focus = true;
  snapshots[0].has_target_candidate = true;
  snapshots[0].woodcutting_candidates.push_back(xptool::core::WoodcuttingTargetCandidate{1001, 2, true});
  snapshots[0].woodcutting_candidates.push_back(xptool::core::WoodcuttingTargetCandidate{1002, 4, true});

  snapshots[1].tick = 11;
  snapshots[1].logged_in = true;
  snapshots[1].in_focus = true;
  snapshots[1].has_target_candidate = true;
  snapshots[1].woodcutting_candidates.push_back(xptool::core::WoodcuttingTargetCandidate{1001, 1, true});
  snapshots[1].woodcutting_candidates.push_back(xptool::core::WoodcuttingTargetCandidate{1002, 3, true});

  snapshots[2].tick = 12;
  snapshots[2].logged_in = true;
  snapshots[2].in_focus = true;
  snapshots[2].has_target_candidate = true;
  snapshots[2].woodcutting_candidates.push_back(xptool::core::WoodcuttingTargetCandidate{1001, 1, true});
  snapshots[2].woodcutting_candidates.push_back(xptool::core::WoodcuttingTargetCandidate{1002, 2, true});

  snapshots[3].tick = 13;
  snapshots[3].logged_in = true;
  snapshots[3].in_focus = true;
  snapshots[3].has_target_candidate = true;
  snapshots[3].woodcutting_candidates.push_back(xptool::core::WoodcuttingTargetCandidate{1002, 1, true});

  snapshots[4].tick = 14;
  snapshots[4].logged_in = true;
  snapshots[4].in_focus = true;
  snapshots[4].has_target_candidate = true;
  snapshots[4].woodcutting_candidates.push_back(xptool::core::WoodcuttingTargetCandidate{1002, 1, true});

  snapshots[5].tick = 15;
  snapshots[5].logged_in = true;
  snapshots[5].in_focus = true;
  snapshots[5].has_target_candidate = false;

  snapshots[6].tick = 16;
  snapshots[6].logged_in = true;
  snapshots[6].in_focus = true;
  snapshots[6].has_target_candidate = true;
  snapshots[6].nearest_target_id = 1003;
  snapshots[6].nearest_target_distance = 6;
  snapshots[6].nearest_target_interactable = true;

  return snapshots;
}

std::vector<ParityRecord> run_actual_trace() {
  xptool::core::RuntimeRunner runner;
  const auto snapshots = build_scenario();
  std::vector<ParityRecord> actual;
  actual.reserve(snapshots.size());
  for (const auto& snapshot : snapshots) {
    const auto tick_result = runner.tick(snapshot);
    ParityRecord record;
    record.tick = snapshot.tick;
    record.dispatched = tick_result.dispatched;
    record.reason_code = tick_result.reason_code;
    record.target_id = tick_result.target_id;
    actual.push_back(record);
  }
  return actual;
}

bool parse_bool_token(const std::string& token) {
  if (token == "1" || token == "true" || token == "TRUE") {
    return true;
  }
  return false;
}

bool load_baseline(const std::string& path, std::vector<ParityRecord>* out) {
  if (out == nullptr) {
    return false;
  }
  std::ifstream file(path);
  if (!file.is_open()) {
    return false;
  }
  out->clear();
  std::string line;
  while (std::getline(file, line)) {
    if (line.empty() || line[0] == '#') {
      continue;
    }
    std::stringstream stream(line);
    std::string tick_token;
    std::string dispatched_token;
    std::string reason_token;
    std::string target_token;
    if (!std::getline(stream, tick_token, ',')) {
      continue;
    }
    if (!std::getline(stream, dispatched_token, ',')) {
      continue;
    }
    if (!std::getline(stream, reason_token, ',')) {
      continue;
    }
    if (!std::getline(stream, target_token, ',')) {
      continue;
    }
    ParityRecord record;
    record.tick = std::stoi(tick_token);
    record.dispatched = parse_bool_token(dispatched_token);
    record.reason_code = reason_token;
    record.target_id = std::stoi(target_token);
    out->push_back(record);
  }
  return !out->empty();
}

int run_parity(const std::vector<ParityRecord>& expected, const std::vector<ParityRecord>& actual) {
  if (expected.size() != actual.size()) {
    std::cerr << "parity_failed size_mismatch expected=" << expected.size() << " actual=" << actual.size() << "\n";
    return 1;
  }

  int outcome_matches = 0;
  int dispatch_tick_matches = 0;
  std::vector<int> expected_dispatch_ticks;
  std::vector<int> actual_dispatch_ticks;
  std::set<std::string> expected_reasons;
  std::set<std::string> actual_reasons;

  for (size_t i = 0; i < expected.size(); ++i) {
    const auto& e = expected[i];
    const auto& a = actual[i];
    if (e.dispatched == a.dispatched) {
      ++outcome_matches;
    }
    if (e.dispatched) {
      expected_dispatch_ticks.push_back(e.tick);
    }
    if (a.dispatched) {
      actual_dispatch_ticks.push_back(a.tick);
    }
    expected_reasons.insert(e.reason_code);
    actual_reasons.insert(a.reason_code);
    std::cout << "tick=" << a.tick << " dispatched=" << (a.dispatched ? "1" : "0") << " reason=" << a.reason_code
              << " target=" << a.target_id << "\n";
  }

  if (expected_dispatch_ticks == actual_dispatch_ticks) {
    dispatch_tick_matches = 1;
  }

  int covered_reasons = 0;
  for (const auto& expected_reason : expected_reasons) {
    if (actual_reasons.find(expected_reason) != actual_reasons.end()) {
      ++covered_reasons;
    }
  }

  const double outcome_match_rate = static_cast<double>(outcome_matches) / static_cast<double>(expected.size());
  const double dispatch_tick_match_rate = dispatch_tick_matches == 1 ? 1.0 : 0.0;
  const double reason_coverage =
      expected_reasons.empty() ? 1.0 : static_cast<double>(covered_reasons) / static_cast<double>(expected_reasons.size());

  std::cout << "metric.outcome_match_rate=" << outcome_match_rate << "\n";
  std::cout << "metric.dispatch_tick_match_rate=" << dispatch_tick_match_rate << "\n";
  std::cout << "metric.reason_coverage=" << reason_coverage << "\n";

  const bool pass = outcome_match_rate >= 0.95 && dispatch_tick_match_rate >= 1.0 && reason_coverage >= 1.0;
  if (!pass) {
    std::cerr << "parity_failed thresholds_not_met\n";
    return 1;
  }

  std::cout << "parity_passed\n";
  return 0;
}

} // namespace

int main(int argc, char** argv) {
  std::string baseline_path = "native-core/parity/woodcutting_baseline_v1.csv";
  if (argc > 1 && argv[1] != nullptr && std::string(argv[1]).size() > 0) {
    baseline_path = argv[1];
  }

  std::vector<ParityRecord> expected;
  if (!load_baseline(baseline_path, &expected)) {
    std::cerr << "failed_to_load_baseline path=" << baseline_path << "\n";
    return 1;
  }

  const auto actual = run_actual_trace();
  return run_parity(expected, actual);
}
