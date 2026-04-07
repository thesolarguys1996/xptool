from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE74_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE74_INTERACTION_SESSION_HOST_FACTORY_CONSOLIDATION_F_PLAN.md"
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
        PHASE74_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase74-interaction-session-host-factory-consolidation-f] FAILED")
        for error in errors:
            print(f"[phase74-interaction-session-host-factory-consolidation-f] ERROR {error}")
        return 1

    phase74_plan_text = _read(PHASE74_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)

    if "## Phase 74 Slice Status" not in phase74_plan_text:
        errors.append("phase74_plan_missing_slice_status")
    if "`74.1` complete." not in phase74_plan_text:
        errors.append("phase74_plan_missing_74_1_complete")
    if "`74.2` complete." not in phase74_plan_text:
        errors.append("phase74_plan_missing_74_2_complete")
    if "`74.3` complete." not in phase74_plan_text:
        errors.append("phase74_plan_missing_74_3_complete")

    if "## Phase 74 (Interaction Session Host-Factory Consolidation F)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase74_section")

    if "PHASE 74 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase74_started")
    if "PHASE 74 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase74_complete")

    required_tasks = [
        "- [x] Define Phase 74 interaction session host-factory consolidation F scope and completion evidence gates.",
        "- [x] Consolidate host-factory service delegation seams for click-event and shutdown focused factories while preserving compatibility sentinel strings.",
        "- [x] Run Phase 74 verification + guard pack and mark `PHASE 74 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase74_line:{task_line}")

    required_delegate_strings = [
        "InteractionSessionClickEventFactory.createClickEventService(onInteractionClickEvent);",
        "InteractionSessionShutdownFactory.createShutdownService(",
    ]
    for delegate_string in required_delegate_strings:
        if delegate_string not in host_factory_text:
            errors.append(f"interaction_session_host_factory_missing_delegate:{delegate_string}")

    compatibility_strings = [
        "InteractionSessionClickEventFactory.createClickEventServiceFromHost(host);",
        "InteractionSessionClickEventFactory.createClickEventHost(onInteractionClickEvent);",
        "InteractionSessionShutdownFactory.createShutdownServiceFromHost(host);",
        "InteractionSessionShutdownFactory.createShutdownHost(",
        "onInteractionClickEvent.accept(clickEvent);",
        "clearPendingPostClickSettle.run();",
        "clearRegistration.run();",
        "releaseInteractionMotorOwnership.run();",
    ]
    for compatibility_string in compatibility_strings:
        if compatibility_string not in host_factory_text:
            errors.append(f"interaction_session_host_factory_missing_compatibility_string:{compatibility_string}")

    if errors:
        print("[phase74-interaction-session-host-factory-consolidation-f] FAILED")
        for error in errors:
            print(f"[phase74-interaction-session-host-factory-consolidation-f] ERROR {error}")
        return 1

    print(
        "[phase74-interaction-session-host-factory-consolidation-f] OK: interaction session host-factory consolidation F Phase 74 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
