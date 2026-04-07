from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE169_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE169_INTERACTION_SESSION_FACTORY_DEFAULT_RUNTIME_SESSION_WIRING_CONSOLIDATION_AC_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
DEFAULT_RUNTIME_SESSION_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeSessionFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE169_PLAN, MIGRATION_PLAN, PHASE_STATUS, TASKS, DEFAULT_RUNTIME_SESSION_FACTORY]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase169-interaction-session-factory-default-runtime-session-wiring-consolidation-ac] FAILED")
        for error in errors:
            print(f"[phase169-interaction-session-factory-default-runtime-session-wiring-consolidation-ac] ERROR {error}")
        return 1

    phase169_plan_text = _read(PHASE169_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    default_runtime_session_factory_text = _read(DEFAULT_RUNTIME_SESSION_FACTORY)

    if "## Phase 169 Slice Status" not in phase169_plan_text:
        errors.append("phase169_plan_missing_slice_status")
    if "`169.1` complete." not in phase169_plan_text:
        errors.append("phase169_plan_missing_169_1_complete")
    if "`169.2` complete." not in phase169_plan_text:
        errors.append("phase169_plan_missing_169_2_complete")
    if "`169.3` complete." not in phase169_plan_text:
        errors.append("phase169_plan_missing_169_3_complete")

    if "## Phase 169 (Interaction Session Factory Default Runtime Session Wiring Consolidation AC)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase169_section")

    if "PHASE 169 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase169_started")
    if "PHASE 169 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase169_complete")

    required_tasks = [
        "- [x] Define Phase 169 interaction-session factory default-runtime-session wiring consolidation AC scope and completion evidence gates.",
        "- [x] Consolidate `InteractionSessionFactoryDefaultRuntimeSessionFactory.createFromDefaultRuntimeBundleFactoryInputs(...)` through focused default runtime-bundle factory ownership.",
        "- [x] Run Phase 169 verification + guard pack and mark `PHASE 169 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase169_line:{task_line}")

    required_factory_strings = [
        "static InteractionSession createFromDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryDefaultRuntimeBundleFactory.createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in default_runtime_session_factory_text:
            errors.append(f"default_runtime_session_factory_missing_string:{required_string}")

    if errors:
        print("[phase169-interaction-session-factory-default-runtime-session-wiring-consolidation-ac] FAILED")
        for error in errors:
            print(f"[phase169-interaction-session-factory-default-runtime-session-wiring-consolidation-ac] ERROR {error}")
        return 1

    print(
        "[phase169-interaction-session-factory-default-runtime-session-wiring-consolidation-ac] OK: "
        "interaction session factory default-runtime-session wiring consolidation AC Phase 169 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
