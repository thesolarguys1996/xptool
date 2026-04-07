from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE21_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE21_INTERACTION_ANCHOR_RESOLUTION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
COMMAND_EXECUTOR = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java"
INTERACTION_ANCHOR_RESOLVER_SERVICE = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/executor/InteractionAnchorResolverService.java"
)
INTERACTION_ANCHOR_RESOLVER_SERVICE_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE21_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        COMMAND_EXECUTOR,
        INTERACTION_ANCHOR_RESOLVER_SERVICE,
        INTERACTION_ANCHOR_RESOLVER_SERVICE_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase21-interaction-anchor-resolution] FAILED")
        for error in errors:
            print(f"[phase21-interaction-anchor-resolution] ERROR {error}")
        return 1

    phase21_plan_text = _read(PHASE21_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    command_executor_text = _read(COMMAND_EXECUTOR)
    resolver_service_text = _read(INTERACTION_ANCHOR_RESOLVER_SERVICE)
    resolver_service_test_text = _read(INTERACTION_ANCHOR_RESOLVER_SERVICE_TEST)

    if "## Phase 21 Slice Status" not in phase21_plan_text:
        errors.append("phase21_plan_missing_slice_status")
    if "`21.1` complete." not in phase21_plan_text:
        errors.append("phase21_plan_missing_21_1_complete")
    if "`21.2` complete." not in phase21_plan_text:
        errors.append("phase21_plan_missing_21_2_complete")
    if "`21.3` complete." not in phase21_plan_text:
        errors.append("phase21_plan_missing_21_3_complete")

    if "## Phase 21 (Interaction Anchor Resolution Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase21_section")

    if "PHASE 21 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase21_started")
    if "PHASE 21 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase21_complete")

    required_tasks = [
        "- [x] Define Phase 21 interaction anchor resolution decomposition scope and completion evidence gates.",
        "- [x] Extract interaction anchor resolution ownership from `CommandExecutor` into focused runtime service.",
        "- [x] Run Phase 21 verification + guard pack and mark `PHASE 21 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase21_line:{task_line}")

    if "private final InteractionAnchorResolverService interactionAnchorResolverService;" not in command_executor_text:
        errors.append("command_executor_missing_interaction_anchor_resolver_service_field")
    if "interactionAnchorResolverService.rememberInteractionAnchorForTileObject(targetObject, fallbackCanvasPoint);" not in command_executor_text:
        errors.append("command_executor_missing_tile_object_anchor_delegate")
    method_signature = "private void rememberInteractionAnchorForTileObject(TileObject targetObject, Point fallbackCanvasPoint) {"
    if method_signature not in command_executor_text:
        errors.append("command_executor_missing_tile_object_anchor_method")
    else:
        start_index = command_executor_text.index(method_signature)
        next_method_index = command_executor_text.find("\n\n    public long getLastInteractionClickSerial()", start_index)
        method_body = command_executor_text[start_index: next_method_index if next_method_index != -1 else len(command_executor_text)]
        if "targetObject.getClickbox()" in method_body:
            errors.append("command_executor_still_owns_tile_object_clickbox_resolution")

    if "final class InteractionAnchorResolverService" not in resolver_service_text:
        errors.append("interaction_anchor_resolver_service_missing_class")
    if "void rememberInteractionAnchorForTileObject(" not in resolver_service_text:
        errors.append("interaction_anchor_resolver_service_missing_tile_object_entrypoint")
    if "void rememberInteractionAnchorForClickbox(" not in resolver_service_text:
        errors.append("interaction_anchor_resolver_service_missing_clickbox_entrypoint")
    if "targetObject.getClickbox()" not in resolver_service_text:
        errors.append("interaction_anchor_resolver_service_missing_clickbox_lookup")

    if "rememberInteractionAnchorForClickboxUsesClickboxCenterWhenAvailable" not in resolver_service_test_text:
        errors.append("interaction_anchor_resolver_service_test_missing_clickbox_center_case")
    if "rememberInteractionAnchorForClickboxFallsBackToProvidedPointWhenNoClickbox" not in resolver_service_test_text:
        errors.append("interaction_anchor_resolver_service_test_missing_fallback_case")
    if "rememberInteractionAnchorForTileObjectWithNullTargetFallsBack" not in resolver_service_test_text:
        errors.append("interaction_anchor_resolver_service_test_missing_null_tile_object_case")

    if errors:
        print("[phase21-interaction-anchor-resolution] FAILED")
        for error in errors:
            print(f"[phase21-interaction-anchor-resolution] ERROR {error}")
        return 1

    print(
        "[phase21-interaction-anchor-resolution] OK: interaction anchor resolution Phase 21 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
