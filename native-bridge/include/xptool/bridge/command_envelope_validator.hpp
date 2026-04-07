#pragma once

#include <string>
#include <unordered_set>

namespace xptool::bridge {

struct CommandValidationResult {
  bool accepted = false;
  std::string reason_code;
  std::string command_id;
  std::string command_type;
};

class CommandEnvelopeValidator {
public:
  CommandEnvelopeValidator(bool enable_verifier, int max_clock_skew_seconds);
  CommandValidationResult validate_line(const std::string& line);

private:
  bool enable_verifier_;
  int max_clock_skew_seconds_;
  std::unordered_set<std::string> seen_command_ids_;
};

} // namespace xptool::bridge
