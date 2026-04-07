from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE16_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE16_MOTOR_PENDING_TELEMETRY_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
COMMAND_EXECUTOR = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java"
PENDING_MOVE_TELEMETRY_SERVICE = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/executor/PendingMoveTelemetryService.java"
)
PENDING_MOVE_TELEMETRY_SERVICE_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE16_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        COMMAND_EXECUTOR,
        PENDING_MOVE_TELEMETRY_SERVICE,
        PENDING_MOVE_TELEMETRY_SERVICE_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase16-motor-pending-telemetry] FAILED")
        for error in errors:
            print(f"[phase16-motor-pending-telemetry] ERROR {error}")
        return 1

    phase16_plan_text = _read(PHASE16_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    command_executor_text = _read(COMMAND_EXECUTOR)

    if "## Phase 16 Slice Status" not in phase16_plan_text:
        errors.append("phase16_plan_missing_slice_status")
    if "`16.1` complete." not in phase16_plan_text:
        errors.append("phase16_plan_missing_16_1_complete")
    if "`16.2` complete." not in phase16_plan_text:
        errors.append("phase16_plan_missing_16_2_complete")
    if "`16.3` complete." not in phase16_plan_text:
        errors.append("phase16_plan_missing_16_3_complete")

    if "## Phase 16 (Motor Pending-Telemetry Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase16_section")

    if "PHASE 16 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase16_started")
    if "PHASE 16 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase16_complete")

    required_tasks = [
        "- [x] Define Phase 16 motor pending-telemetry decomposition scope and completion evidence gates.",
        "- [x] Extract pending-move telemetry ownership from `CommandExecutor` into focused runtime service.",
        "- [x] Run Phase 16 verification + guard pack and mark `PHASE 16 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase16_line:{task_line}")

    if "private final PendingMoveTelemetryService pendingMoveTelemetryService;" not in command_executor_text:
        errors.append("command_executor_missing_pending_move_telemetry_service_field")
    if "import java.util.LinkedHashSet;" in command_executor_text:
        errors.append("command_executor_still_contains_unused_linkedhashset_import")
    if "private void setDropSweepNextSlot(int slot)" in command_executor_text:
        errors.append("command_executor_still_contains_unused_drop_sweep_next_slot_helper")
    if "private void notePendingMoveAge(" in command_executor_text:
        errors.append("command_executor_still_owns_note_pending_move_age")
    if "private void notePendingMoveBlocked(" in command_executor_text:
        errors.append("command_executor_still_owns_note_pending_move_blocked")
    if "private void emitOffscreenPendingMoveEvent(" in command_executor_text:
        errors.append("command_executor_still_owns_pending_move_event_emitter")
    if "pendingMoveTelemetryService::notePendingMoveRemainingDistance" not in command_executor_text:
        errors.append("command_executor_missing_pending_move_telemetry_delegate")

    if errors:
        print("[phase16-motor-pending-telemetry] FAILED")
        for error in errors:
            print(f"[phase16-motor-pending-telemetry] ERROR {error}")
        return 1

    print("[phase16-motor-pending-telemetry] OK: motor pending telemetry Phase 16 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
