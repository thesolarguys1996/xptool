from __future__ import annotations

import argparse
from dataclasses import dataclass, field
from typing import Sequence

from .runtime_strategy import RuntimeStrategy
from .runtime_core.models import Intent
from .drop_service import DropSessionController
from .models import Snapshot

ACTIVITY_NAME = "drop_probe"


@dataclass
class DropProbeConfig:
    item_id: int = 1519
    start_when_full: bool = False


@dataclass
class DropProbeStrategy:
    cfg: DropProbeConfig
    _drop: DropSessionController = field(default_factory=DropSessionController)

    @staticmethod
    def _inventory_slots_used(snapshot: Snapshot) -> int:
        if snapshot.inventory_slots_used is not None:
            return int(snapshot.inventory_slots_used)
        rows = snapshot.raw.get("inventory") if isinstance(snapshot.raw, dict) else None
        if not isinstance(rows, list):
            return 0
        used = 0
        for row in rows:
            if not isinstance(row, dict):
                continue
            item_id = row.get("itemId", row.get("item_id", row.get("id", -1)))
            qty = row.get("quantity", row.get("qty", row.get("count", 0)))
            try:
                if int(item_id) > 0 and int(qty) > 0:
                    used += 1
            except (TypeError, ValueError):
                continue
        return used

    def intents(self, snapshot: Snapshot) -> Sequence[Intent]:
        item_id = max(1, int(self.cfg.item_id))
        inv_count = int(snapshot.inventory_counts.get(item_id, 0))
        has_item = inv_count > 0
        inv_full = self._inventory_slots_used(snapshot) >= 28
        start_condition = has_item and (inv_full if self.cfg.start_when_full else True)

        return self._drop.step(
            snapshot,
            activity=ACTIVITY_NAME,
            policy_key="drop_probe_session",
            start_condition=start_condition,
            candidate_item_id=item_id if has_item else None,
            start_reason="drop_probe_start_drop_session",
            stop_reason_prefix="drop_probe_stop_drop_session",
        )


def build_strategy(args: argparse.Namespace) -> RuntimeStrategy:
    return DropProbeStrategy(
        cfg=DropProbeConfig(
            item_id=max(1, int(getattr(args, "drop_probe_item_id", 1519))),
            start_when_full=bool(getattr(args, "drop_probe_start_when_full", False)),
        )
    )


def add_args(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--drop-probe-item-id", type=int, default=1519)
    parser.add_argument("--drop-probe-start-when-full", action="store_true", default=False)

