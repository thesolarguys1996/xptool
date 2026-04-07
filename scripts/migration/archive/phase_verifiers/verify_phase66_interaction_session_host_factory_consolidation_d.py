from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE66_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE66_INTERACTION_SESSION_HOST_FACTORY_CONSOLIDATION_D_PLAN.md"
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
        PHASE66_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase66-interaction-session-host-factory-consolidation-d] FAILED")
        for error in errors:
            print(f"[phase66-interaction-session-host-factory-consolidation-d] ERROR {error}")
        return 1

    phase66_plan_text = _read(PHASE66_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)

    if "## Phase 66 Slice Status" not in phase66_plan_text:
        errors.append("phase66_plan_missing_slice_status")
    if "`66.1` complete." not in phase66_plan_text:
        errors.append("phase66_plan_missing_66_1_complete")
    if "`66.2` complete." not in phase66_plan_text:
        errors.append("phase66_plan_missing_66_2_complete")
    if "`66.3` complete." not in phase66_plan_text:
        errors.append("phase66_plan_missing_66_3_complete")

    if "## Phase 66 (Interaction Session Host-Factory Consolidation D)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase66_section")

    if "PHASE 66 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase66_started")
    if "PHASE 66 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase66_complete")

    required_tasks = [
        "- [x] Define Phase 66 interaction session host-factory consolidation D scope and completion evidence gates.",
        "- [x] Consolidate host-factory service-from-host delegation seams for registration, motor-ownership, and ownership focused factories while preserving compatibility sentinel strings.",
        "- [x] Run Phase 66 verification + guard pack and mark `PHASE 66 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase66_line:{task_line}")

    required_delegate_strings = [
        "InteractionSessionRegistrationFactory.createRegistrationServiceFromHost(host, sessionInteractionKey);",
        "InteractionSessionMotorOwnershipFactory.createMotorOwnershipServiceFromHost(host);",
        "InteractionSessionOwnershipFactory.createOwnershipServiceFromHost(host);",
    ]
    for delegate_string in required_delegate_strings:
        if delegate_string not in host_factory_text:
            errors.append(f"interaction_session_host_factory_missing_delegate:{delegate_string}")

    compatibility_strings = [
        "return new InteractionSessionRegistrationService(host, sessionInteractionKey);",
        "return new InteractionSessionMotorOwnershipService(host);",
        "return new InteractionSessionOwnershipService(host);",
        "onInteractionClickEvent.accept(clickEvent);",
        "clearPendingPostClickSettle.run();",
        "clearRegistration.run();",
        "releaseInteractionMotorOwnership.run();",
    ]
    for compatibility_string in compatibility_strings:
        if compatibility_string not in host_factory_text:
            errors.append(f"interaction_session_host_factory_missing_compatibility_string:{compatibility_string}")

    if errors:
        print("[phase66-interaction-session-host-factory-consolidation-d] FAILED")
        for error in errors:
            print(f"[phase66-interaction-session-host-factory-consolidation-d] ERROR {error}")
        return 1

    print(
        "[phase66-interaction-session-host-factory-consolidation-d] OK: interaction session host-factory consolidation D Phase 66 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
