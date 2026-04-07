from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE107_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE107_INTERACTION_SESSION_ASSEMBLY_FACTORY_INPUTS_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
ASSEMBLY_FACTORY_INPUTS = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionAssemblyFactoryInputs.java"
)
ASSEMBLY_FACTORY_INPUTS_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionAssemblyFactoryInputsTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE107_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        ASSEMBLY_FACTORY_INPUTS,
        ASSEMBLY_FACTORY_INPUTS_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase107-interaction-session-assembly-factory-inputs-extraction] FAILED")
        for error in errors:
            print(f"[phase107-interaction-session-assembly-factory-inputs-extraction] ERROR {error}")
        return 1

    phase107_plan_text = _read(PHASE107_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    assembly_factory_inputs_text = _read(ASSEMBLY_FACTORY_INPUTS)
    assembly_factory_inputs_test_text = _read(ASSEMBLY_FACTORY_INPUTS_TEST)

    if "## Phase 107 Slice Status" not in phase107_plan_text:
        errors.append("phase107_plan_missing_slice_status")
    if "`107.1` complete." not in phase107_plan_text:
        errors.append("phase107_plan_missing_107_1_complete")
    if "`107.2` complete." not in phase107_plan_text:
        errors.append("phase107_plan_missing_107_2_complete")
    if "`107.3` complete." not in phase107_plan_text:
        errors.append("phase107_plan_missing_107_3_complete")

    if "## Phase 107 (Interaction Session Assembly Factory Inputs Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase107_section")

    if "PHASE 107 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase107_started")
    if "PHASE 107 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase107_complete")

    required_tasks = [
        "- [x] Define Phase 107 interaction-session assembly-factory inputs extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionAssemblyFactoryInputs` ownership for assembly-factory typed inputs.",
        "- [x] Run Phase 107 verification + guard pack and mark `PHASE 107 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase107_line:{task_line}")

    required_inputs_strings = [
        "final class InteractionSessionAssemblyFactoryInputs",
        "final CommandExecutor executor;",
        "final SessionManager sessionManager;",
        "final SessionCommandFacade commandFacade;",
        "final String sessionInteractionKey;",
        "static InteractionSessionAssemblyFactoryInputs forDefaultSession(",
        "static InteractionSessionAssemblyFactoryInputs forSession(",
    ]
    for required_string in required_inputs_strings:
        if required_string not in assembly_factory_inputs_text:
            errors.append(f"assembly_factory_inputs_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionAssemblyFactoryInputsTest",
        "forSessionRetainsProvidedReferencesAndSessionKey",
        "forDefaultSessionRetainsProvidedReferencesAndDefaultSessionKey",
    ]
    for required_string in required_test_strings:
        if required_string not in assembly_factory_inputs_test_text:
            errors.append(f"assembly_factory_inputs_test_missing_string:{required_string}")

    if errors:
        print("[phase107-interaction-session-assembly-factory-inputs-extraction] FAILED")
        for error in errors:
            print(f"[phase107-interaction-session-assembly-factory-inputs-extraction] ERROR {error}")
        return 1

    print(
        "[phase107-interaction-session-assembly-factory-inputs-extraction] OK: interaction session assembly factory "
        "inputs extraction Phase 107 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
