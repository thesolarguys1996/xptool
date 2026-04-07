from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE45_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE45_INTERACTION_SESSION_SHUTDOWN_HOST_DECOMPOSITION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_HOST_FACTORY_SHUTDOWN_SERVICE_HOST_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownServiceHostDecompositionTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE45_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_HOST_FACTORY_SHUTDOWN_SERVICE_HOST_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase45-interaction-session-shutdown-host-decomposition] FAILED")
        for error in errors:
            print(f"[phase45-interaction-session-shutdown-host-decomposition] ERROR {error}")
        return 1

    phase45_plan_text = _read(PHASE45_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    shutdown_service_host_test_text = _read(
        INTERACTION_SESSION_HOST_FACTORY_SHUTDOWN_SERVICE_HOST_TEST
    )

    if "## Phase 45 Slice Status" not in phase45_plan_text:
        errors.append("phase45_plan_missing_slice_status")
    if "`45.1` complete." not in phase45_plan_text:
        errors.append("phase45_plan_missing_45_1_complete")
    if "`45.2` complete." not in phase45_plan_text:
        errors.append("phase45_plan_missing_45_2_complete")
    if "`45.3` complete." not in phase45_plan_text:
        errors.append("phase45_plan_missing_45_3_complete")

    if "## Phase 45 (Interaction Session Shutdown Host Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase45_section")

    if "PHASE 45 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase45_started")
    if "PHASE 45 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase45_complete")

    required_tasks = [
        "- [x] Define Phase 45 interaction session shutdown host decomposition scope and completion evidence gates.",
        "- [x] Extract interaction-session shutdown service host-based construction boundary in `InteractionSessionHostFactory`.",
        "- [x] Run Phase 45 verification + guard pack and mark `PHASE 45 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase45_line:{task_line}")

    if "static InteractionSessionShutdownService createShutdownServiceFromHost(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_shutdown_service_from_host_method")
    if "return createShutdownServiceFromHost(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_shutdown_service_from_host_delegation")

    if (
        "createShutdownServiceFromHostRoutesShutdownLifecycle"
        not in shutdown_service_host_test_text
    ):
        errors.append("interaction_session_host_factory_shutdown_service_host_test_missing_delegate_case")

    if errors:
        print("[phase45-interaction-session-shutdown-host-decomposition] FAILED")
        for error in errors:
            print(f"[phase45-interaction-session-shutdown-host-decomposition] ERROR {error}")
        return 1

    print(
        "[phase45-interaction-session-shutdown-host-decomposition] OK: interaction session shutdown host decomposition Phase 45 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
