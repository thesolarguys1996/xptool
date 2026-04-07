from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
BRIDGE_AGENT_CONFIG = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/bridge/BridgeAgentConfig.java"
BRIDGE_RUNTIME = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/bridge/BridgeRuntime.java"
BRIDGE_SYSTEM_BOOTSTRAP = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/bridge/BridgeSystemPropertyBootstrap.java"
BRIDGE_HEARTBEAT = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/bridge/BridgeHeartbeatService.java"
BRIDGE_IPC = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/bridge/BridgeIpcServer.java"
BRIDGE_AGENT = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/bridge/BridgeAgent.java"
BRIDGE_SHADOW_RUNTIME = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/bridge/BridgeShadowRuntime.java"
BRIDGE_RUNTIME_CONTEXT = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/bridge/BridgeRuntimeContext.java"
LAUNCH_SCRIPT = PROJECT_ROOT / "runelite-plugin/launch-dev-runelite.ps1"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    config_text = _read(BRIDGE_AGENT_CONFIG)
    runtime_text = _read(BRIDGE_RUNTIME)
    bootstrap_text = _read(BRIDGE_SYSTEM_BOOTSTRAP)
    heartbeat_text = _read(BRIDGE_HEARTBEAT)
    ipc_text = _read(BRIDGE_IPC)
    agent_text = _read(BRIDGE_AGENT)
    launch_text = _read(LAUNCH_SCRIPT)

    if BRIDGE_SHADOW_RUNTIME.exists():
        errors.append("bridge_shadow_runtime_file_present")
    if BRIDGE_RUNTIME_CONTEXT.exists():
        errors.append("bridge_runtime_context_file_present")

    forbidden_tokens = [
        "legacyShadowExecutionEnabled",
        "shadowExecutionEnabled",
        "BridgeShadowRuntime",
        "BridgeRuntimeContext",
        "injectorPollIntervalMs",
        "clientThreadTimeoutMs",
    ]
    for token in forbidden_tokens:
        if token in config_text:
            errors.append(f"config_contains_forbidden_token:{token}")

    if "isShadowExecutionEnabled" in runtime_text:
        errors.append("runtime_contains_shadow_execution_accessor")
    if "isShadowRuntimeActive" in runtime_text:
        errors.append("runtime_contains_shadow_runtime_accessor")
    if "bridgeReady.set(config.isLoopbackBindAddress() && config.hasAuthToken())" not in runtime_text:
        errors.append("runtime_missing_ipc_ready_assignment")

    if "legacyShadowExecutionEnabled" in bootstrap_text or "shadowExecutionEnabled" in bootstrap_text:
        errors.append("bootstrap_contains_shadow_properties")

    if "shadowExecutionEnabled" in heartbeat_text:
        errors.append("heartbeat_contains_shadow_execution_field")
    if "shadowRuntimeActive" in heartbeat_text:
        errors.append("heartbeat_contains_shadow_runtime_field")
    if "bridge_ipc_ready" not in heartbeat_text:
        errors.append("heartbeat_missing_bridge_ipc_ready_status")

    if "shadowExecutionEnabled" in ipc_text:
        errors.append("ipc_ping_contains_shadow_execution_field")
    if "shadowRuntimeActive" in ipc_text:
        errors.append("ipc_ping_contains_shadow_runtime_field")

    if "legacyShadowExecutionEnabled" in agent_text:
        errors.append("agent_log_contains_legacy_shadow_field")

    if "$BridgeLegacyShadowRuntime" in launch_text:
        errors.append("launch_script_contains_legacy_shadow_switch")
    if "xptool.bridge.shadowExecutionEnabled" in launch_text:
        errors.append("launch_script_contains_shadow_property")
    if "xptool.bridge.legacyShadowExecutionEnabled" in launch_text:
        errors.append("launch_script_contains_legacy_shadow_property")

    if errors:
        print("[java-runtime-ownership] FAILED")
        for error in errors:
            print(f"[java-runtime-ownership] ERROR {error}")
        return 1

    print("[java-runtime-ownership] OK: Java shadow runtime path is fully removed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
