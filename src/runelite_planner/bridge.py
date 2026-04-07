from __future__ import annotations

import json
import os
import re
import time
from typing import Dict, Optional, Any

from .command_bus_contract import build_command_bus_record
from .models import RuntimeCommand, Snapshot

# Example log line:
#   xptool.snapshot {"tick":123,"loggedIn":true,...}
SNAPSHOT_PATTERN = re.compile(r"xptool\.snapshot\s+(\{.*\})")
EXECUTION_PATTERN = re.compile(r"xptool\.execution\s+(\{.*\})")


def parse_snapshot_line(line: str) -> Optional[Snapshot]:
    """
    Parse a single client.log line, returning a Snapshot if it contains
    xptool snapshot JSON; otherwise return None.
    """
    match = SNAPSHOT_PATTERN.search(line)
    if not match:
        return None

    try:
        data: Dict[str, Any] = json.loads(match.group(1))
    except json.JSONDecodeError:
        return None

    tick = int(data.get("tick", 0))

    # Normalized flags with defensive defaults.
    logged_in = bool(data.get("loggedIn", False))
    bank_open = bool(data.get("bankOpen", False))
    shop_open = bool(data.get("shopOpen", False))
    world_id_raw = data.get("worldId", data.get("world"))
    try:
        world_id = int(world_id_raw)
    except (TypeError, ValueError):
        world_id = None

    # Optional maps: itemId -> quantity
    raw_inventory_counts = data.get("inventoryCounts") or data.get("inventoryItemCounts") or {}
    raw_bank_counts = data.get("bankCounts") or data.get("bankItemCounts") or {}
    raw_shop_counts = data.get("shopCounts") or data.get("shopItemCounts") or {}

    inventory_counts = {
        int(k): int(v) for k, v in raw_inventory_counts.items()
        if _is_int_like(k) and _is_int_like(v)
    }
    bank_counts = {
        int(k): int(v) for k, v in raw_bank_counts.items()
        if _is_int_like(k) and _is_int_like(v)
    }
    shop_counts = {
        int(k): int(v) for k, v in raw_shop_counts.items()
        if _is_int_like(k) and _is_int_like(v)
    }

    # Fallback: derive counts from inventory arrays when count maps are absent.
    if not inventory_counts:
        inventory_counts = _counts_from_item_rows(data.get("inventory"))
    if not bank_counts:
        bank_counts = _counts_from_item_rows(data.get("bankInventory"))
    if not shop_counts:
        shop_counts = _counts_from_item_rows(data.get("shopInventory"))

    inventory_slots_used = data.get("inventorySlotsUsed")
    if inventory_slots_used is not None:
        try:
            inventory_slots_used = int(inventory_slots_used)
        except (TypeError, ValueError):
            inventory_slots_used = None

    # Player animation (0 or -1 usually means idle depending on client)
    if "playerAnimation" in data:
        player_animation_raw = data.get("playerAnimation")
    elif "player_animation" in data:
        player_animation_raw = data.get("player_animation")
    else:
        player_animation_raw = data.get("animation")

    try:
        player_animation = int(player_animation_raw)
    except (TypeError, ValueError):
        player_animation = None

    hitpoints_current_raw = data.get("hitpointsCurrent", data.get("hpCurrent"))
    hitpoints_max_raw = data.get("hitpointsMax", data.get("hpMax"))
    try:
        hitpoints_current = int(hitpoints_current_raw)
    except (TypeError, ValueError):
        hitpoints_current = None
    try:
        hitpoints_max = int(hitpoints_max_raw)
    except (TypeError, ValueError):
        hitpoints_max = None

    nearest_tree = data.get("nearestTree")
    nearest_tree_id: Optional[int] = None
    nearest_tree_world_x: Optional[int] = None
    nearest_tree_world_y: Optional[int] = None
    nearest_tree_distance: Optional[int] = None
    nearest_tree_interactable: Optional[bool] = None
    if isinstance(nearest_tree, dict):
        try:
            if nearest_tree.get("id") is not None:
                nearest_tree_id = int(nearest_tree.get("id"))
            if nearest_tree.get("worldX") is not None:
                nearest_tree_world_x = int(nearest_tree.get("worldX"))
            if nearest_tree.get("worldY") is not None:
                nearest_tree_world_y = int(nearest_tree.get("worldY"))
            if nearest_tree.get("distance") is not None:
                nearest_tree_distance = int(nearest_tree.get("distance"))
            if nearest_tree.get("interactable") is not None:
                nearest_tree_interactable = bool(nearest_tree.get("interactable"))
        except (TypeError, ValueError):
            nearest_tree_id = None
            nearest_tree_world_x = None
            nearest_tree_world_y = None
            nearest_tree_distance = None
            nearest_tree_interactable = None

    return Snapshot(
        tick=tick,
        logged_in=logged_in,
        bank_open=bank_open,
        shop_open=shop_open,
        world_id=world_id,
        inventory_counts=inventory_counts,
        bank_counts=bank_counts,
        shop_counts=shop_counts,
        inventory_slots_used=inventory_slots_used,
        player_animation=player_animation,
        hitpoints_current=hitpoints_current,
        hitpoints_max=hitpoints_max,
        nearest_tree_id=nearest_tree_id,
        nearest_tree_world_x=nearest_tree_world_x,
        nearest_tree_world_y=nearest_tree_world_y,
        nearest_tree_distance=nearest_tree_distance,
        nearest_tree_interactable=nearest_tree_interactable,
        raw=data,
    )


def parse_execution_line(line: str) -> Optional[Dict[str, Any]]:
    """
    Parse one xptool.execution JSON line from plugin logs.
    """
    match = EXECUTION_PATTERN.search(line)
    if not match:
        return None

    try:
        data: Dict[str, Any] = json.loads(match.group(1))
    except json.JSONDecodeError:
        return None

    status = str(data.get("status") or "").strip().lower()
    reason = str(data.get("reason") or "")
    event_type = str(data.get("eventType") or "").strip().upper()
    command_id = str(data.get("commandId") or "")
    source = str(data.get("source") or "")
    command_type = str(data.get("commandType") or "")
    command_tick_raw = data.get("commandTick")
    try:
        command_tick = int(command_tick_raw)
    except (TypeError, ValueError):
        command_tick = -1

    details = data.get("details")
    if not isinstance(details, dict):
        details = {}

    return {
        "status": status,
        "eventType": event_type,
        "reason": reason,
        "commandId": command_id,
        "source": source,
        "commandType": command_type,
        "commandTick": command_tick,
        "details": details,
    }


def _is_int_like(value: Any) -> bool:
    try:
        int(value)
        return True
    except (TypeError, ValueError):
        return False


def _counts_from_item_rows(rows: Any) -> Dict[int, int]:
    out: Dict[int, int] = {}
    if not isinstance(rows, list):
        return out
    for row in rows:
        if not isinstance(row, dict):
            continue
        item_id_raw = row.get("itemId", row.get("item_id", row.get("id")))
        qty_raw = row.get("quantity", row.get("qty", row.get("count", 0)))
        if not _is_int_like(item_id_raw) or not _is_int_like(qty_raw):
            continue
        item_id = int(item_id_raw)
        qty = int(qty_raw)
        if item_id <= 0 or qty <= 0:
            continue
        out[item_id] = out.get(item_id, 0) + qty
    return out


class CommandBusWriter:
    """
    Writes commands to a NDJSON file in a plugin-consumable COMMAND format.

    Each line looks roughly like:

        {
          "type": "COMMAND",
          "tick": 123,
          "source": "xptool.planner",
          "payload": {
            "commandId": "...",
            "createdAtUnixMillis": ...,
            "commandType": "BANK_OPEN_SAFE",
            "commandPayload": { ... },
            "reason": "woodcutting_chop_selected_tree"
          }
        }
    """

    def __init__(
        self,
        path: str,
        *,
        source: str = "xptool.planner",
        flush_every_n: int = 1,
        flush_interval_seconds: float = 0.02,
        emit_command_envelope: bool = True,
        command_envelope_signing_key: str = "",
        command_envelope_session_id: str = "",
        command_envelope_canonical_method: str = "COMMAND",
        command_envelope_canonical_path: str = "/v1/planner/decision",
    ) -> None:
        self.path = path
        self.source = source
        self.emit_command_envelope = bool(emit_command_envelope)
        signing_key = str(command_envelope_signing_key or "").strip()
        if not signing_key:
            signing_key = str(os.environ.get("XPTOOL_COMMAND_ENVELOPE_SIGNING_KEY", "") or "").strip()
        if not signing_key:
            signing_key = str(os.environ.get("XPTOOL_REMOTE_PLANNER_SIGNING_KEY", "") or "").strip()
        self.command_envelope_signing_key = signing_key
        envelope_session_id = str(command_envelope_session_id or "").strip()
        if not envelope_session_id:
            envelope_session_id = str(os.environ.get("XPTOOL_COMMAND_ENVELOPE_SESSION_ID", "") or "").strip()
        self.command_envelope_session_id = envelope_session_id
        self.command_envelope_canonical_method = str(command_envelope_canonical_method or "COMMAND")
        self.command_envelope_canonical_path = str(command_envelope_canonical_path or "/v1/planner/decision")
        parent = os.path.dirname(path)
        if parent:
            os.makedirs(parent, exist_ok=True)
        self._fh = open(path, "a", encoding="utf-8")
        self._flush_every_n = max(1, int(flush_every_n))
        self._flush_interval_seconds = max(0.01, float(flush_interval_seconds))
        self._pending_writes = 0
        self._last_flush_monotonic = time.monotonic()

    def write_command(self, command: RuntimeCommand) -> str:
        """
        Append one COMMAND line to the output file and return commandId.
        """
        record, command_id = build_command_bus_record(
            command,
            default_source=self.source,
            emit_command_envelope=self.emit_command_envelope,
            command_envelope_signing_key=self.command_envelope_signing_key,
            command_envelope_session_id=self.command_envelope_session_id,
            command_envelope_canonical_method=self.command_envelope_canonical_method,
            command_envelope_canonical_path=self.command_envelope_canonical_path,
        )

        line = json.dumps(record, separators=(",", ":"))
        self._fh.write(line + "\n")
        self._pending_writes += 1

        now_monotonic = time.monotonic()
        should_flush = (
            self._pending_writes >= self._flush_every_n
            or (now_monotonic - self._last_flush_monotonic) >= self._flush_interval_seconds
        )
        if should_flush:
            self._fh.flush()
            self._pending_writes = 0
            self._last_flush_monotonic = now_monotonic
        return command_id

    def close(self) -> None:
        try:
            self._fh.flush()
            self._fh.close()
        except Exception:
            pass

