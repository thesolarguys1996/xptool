from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE58_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE58_INTERACTION_SESSION_HOST_FACTORY_CONSOLIDATION_B_PLAN.md"
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
        PHASE58_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase58-interaction-session-host-factory-consolidation-b] FAILED")
        for error in errors:
            print(f"[phase58-interaction-session-host-factory-consolidation-b] ERROR {error}")
        return 1

    phase58_plan_text = _read(PHASE58_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)

    if "## Phase 58 Slice Status" not in phase58_plan_text:
        errors.append("phase58_plan_missing_slice_status")
    if "`58.1` complete." not in phase58_plan_text:
        errors.append("phase58_plan_missing_58_1_complete")
    if "`58.2` complete." not in phase58_plan_text:
        errors.append("phase58_plan_missing_58_2_complete")
    if "`58.3` complete." not in phase58_plan_text:
        errors.append("phase58_plan_missing_58_3_complete")

    if "## Phase 58 (Interaction Session Host-Factory Consolidation B)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase58_section")

    if "PHASE 58 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase58_started")
    if "PHASE 58 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase58_complete")

    required_tasks = [
        "- [x] Define Phase 58 interaction session host-factory consolidation B scope and completion evidence gates.",
        "- [x] Consolidate host-factory delegation seams for command-router service and click-event host focused factories while preserving legacy compatibility wrappers.",
        "- [x] Run Phase 58 verification + guard pack and mark `PHASE 58 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase58_line:{task_line}")

    required_delegate_strings = [
        "InteractionSessionCommandRouterFactory.createCommandRouterServiceFromHost(host);",
        "InteractionSessionCommandRouterHostFactory.createCommandRouterHostFromDelegates(",
        "InteractionSessionClickEventFactory.createClickEventServiceFromHost(host);",
        "InteractionSessionClickEventFactory.createClickEventHost(onInteractionClickEvent);",
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
        print("[phase58-interaction-session-host-factory-consolidation-b] FAILED")
        for error in errors:
            print(f"[phase58-interaction-session-host-factory-consolidation-b] ERROR {error}")
        return 1

    print(
        "[phase58-interaction-session-host-factory-consolidation-b] OK: interaction session host-factory consolidation B Phase 58 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
