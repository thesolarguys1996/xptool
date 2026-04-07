from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE123_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE123_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_KEY_POLICY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
KEY_POLICY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleKeyPolicy.java"
)
KEY_POLICY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleKeyPolicyTest.java"
)
RUNTIME_BUNDLE_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactory.java"
)
RUNTIME_BUNDLE_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE123_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        KEY_POLICY,
        KEY_POLICY_TEST,
        RUNTIME_BUNDLE_FACTORY,
        RUNTIME_BUNDLE_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase123-interaction-session-factory-runtime-bundle-key-policy-extraction] FAILED")
        for error in errors:
            print(f"[phase123-interaction-session-factory-runtime-bundle-key-policy-extraction] ERROR {error}")
        return 1

    phase123_plan_text = _read(PHASE123_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    key_policy_text = _read(KEY_POLICY)
    key_policy_test_text = _read(KEY_POLICY_TEST)
    runtime_bundle_factory_text = _read(RUNTIME_BUNDLE_FACTORY)
    runtime_bundle_factory_test_text = _read(RUNTIME_BUNDLE_FACTORY_TEST)

    if "## Phase 123 Slice Status" not in phase123_plan_text:
        errors.append("phase123_plan_missing_slice_status")
    if "`123.1` complete." not in phase123_plan_text:
        errors.append("phase123_plan_missing_123_1_complete")
    if "`123.2` complete." not in phase123_plan_text:
        errors.append("phase123_plan_missing_123_2_complete")
    if "`123.3` complete." not in phase123_plan_text:
        errors.append("phase123_plan_missing_123_3_complete")

    if "## Phase 123 (Interaction Session Factory Runtime Bundle Key Policy Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase123_section")

    if "PHASE 123 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase123_started")
    if "PHASE 123 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase123_complete")

    required_tasks = [
        "- [x] Define Phase 123 interaction-session factory runtime-bundle key-policy extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryRuntimeBundleKeyPolicy` ownership for interaction-session factory default runtime-bundle session-key policy.",
        "- [x] Run Phase 123 verification + guard pack and mark `PHASE 123 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase123_line:{task_line}")

    required_key_policy_strings = [
        "final class InteractionSessionFactoryRuntimeBundleKeyPolicy",
        'private static final String DEFAULT_SESSION_INTERACTION_KEY = "interaction";',
        "static String defaultSessionInteractionKey()",
    ]
    for required_string in required_key_policy_strings:
        if required_string not in key_policy_text:
            errors.append(f"key_policy_missing_string:{required_string}")

    required_key_policy_test_strings = [
        "class InteractionSessionFactoryRuntimeBundleKeyPolicyTest",
        "defaultSessionInteractionKeyReturnsInteractionKey",
    ]
    for required_string in required_key_policy_test_strings:
        if required_string not in key_policy_test_text:
            errors.append(f"key_policy_test_missing_string:{required_string}")

    required_runtime_bundle_factory_strings = [
        "static InteractionSessionAssemblyFactoryInputs createDefaultAssemblyFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleKeyPolicy.defaultSessionInteractionKey()",
        "static InteractionSessionRuntimeBundle createRuntimeBundleFromFactoryInputs(",
    ]
    for required_string in required_runtime_bundle_factory_strings:
        if required_string not in runtime_bundle_factory_text:
            errors.append(f"runtime_bundle_factory_missing_string:{required_string}")

    required_runtime_bundle_factory_test_strings = [
        "createDefaultAssemblyFactoryInputsUsesPolicyDefaultSessionKey",
        '"createDefaultAssemblyFactoryInputs",',
        '"createRuntimeBundleFromFactoryInputs",',
    ]
    for required_string in required_runtime_bundle_factory_test_strings:
        if required_string not in runtime_bundle_factory_test_text:
            errors.append(f"runtime_bundle_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase123-interaction-session-factory-runtime-bundle-key-policy-extraction] FAILED")
        for error in errors:
            print(f"[phase123-interaction-session-factory-runtime-bundle-key-policy-extraction] ERROR {error}")
        return 1

    print(
        "[phase123-interaction-session-factory-runtime-bundle-key-policy-extraction] OK: interaction session "
        "factory runtime-bundle key-policy extraction Phase 123 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
