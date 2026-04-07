from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE93_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE93_INTERACTION_SESSION_CONSTRUCTOR_RUNTIME_BUNDLE_EXTRACTION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE93_PLAN, MIGRATION_PLAN, PHASE_STATUS, TASKS, INTERACTION_SESSION]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase93-interaction-session-constructor-runtime-bundle-extraction] FAILED")
        for error in errors:
            print(f"[phase93-interaction-session-constructor-runtime-bundle-extraction] ERROR {error}")
        return 1

    phase93_plan_text = _read(PHASE93_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_text = _read(INTERACTION_SESSION)

    if "## Phase 93 Slice Status" not in phase93_plan_text:
        errors.append("phase93_plan_missing_slice_status")
    if "`93.1` complete." not in phase93_plan_text:
        errors.append("phase93_plan_missing_93_1_complete")
    if "`93.2` complete." not in phase93_plan_text:
        errors.append("phase93_plan_missing_93_2_complete")
    if "`93.3` complete." not in phase93_plan_text:
        errors.append("phase93_plan_missing_93_3_complete")

    if "## Phase 93 (Interaction Session Constructor Runtime Bundle Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase93_section")

    if "PHASE 93 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase93_started")
    if "PHASE 93 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase93_complete")

    required_tasks = [
        "- [x] Define Phase 93 interaction session constructor runtime-bundle extraction scope and completion evidence gates.",
        "- [x] Extract interaction-session constructor runtime-bundle injection seam and remove unused constructor-owned runtime-service fields.",
        "- [x] Run Phase 93 verification + guard pack and mark `PHASE 93 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase93_line:{task_line}")

    required_session_strings = [
        "this(InteractionSessionAssemblyFactory.createRuntimeBundle(executor, sessionManager, commandFacade));",
        "InteractionSession(InteractionSessionRuntimeBundle runtimeBundle)",
    ]
    for required_string in required_session_strings:
        if required_string not in interaction_session_text:
            errors.append(f"interaction_session_missing_string:{required_string}")

    removed_fields = [
        "private final InteractionSessionRegistrationService interactionSessionRegistrationService;",
        "private final InteractionSessionMotorOwnershipService interactionSessionMotorOwnershipService;",
        "private final InteractionPostClickSettleService interactionPostClickSettleService;",
    ]
    for removed_field in removed_fields:
        if removed_field in interaction_session_text:
            errors.append(f"interaction_session_still_contains_removed_field:{removed_field}")

    if errors:
        print("[phase93-interaction-session-constructor-runtime-bundle-extraction] FAILED")
        for error in errors:
            print(f"[phase93-interaction-session-constructor-runtime-bundle-extraction] ERROR {error}")
        return 1

    print("[phase93-interaction-session-constructor-runtime-bundle-extraction] OK: interaction session constructor runtime bundle extraction Phase 93 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
