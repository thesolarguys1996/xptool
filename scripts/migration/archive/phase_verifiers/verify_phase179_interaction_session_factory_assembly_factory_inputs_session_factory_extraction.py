from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE179_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE179_INTERACTION_SESSION_FACTORY_ASSEMBLY_FACTORY_INPUTS_SESSION_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
ASSEMBLY_FACTORY_INPUTS_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyFactoryInputsSessionFactory.java"
)
ASSEMBLY_FACTORY_INPUTS_SESSION_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryAssemblyFactoryInputsSessionFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE179_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        ASSEMBLY_FACTORY_INPUTS_SESSION_FACTORY,
        ASSEMBLY_FACTORY_INPUTS_SESSION_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase179-interaction-session-factory-assembly-factory-inputs-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase179-interaction-session-factory-assembly-factory-inputs-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase179_plan_text = _read(PHASE179_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    assembly_factory_inputs_session_factory_text = _read(ASSEMBLY_FACTORY_INPUTS_SESSION_FACTORY)
    assembly_factory_inputs_session_factory_test_text = _read(ASSEMBLY_FACTORY_INPUTS_SESSION_FACTORY_TEST)

    if "## Phase 179 Slice Status" not in phase179_plan_text:
        errors.append("phase179_plan_missing_slice_status")
    if "`179.1` complete." not in phase179_plan_text:
        errors.append("phase179_plan_missing_179_1_complete")
    if "`179.2` complete." not in phase179_plan_text:
        errors.append("phase179_plan_missing_179_2_complete")
    if "`179.3` complete." not in phase179_plan_text:
        errors.append("phase179_plan_missing_179_3_complete")

    if (
        "## Phase 179 (Interaction Session Factory Assembly Factory Inputs Session Factory Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase179_section")

    if "PHASE 179 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase179_started")
    if "PHASE 179 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase179_complete")

    required_tasks = [
        "- [x] Define Phase 179 interaction-session factory assembly-factory-inputs-session-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryAssemblyFactoryInputsSessionFactory` ownership for interaction-session assembly-factory-input session creation seams.",
        "- [x] Run Phase 179 verification + guard pack and mark `PHASE 179 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase179_line:{task_line}")

    required_factory_strings = [
        "final class InteractionSessionFactoryAssemblyFactoryInputsSessionFactory",
        "static InteractionSession createFromAssemblyFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in assembly_factory_inputs_session_factory_text:
            errors.append(f"assembly_factory_inputs_session_factory_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionFactoryAssemblyFactoryInputsSessionFactoryTest",
        "exposesAssemblyFactoryInputsSessionFactoryEntryPoints",
    ]
    for required_string in required_test_strings:
        if required_string not in assembly_factory_inputs_session_factory_test_text:
            errors.append(f"assembly_factory_inputs_session_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase179-interaction-session-factory-assembly-factory-inputs-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase179-interaction-session-factory-assembly-factory-inputs-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase179-interaction-session-factory-assembly-factory-inputs-session-factory-extraction] OK: "
        "interaction session factory assembly-factory-inputs-session-factory extraction Phase 179 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
