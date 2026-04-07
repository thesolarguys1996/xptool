from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE97_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE97_INTERACTION_SESSION_RUNTIME_OPERATIONS_EXTRACTION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java"
INTERACTION_SESSION_RUNTIME_OPERATIONS = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeOperations.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE97_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION,
        INTERACTION_SESSION_RUNTIME_OPERATIONS,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase97-interaction-session-runtime-operations-extraction] FAILED")
        for error in errors:
            print(f"[phase97-interaction-session-runtime-operations-extraction] ERROR {error}")
        return 1

    phase97_plan_text = _read(PHASE97_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_text = _read(INTERACTION_SESSION)
    interaction_session_runtime_operations_text = _read(INTERACTION_SESSION_RUNTIME_OPERATIONS)

    if "## Phase 97 Slice Status" not in phase97_plan_text:
        errors.append("phase97_plan_missing_slice_status")
    if "`97.1` complete." not in phase97_plan_text:
        errors.append("phase97_plan_missing_97_1_complete")
    if "`97.2` complete." not in phase97_plan_text:
        errors.append("phase97_plan_missing_97_2_complete")
    if "`97.3` complete." not in phase97_plan_text:
        errors.append("phase97_plan_missing_97_3_complete")

    if "## Phase 97 (Interaction Session Runtime Operations Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase97_section")

    if "PHASE 97 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase97_started")
    if "PHASE 97 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase97_complete")

    required_tasks = [
        "- [x] Define Phase 97 interaction-session runtime-operations extraction scope and completion evidence gates.",
        "- [x] Extract interaction-session command/click/tick/shutdown delegation into focused `InteractionSessionRuntimeOperations` ownership.",
        "- [x] Run Phase 97 verification + guard pack and mark `PHASE 97 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase97_line:{task_line}")

    required_session_strings = [
        "private final InteractionSessionRuntimeOperations interactionSessionRuntimeOperations;",
        "InteractionSession(InteractionSessionRuntimeBundle runtimeBundle)",
        "InteractionSession(InteractionSessionRuntimeOperations runtimeOperations)",
        "this(InteractionSessionAssemblyFactory.createRuntimeBundle(executor, sessionManager, commandFacade));",
    ]
    for required_string in required_session_strings:
        if required_string not in interaction_session_text:
            errors.append(f"interaction_session_missing_string:{required_string}")

    removed_fields = [
        "private final InteractionSessionCommandRouter interactionSessionCommandRouter;",
        "private final InteractionSessionClickEventService interactionSessionClickEventService;",
        "private final InteractionSessionOwnershipService interactionSessionOwnershipService;",
        "private final InteractionSessionShutdownService interactionSessionShutdownService;",
    ]
    for removed_field in removed_fields:
        if removed_field in interaction_session_text:
            errors.append(f"interaction_session_still_contains_removed_field:{removed_field}")

    required_runtime_operations_strings = [
        "final class InteractionSessionRuntimeOperations",
        "boolean supports(String commandType)",
        "CommandExecutor.CommandDecision execute(String commandType, JsonObject payload, MotionProfile motionProfile)",
        "void onInteractionClickEvent(InteractionClickEvent clickEvent)",
        "void onGameTick()",
        "void shutdown()",
    ]
    for required_string in required_runtime_operations_strings:
        if required_string not in interaction_session_runtime_operations_text:
            errors.append(f"runtime_operations_missing_string:{required_string}")

    if errors:
        print("[phase97-interaction-session-runtime-operations-extraction] FAILED")
        for error in errors:
            print(f"[phase97-interaction-session-runtime-operations-extraction] ERROR {error}")
        return 1

    print(
        "[phase97-interaction-session-runtime-operations-extraction] OK: interaction session runtime operations extraction "
        "Phase 97 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
