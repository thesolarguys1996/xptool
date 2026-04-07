#include "xptool/ui/status_overlay.hpp"
#include "xptool/ui/telemetry_parser.hpp"
#include "xptool/ui/ui_config.hpp"

#include <cstdlib>
#include <fstream>
#include <iostream>
#include <string>

namespace {

void print_help() {
  std::cout << "xptool_native_ui\n"
            << "Usage:\n"
            << "  xptool_native_ui --telemetry-path <path> [options]\n\n"
            << "Options:\n"
            << "  --config-path <path>       Optional key=value config file\n"
            << "  --tail-lines <n>           Only parse last n telemetry lines\n"
            << "  --write-overlay <path>     Write rendered overlay text to file\n"
            << "  --help                     Show this help\n";
}

bool require_value(int argc, char** argv, int index, const std::string& flag_name, std::string* out) {
  if (out == nullptr || index + 1 >= argc) {
    std::cerr << "missing value for " << flag_name << "\n";
    return false;
  }
  *out = argv[index + 1];
  return true;
}

} // namespace

int main(int argc, char** argv) {
  std::string telemetry_path;
  std::string config_path;
  std::string write_overlay_path;
  int tail_lines = 0;

  for (int i = 1; i < argc; ++i) {
    const std::string arg = argv[i];
    if (arg == "--help") {
      print_help();
      return 0;
    }
    if (arg == "--telemetry-path") {
      if (!require_value(argc, argv, i, arg, &telemetry_path)) {
        return 1;
      }
      ++i;
      continue;
    }
    if (arg == "--config-path") {
      if (!require_value(argc, argv, i, arg, &config_path)) {
        return 1;
      }
      ++i;
      continue;
    }
    if (arg == "--tail-lines") {
      std::string value;
      if (!require_value(argc, argv, i, arg, &value)) {
        return 1;
      }
      tail_lines = std::max(0, std::atoi(value.c_str()));
      ++i;
      continue;
    }
    if (arg == "--write-overlay") {
      if (!require_value(argc, argv, i, arg, &write_overlay_path)) {
        return 1;
      }
      ++i;
      continue;
    }
    std::cerr << "unknown option: " << arg << "\n";
    print_help();
    return 1;
  }

  if (telemetry_path.empty()) {
    std::cerr << "missing required --telemetry-path\n";
    return 1;
  }

  const xptool::ui::UiConfig config = xptool::ui::load_ui_config(config_path);
  const auto events = xptool::ui::read_telemetry_file(telemetry_path, tail_lines);
  if (events.empty()) {
    std::cerr << "no telemetry events parsed path=" << telemetry_path << "\n";
    return 2;
  }

  const auto summary = xptool::ui::build_overlay_summary(events, config);
  const std::string rendered = xptool::ui::render_overlay_text(summary, config, telemetry_path);
  std::cout << rendered;

  if (!write_overlay_path.empty()) {
    std::ofstream out(write_overlay_path, std::ios::trunc);
    if (out.is_open()) {
      out << rendered;
      out.close();
    } else {
      std::cerr << "failed to write overlay path=" << write_overlay_path << "\n";
      return 3;
    }
  }
  return 0;
}
