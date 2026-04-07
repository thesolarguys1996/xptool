from __future__ import annotations

import os
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[2]
RUNTIME_DIR = PROJECT_ROOT / "runtime"
STATE_DIR = RUNTIME_DIR / "xptool-state"
PLUGIN_TOOLS_DIR = PROJECT_ROOT / "runelite-plugin" / "tools"
DEFAULT_LOG_PATH = RUNTIME_DIR / "client.log"
DEFAULT_COMMAND_BUS_PATH = PLUGIN_TOOLS_DIR / "command-bus.ndjson"
DEFAULT_CONTROL_PLANE_POLICY_PATH = STATE_DIR / "control-plane-policy.json"
DEFAULT_CONTROL_PLANE_AUDIT_PATH = STATE_DIR / "control-plane-audit.ndjson"


def _ensure_parent_dir(path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)


def _runelite_client_log_path() -> Path:
    user_profile = os.environ.get("USERPROFILE")
    if not user_profile:
        return Path.home() / ".runelite" / "logs" / "client.log"
    return Path(user_profile) / ".runelite" / "logs" / "client.log"


def default_log_path() -> str:
    runelite_log = _runelite_client_log_path()
    if runelite_log.exists():
        return str(runelite_log)
    _ensure_parent_dir(DEFAULT_LOG_PATH)
    return str(DEFAULT_LOG_PATH)


def default_command_out_path() -> str:
    _ensure_parent_dir(DEFAULT_COMMAND_BUS_PATH)
    return str(DEFAULT_COMMAND_BUS_PATH)


def default_control_plane_policy_path() -> str:
    _ensure_parent_dir(DEFAULT_CONTROL_PLANE_POLICY_PATH)
    return str(DEFAULT_CONTROL_PLANE_POLICY_PATH)


def default_control_plane_audit_path() -> str:
    _ensure_parent_dir(DEFAULT_CONTROL_PLANE_AUDIT_PATH)
    return str(DEFAULT_CONTROL_PLANE_AUDIT_PATH)
