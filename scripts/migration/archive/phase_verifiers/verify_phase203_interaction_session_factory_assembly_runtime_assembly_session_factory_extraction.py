from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE203_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE203_INTERACTION_SESSION_FACTORY_ASSEMBLY_RUNTIME_ASSEMBLY_SESSION_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
ASSEMBLY_RUNTIME_ASSEMBLY_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeAssemblySessionFactory.java"
)
ASSEMBLY_RUNTIME_ASSEMBLY_SESSION_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeAssemblySessionFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE203_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        ASSEMBLY_RUNTIME_ASSEMBLY_SESSION_FACTORY,
        ASSEMBLY_RUNTIME_ASSEMBLY_SESSION_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase203-interaction-session-factory-assembly-runtime-assembly-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase203-interaction-session-factory-assembly-runtime-assembly-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase203_plan_text = _read(PHASE203_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    assembly_runtime_assembly_session_factory_text = _read(ASSEMBLY_RUNTIME_ASSEMBLY_SESSION_FACTORY)
    assembly_runtime_assembly_session_factory_test_text = _read(ASSEMBLY_RUNTIME_ASSEMBLY_SESSION_FACTORY_TEST)

    if "## Phase 203 Slice Status" not in phase203_plan_text:
        errors.append("phase203_plan_missing_slice_status")
    if "`203.1` complete." not in phase203_plan_text:
        errors.append("phase203_plan_missing_203_1_complete")
    if "`203.2` complete." not in phase203_plan_text:
        errors.append("phase203_plan_missing_203_2_complete")
    if "`203.3` complete." not in phase203_plan_text:
        errors.append("phase203_plan_missing_203_3_complete")

    if (
        "## Phase 203 (Interaction Session Factory Assembly Runtime Assembly Session Factory Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase203_section")

    if "PHASE 203 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase203_started")
    if "PHASE 203 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase203_complete")

    required_tasks = [
        "- [x] Define Phase 203 interaction-session factory assembly-runtime-assembly-session-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryAssemblyRuntimeAssemblySessionFactory` ownership for interaction-session assembly-runtime assembly session routing seams.",
        "- [x] Run Phase 203 verification + guard pack and mark `PHASE 203 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase203_line:{task_line}")

    required_factory_strings = [
        "final class InteractionSessionFactoryAssemblyRuntimeAssemblySessionFactory",
        "static InteractionSession createFromAssemblyFactoryInputs(",
        "InteractionSessionFactoryAssemblyFactoryInputsSessionFactory.createFromAssemblyFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in assembly_runtime_assembly_session_factory_text:
            errors.append(f"assembly_runtime_assembly_session_factory_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionFactoryAssemblyRuntimeAssemblySessionFactoryTest",
        "exposesAssemblyRuntimeAssemblySessionFactoryEntryPoints",
    ]
    for required_string in required_test_strings:
        if required_string not in assembly_runtime_assembly_session_factory_test_text:
            errors.append(f"assembly_runtime_assembly_session_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase203-interaction-session-factory-assembly-runtime-assembly-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase203-interaction-session-factory-assembly-runtime-assembly-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase203-interaction-session-factory-assembly-runtime-assembly-session-factory-extraction] OK: "
        "interaction session factory assembly-runtime-assembly-session-factory extraction Phase 203 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
