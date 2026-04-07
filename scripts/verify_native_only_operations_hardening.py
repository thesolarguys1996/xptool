from __future__ import annotations

import re
import subprocess
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
BOOTSTRAP_RUNTIME = PROJECT_ROOT / "scripts/bootstrap-runtime.ps1"
CUTOVER_RUNBOOK = PROJECT_ROOT / "docs/NATIVE_CLIENT_CUTOVER_RUNBOOK.md"
SOAK_SIGNOFF = PROJECT_ROOT / "docs/NATIVE_SOAK_SIGNOFF.md"
INCIDENT_TRIAGE_RUNBOOK = PROJECT_ROOT / "docs/NATIVE_INCIDENT_TRIAGE_RUNBOOK.md"
JAVA_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
RUNELITE_PLUGIN_PROPERTIES = PROJECT_ROOT / "runelite-plugin/src/main/resources/runelite-plugin.properties"
XPTOOL_PLUGIN = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/XPToolPlugin.java"
XPTOOL_CONFIG = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/XPToolConfig.java"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def _collect_java_inventory() -> tuple[int, int, int]:
    tracked = subprocess.check_output(
        ["git", "ls-tree", "-r", "--name-only", "HEAD", "--", "runelite-plugin/src/main/java"],
        cwd=PROJECT_ROOT,
        text=True,
    ).splitlines()
    files = [path for path in tracked if path.endswith(".java")]
    total = len(files)
    delete_first = 0
    for file_path in files:
        name = Path(file_path).name
        if "HostAdapter" in name or "Wiring" in name or "Bundle" in name or "Inputs" in name:
            delete_first += 1
    port_first = total - delete_first
    return total, delete_first, port_first


def _extract_inventory_count(inventory_text: str, heading: str) -> int | None:
    pattern = re.compile(rf"{re.escape(heading)}.*?Count: `(\d+)`", flags=re.DOTALL)
    match = pattern.search(inventory_text)
    if not match:
        return None
    return int(match.group(1))


def main() -> int:
    errors: list[str] = []

    required_paths = [
        BOOTSTRAP_RUNTIME,
        CUTOVER_RUNBOOK,
        SOAK_SIGNOFF,
        INCIDENT_TRIAGE_RUNBOOK,
        JAVA_INVENTORY,
        RUNELITE_PLUGIN_PROPERTIES,
        PHASE_STATUS,
        TASKS,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[native-only-hardening] FAILED")
        for error in errors:
            print(f"[native-only-hardening] ERROR {error}")
        return 1

    bootstrap_text = _read(BOOTSTRAP_RUNTIME)
    runbook_text = _read(CUTOVER_RUNBOOK)
    soak_signoff_text = _read(SOAK_SIGNOFF)
    incident_triage_text = _read(INCIDENT_TRIAGE_RUNBOOK)
    inventory_text = _read(JAVA_INVENTORY)
    plugin_properties_text = _read(RUNELITE_PLUGIN_PROPERTIES)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)

    if "LegacyJavaShim" in bootstrap_text:
        errors.append("bootstrap_runtime_contains_legacy_java_shim_option")
    if "LegacyJavaShim" in runbook_text:
        errors.append("cutover_runbook_contains_legacy_java_shim_option")
    if "native-only defaults" not in runbook_text:
        errors.append("cutover_runbook_missing_native_only_defaults_language")
    if "docs/NATIVE_INCIDENT_TRIAGE_RUNBOOK.md" not in runbook_text:
        errors.append("cutover_runbook_missing_incident_triage_reference")
    if "docs/NATIVE_INCIDENT_TRIAGE_RUNBOOK.md" not in soak_signoff_text:
        errors.append("soak_signoff_missing_incident_triage_reference")
    if "## Required Artifact Bundle" not in incident_triage_text:
        errors.append("incident_triage_missing_artifact_bundle_section")
    if "## Triage Flow" not in incident_triage_text:
        errors.append("incident_triage_missing_triage_flow_section")

    if XPTOOL_PLUGIN.exists():
        errors.append("xptool_plugin_file_present")
    if XPTOOL_CONFIG.exists():
        errors.append("xptool_config_file_present")
    if "plugins=com.xptool.XPToolPlugin" in plugin_properties_text:
        errors.append("runelite_plugin_properties_registers_removed_plugin")
    if "plugins=" not in plugin_properties_text:
        errors.append("runelite_plugin_properties_missing_plugins_key")

    observed_total, observed_delete_first, observed_port_first = _collect_java_inventory()
    total_match = re.search(r"Total Java files remaining: `(\d+)`", inventory_text)
    if not total_match:
        errors.append("java_inventory_missing_current_total")
    else:
        inventory_total = int(total_match.group(1))
        if inventory_total != observed_total:
            errors.append(
                f"java_inventory_total_mismatch inventory={inventory_total} observed={observed_total}"
            )
    if "Delete-First Scaffolding Set" not in inventory_text:
        errors.append("java_inventory_missing_delete_first_section")
    inventory_delete_first = _extract_inventory_count(inventory_text, "## Delete-First Scaffolding Set")
    if inventory_delete_first is None:
        errors.append("java_inventory_missing_delete_first_count")
    elif inventory_delete_first != observed_delete_first:
        errors.append(
            f"java_inventory_delete_first_mismatch inventory={inventory_delete_first} observed={observed_delete_first}"
        )
    inventory_port_first = _extract_inventory_count(inventory_text, "## Port-First Behavior Set")
    if inventory_port_first is None:
        errors.append("java_inventory_missing_port_first_count")
    elif inventory_port_first != observed_port_first:
        errors.append(
            f"java_inventory_port_first_mismatch inventory={inventory_port_first} observed={observed_port_first}"
        )

    if "PHASE 10 STARTED" not in phase_status_text and "PHASE 10 COMPLETE" not in phase_status_text:
        errors.append("phase10_status_missing_started_or_complete")
    if "Phase 10.2" not in phase_status_text:
        errors.append("phase10_status_missing_10_2_evidence")
    if "Phase 10.3" not in phase_status_text:
        errors.append("phase10_status_missing_10_3_evidence")
    if "Phase 10.4" not in phase_status_text:
        errors.append("phase10_status_missing_10_4_evidence")
    if "- [x] Audit docs/scripts for native-only operational consistency and patch stale ownership assumptions." not in tasks_text:
        errors.append("tasks_missing_phase10_2_checklist_item")
    if "- [x] Tighten native-only verification/soak gates for inventory drift and host-cutover invariants." not in tasks_text:
        errors.append("tasks_missing_phase10_3_checklist_item")
    if "- [x] Add reliability/incident triage runbook updates and artifact capture guidance." not in tasks_text:
        errors.append("tasks_missing_phase10_4_checklist_item")
    if "- [x] Complete Phase 10 signoff pack and mark `PHASE 10 COMPLETE`." not in tasks_text:
        errors.append("tasks_missing_phase10_5_checklist_item")

    if "PHASE 10 COMPLETE" in phase_status_text and "Phase 10.5" not in phase_status_text:
        errors.append("phase10_complete_missing_10_5_evidence")

    if errors:
        print("[native-only-hardening] FAILED")
        for error in errors:
            print(f"[native-only-hardening] ERROR {error}")
        return 1

    print("[native-only-hardening] OK: native-only operations hardening baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
