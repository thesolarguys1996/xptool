from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE54_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE54_INTERACTION_SESSION_HOST_FACTORY_CONSOLIDATION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE54_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase54-interaction-session-host-factory-consolidation] FAILED")
        for error in errors:
            print(f"[phase54-interaction-session-host-factory-consolidation] ERROR {error}")
        return 1

    phase54_plan_text = _read(PHASE54_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)

    if "## Phase 54 Slice Status" not in phase54_plan_text:
        errors.append("phase54_plan_missing_slice_status")
    if "`54.1` complete." not in phase54_plan_text:
        errors.append("phase54_plan_missing_54_1_complete")
    if "`54.2` complete." not in phase54_plan_text:
        errors.append("phase54_plan_missing_54_2_complete")
    if "`54.3` complete." not in phase54_plan_text:
        errors.append("phase54_plan_missing_54_3_complete")

    if "## Phase 54 (Interaction Session Host-Factory Focused-Factory Consolidation)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase54_section")

    if "PHASE 54 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase54_started")
    if "PHASE 54 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase54_complete")

    required_tasks = [
        "- [x] Define Phase 54 interaction session host-factory focused-factory consolidation scope and completion evidence gates.",
        "- [x] Consolidate `InteractionSessionHostFactory` delegation boundaries across focused factory components while preserving compatibility signatures.",
        "- [x] Run Phase 54 verification + guard pack and mark `PHASE 54 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase54_line:{task_line}")

    required_delegate_strings = [
        "InteractionPostClickSettleFactory.createPostClickSettleServiceFromHost(",
        "InteractionPostClickSettleFactory.createPostClickSettleHost(",
        "InteractionPostClickSettleFactory.createPostClickSettleHostFromDelegates(",
        "InteractionSessionCommandRouterHostFactory.createCommandRouterHostFromDelegates(",
        "InteractionSessionClickEventFactory.createClickEventServiceFromHost(",
        "InteractionSessionRegistrationFactory.createRegistrationHost(",
        "InteractionSessionRegistrationFactory.createRegistrationHostFromDelegates(",
        "InteractionSessionMotorOwnershipFactory.createMotorOwnershipHost(",
        "InteractionSessionOwnershipFactory.createOwnershipHost(",
        "InteractionSessionOwnershipFactory.createOwnershipHostFromDelegates(",
        "InteractionSessionShutdownFactory.createShutdownServiceFromHost(",
        "InteractionSessionShutdownFactory.createShutdownHost(",
    ]
    for delegate_string in required_delegate_strings:
        if delegate_string not in host_factory_text:
            errors.append(f"interaction_session_host_factory_missing_delegate:{delegate_string}")

    compatibility_strings = [
        "onInteractionClickEvent.accept(clickEvent);",
        "releaseInteractionMotorOwnership.run();",
        "return new InteractionSessionOwnershipService(host);",
    ]
    for compatibility_string in compatibility_strings:
        if compatibility_string not in host_factory_text:
            errors.append(f"interaction_session_host_factory_missing_compatibility_string:{compatibility_string}")

    if errors:
        print("[phase54-interaction-session-host-factory-consolidation] FAILED")
        for error in errors:
            print(f"[phase54-interaction-session-host-factory-consolidation] ERROR {error}")
        return 1

    print(
        "[phase54-interaction-session-host-factory-consolidation] OK: interaction session host-factory focused-factory consolidation Phase 54 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
