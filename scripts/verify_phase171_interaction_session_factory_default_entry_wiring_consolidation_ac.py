from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE171_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE171_INTERACTION_SESSION_FACTORY_DEFAULT_ENTRY_WIRING_CONSOLIDATION_AC_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
DEFAULT_ENTRY_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultEntryFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE171_PLAN, MIGRATION_PLAN, PHASE_STATUS, TASKS, DEFAULT_ENTRY_FACTORY]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase171-interaction-session-factory-default-entry-wiring-consolidation-ac] FAILED")
        for error in errors:
            print(f"[phase171-interaction-session-factory-default-entry-wiring-consolidation-ac] ERROR {error}")
        return 1

    phase171_plan_text = _read(PHASE171_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    default_entry_factory_text = _read(DEFAULT_ENTRY_FACTORY)

    if "## Phase 171 Slice Status" not in phase171_plan_text:
        errors.append("phase171_plan_missing_slice_status")
    if "`171.1` complete." not in phase171_plan_text:
        errors.append("phase171_plan_missing_171_1_complete")
    if "`171.2` complete." not in phase171_plan_text:
        errors.append("phase171_plan_missing_171_2_complete")
    if "`171.3` complete." not in phase171_plan_text:
        errors.append("phase171_plan_missing_171_3_complete")

    if "## Phase 171 (Interaction Session Factory Default Entry Wiring Consolidation AC)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase171_section")

    if "PHASE 171 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase171_started")
    if "PHASE 171 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase171_complete")

    required_tasks = [
        "- [x] Define Phase 171 interaction-session factory default-entry wiring consolidation AC scope and completion evidence gates.",
        "- [x] Consolidate `InteractionSessionFactoryDefaultEntryFactory` routing through focused default-entry runtime-session factory ownership.",
        "- [x] Run Phase 171 verification + guard pack and mark `PHASE 171 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase171_line:{task_line}")

    required_factory_strings = [
        "static InteractionSession createFromFactoryInputs(",
        "return createFromFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleKeyPolicy.defaultSessionInteractionKey()",
        "InteractionSessionFactoryDefaultEntryRuntimeSessionFactory.createFromDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryDefaultRuntimeSessionFactory.createFromDefaultRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in default_entry_factory_text:
            errors.append(f"default_entry_factory_missing_string:{required_string}")

    if errors:
        print("[phase171-interaction-session-factory-default-entry-wiring-consolidation-ac] FAILED")
        for error in errors:
            print(f"[phase171-interaction-session-factory-default-entry-wiring-consolidation-ac] ERROR {error}")
        return 1

    print(
        "[phase171-interaction-session-factory-default-entry-wiring-consolidation-ac] OK: "
        "interaction session factory default-entry wiring consolidation AC Phase 171 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
