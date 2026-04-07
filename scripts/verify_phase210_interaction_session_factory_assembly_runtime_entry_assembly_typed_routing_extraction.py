from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE210_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE210_INTERACTION_SESSION_FACTORY_ASSEMBLY_RUNTIME_ENTRY_ASSEMBLY_TYPED_ROUTING_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
FACTORY = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java"
ASSEMBLY_RUNTIME_ENTRY_ASSEMBLY_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeEntryAssemblySessionFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE210_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        FACTORY,
        ASSEMBLY_RUNTIME_ENTRY_ASSEMBLY_SESSION_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase210-interaction-session-factory-assembly-runtime-entry-assembly-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase210-interaction-session-factory-assembly-runtime-entry-assembly-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase210_plan_text = _read(PHASE210_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    factory_text = _read(FACTORY)
    assembly_runtime_entry_assembly_session_factory_text = _read(
        ASSEMBLY_RUNTIME_ENTRY_ASSEMBLY_SESSION_FACTORY
    )

    if "## Phase 210 Slice Status" not in phase210_plan_text:
        errors.append("phase210_plan_missing_slice_status")
    if "`210.1` complete." not in phase210_plan_text:
        errors.append("phase210_plan_missing_210_1_complete")
    if "`210.2` complete." not in phase210_plan_text:
        errors.append("phase210_plan_missing_210_2_complete")
    if "`210.3` complete." not in phase210_plan_text:
        errors.append("phase210_plan_missing_210_3_complete")

    if (
        "## Phase 210 (Interaction Session Factory Assembly Runtime Entry Assembly Typed Routing Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase210_section")

    if "PHASE 210 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase210_started")
    if "PHASE 210 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase210_complete")

    required_tasks = [
        "- [x] Define Phase 210 interaction-session factory assembly-runtime entry assembly typed routing extraction scope and completion evidence gates.",
        "- [x] Consolidate `InteractionSessionFactory.createFromAssemblyFactoryInputs(...)` through focused assembly-runtime entry assembly session routing ownership.",
        "- [x] Run Phase 210 verification + guard pack and mark `PHASE 210 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase210_line:{task_line}")

    required_factory_strings = [
        "InteractionSessionFactoryAssemblyRuntimeEntryAssemblySessionFactory.createFromAssemblyFactoryInputs(",
        "InteractionSessionFactoryAssemblyRuntimeSessionFactory.createFromAssemblyFactoryInputs(",
        "InteractionSessionFactoryAssemblyFactoryInputsSessionFactory.createFromAssemblyFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in factory_text:
            errors.append(f"factory_missing_string:{required_string}")

    if "InteractionSessionFactoryAssemblyRuntimeSessionFactory.createFromAssemblyFactoryInputs(" not in assembly_runtime_entry_assembly_session_factory_text:
        errors.append("assembly_runtime_entry_assembly_session_factory_missing_assembly_runtime_delegate")

    if errors:
        print("[phase210-interaction-session-factory-assembly-runtime-entry-assembly-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase210-interaction-session-factory-assembly-runtime-entry-assembly-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase210-interaction-session-factory-assembly-runtime-entry-assembly-typed-routing-extraction] OK: "
        "interaction session factory assembly-runtime entry assembly typed routing extraction Phase 210 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
