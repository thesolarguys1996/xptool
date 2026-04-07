from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE175_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE175_INTERACTION_SESSION_FACTORY_DEFAULT_ENTRY_WIRING_CONSOLIDATION_AD_PLAN.md"
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

    required_paths = [PHASE175_PLAN, MIGRATION_PLAN, PHASE_STATUS, TASKS, DEFAULT_ENTRY_FACTORY]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase175-interaction-session-factory-default-entry-wiring-consolidation-ad] FAILED")
        for error in errors:
            print(f"[phase175-interaction-session-factory-default-entry-wiring-consolidation-ad] ERROR {error}")
        return 1

    phase175_plan_text = _read(PHASE175_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    default_entry_factory_text = _read(DEFAULT_ENTRY_FACTORY)

    if "## Phase 175 Slice Status" not in phase175_plan_text:
        errors.append("phase175_plan_missing_slice_status")
    if "`175.1` complete." not in phase175_plan_text:
        errors.append("phase175_plan_missing_175_1_complete")
    if "`175.2` complete." not in phase175_plan_text:
        errors.append("phase175_plan_missing_175_2_complete")
    if "`175.3` complete." not in phase175_plan_text:
        errors.append("phase175_plan_missing_175_3_complete")

    if "## Phase 175 (Interaction Session Factory Default Entry Wiring Consolidation AD)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase175_section")

    if "PHASE 175 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase175_started")
    if "PHASE 175 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase175_complete")

    required_tasks = [
        "- [x] Define Phase 175 interaction-session factory default-entry wiring consolidation AD scope and completion evidence gates.",
        "- [x] Consolidate `InteractionSessionFactoryDefaultEntryFactory.createFromFactoryInputs(...)` through focused default-entry runtime-bundle-factory-input routing ownership.",
        "- [x] Run Phase 175 verification + guard pack and mark `PHASE 175 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase175_line:{task_line}")

    required_factory_strings = [
        "static InteractionSession createFromFactoryInputs(",
        "InteractionSessionFactoryDefaultEntryRuntimeBundleFactoryInputsFactory.createDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleKeyPolicy.defaultSessionInteractionKey()",
        "return createFromDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryDefaultEntryRuntimeSessionFactory.createFromDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryDefaultRuntimeSessionFactory.createFromDefaultRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in default_entry_factory_text:
            errors.append(f"default_entry_factory_missing_string:{required_string}")

    if errors:
        print("[phase175-interaction-session-factory-default-entry-wiring-consolidation-ad] FAILED")
        for error in errors:
            print(f"[phase175-interaction-session-factory-default-entry-wiring-consolidation-ad] ERROR {error}")
        return 1

    print(
        "[phase175-interaction-session-factory-default-entry-wiring-consolidation-ad] OK: "
        "interaction session factory default-entry wiring consolidation AD Phase 175 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
