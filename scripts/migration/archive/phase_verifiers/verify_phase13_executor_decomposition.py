from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE13_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE13_EXECUTOR_DECOMPOSITION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
COMMAND_EXECUTOR = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java"
COMMAND_ID_DEDUPE = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/executor/CommandIdDeduplicationService.java"
COMMAND_FILE_PATH_RESOLVER = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/executor/CommandFilePathResolver.java"
)
MANUAL_METRICS_GATE_TELEMETRY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/executor/ManualMetricsGateTelemetryService.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE13_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        COMMAND_EXECUTOR,
        COMMAND_ID_DEDUPE,
        COMMAND_FILE_PATH_RESOLVER,
        MANUAL_METRICS_GATE_TELEMETRY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase13-executor-decomposition] FAILED")
        for error in errors:
            print(f"[phase13-executor-decomposition] ERROR {error}")
        return 1

    phase13_plan_text = _read(PHASE13_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    command_executor_text = _read(COMMAND_EXECUTOR)

    if "## Phase 13 Slice Status" not in phase13_plan_text:
        errors.append("phase13_plan_missing_slice_status")
    if "`13.1` complete." not in phase13_plan_text:
        errors.append("phase13_plan_missing_13_1_complete")
    if "`13.2` complete." not in phase13_plan_text:
        errors.append("phase13_plan_missing_13_2_complete")
    if "`13.3` complete." not in phase13_plan_text:
        errors.append("phase13_plan_missing_13_3_complete")

    if "## Phase 13 (Executor Compatibility Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase13_section")

    if "PHASE 13 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase13_started")
    if "PHASE 13 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase13_complete")

    required_tasks = [
        "- [x] Define Phase 13 executor compatibility-decomposition scope and completion evidence gates.",
        "- [x] Extract command-ingest id/path policy and manual-metrics gate telemetry ownership from `CommandExecutor` into focused runtime services.",
        "- [x] Run Phase 13 verification + guard pack and mark `PHASE 13 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase13_line:{task_line}")

    if "private final Set<String> seenCommandIds = new LinkedHashSet<>()" in command_executor_text:
        errors.append("command_executor_still_owns_seen_command_id_set")
    if "private boolean isDuplicateCommandId(String commandId)" in command_executor_text:
        errors.append("command_executor_still_owns_duplicate_command_id_logic")
    if "private String resolveCommandFilePath()" in command_executor_text:
        errors.append("command_executor_still_owns_command_file_path_policy")
    if "private void maybeEmitManualMetricsRuntimeGateEvent" not in command_executor_text:
        errors.append("command_executor_missing_manual_metrics_gate_delegate")
    if "manualMetricsGateTelemetryService.emitGateEvent" not in command_executor_text:
        errors.append("command_executor_missing_manual_metrics_gate_service_delegate")

    if errors:
        print("[phase13-executor-decomposition] FAILED")
        for error in errors:
            print(f"[phase13-executor-decomposition] ERROR {error}")
        return 1

    print("[phase13-executor-decomposition] OK: executor decomposition Phase 13 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
