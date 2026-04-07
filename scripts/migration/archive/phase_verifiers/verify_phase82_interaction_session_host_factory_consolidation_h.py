from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE82_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE82_INTERACTION_SESSION_HOST_FACTORY_CONSOLIDATION_H_PLAN.md"
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
        PHASE82_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase82-interaction-session-host-factory-consolidation-h] FAILED")
        for error in errors:
            print(f"[phase82-interaction-session-host-factory-consolidation-h] ERROR {error}")
        return 1

    phase82_plan_text = _read(PHASE82_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)

    if "## Phase 82 Slice Status" not in phase82_plan_text:
        errors.append("phase82_plan_missing_slice_status")
    if "`82.1` complete." not in phase82_plan_text:
        errors.append("phase82_plan_missing_82_1_complete")
    if "`82.2` complete." not in phase82_plan_text:
        errors.append("phase82_plan_missing_82_2_complete")
    if "`82.3` complete." not in phase82_plan_text:
        errors.append("phase82_plan_missing_82_3_complete")

    if "## Phase 82 (Interaction Session Host-Factory Consolidation H)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase82_section")

    if "PHASE 82 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase82_started")
    if "PHASE 82 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase82_complete")

    required_tasks = [
        "- [x] Define Phase 82 interaction session host-factory consolidation H scope and completion evidence gates.",
        "- [x] Consolidate host-factory composite service delegation seams for post-click-settle and command-router focused factories while preserving compatibility sentinel strings.",
        "- [x] Run Phase 82 verification + guard pack and mark `PHASE 82 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase82_line:{task_line}")

    required_delegate_strings = [
        "InteractionPostClickSettleFactory.createPostClickSettleService(",
        "InteractionSessionCommandRouterFactory.createCommandRouterService(commandFacade);",
    ]
    for delegate_string in required_delegate_strings:
        if delegate_string not in host_factory_text:
            errors.append(f"interaction_session_host_factory_missing_delegate:{delegate_string}")

    compatibility_strings = [
        "return createPostClickSettleServiceFromHost(",
        "createPostClickSettleHost(",
        "return createCommandRouterServiceFromHost(createCommandRouterHost(commandFacade));",
        "InteractionSessionCommandRouterFactory.createCommandRouterServiceFromHost(host);",
        "InteractionSessionCommandRouterHostFactory.createCommandRouterHostFromDelegates(",
        "onInteractionClickEvent.accept(clickEvent);",
        "clearPendingPostClickSettle.run();",
        "clearRegistration.run();",
        "releaseInteractionMotorOwnership.run();",
    ]
    for compatibility_string in compatibility_strings:
        if compatibility_string not in host_factory_text:
            errors.append(f"interaction_session_host_factory_missing_compatibility_string:{compatibility_string}")

    if errors:
        print("[phase82-interaction-session-host-factory-consolidation-h] FAILED")
        for error in errors:
            print(f"[phase82-interaction-session-host-factory-consolidation-h] ERROR {error}")
        return 1

    print(
        "[phase82-interaction-session-host-factory-consolidation-h] OK: interaction session host-factory consolidation H Phase 82 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
