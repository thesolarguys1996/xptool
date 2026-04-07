from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE91_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE91_INTERACTION_SESSION_ASSEMBLY_CONSOLIDATION_K_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
ASSEMBLY_FACTORY = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionAssemblyFactory.java"
INTERACTION_SESSION = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE91_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        ASSEMBLY_FACTORY,
        INTERACTION_SESSION,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase91-interaction-session-assembly-consolidation-k] FAILED")
        for error in errors:
            print(f"[phase91-interaction-session-assembly-consolidation-k] ERROR {error}")
        return 1

    phase91_plan_text = _read(PHASE91_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    assembly_factory_text = _read(ASSEMBLY_FACTORY)
    interaction_session_text = _read(INTERACTION_SESSION)

    if "## Phase 91 Slice Status" not in phase91_plan_text:
        errors.append("phase91_plan_missing_slice_status")
    if "`91.1` complete." not in phase91_plan_text:
        errors.append("phase91_plan_missing_91_1_complete")
    if "`91.2` complete." not in phase91_plan_text:
        errors.append("phase91_plan_missing_91_2_complete")
    if "`91.3` complete." not in phase91_plan_text:
        errors.append("phase91_plan_missing_91_3_complete")

    if "## Phase 91 (Interaction Session Assembly Consolidation K)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase91_section")

    if "PHASE 91 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase91_started")
    if "PHASE 91 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase91_complete")

    required_tasks = [
        "- [x] Define Phase 91 interaction session assembly consolidation K scope and completion evidence gates.",
        "- [x] Consolidate interaction-session assembly runtime bundle delegation seam while preserving compatibility sentinel strings.",
        "- [x] Run Phase 91 verification + guard pack and mark `PHASE 91 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase91_line:{task_line}")

    required_delegate_strings = [
        "return createRuntimeBundleForSession(",
        "InteractionSessionRuntimeBundleFactory.createRuntimeBundleFromServices(",
    ]
    for delegate_string in required_delegate_strings:
        if delegate_string not in assembly_factory_text:
            errors.append(f"assembly_factory_missing_delegate:{delegate_string}")

    compatibility_strings = [
        "return new InteractionSessionRuntimeBundle(",
        "InteractionSessionAssemblyFactory.createRuntimeBundle(",
        "this.interactionSessionCommandRouter = runtimeBundle.interactionSessionCommandRouter;",
    ]
    for compatibility_string in compatibility_strings:
        if compatibility_string not in assembly_factory_text + "\n" + interaction_session_text:
            errors.append(f"missing_compatibility_string:{compatibility_string}")

    if errors:
        print("[phase91-interaction-session-assembly-consolidation-k] FAILED")
        for error in errors:
            print(f"[phase91-interaction-session-assembly-consolidation-k] ERROR {error}")
        return 1

    print(
        "[phase91-interaction-session-assembly-consolidation-k] OK: interaction session assembly consolidation K Phase 91 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
