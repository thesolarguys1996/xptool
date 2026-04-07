from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE120_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE120_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_TYPED_ENTRY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE120_PLAN, MIGRATION_PLAN, PHASE_STATUS, TASKS, INTERACTION_SESSION_FACTORY]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase120-interaction-session-factory-runtime-bundle-typed-entry-extraction] FAILED")
        for error in errors:
            print(f"[phase120-interaction-session-factory-runtime-bundle-typed-entry-extraction] ERROR {error}")
        return 1

    phase120_plan_text = _read(PHASE120_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_factory_text = _read(INTERACTION_SESSION_FACTORY)

    if "## Phase 120 Slice Status" not in phase120_plan_text:
        errors.append("phase120_plan_missing_slice_status")
    if "`120.1` complete." not in phase120_plan_text:
        errors.append("phase120_plan_missing_120_1_complete")
    if "`120.2` complete." not in phase120_plan_text:
        errors.append("phase120_plan_missing_120_2_complete")
    if "`120.3` complete." not in phase120_plan_text:
        errors.append("phase120_plan_missing_120_3_complete")

    if "## Phase 120 (Interaction Session Factory Runtime Bundle Typed Entry Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase120_section")

    if "PHASE 120 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase120_started")
    if "PHASE 120 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase120_complete")

    required_tasks = [
        "- [x] Define Phase 120 interaction-session factory runtime-bundle typed-entry extraction scope and completion evidence gates.",
        "- [x] Extract typed-entry runtime-bundle creation seam in `InteractionSessionFactory` through `InteractionSessionFactoryRuntimeBundleFactory` ownership.",
        "- [x] Run Phase 120 verification + guard pack and mark `PHASE 120 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase120_line:{task_line}")

    required_factory_strings = [
        "static InteractionSession createFromAssemblyFactoryInputs(",
        "return createFromRuntimeBundle(",
        "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromAssemblyFactoryInputs(",
        "InteractionSessionAssemblyFactory.createRuntimeBundleForSession(",
    ]
    for required_string in required_factory_strings:
        if required_string not in interaction_session_factory_text:
            errors.append(f"interaction_session_factory_missing_string:{required_string}")

    if errors:
        print("[phase120-interaction-session-factory-runtime-bundle-typed-entry-extraction] FAILED")
        for error in errors:
            print(f"[phase120-interaction-session-factory-runtime-bundle-typed-entry-extraction] ERROR {error}")
        return 1

    print(
        "[phase120-interaction-session-factory-runtime-bundle-typed-entry-extraction] OK: interaction session "
        "factory runtime-bundle typed-entry extraction Phase 120 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
