from __future__ import annotations

import argparse
from dataclasses import dataclass
from enum import Enum
from typing import Sequence

from .runtime_strategy import RuntimeStrategy
from .runtime_core.models import Intent, IntentKind
from .models import Snapshot

ACTIVITY_NAME = "bank_probe"


class BankProbePhase(str, Enum):
    OPEN = "OPEN"
    WITHDRAW = "WITHDRAW"
    DEPOSIT = "DEPOSIT"
    CLOSE = "CLOSE"


@dataclass
class BankProbeConfig:
    target_world_x: int = -1
    target_world_y: int = -1
    item_id: int = 1511
    withdraw_quantity: str = "1"
    deposit_quantity: str = "ALL"


@dataclass
class BankProbeStrategy:
    cfg: BankProbeConfig
    _phase: BankProbePhase = BankProbePhase.OPEN

    def _target_valid(self) -> bool:
        return int(self.cfg.target_world_x) > 0 and int(self.cfg.target_world_y) > 0

    def _item_valid(self) -> bool:
        return int(self.cfg.item_id) > 0

    def _inv_count(self, snapshot: Snapshot) -> int:
        return int(snapshot.inventory_counts.get(int(self.cfg.item_id), 0))

    def _bank_count(self, snapshot: Snapshot) -> int:
        return int(snapshot.bank_counts.get(int(self.cfg.item_id), 0))

    def intents(self, snapshot: Snapshot) -> Sequence[Intent]:
        if not snapshot.logged_in:
            self._phase = BankProbePhase.OPEN
            return []
        if not self._target_valid() or not self._item_valid():
            return []

        inv_count = self._inv_count(snapshot)
        bank_count = self._bank_count(snapshot)

        if self._phase == BankProbePhase.OPEN:
            if snapshot.bank_open:
                self._phase = BankProbePhase.WITHDRAW
                return []
            return [
                Intent(
                    intent_key=(
                        f"{ACTIVITY_NAME}:OPEN_BANK:{self.cfg.target_world_x}:{self.cfg.target_world_y}"
                    ),
                    activity=ACTIVITY_NAME,
                    kind=IntentKind.OPEN_BANK,
                    target={
                        "targetWorldX": int(self.cfg.target_world_x),
                        "targetWorldY": int(self.cfg.target_world_y),
                    },
                    params={
                        "targetWorldX": int(self.cfg.target_world_x),
                        "targetWorldY": int(self.cfg.target_world_y),
                    },
                    policy_key="bank_probe_open",
                    reason="bank_probe_open_bank",
                )
            ]

        if self._phase == BankProbePhase.WITHDRAW:
            if not snapshot.bank_open:
                self._phase = BankProbePhase.OPEN
                return []
            if inv_count > 0:
                self._phase = BankProbePhase.DEPOSIT
                return []
            if bank_count <= 0:
                return []
            return [
                Intent(
                    intent_key=f"{ACTIVITY_NAME}:WITHDRAW_ITEM:{self.cfg.item_id}",
                    activity=ACTIVITY_NAME,
                    kind=IntentKind.WITHDRAW_ITEM,
                    target={"itemId": int(self.cfg.item_id)},
                    params={
                        "itemId": int(self.cfg.item_id),
                        "quantity": str(self.cfg.withdraw_quantity),
                    },
                    policy_key="bank_probe_withdraw",
                    reason="bank_probe_withdraw_item",
                )
            ]

        if self._phase == BankProbePhase.DEPOSIT:
            if not snapshot.bank_open:
                self._phase = BankProbePhase.OPEN
                return []
            if inv_count <= 0:
                self._phase = BankProbePhase.CLOSE
                return []
            return [
                Intent(
                    intent_key=f"{ACTIVITY_NAME}:DEPOSIT_ITEM:{self.cfg.item_id}",
                    activity=ACTIVITY_NAME,
                    kind=IntentKind.DEPOSIT_ITEM,
                    target={"itemId": int(self.cfg.item_id)},
                    params={
                        "itemId": int(self.cfg.item_id),
                        "quantity": str(self.cfg.deposit_quantity),
                    },
                    policy_key="bank_probe_deposit",
                    reason="bank_probe_deposit_item",
                )
            ]

        if self._phase == BankProbePhase.CLOSE:
            if not snapshot.bank_open:
                self._phase = BankProbePhase.OPEN
                return []
            return [
                Intent(
                    intent_key=f"{ACTIVITY_NAME}:CLOSE_BANK",
                    activity=ACTIVITY_NAME,
                    kind=IntentKind.CLOSE_BANK,
                    target={},
                    params={},
                    policy_key="bank_probe_close",
                    reason="bank_probe_close_bank",
                )
            ]

        self._phase = BankProbePhase.OPEN
        return []


def build_strategy(args: argparse.Namespace) -> RuntimeStrategy:
    return BankProbeStrategy(
        cfg=BankProbeConfig(
            target_world_x=int(getattr(args, "bank_probe_target_world_x", -1)),
            target_world_y=int(getattr(args, "bank_probe_target_world_y", -1)),
            item_id=max(1, int(getattr(args, "bank_probe_item_id", 1511))),
            withdraw_quantity=str(getattr(args, "bank_probe_withdraw_quantity", "1")),
            deposit_quantity=str(getattr(args, "bank_probe_deposit_quantity", "ALL")),
        )
    )


def add_args(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--bank-probe-target-world-x", type=int, default=-1)
    parser.add_argument("--bank-probe-target-world-y", type=int, default=-1)
    parser.add_argument("--bank-probe-item-id", type=int, default=1511)
    parser.add_argument("--bank-probe-withdraw-quantity", default="1")
    parser.add_argument("--bank-probe-deposit-quantity", default="ALL")

