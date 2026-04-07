from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE198_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE198_INTERACTION_SESSION_FACTORY_ENTRY_SERVICE_INPUT_TYPED_ROUTING_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
ENTRY_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryEntrySessionFactory.java"
)
ENTRY_SERVICE_INPUTS_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryEntryServiceInputsSessionFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE198_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        ENTRY_SESSION_FACTORY,
        ENTRY_SERVICE_INPUTS_SESSION_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase198-interaction-session-factory-entry-service-input-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase198-interaction-session-factory-entry-service-input-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase198_plan_text = _read(PHASE198_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    entry_session_factory_text = _read(ENTRY_SESSION_FACTORY)
    entry_service_inputs_session_factory_text = _read(ENTRY_SERVICE_INPUTS_SESSION_FACTORY)

    if "## Phase 198 Slice Status" not in phase198_plan_text:
        errors.append("phase198_plan_missing_slice_status")
    if "`198.1` complete." not in phase198_plan_text:
        errors.append("phase198_plan_missing_198_1_complete")
    if "`198.2` complete." not in phase198_plan_text:
        errors.append("phase198_plan_missing_198_2_complete")
    if "`198.3` complete." not in phase198_plan_text:
        errors.append("phase198_plan_missing_198_3_complete")

    if (
        "## Phase 198 (Interaction Session Factory Entry Service Input Typed Routing Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase198_section")

    if "PHASE 198 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase198_started")
    if "PHASE 198 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase198_complete")

    required_tasks = [
        "- [x] Define Phase 198 interaction-session factory entry service-input typed routing extraction scope and completion evidence gates.",
        "- [x] Consolidate `InteractionSessionFactoryEntrySessionFactory.create(...)` through focused entry service-input session routing ownership.",
        "- [x] Run Phase 198 verification + guard pack and mark `PHASE 198 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase198_line:{task_line}")

    required_entry_session_strings = [
        "static InteractionSession create(",
        "InteractionSessionFactoryEntryServiceInputsSessionFactory.create(",
        "InteractionSessionFactoryServiceInputsSessionFactory.createFromServices(",
    ]
    for required_string in required_entry_session_strings:
        if required_string not in entry_session_factory_text:
            errors.append(f"entry_session_factory_missing_string:{required_string}")

    if "InteractionSessionFactoryServiceInputsSessionFactory.createFromServices(" not in entry_service_inputs_session_factory_text:
        errors.append("entry_service_inputs_session_factory_missing_service_inputs_delegate")

    if errors:
        print("[phase198-interaction-session-factory-entry-service-input-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase198-interaction-session-factory-entry-service-input-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase198-interaction-session-factory-entry-service-input-typed-routing-extraction] OK: "
        "interaction session factory entry service-input typed routing extraction Phase 198 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
