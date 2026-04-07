from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE15_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE15_DROP_RUNTIME_DECOMPOSITION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
COMMAND_EXECUTOR = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java"
DROP_SWEEP_SESSION_SERVICE = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/executor/DropSweepSessionService.java"
)
DROP_SWEEP_INVENTORY_SERVICE = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/executor/DropSweepInventoryService.java"
)
DROP_SWEEP_SESSION_SERVICE_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/executor/DropSweepSessionServiceTest.java"
)
DROP_SWEEP_INVENTORY_SERVICE_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/executor/DropSweepInventoryServiceTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE15_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        COMMAND_EXECUTOR,
        DROP_SWEEP_SESSION_SERVICE,
        DROP_SWEEP_INVENTORY_SERVICE,
        DROP_SWEEP_SESSION_SERVICE_TEST,
        DROP_SWEEP_INVENTORY_SERVICE_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase15-drop-runtime-decomposition] FAILED")
        for error in errors:
            print(f"[phase15-drop-runtime-decomposition] ERROR {error}")
        return 1

    phase15_plan_text = _read(PHASE15_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    command_executor_text = _read(COMMAND_EXECUTOR)

    if "## Phase 15 Slice Status" not in phase15_plan_text:
        errors.append("phase15_plan_missing_slice_status")
    if "`15.1` complete." not in phase15_plan_text:
        errors.append("phase15_plan_missing_15_1_complete")
    if "`15.2` complete." not in phase15_plan_text:
        errors.append("phase15_plan_missing_15_2_complete")
    if "`15.3` complete." not in phase15_plan_text:
        errors.append("phase15_plan_missing_15_3_complete")

    if "## Phase 15 (Drop Runtime Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase15_section")

    if "PHASE 15 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase15_started")
    if "PHASE 15 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase15_complete")

    required_tasks = [
        "- [x] Define Phase 15 drop-runtime decomposition scope and completion evidence gates.",
        "- [x] Extract drop-sweep session state and drop-target inventory policy ownership from `CommandExecutor`.",
        "- [x] Run Phase 15 verification + guard pack and mark `PHASE 15 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase15_line:{task_line}")

    if "private final DropSweepSessionService dropSweepSessionService;" not in command_executor_text:
        errors.append("command_executor_missing_drop_sweep_session_service")
    if "private final DropSweepInventoryService dropSweepInventoryService;" not in command_executor_text:
        errors.append("command_executor_missing_drop_sweep_inventory_service")
    if "private boolean dropSweepSessionActive" in command_executor_text:
        errors.append("command_executor_still_owns_drop_sweep_session_active_field")
    if "private int dropSweepItemId" in command_executor_text:
        errors.append("command_executor_still_owns_drop_sweep_item_id_field")
    if "private boolean isDropSweepTargetItem(" in command_executor_text:
        errors.append("command_executor_still_owns_drop_target_item_policy")
    if "private boolean isBeginnerClueOrBoxItem(" in command_executor_text:
        errors.append("command_executor_still_owns_beginner_clue_alias_policy")
    if "dropSweepSessionService.beginSession(" not in command_executor_text:
        errors.append("command_executor_missing_drop_sweep_session_delegate_begin")
    if "dropSweepInventoryService.findInventorySlotFrom(" not in command_executor_text:
        errors.append("command_executor_missing_drop_sweep_inventory_delegate_find_slot")
    if "dropSweepInventoryService.countDropSweepTargetItems(" not in command_executor_text:
        errors.append("command_executor_missing_drop_sweep_inventory_delegate_count")

    if errors:
        print("[phase15-drop-runtime-decomposition] FAILED")
        for error in errors:
            print(f"[phase15-drop-runtime-decomposition] ERROR {error}")
        return 1

    print("[phase15-drop-runtime-decomposition] OK: drop runtime decomposition Phase 15 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
