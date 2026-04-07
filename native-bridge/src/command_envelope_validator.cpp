#include "xptool/bridge/command_envelope_validator.hpp"

#include "xptool/bridge/command_policy.hpp"

#include <chrono>
#include <cmath>
#include <ctime>
#include <iomanip>
#include <optional>
#include <regex>
#include <sstream>
#include <string>

namespace xptool::bridge {
namespace {

std::optional<std::string> extract_string_field(const std::string& json, const std::string& key) {
  const std::regex pattern("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
  std::smatch match;
  if (!std::regex_search(json, match, pattern) || match.size() < 2) {
    return std::nullopt;
  }
  return match[1].str();
}

bool has_object_field(const std::string& json, const std::string& key) {
  const std::regex pattern("\"" + key + "\"\\s*:\\s*\\{");
  return std::regex_search(json, pattern);
}

bool is_valid_command_type_token(const std::string& command_type) {
  if (command_type.empty()) {
    return false;
  }
  static const std::regex token("^[A-Z0-9_]+$");
  return std::regex_match(command_type, token);
}

bool try_parse_utc_iso8601(const std::string& value, std::time_t* out_utc) {
  if (out_utc == nullptr) {
    return false;
  }
  std::tm tm{};
  std::istringstream stream(value);
  stream >> std::get_time(&tm, "%Y-%m-%dT%H:%M:%SZ");
  if (stream.fail()) {
    return false;
  }
#if defined(_WIN32)
  *out_utc = _mkgmtime(&tm);
#else
  *out_utc = timegm(&tm);
#endif
  return *out_utc >= 0;
}

std::string validate_schema_basics(const std::string& line, std::string* out_command_id, std::string* out_command_type, std::time_t* out_issued_at_utc) {
  const auto schema_version = extract_string_field(line, "schemaVersion");
  if (!schema_version.has_value() || schema_version.value() != "1.0") {
    return "invalid_schema";
  }

  const auto command_id = extract_string_field(line, "commandId");
  if (!command_id.has_value() || command_id->empty()) {
    return "invalid_schema";
  }
  if (out_command_id != nullptr) {
    *out_command_id = command_id.value();
  }

  const auto command_type = extract_string_field(line, "commandType");
  if (!command_type.has_value() || !is_valid_command_type_token(command_type.value())) {
    return "invalid_schema";
  }
  if (out_command_type != nullptr) {
    *out_command_type = command_type.value();
  }

  if (!has_object_field(line, "payload")) {
    return "invalid_schema";
  }

  const auto issued_at_utc = extract_string_field(line, "issuedAtUtc");
  if (!issued_at_utc.has_value() || issued_at_utc->empty()) {
    return "invalid_schema";
  }
  if (out_issued_at_utc != nullptr) {
    std::time_t parsed{};
    if (!try_parse_utc_iso8601(issued_at_utc.value(), &parsed)) {
      return "invalid_schema";
    }
    *out_issued_at_utc = parsed;
  }

  return {};
}

} // namespace

CommandEnvelopeValidator::CommandEnvelopeValidator(bool enable_verifier, int max_clock_skew_seconds)
    : enable_verifier_(enable_verifier), max_clock_skew_seconds_(max_clock_skew_seconds <= 0 ? 300 : max_clock_skew_seconds) {}

CommandValidationResult CommandEnvelopeValidator::validate_line(const std::string& line) {
  CommandValidationResult result{};
  std::time_t issued_at_utc{};
  const std::string schema_error = validate_schema_basics(line, &result.command_id, &result.command_type, &issued_at_utc);
  if (!schema_error.empty()) {
    result.accepted = false;
    result.reason_code = schema_error;
    return result;
  }

  if (!is_supported_command_type(result.command_type)) {
    result.accepted = false;
    result.reason_code = "unsupported_command_type";
    return result;
  }

  if (enable_verifier_) {
    const auto now = std::chrono::system_clock::to_time_t(std::chrono::system_clock::now());
    const auto delta_seconds = static_cast<long long>(std::llabs(static_cast<long long>(now - issued_at_utc)));
    if (delta_seconds > static_cast<long long>(max_clock_skew_seconds_)) {
      result.accepted = false;
      result.reason_code = "replay_rejected_timestamp";
      return result;
    }
    if (seen_command_ids_.find(result.command_id) != seen_command_ids_.end()) {
      result.accepted = false;
      result.reason_code = "replay_rejected_command_id";
      return result;
    }
    seen_command_ids_.insert(result.command_id);
  }

  result.accepted = true;
  result.reason_code = "accepted";
  return result;
}

} // namespace xptool::bridge
