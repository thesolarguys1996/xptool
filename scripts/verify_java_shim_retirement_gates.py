from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
XPTOOL_PLUGIN = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/XPToolPlugin.java"
XPTOOL_CONFIG = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/XPToolConfig.java"
COMMAND_EXECUTOR = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java"
RUNELITE_PLUGIN_PROPERTIES = PROJECT_ROOT / "runelite-plugin/src/main/resources/runelite-plugin.properties"
PHASE9_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE9_SHIM_RETIREMENT_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    for path in (COMMAND_EXECUTOR, RUNELITE_PLUGIN_PROPERTIES, PHASE9_PLAN, PHASE_STATUS, TASKS):
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[java-shim-retirement] FAILED")
        for error in errors:
            print(f"[java-shim-retirement] ERROR {error}")
        return 1

    command_executor_text = _read(COMMAND_EXECUTOR)
    runelite_plugin_properties_text = _read(RUNELITE_PLUGIN_PROPERTIES)
    phase9_plan_text = _read(PHASE9_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)

    if XPTOOL_PLUGIN.exists():
        errors.append("xptool_plugin_file_still_present")
    if XPTOOL_CONFIG.exists():
        errors.append("xptool_config_file_still_present")
    if "plugins=com.xptool.XPToolPlugin" in runelite_plugin_properties_text:
        errors.append("runelite_plugin_properties_still_registers_xptool_plugin")

    if "XPToolConfig" in command_executor_text:
        errors.append("command_executor_still_depends_on_xptool_config")
    if 'System.getProperty("xptool.leftClickDropItemIds")' not in command_executor_text:
        errors.append("command_executor_missing_system_property_drop_id_source")

    if "PHASE 9 STARTED" not in phase_status_text and "PHASE 9 COMPLETE" not in phase_status_text:
        errors.append("phase9_status_missing_started_or_complete")
    if "Phase 9.3" not in phase_status_text:
        errors.append("phase9_status_missing_9_3_evidence")
    if "Phase 9.4" not in phase_status_text:
        errors.append("phase9_status_missing_9_4_evidence")
    if "Cutover Gates (Must Pass Before Removal)" not in phase9_plan_text:
        errors.append("phase9_plan_missing_cutover_gates_section")
    if "`9.3` complete." not in phase9_plan_text:
        errors.append("phase9_plan_missing_9_3_complete_marker")
    if "`9.4` complete." not in phase9_plan_text:
        errors.append("phase9_plan_missing_9_4_complete_marker")
    if "`9.5` complete." not in phase9_plan_text:
        errors.append("phase9_plan_missing_9_5_complete_marker")
    if "- [x] Define Phase 9 Java shim retirement checklist and cutover gates (`XPToolPlugin`/`XPToolConfig`)." not in tasks_text:
        errors.append("tasks_missing_phase9_checklist_item")
    if "- [x] Remove `XPToolConfig` and `provideConfig(...)` shim wiring." not in tasks_text:
        errors.append("tasks_missing_phase9_3_checklist_item")
    if "- [x] Remove `XPToolPlugin` and RuneLite plugin registration shim references." not in tasks_text:
        errors.append("tasks_missing_phase9_4_checklist_item")
    if "- [x] Run full Phase 9 gate pack and mark `PHASE 9 COMPLETE`." not in tasks_text:
        errors.append("tasks_missing_phase9_complete_gate_item")

    if errors:
        print("[java-shim-retirement] FAILED")
        for error in errors:
            print(f"[java-shim-retirement] ERROR {error}")
        return 1

    print("[java-shim-retirement] OK: Java shim retirement gates are enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
