#include "xptool/bridge/telemetry_event.hpp"

#include <chrono>
#include <ctime>
#include <iomanip>
#include <iostream>
#include <sstream>

namespace xptool::bridge {

TelemetrySink::TelemetrySink(const std::string& out_path, bool emit_stdout)
    : out_path_(out_path), emit_stdout_(emit_stdout), sequence_(0), file_() {
  if (!out_path_.empty()) {
    file_.open(out_path_, std::ios::app);
  }
}

TelemetrySink::~TelemetrySink() {
  if (file_.is_open()) {
    file_.flush();
    file_.close();
  }
}

void TelemetrySink::emit(const std::string& event_type, const std::string& reason_code, const std::map<std::string, std::string>& data) {
  ++sequence_;
  std::ostringstream line;
  line << "{";
  line << "\"schemaVersion\":\"1.0\",";
  line << "\"eventId\":\"native-bridge-" << sequence_ << "\",";
  line << "\"eventType\":\"" << escape_json(event_type) << "\",";
  line << "\"eventTimeUtc\":\"" << utc_now_iso8601() << "\",";
  line << "\"source\":\"native-bridge\",";
  line << "\"reasonCode\":\"" << escape_json(reason_code) << "\",";
  line << "\"data\":{";
  bool first = true;
  for (const auto& [key, value] : data) {
    if (!first) {
      line << ",";
    }
    first = false;
    line << "\"" << escape_json(key) << "\":\"" << escape_json(value) << "\"";
  }
  line << "}}";
  write_line(line.str());
}

void TelemetrySink::write_line(const std::string& line) {
  if (file_.is_open()) {
    file_ << line << "\n";
    file_.flush();
  }
  if (emit_stdout_) {
    std::cout << line << "\n";
  }
}

std::string utc_now_iso8601() {
  const auto now = std::chrono::system_clock::now();
  const std::time_t now_time_t = std::chrono::system_clock::to_time_t(now);
  std::tm utc_tm{};
#if defined(_WIN32)
  gmtime_s(&utc_tm, &now_time_t);
#else
  gmtime_r(&now_time_t, &utc_tm);
#endif
  std::ostringstream out;
  out << std::put_time(&utc_tm, "%Y-%m-%dT%H:%M:%SZ");
  return out.str();
}

std::string escape_json(const std::string& value) {
  std::string out;
  out.reserve(value.size() + 8);
  for (char c : value) {
    switch (c) {
      case '\\':
        out += "\\\\";
        break;
      case '\"':
        out += "\\\"";
        break;
      case '\b':
        out += "\\b";
        break;
      case '\f':
        out += "\\f";
        break;
      case '\n':
        out += "\\n";
        break;
      case '\r':
        out += "\\r";
        break;
      case '\t':
        out += "\\t";
        break;
      default:
        out += c;
        break;
    }
  }
  return out;
}

} // namespace xptool::bridge
