#pragma once

#include <fstream>
#include <map>
#include <string>

namespace xptool::bridge {

class TelemetrySink {
public:
  explicit TelemetrySink(const std::string& out_path, bool emit_stdout);
  ~TelemetrySink();

  void emit(const std::string& event_type, const std::string& reason_code, const std::map<std::string, std::string>& data);

private:
  void write_line(const std::string& line);
  std::string out_path_;
  bool emit_stdout_;
  unsigned long long sequence_;
  std::ofstream file_;
};

std::string utc_now_iso8601();
std::string escape_json(const std::string& value);

} // namespace xptool::bridge
