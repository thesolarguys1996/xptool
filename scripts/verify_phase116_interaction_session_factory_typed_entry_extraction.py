from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE112_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE116_INTERACTION_SESSION_FACTORY_TYPED_ENTRY_EXTRACTION_PLAN.md"
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

    required_paths = [PHASE112_PLAN, MIGRATION_PLAN, PHASE_STATUS, TASKS, INTERACTION_SESSION_FACTORY]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase116-interaction-session-factory-typed-entry-extraction] FAILED")
        for error in errors:
            print(f"[phase116-interaction-session-factory-typed-entry-extraction] ERROR {error}")
        return 1

    phase116_plan_text = _read(PHASE112_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_factory_text = _read(INTERACTION_SESSION_FACTORY)

    if "## Phase 116 Slice Status" not in phase116_plan_text:
        errors.append("phase116_plan_missing_slice_status")
    if "`116.1` complete." not in phase116_plan_text:
        errors.append("phase116_plan_missing_112_1_complete")
    if "`116.2` complete." not in phase116_plan_text:
        errors.append("phase116_plan_missing_112_2_complete")
    if "`116.3` complete." not in phase116_plan_text:
        errors.append("phase116_plan_missing_112_3_complete")

    if "## Phase 116 (Interaction Session Factory Typed Entry Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase116_section")

    if "PHASE 116 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase116_started")
    if "PHASE 116 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase116_complete")

    required_tasks = [
        "- [x] Define Phase 116 interaction-session factory typed-entry extraction scope and completion evidence gates.",
        "- [x] Extract typed-entry interaction-session construction seam in `InteractionSessionFactory` ownership.",
        "- [x] Run Phase 116 verification + guard pack and mark `PHASE 116 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase116_line:{task_line}")

    required_factory_strings = [
        "return createFromFactoryInputs(",
        "InteractionSessionFactoryInputs.fromServices(",
        "static InteractionSession createFromFactoryInputs(",
        "factoryInputs.createAssemblyFactoryInputs(SESSION_INTERACTION)",
        "InteractionSessionAssemblyFactoryInputs.forDefaultSession(",
    ]
    for required_string in required_factory_strings:
        if required_string not in interaction_session_factory_text:
            errors.append(f"interaction_session_factory_missing_string:{required_string}")

    if errors:
        print("[phase116-interaction-session-factory-typed-entry-extraction] FAILED")
        for error in errors:
            print(f"[phase116-interaction-session-factory-typed-entry-extraction] ERROR {error}")
        return 1

    print(
        "[phase116-interaction-session-factory-typed-entry-extraction] OK: interaction session factory typed-entry "
        "extraction Phase 116 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
