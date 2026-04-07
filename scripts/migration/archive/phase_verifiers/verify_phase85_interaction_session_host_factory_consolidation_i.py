from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE85_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE85_INTERACTION_SESSION_HOST_FACTORY_CONSOLIDATION_I_PLAN.md"
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
        PHASE85_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase85-interaction-session-host-factory-consolidation-i] FAILED")
        for error in errors:
            print(f"[phase85-interaction-session-host-factory-consolidation-i] ERROR {error}")
        return 1

    phase85_plan_text = _read(PHASE85_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)

    if "## Phase 85 Slice Status" not in phase85_plan_text:
        errors.append("phase85_plan_missing_slice_status")
    if "`85.1` complete." not in phase85_plan_text:
        errors.append("phase85_plan_missing_85_1_complete")
    if "`85.2` complete." not in phase85_plan_text:
        errors.append("phase85_plan_missing_85_2_complete")
    if "`85.3` complete." not in phase85_plan_text:
        errors.append("phase85_plan_missing_85_3_complete")

    if "## Phase 85 (Interaction Session Host-Factory Consolidation I)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase85_section")

    if "PHASE 85 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase85_started")
    if "PHASE 85 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase85_complete")

    required_tasks = [
        "- [x] Define Phase 85 interaction session host-factory consolidation I scope and completion evidence gates.",
        "- [x] Consolidate host-factory ownership composite service delegation seam while preserving compatibility sentinel strings.",
        "- [x] Run Phase 85 verification + guard pack and mark `PHASE 85 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase85_line:{task_line}")

    required_delegate_strings = [
        "InteractionSessionOwnershipFactory.createOwnershipService(",
        "InteractionPostClickSettleFactory.createPostClickSettleService(",
        "InteractionSessionCommandRouterFactory.createCommandRouterService(commandFacade);",
    ]
    for delegate_string in required_delegate_strings:
        if delegate_string not in host_factory_text:
            errors.append(f"interaction_session_host_factory_missing_delegate:{delegate_string}")

    compatibility_strings = [
        "return createOwnershipServiceFromHost(",
        "createOwnershipHost(",
        "InteractionSessionOwnershipFactory.createOwnershipServiceFromHost(host);",
        "return createPostClickSettleServiceFromHost(",
        "return createCommandRouterServiceFromHost(createCommandRouterHost(commandFacade));",
        "onInteractionClickEvent.accept(clickEvent);",
        "clearPendingPostClickSettle.run();",
        "clearRegistration.run();",
        "releaseInteractionMotorOwnership.run();",
    ]
    for compatibility_string in compatibility_strings:
        if compatibility_string not in host_factory_text:
            errors.append(f"interaction_session_host_factory_missing_compatibility_string:{compatibility_string}")

    if errors:
        print("[phase85-interaction-session-host-factory-consolidation-i] FAILED")
        for error in errors:
            print(f"[phase85-interaction-session-host-factory-consolidation-i] ERROR {error}")
        return 1

    print(
        "[phase85-interaction-session-host-factory-consolidation-i] OK: interaction session host-factory consolidation I Phase 85 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
