from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE188_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE188_INTERACTION_SESSION_FACTORY_SERVICE_INPUT_TYPED_ROUTING_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
FACTORY = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java"
ENTRY_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryEntrySessionFactory.java"
)
SERVICE_INPUTS_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryServiceInputsSessionFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE188_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        FACTORY,
        ENTRY_SESSION_FACTORY,
        SERVICE_INPUTS_SESSION_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase188-interaction-session-factory-service-input-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase188-interaction-session-factory-service-input-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase188_plan_text = _read(PHASE188_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    factory_text = _read(FACTORY)
    entry_session_factory_text = _read(ENTRY_SESSION_FACTORY)
    service_inputs_session_factory_text = _read(SERVICE_INPUTS_SESSION_FACTORY)

    if "## Phase 188 Slice Status" not in phase188_plan_text:
        errors.append("phase188_plan_missing_slice_status")
    if "`188.1` complete." not in phase188_plan_text:
        errors.append("phase188_plan_missing_188_1_complete")
    if "`188.2` complete." not in phase188_plan_text:
        errors.append("phase188_plan_missing_188_2_complete")
    if "`188.3` complete." not in phase188_plan_text:
        errors.append("phase188_plan_missing_188_3_complete")

    if (
        "## Phase 188 (Interaction Session Factory Service Input Typed Routing Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase188_section")

    if "PHASE 188 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase188_started")
    if "PHASE 188 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase188_complete")

    required_tasks = [
        "- [x] Define Phase 188 interaction-session factory service-input typed routing extraction scope and completion evidence gates.",
        "- [x] Consolidate `InteractionSessionFactory.create(...)` through focused service-input session routing ownership.",
        "- [x] Run Phase 188 verification + guard pack and mark `PHASE 188 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase188_line:{task_line}")

    required_factory_strings = [
        "public static InteractionSession create(",
        "InteractionSessionFactoryEntrySessionFactory.create(",
        "return createFromFactoryInputs(",
        "InteractionSessionFactoryInputs.fromServices(",
    ]
    for required_string in required_factory_strings:
        if required_string not in factory_text:
            errors.append(f"factory_missing_string:{required_string}")

    if "InteractionSessionFactoryServiceInputsSessionFactory.createFromServices(" not in entry_session_factory_text:
        errors.append("entry_session_factory_missing_service_inputs_delegate")

    required_service_factory_strings = [
        "InteractionSessionFactoryInputs.fromServices(",
        "InteractionSessionFactory.createFromFactoryInputs(",
    ]
    for required_string in required_service_factory_strings:
        if required_string not in service_inputs_session_factory_text:
            errors.append(f"service_inputs_session_factory_missing_string:{required_string}")

    if errors:
        print("[phase188-interaction-session-factory-service-input-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase188-interaction-session-factory-service-input-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase188-interaction-session-factory-service-input-typed-routing-extraction] OK: "
        "interaction session factory service-input typed routing extraction Phase 188 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
