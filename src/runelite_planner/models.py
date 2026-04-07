from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Dict, Optional


@dataclass
class Snapshot:
    """
    Normalized snapshot emitted by the RuneLite XPTool plugin.

    The plugin writes log lines like:

        xptool.snapshot {"tick": 123, "loggedIn": true, ...}

    We surface core fields and keep the raw JSON for anything more advanced.
    """

    tick: int
    logged_in: bool
    bank_open: bool
    shop_open: bool = False
    world_id: Optional[int] = None

    # Banking-related aggregates (all optional; plugin can fill them when ready)
    inventory_counts: Dict[int, int] = field(default_factory=dict)
    bank_counts: Dict[int, int] = field(default_factory=dict)
    shop_counts: Dict[int, int] = field(default_factory=dict)
    inventory_slots_used: Optional[int] = None
    player_animation: Optional[int] = None
    hitpoints_current: Optional[int] = None
    hitpoints_max: Optional[int] = None
    nearest_tree_id: Optional[int] = None
    nearest_tree_world_x: Optional[int] = None
    nearest_tree_world_y: Optional[int] = None
    nearest_tree_distance: Optional[int] = None
    nearest_tree_interactable: Optional[bool] = None

    # Full raw JSON payload from the log line.
    raw: Dict[str, Any] = field(default_factory=dict)


@dataclass
class RuntimeCommand:
    """
    Single high-level command produced by a strategy.

    - command_type: short machine-friendly name (e.g. "BANK_OPEN_SAFE")
    - payload: free-form dict passed through to the plugin
    - reason: human-friendly explanation for logs / debugging
    - tick: game tick this decision was made on (optional)
    - source: where this command originated ("woodcutting", "probe", etc)
    """

    command_type: str
    payload: Dict[str, Any] = field(default_factory=dict)
    reason: Optional[str] = None
    tick: Optional[int] = None
    source: Optional[str] = None

