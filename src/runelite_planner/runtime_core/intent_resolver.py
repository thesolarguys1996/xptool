from __future__ import annotations

from dataclasses import dataclass
from typing import Sequence

from .models import Intent, IntentKind, ResolvedIntent


@dataclass(frozen=True)
class IntentResolver:
    """
    Picks exactly one intent per snapshot tick.
    """

    def resolve(self, *, snapshot_tick: int, intents: Sequence[Intent]) -> ResolvedIntent | None:
        if not intents:
            return None

        ranked = sorted(
            intents,
            key=lambda intent: (-self._priority(intent.kind), str(intent.intent_key)),
        )
        selected = ranked[0]
        dropped = tuple(intent.intent_key for intent in ranked[1:])
        return ResolvedIntent(
            selected=selected,
            snapshot_tick=snapshot_tick,
            resolver_reason=f"priority:{selected.kind.value}",
            dropped_intent_keys=dropped,
        )

    @staticmethod
    def _priority(kind: IntentKind) -> int:
        if kind == IntentKind.STOP_DROP_SESSION:
            return 110
        if kind == IntentKind.START_DROP_SESSION:
            return 100
        if kind == IntentKind.DROP_ITEM:
            return 100
        if kind == IntentKind.DEPOSIT_ITEM:
            return 90
        if kind == IntentKind.WITHDRAW_ITEM:
            return 88
        if kind == IntentKind.EAT_FOOD:
            return 92
        if kind == IntentKind.CLOSE_BANK:
            return 85
        if kind == IntentKind.SCENE_OBJECT_ACTION:
            return 72
        if kind == IntentKind.GROUND_ITEM_ACTION:
            return 74
        if kind == IntentKind.WALK_TO_WORLDPOINT:
            return 70
        if kind == IntentKind.MINE_ROCK:
            return 50
        if kind == IntentKind.FISH_SPOT:
            return 50
        if kind == IntentKind.ATTACK_NPC:
            return 50
        if kind == IntentKind.CHOP_TREE:
            return 50
        if kind == IntentKind.OPEN_BANK:
            return 40
        return 0
