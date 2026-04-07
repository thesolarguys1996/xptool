from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE109_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE109_INTERACTION_SESSION_FACTORY_WIRING_CONSOLIDATION_O_PLAN.md"
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

    required_paths = [PHASE109_PLAN, MIGRATION_PLAN, PHASE_STATUS, TASKS, INTERACTION_SESSION_FACTORY]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase109-interaction-session-factory-wiring-consolidation-o] FAILED")
        for error in errors:
            print(f"[phase109-interaction-session-factory-wiring-consolidation-o] ERROR {error}")
        return 1

    phase109_plan_text = _read(PHASE109_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_factory_text = _read(INTERACTION_SESSION_FACTORY)

    if "## Phase 109 Slice Status" not in phase109_plan_text:
        errors.append("phase109_plan_missing_slice_status")
    if "`109.1` complete." not in phase109_plan_text:
        errors.append("phase109_plan_missing_109_1_complete")
    if "`109.2` complete." not in phase109_plan_text:
        errors.append("phase109_plan_missing_109_2_complete")
    if "`109.3` complete." not in phase109_plan_text:
        errors.append("phase109_plan_missing_109_3_complete")

    if "## Phase 109 (Interaction Session Factory Wiring Consolidation O)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase109_section")

    if "PHASE 109 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase109_started")
    if "PHASE 109 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase109_complete")

    required_tasks = [
        "- [x] Define Phase 109 interaction-session factory wiring consolidation O scope and completion evidence gates.",
        "- [x] Consolidate `InteractionSessionFactory` runtime-bundle seam through typed `InteractionSessionAssemblyFactoryInputs` ownership.",
        "- [x] Run Phase 109 verification + guard pack and mark `PHASE 109 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase109_line:{task_line}")

    required_factory_strings = [
        "private static final String SESSION_INTERACTION = \"interaction\";",
        "return createFromAssemblyFactoryInputs(",
        "InteractionSessionAssemblyFactoryInputs.forDefaultSession(",
        "static InteractionSession createFromAssemblyFactoryInputs(",
        "InteractionSessionAssemblyFactory.createRuntimeBundleForSession(assemblyFactoryInputs)",
        "InteractionSessionAssemblyFactory.createRuntimeBundle(",
    ]
    for required_string in required_factory_strings:
        if required_string not in interaction_session_factory_text:
            errors.append(f"interaction_session_factory_missing_string:{required_string}")

    if errors:
        print("[phase109-interaction-session-factory-wiring-consolidation-o] FAILED")
        for error in errors:
            print(f"[phase109-interaction-session-factory-wiring-consolidation-o] ERROR {error}")
        return 1

    print(
        "[phase109-interaction-session-factory-wiring-consolidation-o] OK: interaction session factory wiring "
        "consolidation O Phase 109 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
