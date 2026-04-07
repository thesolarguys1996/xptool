from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE20_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE20_INTERACTION_CLICK_TELEMETRY_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
COMMAND_EXECUTOR = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java"
INTERACTION_CLICK_TELEMETRY_SERVICE = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/executor/InteractionClickTelemetryService.java"
)
INTERACTION_CLICK_TELEMETRY_SERVICE_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE20_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        COMMAND_EXECUTOR,
        INTERACTION_CLICK_TELEMETRY_SERVICE,
        INTERACTION_CLICK_TELEMETRY_SERVICE_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase20-interaction-click-telemetry] FAILED")
        for error in errors:
            print(f"[phase20-interaction-click-telemetry] ERROR {error}")
        return 1

    phase20_plan_text = _read(PHASE20_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    command_executor_text = _read(COMMAND_EXECUTOR)
    telemetry_service_text = _read(INTERACTION_CLICK_TELEMETRY_SERVICE)
    telemetry_service_test_text = _read(INTERACTION_CLICK_TELEMETRY_SERVICE_TEST)

    if "## Phase 20 Slice Status" not in phase20_plan_text:
        errors.append("phase20_plan_missing_slice_status")
    if "`20.1` complete." not in phase20_plan_text:
        errors.append("phase20_plan_missing_20_1_complete")
    if "`20.2` complete." not in phase20_plan_text:
        errors.append("phase20_plan_missing_20_2_complete")
    if "`20.3` complete." not in phase20_plan_text:
        errors.append("phase20_plan_missing_20_3_complete")

    if "## Phase 20 (Interaction Click Telemetry Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase20_section")

    if "PHASE 20 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase20_started")
    if "PHASE 20 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase20_complete")

    required_tasks = [
        "- [x] Define Phase 20 interaction click telemetry decomposition scope and completion evidence gates.",
        "- [x] Extract interaction-click telemetry/state ownership from `CommandExecutor` into focused runtime service.",
        "- [x] Run Phase 20 verification + guard pack and mark `PHASE 20 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase20_line:{task_line}")

    if "private final InteractionClickTelemetryService interactionClickTelemetryService;" not in command_executor_text:
        errors.append("command_executor_missing_interaction_click_telemetry_service_field")
    if "private long interactionClickSerial = 0L;" in command_executor_text:
        errors.append("command_executor_still_owns_interaction_click_serial")
    if "private long lastInteractionClickAtMs = 0L;" in command_executor_text:
        errors.append("command_executor_still_owns_last_interaction_click_at_ms")
    if "private Point lastInteractionClickCanvasPoint = null;" in command_executor_text:
        errors.append("command_executor_still_owns_last_interaction_click_canvas_point")
    if "private Point lastInteractionAnchorCenterCanvasPoint = null;" in command_executor_text:
        errors.append("command_executor_still_owns_last_interaction_anchor_center")
    if "private Rectangle lastInteractionAnchorBoundsCanvas = null;" in command_executor_text:
        errors.append("command_executor_still_owns_last_interaction_anchor_bounds")
    if "interactionClickTelemetryService.noteInteractionClickSuccess(clickType);" not in command_executor_text:
        errors.append("command_executor_missing_click_success_delegate")
    if "interactionClickTelemetryService.rememberInteractionAnchor(anchorCenterCanvasPoint, anchorBoundsCanvas);" not in command_executor_text:
        errors.append("command_executor_missing_remember_anchor_delegate")
    if "return interactionClickTelemetryService.interactionClickSerial();" not in command_executor_text:
        errors.append("command_executor_missing_click_serial_delegate")
    if "return interactionClickTelemetryService.lastInteractionClickCanvasPoint();" not in command_executor_text:
        errors.append("command_executor_missing_last_click_point_delegate")
    if "return interactionClickTelemetryService.isInteractionClickFresh(maxAgeMs);" not in command_executor_text:
        errors.append("command_executor_missing_click_freshness_delegate")
    if "private void emitInteractionClickTelemetry(" in command_executor_text:
        errors.append("command_executor_still_owns_interaction_click_telemetry_emitter")

    if "final class InteractionClickTelemetryService" not in telemetry_service_text:
        errors.append("interaction_click_telemetry_service_missing_class")
    if "void noteInteractionClickSuccess(" not in telemetry_service_text:
        errors.append("interaction_click_telemetry_service_missing_click_success")
    if "void rememberInteractionAnchor(" not in telemetry_service_text:
        errors.append("interaction_click_telemetry_service_missing_remember_anchor")
    if "Optional<Point> lastInteractionClickCanvasPoint()" not in telemetry_service_text:
        errors.append("interaction_click_telemetry_service_missing_last_click_getter")
    if "void emitInteractionClickTelemetry(" not in telemetry_service_text:
        errors.append("interaction_click_telemetry_service_missing_emitter")

    if "noteInteractionClickSuccessTracksStateAndEmitsTelemetryAndSettleEvent" not in telemetry_service_test_text:
        errors.append("interaction_click_telemetry_service_test_missing_click_success_case")
    if "dropSweepTelemetryUsesThrottleUnlessPixelRepeats" not in telemetry_service_test_text:
        errors.append("interaction_click_telemetry_service_test_missing_drop_sweep_throttle_case")
    if "rememberInteractionAnchorMaintainsUsableAnchorState" not in telemetry_service_test_text:
        errors.append("interaction_click_telemetry_service_test_missing_anchor_case")

    if errors:
        print("[phase20-interaction-click-telemetry] FAILED")
        for error in errors:
            print(f"[phase20-interaction-click-telemetry] ERROR {error}")
        return 1

    print(
        "[phase20-interaction-click-telemetry] OK: interaction click telemetry Phase 20 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
