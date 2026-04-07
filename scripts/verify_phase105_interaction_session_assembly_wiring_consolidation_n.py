from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE105_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE105_INTERACTION_SESSION_ASSEMBLY_WIRING_CONSOLIDATION_N_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
ASSEMBLY_FACTORY = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionAssemblyFactory.java"
RUNTIME_BUNDLE_FACTORY_INPUTS = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeBundleFactoryInputs.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE105_PLAN, MIGRATION_PLAN, PHASE_STATUS, TASKS, ASSEMBLY_FACTORY, RUNTIME_BUNDLE_FACTORY_INPUTS]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase105-interaction-session-assembly-wiring-consolidation-n] FAILED")
        for error in errors:
            print(f"[phase105-interaction-session-assembly-wiring-consolidation-n] ERROR {error}")
        return 1

    phase105_plan_text = _read(PHASE105_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    assembly_factory_text = _read(ASSEMBLY_FACTORY)

    if "## Phase 105 Slice Status" not in phase105_plan_text:
        errors.append("phase105_plan_missing_slice_status")
    if "`105.1` complete." not in phase105_plan_text:
        errors.append("phase105_plan_missing_105_1_complete")
    if "`105.2` complete." not in phase105_plan_text:
        errors.append("phase105_plan_missing_105_2_complete")
    if "`105.3` complete." not in phase105_plan_text:
        errors.append("phase105_plan_missing_105_3_complete")

    if "## Phase 105 (Interaction Session Assembly Wiring Consolidation N)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase105_section")

    if "PHASE 105 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase105_started")
    if "PHASE 105 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase105_complete")

    required_tasks = [
        "- [x] Define Phase 105 interaction-session assembly wiring consolidation N scope and completion evidence gates.",
        "- [x] Consolidate `InteractionSessionAssemblyFactory` runtime-bundle seam through typed `InteractionSessionRuntimeBundleFactoryInputs` ownership.",
        "- [x] Run Phase 105 verification + guard pack and mark `PHASE 105 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase105_line:{task_line}")

    required_assembly_strings = [
        "return InteractionSessionRuntimeBundleFactory.createRuntimeBundle(",
        "InteractionSessionRuntimeBundleFactoryInputs.fromServices(",
        "InteractionSessionRuntimeBundleFactory.createRuntimeBundleFromServices(",
    ]
    for required_string in required_assembly_strings:
        if required_string not in assembly_factory_text:
            errors.append(f"assembly_factory_missing_string:{required_string}")

    if errors:
        print("[phase105-interaction-session-assembly-wiring-consolidation-n] FAILED")
        for error in errors:
            print(f"[phase105-interaction-session-assembly-wiring-consolidation-n] ERROR {error}")
        return 1

    print(
        "[phase105-interaction-session-assembly-wiring-consolidation-n] OK: interaction session assembly wiring "
        "consolidation N Phase 105 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
