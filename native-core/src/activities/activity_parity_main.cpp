#include "xptool/core/repeat_target_selector.hpp"
#include "xptool/core/runtime_runner.hpp"
#include "xptool/core/state_snapshot.hpp"

#include <fstream>
#include <iostream>
#include <map>
#include <set>
#include <sstream>
#include <string>
#include <vector>

namespace {

struct ParityRecord {
  std::string activity_key;
  int tick = -1;
  bool dispatched = false;
  std::string reason_code;
  int target_id = -1;
};

bool parse_bool_token(const std::string& token) { return token == "1" || token == "true" || token == "TRUE"; }

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
    std::string activity_token;
    std::string tick_token;
    std::string dispatched_token;
    std::string reason_token;
    std::string target_token;
    if (!std::getline(stream, activity_token, ',')) {
      continue;
    }
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
    record.activity_key = activity_token;
    record.tick = std::stoi(tick_token);
    record.dispatched = parse_bool_token(dispatched_token);
    record.reason_code = reason_token;
    record.target_id = std::stoi(target_token);
    out->push_back(record);
  }
  return !out->empty();
}

void add_candidate(std::vector<xptool::core::RepeatTargetCandidate>* out, int id, int distance) {
  if (out == nullptr) {
    return;
  }
  xptool::core::RepeatTargetCandidate candidate;
  candidate.target_id = id;
  candidate.distance = distance;
  candidate.interactable = true;
  out->push_back(candidate);
}

std::vector<ParityRecord> run_activity_scenario(const std::string& activity_key, int base_tick, int id_a, int id_b, bool banking_mode) {
  xptool::core::RuntimeRunner runner;
  std::vector<xptool::core::StateSnapshot> snapshots(5);
  for (int i = 0; i < 5; ++i) {
    snapshots[i].activity_key = activity_key;
    snapshots[i].tick = base_tick + i;
    snapshots[i].logged_in = true;
    snapshots[i].in_focus = true;
    snapshots[i].has_target_candidate = true;
  }

  if (banking_mode) {
    add_candidate(&snapshots[0].banking_candidates, id_a, 2);
    add_candidate(&snapshots[0].banking_candidates, id_b, 4);
    add_candidate(&snapshots[1].banking_candidates, id_a, 1);
    add_candidate(&snapshots[1].banking_candidates, id_b, 2);
    add_candidate(&snapshots[2].banking_candidates, id_a, 1);
    add_candidate(&snapshots[2].banking_candidates, id_b, 2);
    snapshots[3].bank_open = true;
    snapshots[3].has_target_candidate = false;
    add_candidate(&snapshots[4].banking_candidates, id_b, 1);
  } else if (activity_key == "mining") {
    add_candidate(&snapshots[0].mining_candidates, id_a, 2);
    add_candidate(&snapshots[0].mining_candidates, id_b, 4);
    add_candidate(&snapshots[1].mining_candidates, id_a, 1);
    add_candidate(&snapshots[1].mining_candidates, id_b, 2);
    add_candidate(&snapshots[2].mining_candidates, id_a, 1);
    add_candidate(&snapshots[2].mining_candidates, id_b, 2);
    add_candidate(&snapshots[3].mining_candidates, id_b, 1);
    add_candidate(&snapshots[4].mining_candidates, id_b, 1);
  } else if (activity_key == "fishing") {
    add_candidate(&snapshots[0].fishing_candidates, id_a, 2);
    add_candidate(&snapshots[0].fishing_candidates, id_b, 4);
    add_candidate(&snapshots[1].fishing_candidates, id_a, 1);
    add_candidate(&snapshots[1].fishing_candidates, id_b, 2);
    add_candidate(&snapshots[2].fishing_candidates, id_a, 1);
    add_candidate(&snapshots[2].fishing_candidates, id_b, 2);
    add_candidate(&snapshots[3].fishing_candidates, id_b, 1);
    add_candidate(&snapshots[4].fishing_candidates, id_b, 1);
  } else {
    add_candidate(&snapshots[0].combat_candidates, id_a, 2);
    add_candidate(&snapshots[0].combat_candidates, id_b, 4);
    add_candidate(&snapshots[1].combat_candidates, id_a, 1);
    add_candidate(&snapshots[1].combat_candidates, id_b, 2);
    add_candidate(&snapshots[2].combat_candidates, id_a, 1);
    add_candidate(&snapshots[2].combat_candidates, id_b, 2);
    add_candidate(&snapshots[3].combat_candidates, id_b, 1);
    add_candidate(&snapshots[4].combat_candidates, id_b, 1);
  }

  std::vector<ParityRecord> actual;
  actual.reserve(snapshots.size());
  for (const auto& snapshot : snapshots) {
    const auto result = runner.tick(snapshot);
    ParityRecord record;
    record.activity_key = activity_key;
    record.tick = snapshot.tick;
    record.dispatched = result.dispatched;
    record.reason_code = result.reason_code;
    record.target_id = result.target_id;
    actual.push_back(record);
  }
  return actual;
}

std::vector<ParityRecord> run_actual_trace() {
  std::vector<ParityRecord> output;
  const auto mining = run_activity_scenario("mining", 200, 3001, 3002, false);
  const auto fishing = run_activity_scenario("fishing", 300, 4001, 4002, false);
  const auto combat = run_activity_scenario("combat", 400, 5001, 5002, false);
  const auto banking = run_activity_scenario("banking", 500, 6001, 6002, true);
  output.insert(output.end(), mining.begin(), mining.end());
  output.insert(output.end(), fishing.begin(), fishing.end());
  output.insert(output.end(), combat.begin(), combat.end());
  output.insert(output.end(), banking.begin(), banking.end());
  return output;
}

bool evaluate_activity(
    const std::string& activity_key,
    const std::vector<ParityRecord>& expected,
    const std::vector<ParityRecord>& actual,
    double* out_outcome_rate,
    double* out_dispatch_tick_rate,
    double* out_reason_coverage) {
  std::vector<ParityRecord> e;
  std::vector<ParityRecord> a;
  for (const auto& row : expected) {
    if (row.activity_key == activity_key) {
      e.push_back(row);
    }
  }
  for (const auto& row : actual) {
    if (row.activity_key == activity_key) {
      a.push_back(row);
    }
  }
  if (e.size() != a.size() || e.empty()) {
    return false;
  }

  int outcome_matches = 0;
  std::vector<int> expected_dispatch_ticks;
  std::vector<int> actual_dispatch_ticks;
  std::set<std::string> expected_reasons;
  std::set<std::string> actual_reasons;

  for (size_t i = 0; i < e.size(); ++i) {
    if (e[i].dispatched == a[i].dispatched) {
      ++outcome_matches;
    }
    if (e[i].dispatched) {
      expected_dispatch_ticks.push_back(e[i].tick);
    }
    if (a[i].dispatched) {
      actual_dispatch_ticks.push_back(a[i].tick);
    }
    expected_reasons.insert(e[i].reason_code);
    actual_reasons.insert(a[i].reason_code);
    std::cout << "activity=" << activity_key << " tick=" << a[i].tick << " dispatched=" << (a[i].dispatched ? "1" : "0")
              << " reason=" << a[i].reason_code << " target=" << a[i].target_id << "\n";
  }

  int covered_reasons = 0;
  for (const auto& expected_reason : expected_reasons) {
    if (actual_reasons.find(expected_reason) != actual_reasons.end()) {
      ++covered_reasons;
    }
  }

  const double outcome_rate = static_cast<double>(outcome_matches) / static_cast<double>(e.size());
  const double dispatch_rate = (expected_dispatch_ticks == actual_dispatch_ticks) ? 1.0 : 0.0;
  const double coverage = static_cast<double>(covered_reasons) / static_cast<double>(expected_reasons.size());

  if (out_outcome_rate != nullptr) {
    *out_outcome_rate = outcome_rate;
  }
  if (out_dispatch_tick_rate != nullptr) {
    *out_dispatch_tick_rate = dispatch_rate;
  }
  if (out_reason_coverage != nullptr) {
    *out_reason_coverage = coverage;
  }
  return true;
}

} // namespace

int main(int argc, char** argv) {
  std::string baseline_path = "native-core/parity/activity_baseline_v1.csv";
  if (argc > 1 && argv[1] != nullptr && std::string(argv[1]).size() > 0) {
    baseline_path = argv[1];
  }

  std::vector<ParityRecord> expected;
  if (!load_baseline(baseline_path, &expected)) {
    std::cerr << "failed_to_load_baseline path=" << baseline_path << "\n";
    return 1;
  }
  const auto actual = run_actual_trace();

  const std::vector<std::string> activities = {"mining", "fishing", "combat", "banking"};
  bool all_passed = true;
  for (const auto& activity_key : activities) {
    double outcome = 0.0;
    double dispatch = 0.0;
    double coverage = 0.0;
    if (!evaluate_activity(activity_key, expected, actual, &outcome, &dispatch, &coverage)) {
      std::cerr << "parity_failed activity=" << activity_key << " reason=missing_rows\n";
      return 1;
    }
    std::cout << "metric." << activity_key << ".outcome_match_rate=" << outcome << "\n";
    std::cout << "metric." << activity_key << ".dispatch_tick_match_rate=" << dispatch << "\n";
    std::cout << "metric." << activity_key << ".reason_coverage=" << coverage << "\n";
    const bool pass = outcome >= 0.95 && dispatch >= 1.0 && coverage >= 1.0;
    if (!pass) {
      all_passed = false;
    }
  }

  if (!all_passed) {
    std::cerr << "parity_failed thresholds_not_met\n";
    return 1;
  }
  std::cout << "parity_passed\n";
  return 0;
}
