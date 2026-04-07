from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE14_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE14_EXECUTOR_UTILITY_DECOMPOSITION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
COMMAND_EXECUTOR = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java"
EXECUTOR_VALUE_PARSERS = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/executor/ExecutorValueParsers.java"
EXECUTOR_VALUE_PARSERS_TEST = PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/executor/ExecutorValueParsersTest.java"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE14_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        COMMAND_EXECUTOR,
        EXECUTOR_VALUE_PARSERS,
        EXECUTOR_VALUE_PARSERS_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase14-executor-utility-decomposition] FAILED")
        for error in errors:
            print(f"[phase14-executor-utility-decomposition] ERROR {error}")
        return 1

    phase14_plan_text = _read(PHASE14_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    command_executor_text = _read(COMMAND_EXECUTOR)

    if "## Phase 14 Slice Status" not in phase14_plan_text:
        errors.append("phase14_plan_missing_slice_status")
    if "`14.1` complete." not in phase14_plan_text:
        errors.append("phase14_plan_missing_14_1_complete")
    if "`14.2` complete." not in phase14_plan_text:
        errors.append("phase14_plan_missing_14_2_complete")
    if "`14.3` complete." not in phase14_plan_text:
        errors.append("phase14_plan_missing_14_3_complete")

    if "## Phase 14 (Executor Utility Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase14_section")

    if "PHASE 14 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase14_started")
    if "PHASE 14 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase14_complete")

    required_tasks = [
        "- [x] Define Phase 14 executor utility-decomposition scope and completion evidence gates.",
        "- [x] Extract utility helper ownership (`details`/`asInt`) and remove dead helper paths from `CommandExecutor`.",
        "- [x] Run Phase 14 verification + guard pack and mark `PHASE 14 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase14_line:{task_line}")

    if "private static int elapsedTicksSince(" in command_executor_text:
        errors.append("command_executor_still_owns_elapsed_ticks_helper")
    if "private static int asInt(" in command_executor_text:
        errors.append("command_executor_still_owns_as_int_helper")
    if "private static JsonObject details(" in command_executor_text:
        errors.append("command_executor_still_owns_details_helper")
    if "CommandExecutor::details" in command_executor_text:
        errors.append("command_executor_still_uses_local_details_method_reference")
    if "ExecutorValueParsers::details" not in command_executor_text:
        errors.append("command_executor_missing_executor_value_parsers_details_delegation")
    if "ExecutorValueParsers.asInt(" not in command_executor_text:
        errors.append("command_executor_missing_executor_value_parsers_as_int_delegation")

    if errors:
        print("[phase14-executor-utility-decomposition] FAILED")
        for error in errors:
            print(f"[phase14-executor-utility-decomposition] ERROR {error}")
        return 1

    print("[phase14-executor-utility-decomposition] OK: executor utility decomposition Phase 14 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
