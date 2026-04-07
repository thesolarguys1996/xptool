from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE87_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE87_INTERACTION_SESSION_ASSEMBLY_FACTORY_EXTRACTION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java"
INTERACTION_SESSION_ASSEMBLY_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionAssemblyFactory.java"
)
INTERACTION_SESSION_RUNTIME_BUNDLE = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeBundle.java"
)
INTERACTION_SESSION_ASSEMBLY_FACTORY_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionAssemblyFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE87_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION,
        INTERACTION_SESSION_ASSEMBLY_FACTORY,
        INTERACTION_SESSION_RUNTIME_BUNDLE,
        INTERACTION_SESSION_ASSEMBLY_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase87-interaction-session-assembly-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase87-interaction-session-assembly-factory-extraction] ERROR {error}")
        return 1

    phase87_plan_text = _read(PHASE87_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_text = _read(INTERACTION_SESSION)
    assembly_factory_text = _read(INTERACTION_SESSION_ASSEMBLY_FACTORY)
    runtime_bundle_text = _read(INTERACTION_SESSION_RUNTIME_BUNDLE)

    if "## Phase 87 Slice Status" not in phase87_plan_text:
        errors.append("phase87_plan_missing_slice_status")
    if "`87.1` complete." not in phase87_plan_text:
        errors.append("phase87_plan_missing_87_1_complete")
    if "`87.2` complete." not in phase87_plan_text:
        errors.append("phase87_plan_missing_87_2_complete")
    if "`87.3` complete." not in phase87_plan_text:
        errors.append("phase87_plan_missing_87_3_complete")

    if "## Phase 87 (Interaction Session Assembly Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase87_section")

    if "PHASE 87 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase87_started")
    if "PHASE 87 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase87_complete")

    required_tasks = [
        "- [x] Define Phase 87 interaction session assembly factory extraction scope and completion evidence gates.",
        "- [x] Extract interaction-session constructor assembly into focused `InteractionSessionAssemblyFactory` ownership.",
        "- [x] Run Phase 87 verification + guard pack and mark `PHASE 87 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase87_line:{task_line}")

    if "InteractionSessionAssemblyFactory.createRuntimeBundle(" not in interaction_session_text:
        errors.append("interaction_session_missing_runtime_bundle_factory_call")
    if "this.interactionSessionCommandRouter = runtimeBundle.interactionSessionCommandRouter;" not in interaction_session_text:
        errors.append("interaction_session_missing_runtime_bundle_mapping")

    if "static InteractionSessionRuntimeBundle createRuntimeBundle(" not in assembly_factory_text:
        errors.append("assembly_factory_missing_create_runtime_bundle")
    if "static InteractionSessionRuntimeBundle createRuntimeBundleFromServices(" not in assembly_factory_text:
        errors.append("assembly_factory_missing_bundle_from_services")

    if "final class InteractionSessionRuntimeBundle" not in runtime_bundle_text:
        errors.append("runtime_bundle_missing_type")
    if "final InteractionSessionCommandRouter interactionSessionCommandRouter;" not in runtime_bundle_text:
        errors.append("runtime_bundle_missing_router_field")

    if errors:
        print("[phase87-interaction-session-assembly-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase87-interaction-session-assembly-factory-extraction] ERROR {error}")
        return 1

    print(
        "[phase87-interaction-session-assembly-factory-extraction] OK: interaction session assembly factory extraction Phase 87 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
