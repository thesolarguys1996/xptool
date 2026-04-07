from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE78_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE78_INTERACTION_SESSION_HOST_FACTORY_CONSOLIDATION_G_PLAN.md"
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
        PHASE78_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase78-interaction-session-host-factory-consolidation-g] FAILED")
        for error in errors:
            print(f"[phase78-interaction-session-host-factory-consolidation-g] ERROR {error}")
        return 1

    phase78_plan_text = _read(PHASE78_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)

    if "## Phase 78 Slice Status" not in phase78_plan_text:
        errors.append("phase78_plan_missing_slice_status")
    if "`78.1` complete." not in phase78_plan_text:
        errors.append("phase78_plan_missing_78_1_complete")
    if "`78.2` complete." not in phase78_plan_text:
        errors.append("phase78_plan_missing_78_2_complete")
    if "`78.3` complete." not in phase78_plan_text:
        errors.append("phase78_plan_missing_78_3_complete")

    if "## Phase 78 (Interaction Session Host-Factory Consolidation G)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase78_section")

    if "PHASE 78 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase78_started")
    if "PHASE 78 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase78_complete")

    required_tasks = [
        "- [x] Define Phase 78 interaction session host-factory consolidation G scope and completion evidence gates.",
        "- [x] Consolidate host-factory composite service delegation seams for registration and motor focused factories while preserving compatibility sentinel strings.",
        "- [x] Run Phase 78 verification + guard pack and mark `PHASE 78 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase78_line:{task_line}")

    required_delegate_strings = [
        "InteractionSessionRegistrationFactory.createRegistrationService(sessionManager, sessionInteractionKey);",
        "InteractionSessionMotorOwnershipFactory.createMotorOwnershipService(executor);",
    ]
    for delegate_string in required_delegate_strings:
        if delegate_string not in host_factory_text:
            errors.append(f"interaction_session_host_factory_missing_delegate:{delegate_string}")

    compatibility_strings = [
        "return createRegistrationServiceFromHost(",
        "createRegistrationHost(sessionManager)",
        "return createMotorOwnershipServiceFromHost(",
        "createMotorOwnershipHost(executor)",
        "return new InteractionSessionRegistrationService(host, sessionInteractionKey);",
        "return new InteractionSessionMotorOwnershipService(host);",
        "onInteractionClickEvent.accept(clickEvent);",
        "clearPendingPostClickSettle.run();",
        "clearRegistration.run();",
        "releaseInteractionMotorOwnership.run();",
    ]
    for compatibility_string in compatibility_strings:
        if compatibility_string not in host_factory_text:
            errors.append(f"interaction_session_host_factory_missing_compatibility_string:{compatibility_string}")

    if errors:
        print("[phase78-interaction-session-host-factory-consolidation-g] FAILED")
        for error in errors:
            print(f"[phase78-interaction-session-host-factory-consolidation-g] ERROR {error}")
        return 1

    print(
        "[phase78-interaction-session-host-factory-consolidation-g] OK: interaction session host-factory consolidation G Phase 78 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
