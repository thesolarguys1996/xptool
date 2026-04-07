from __future__ import annotations

import argparse
from dataclasses import dataclass, field
from typing import Dict, Optional, Sequence

from .activity_profiles import (
    MINING_PROFILE_DB_PARITY,
    MiningBehaviorProfile,
    resolve_mining_behavior_profile,
)
from .runtime_strategy import RuntimeStrategy
from .runtime_core.models import Intent, IntentKind
from .drop_service import DropSessionController
from .models import Snapshot

ACTIVITY_NAME = "mining"
COMMON_ORE_ITEM_IDS: tuple[int, ...] = (
    440,  # iron ore
    436,  # copper ore
    438,  # tin ore
    453,  # coal
    447,  # mithril ore
    449,  # adamantite ore
    451,  # runite ore
    444,  # gold ore
    442,  # silver ore
    434,  # clay
)


@dataclass
class MiningConfig:
    auto_drop_when_full: bool = True
    ore_item_id: int = 440
    stop_when_inventory_full: bool = True
    tuning_profile: str = MINING_PROFILE_DB_PARITY


@dataclass
class MiningStrategy:
    cfg: MiningConfig
    _drop: DropSessionController = field(default_factory=DropSessionController)
    _profile: MiningBehaviorProfile = field(init=False)

    def __post_init__(self) -> None:
        self._profile = resolve_mining_behavior_profile(self.cfg.tuning_profile)

    @staticmethod
    def _inventory_counts(snapshot: Snapshot) -> Dict[int, int]:
        if snapshot.inventory_counts:
            return dict(snapshot.inventory_counts)
        out: Dict[int, int] = {}
        inventory_rows = snapshot.raw.get("inventory") if isinstance(snapshot.raw, dict) else None
        if not isinstance(inventory_rows, list):
            return out
        for row in inventory_rows:
            if not isinstance(row, dict):
                continue
            item_id = row.get("itemId", row.get("item_id", row.get("id", -1)))
            qty = row.get("quantity", row.get("qty", row.get("count", 0)))
            try:
                item_id_int = int(item_id)
                qty_int = int(qty)
            except (TypeError, ValueError):
                continue
            if item_id_int <= 0 or qty_int <= 0:
                continue
            out[item_id_int] = out.get(item_id_int, 0) + qty_int
        return out

    def _resolve_drop_item_id(self, inventory_counts: Dict[int, int]) -> Optional[int]:
        preferred = max(1, int(self.cfg.ore_item_id))
        if int(inventory_counts.get(preferred, 0)) > 0:
            return preferred
        for item_id in COMMON_ORE_ITEM_IDS:
            if int(inventory_counts.get(item_id, 0)) > 0:
                return item_id
        fallback_id: Optional[int] = None
        fallback_qty = 0
        for item_id, qty in inventory_counts.items():
            item_qty = int(qty)
            if item_id <= 0 or item_qty <= 1:
                continue
            if item_qty > fallback_qty:
                fallback_id = int(item_id)
                fallback_qty = item_qty
        if fallback_id is not None:
            return fallback_id
        return None

    @staticmethod
    def _inventory_slots_used(snapshot: Snapshot) -> Optional[int]:
        if snapshot.inventory_slots_used is not None:
            return snapshot.inventory_slots_used
        inventory_rows = snapshot.raw.get("inventory") if isinstance(snapshot.raw, dict) else None
        if not isinstance(inventory_rows, list):
            return None
        used = 0
        for row in inventory_rows:
            if not isinstance(row, dict):
                continue
            item_id = row.get("itemId", row.get("item_id", row.get("id", -1)))
            qty = row.get("quantity", row.get("qty", row.get("count", 0)))
            try:
                item_id_int = int(item_id)
                qty_int = int(qty)
            except (TypeError, ValueError):
                continue
            if item_id_int > 0 and qty_int > 0:
                used += 1
        return used

    @staticmethod
    def _is_animation_active(snapshot: Snapshot) -> bool:
        return snapshot.player_animation not in (None, -1, 0)

    def intents(self, snapshot: Snapshot) -> Sequence[Intent]:
        if not snapshot.logged_in:
            forced_stop = self._drop.stop_session(
                activity=ACTIVITY_NAME,
                policy_key="mining_drop_session",
                reason="mining_stop_drop_session_logged_out",
            )
            if forced_stop:
                return forced_stop
            return []
        if snapshot.bank_open:
            forced_stop = self._drop.stop_session(
                activity=ACTIVITY_NAME,
                policy_key="mining_drop_session",
                reason="mining_stop_drop_session_bank_open",
            )
            if forced_stop:
                return forced_stop
            return []

        slots_used = self._inventory_slots_used(snapshot)
        inv_full = slots_used is not None and slots_used >= int(self._profile.inventory_full_slots)
        inventory_counts = self._inventory_counts(snapshot)
        drop_item_id = self._resolve_drop_item_id(inventory_counts)
        active_drop_item_id = drop_item_id
        if self._drop.session_active:
            session_item_id = int(self._drop.session_item_id or -1)
            session_item_remaining = session_item_id > 0 and int(inventory_counts.get(session_item_id, 0)) > 0
            if session_item_remaining:
                active_drop_item_id = session_item_id
            else:
                active_drop_item_id = None

        if not self.cfg.auto_drop_when_full:
            forced_stop = self._drop.stop_session(
                activity=ACTIVITY_NAME,
                policy_key="mining_drop_session",
                reason="mining_stop_drop_session_disabled",
            )
            if forced_stop:
                return forced_stop

        drop_intents = self._drop.step(
            snapshot,
            activity=ACTIVITY_NAME,
            policy_key="mining_drop_session",
            start_condition=bool(self.cfg.auto_drop_when_full and inv_full and drop_item_id is not None),
            candidate_item_id=active_drop_item_id,
            start_reason="mining_start_drop_session",
            stop_reason_prefix="mining_stop_drop_session",
        )
        if drop_intents:
            return drop_intents
        if self._drop.session_active:
            return []

        if self.cfg.stop_when_inventory_full and inv_full:
            return []

        if not self._is_animation_active(snapshot):
            return [
                Intent(
                    intent_key=f"{ACTIVITY_NAME}:MINE_ROCK:SELECTED",
                    activity=ACTIVITY_NAME,
                    kind=IntentKind.MINE_ROCK,
                    target={"targetCategory": "SELECTED"},
                    params={"targetCategory": "SELECTED"},
                    policy_key="mining_mine",
                    reason="mining_mine_selected_rock",
                )
            ]
        return []


def build_strategy(args: argparse.Namespace) -> RuntimeStrategy:
    return MiningStrategy(
        cfg=MiningConfig(
            auto_drop_when_full=bool(getattr(args, "mining_auto_drop_when_full", True)),
            ore_item_id=max(1, int(getattr(args, "mining_ore_item_id", 440))),
            stop_when_inventory_full=bool(getattr(args, "mining_stop_when_inventory_full", True)),
            tuning_profile=str(
                getattr(args, "mining_tuning_profile", MINING_PROFILE_DB_PARITY) or MINING_PROFILE_DB_PARITY
            ),
        )
    )


def add_args(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--mining-ore-item-id", type=int, default=440)
    parser.add_argument(
        "--mining-tuning-profile",
        type=str,
        default=MINING_PROFILE_DB_PARITY,
        choices=(MINING_PROFILE_DB_PARITY,),
    )
    parser.add_argument("--mining-auto-drop-when-full", action="store_true", default=True)
    parser.add_argument(
        "--mining-no-auto-drop-when-full",
        action="store_false",
        dest="mining_auto_drop_when_full",
    )
    parser.add_argument("--mining-stop-when-inventory-full", action="store_true", default=True)
    parser.add_argument(
        "--mining-no-stop-when-inventory-full",
        action="store_false",
        dest="mining_stop_when_inventory_full",
    )

