from __future__ import annotations

import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PLANNER_DIR = ROOT / "src" / "runelite_planner"
PLUGIN_EXECUTOR = ROOT / "runelite-plugin" / "src" / "main" / "java" / "com" / "xptool" / "executor" / "CommandExecutor.java"
PLANNER_MOTION = PLANNER_DIR / "runtime_core" / "motion_engine.py"
PLUGIN_MOTION = ROOT / "runelite-plugin" / "src" / "main" / "java" / "com" / "xptool" / "motion" / "MotionProfile.java"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def planner_command_types() -> set[str]:
    out: set[str] = set()
    for path in PLANNER_DIR.rglob("*.py"):
        text = _read(path)
        out.update(re.findall(r'command_type\s*=\s*"([A-Z0-9_]+)"', text))
        out.update(re.findall(r'command_type\s*:\s*str\s*=\s*"([A-Z0-9_]+)"', text))
    return out


def plugin_supported_command_types(plugin_text: str) -> set[str]:
    return set(re.findall(r'"([A-Z0-9_]+)"\.equals\(commandType\)', plugin_text))


def planner_target_categories() -> set[str]:
    out: set[str] = set()
    for path in PLANNER_DIR.glob("*cutting.py"):
        text = _read(path)
        out.update(re.findall(r'"targetCategory"\s*:\s*"([A-Z_]+)"', text))
    return out


def plugin_explicit_target_categories(plugin_text: str) -> set[str]:
    return set(re.findall(r'"([A-Z_]+)"\.equals\(targetCategory\)', plugin_text))


def planner_motion_profiles() -> set[str]:
    if not PLANNER_MOTION.exists():
        # Legacy fallback for older planner layout.
        legacy = PLANNER_DIR / "motion_profiles.py"
        if legacy.exists():
            text = _read(legacy)
            return set(re.findall(r'"([a-z]+)"\s*:\s*MotionProfile\(', text))
        return set()
    text = _read(PLANNER_MOTION)
    profiles = set(re.findall(r'profile_name:\s*str\s*=\s*"([a-z]+)"', text))
    return profiles


def plugin_motion_profiles() -> set[str]:
    text = _read(PLUGIN_MOTION)
    explicit = set(re.findall(r'"([a-z]+)"\.equals\(name\)', text))
    defaults = set(re.findall(r'DEFAULT_PAYLOAD_MOTION_PROFILE\s*=\s*"([a-z]+)"', text))
    return explicit | defaults


def plugin_default_command_path(plugin_text: str) -> Path:
    match = re.search(r'DEFAULT_COMMAND_FILE_PATH\s*=\s*"([^"]+)";', plugin_text)
    if not match:
        raise ValueError("Could not find DEFAULT_COMMAND_FILE_PATH in plugin executor")
    return (ROOT / match.group(1)).resolve()


def planner_default_command_path() -> Path:
    sys.path.insert(0, str(ROOT / "src"))
    from runelite_planner.paths import default_command_out_path  # pylint: disable=import-outside-toplevel

    return Path(default_command_out_path()).resolve()


def main() -> int:
    plugin_text = _read(PLUGIN_EXECUTOR)

    planner_types = planner_command_types()
    plugin_types = plugin_supported_command_types(plugin_text)
    missing_types = sorted(planner_types - plugin_types)

    planner_categories = planner_target_categories()
    plugin_categories = plugin_explicit_target_categories(plugin_text)
    supported_categories = plugin_categories | {"TREE"}
    missing_categories = sorted(planner_categories - supported_categories)

    planner_profiles = planner_motion_profiles()
    plugin_profiles = plugin_motion_profiles()
    missing_profiles = sorted(planner_profiles - plugin_profiles)

    planner_path = planner_default_command_path()
    plugin_path = plugin_default_command_path(plugin_text)
    path_match = planner_path == plugin_path

    print("Planner command types:", ", ".join(sorted(planner_types)))
    print("Plugin command types:", ", ".join(sorted(plugin_types)))
    print("Planner target categories:", ", ".join(sorted(planner_categories)))
    print("Plugin target categories:", ", ".join(sorted(supported_categories)))
    print("Planner motion profiles:", ", ".join(sorted(planner_profiles)))
    print("Plugin motion profiles:", ", ".join(sorted(plugin_profiles)))
    print("Planner default command bus:", planner_path)
    print("Plugin default command bus:", plugin_path)

    problems: list[str] = []
    if missing_types:
        problems.append(f"planner command types not supported by plugin: {', '.join(missing_types)}")
    if missing_categories:
        problems.append(f"planner target categories not supported by plugin: {', '.join(missing_categories)}")
    if missing_profiles:
        problems.append(f"planner motion profiles not supported by plugin: {', '.join(missing_profiles)}")
    if not path_match:
        problems.append("planner/plugin default command bus paths differ")

    if problems:
        print("\nLinking check failed:")
        for problem in problems:
            print(f"- {problem}")
        return 1

    print("\nLinking check passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

