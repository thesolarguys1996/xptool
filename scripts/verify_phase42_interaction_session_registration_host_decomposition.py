from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE42_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE42_INTERACTION_SESSION_REGISTRATION_HOST_DECOMPOSITION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_HOST_FACTORY_REGISTRATION_SERVICE_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationServiceFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE42_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_HOST_FACTORY_REGISTRATION_SERVICE_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase42-interaction-session-registration-host-decomposition] FAILED")
        for error in errors:
            print(f"[phase42-interaction-session-registration-host-decomposition] ERROR {error}")
        return 1

    phase42_plan_text = _read(PHASE42_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    registration_service_factory_test_text = _read(
        INTERACTION_SESSION_HOST_FACTORY_REGISTRATION_SERVICE_FACTORY_TEST
    )

    if "## Phase 42 Slice Status" not in phase42_plan_text:
        errors.append("phase42_plan_missing_slice_status")
    if "`42.1` complete." not in phase42_plan_text:
        errors.append("phase42_plan_missing_42_1_complete")
    if "`42.2` complete." not in phase42_plan_text:
        errors.append("phase42_plan_missing_42_2_complete")
    if "`42.3` complete." not in phase42_plan_text:
        errors.append("phase42_plan_missing_42_3_complete")

    if "## Phase 42 (Interaction Session Registration Host Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase42_section")

    if "PHASE 42 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase42_started")
    if "PHASE 42 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase42_complete")

    required_tasks = [
        "- [x] Define Phase 42 interaction session registration host decomposition scope and completion evidence gates.",
        "- [x] Extract interaction-session registration service host-based construction boundary in `InteractionSessionHostFactory`.",
        "- [x] Run Phase 42 verification + guard pack and mark `PHASE 42 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase42_line:{task_line}")

    if "static InteractionSessionRegistrationService createRegistrationServiceFromHost(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_registration_service_from_host_method")
    if "return createRegistrationServiceFromHost(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_registration_service_from_host_delegation")

    if (
        "createRegistrationServiceFromHostEnsuresAndClearsRegistrationLifecycle"
        not in registration_service_factory_test_text
    ):
        errors.append("interaction_session_host_factory_registration_service_test_missing_delegate_case")

    if errors:
        print("[phase42-interaction-session-registration-host-decomposition] FAILED")
        for error in errors:
            print(f"[phase42-interaction-session-registration-host-decomposition] ERROR {error}")
        return 1

    print(
        "[phase42-interaction-session-registration-host-decomposition] OK: interaction session registration host decomposition Phase 42 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
