from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE108_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE108_INTERACTION_SESSION_ASSEMBLY_FACTORY_TYPED_ENTRY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
ASSEMBLY_FACTORY = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionAssemblyFactory.java"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE108_PLAN, MIGRATION_PLAN, PHASE_STATUS, TASKS, ASSEMBLY_FACTORY]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase108-interaction-session-assembly-factory-typed-entry-extraction] FAILED")
        for error in errors:
            print(f"[phase108-interaction-session-assembly-factory-typed-entry-extraction] ERROR {error}")
        return 1

    phase108_plan_text = _read(PHASE108_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    assembly_factory_text = _read(ASSEMBLY_FACTORY)

    if "## Phase 108 Slice Status" not in phase108_plan_text:
        errors.append("phase108_plan_missing_slice_status")
    if "`108.1` complete." not in phase108_plan_text:
        errors.append("phase108_plan_missing_108_1_complete")
    if "`108.2` complete." not in phase108_plan_text:
        errors.append("phase108_plan_missing_108_2_complete")
    if "`108.3` complete." not in phase108_plan_text:
        errors.append("phase108_plan_missing_108_3_complete")

    if "## Phase 108 (Interaction Session Assembly Factory Typed Entry Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase108_section")

    if "PHASE 108 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase108_started")
    if "PHASE 108 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase108_complete")

    required_tasks = [
        "- [x] Define Phase 108 interaction-session assembly-factory typed-entry extraction scope and completion evidence gates.",
        "- [x] Extract typed-entry runtime-bundle assembly seams in `InteractionSessionAssemblyFactory` ownership.",
        "- [x] Run Phase 108 verification + guard pack and mark `PHASE 108 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase108_line:{task_line}")

    required_assembly_strings = [
        "static InteractionSessionRuntimeBundle createRuntimeBundleForSession(",
        "InteractionSessionAssemblyFactoryInputs assemblyFactoryInputs",
        "static InteractionSessionRuntimeBundle createRuntimeBundleFromInputs(",
        "return createRuntimeBundleFromInputs(assemblyFactoryInputs);",
        "InteractionSessionAssemblyFactoryInputs.forSession(",
        "InteractionSessionAssemblyFactoryInputs.forDefaultSession(",
        "return createRuntimeBundleFromServices(",
    ]
    for required_string in required_assembly_strings:
        if required_string not in assembly_factory_text:
            errors.append(f"assembly_factory_missing_string:{required_string}")

    if errors:
        print("[phase108-interaction-session-assembly-factory-typed-entry-extraction] FAILED")
        for error in errors:
            print(f"[phase108-interaction-session-assembly-factory-typed-entry-extraction] ERROR {error}")
        return 1

    print(
        "[phase108-interaction-session-assembly-factory-typed-entry-extraction] OK: interaction session assembly "
        "factory typed-entry extraction Phase 108 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
