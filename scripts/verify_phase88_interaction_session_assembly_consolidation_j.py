from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE88_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE88_INTERACTION_SESSION_ASSEMBLY_CONSOLIDATION_J_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java"
INTERACTION_SESSION_ASSEMBLY_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionAssemblyFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE88_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION,
        INTERACTION_SESSION_ASSEMBLY_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase88-interaction-session-assembly-consolidation-j] FAILED")
        for error in errors:
            print(f"[phase88-interaction-session-assembly-consolidation-j] ERROR {error}")
        return 1

    phase88_plan_text = _read(PHASE88_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_text = _read(INTERACTION_SESSION)
    assembly_factory_text = _read(INTERACTION_SESSION_ASSEMBLY_FACTORY)

    if "## Phase 88 Slice Status" not in phase88_plan_text:
        errors.append("phase88_plan_missing_slice_status")
    if "`88.1` complete." not in phase88_plan_text:
        errors.append("phase88_plan_missing_88_1_complete")
    if "`88.2` complete." not in phase88_plan_text:
        errors.append("phase88_plan_missing_88_2_complete")
    if "`88.3` complete." not in phase88_plan_text:
        errors.append("phase88_plan_missing_88_3_complete")

    if "## Phase 88 (Interaction Session Assembly Consolidation J)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase88_section")

    if "PHASE 88 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase88_started")
    if "PHASE 88 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase88_complete")

    required_tasks = [
        "- [x] Define Phase 88 interaction session assembly consolidation J scope and completion evidence gates.",
        "- [x] Consolidate interaction-session assembly factory session-key runtime bundle seam while preserving compatibility sentinel strings.",
        "- [x] Run Phase 88 verification + guard pack and mark `PHASE 88 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase88_line:{task_line}")

    required_factory_strings = [
        "return createRuntimeBundleForSession(",
        "static InteractionSessionRuntimeBundle createRuntimeBundleForSession(",
        "String sessionInteractionKey",
        "InteractionSessionHostFactory.createCommandRouterService(commandFacade);",
        "return createRuntimeBundleFromServices(",
    ]
    for required_string in required_factory_strings:
        if required_string not in assembly_factory_text:
            errors.append(f"assembly_factory_missing_string:{required_string}")

    compatibility_strings = [
        "InteractionSessionAssemblyFactory.createRuntimeBundle(",
        "this.interactionSessionCommandRouter = runtimeBundle.interactionSessionCommandRouter;",
    ]
    for compatibility_string in compatibility_strings:
        if compatibility_string not in interaction_session_text:
            errors.append(f"interaction_session_missing_compatibility_string:{compatibility_string}")

    if errors:
        print("[phase88-interaction-session-assembly-consolidation-j] FAILED")
        for error in errors:
            print(f"[phase88-interaction-session-assembly-consolidation-j] ERROR {error}")
        return 1

    print(
        "[phase88-interaction-session-assembly-consolidation-j] OK: interaction session assembly consolidation J Phase 88 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
