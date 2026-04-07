from __future__ import annotations

import argparse
import random
from dataclasses import dataclass, field
from typing import Sequence

from .activity_profiles import (
    COMBAT_PROFILE_DB_PARITY,
    CombatBehaviorProfile,
    resolve_combat_behavior_profile,
)
from .runtime_strategy import RuntimeStrategy
from .runtime_core.models import Intent, IntentKind
from .models import Snapshot

ACTIVITY_NAME = "combat"


@dataclass
class CombatConfig:
    target_npc_id: int = -1
    target_npc_ids: tuple[int, ...] = ()
    encounter_profile: str = "none"
    target_world_x: int = -1
    target_world_y: int = -1
    target_max_distance: int = 8
    max_chase_distance: int = 8
    eat_at_hitpoints: int = -1
    eat_randomized_pct: float = 0.0
    food_item_id: int = -1
    tuning_profile: str = COMBAT_PROFILE_DB_PARITY


@dataclass
class CombatStrategy:
    cfg: CombatConfig
    _rolled_eat_threshold: int | None = None
    _profile: CombatBehaviorProfile = field(init=False)

    def __post_init__(self) -> None:
        self._profile = resolve_combat_behavior_profile(self.cfg.tuning_profile)

    @staticmethod
    def _is_animation_active(snapshot: Snapshot) -> bool:
        return snapshot.player_animation not in (None, -1, 0)

    def intents(self, snapshot: Snapshot) -> Sequence[Intent]:
        if not snapshot.logged_in:
            return []
        if snapshot.bank_open:
            return []

        food_item_id = int(self.cfg.food_item_id)
        eat_threshold = self._resolve_eat_threshold(snapshot)
        current_hp = snapshot.hitpoints_current
        if (
            food_item_id > 0
            and eat_threshold > 0
            and current_hp is not None
            and int(current_hp) <= eat_threshold
            and int(snapshot.inventory_counts.get(food_item_id, 0)) > 0
        ):
            return [
                Intent(
                    intent_key=f"{ACTIVITY_NAME}:EAT_FOOD:{food_item_id}",
                    activity=ACTIVITY_NAME,
                    kind=IntentKind.EAT_FOOD,
                    target={"itemId": food_item_id},
                    params={
                        "itemId": food_item_id,
                        "eatAtHitpoints": eat_threshold,
                        "eatAtHitpointsBase": int(self.cfg.eat_at_hitpoints),
                        "eatAtRandomizedPct": self._bounded_randomized_pct(),
                    },
                    policy_key="combat_eat_food",
                    reason="combat_eat_food_threshold",
                )
            ]

        if self._is_animation_active(snapshot):
            return []

        params = {"targetCategory": "NEAREST_ATTACKABLE"}
        encounter_profile = str(self.cfg.encounter_profile or "none").strip().lower()
        if encounter_profile:
            params["encounterProfile"] = encounter_profile
        if self.cfg.target_npc_ids:
            params["targetNpcIds"] = [int(v) for v in self.cfg.target_npc_ids if int(v) > 0]
        if int(self.cfg.target_npc_id) > 0:
            params["targetNpcId"] = int(self.cfg.target_npc_id)
        if int(self.cfg.target_world_x) > 0:
            params["targetWorldX"] = int(self.cfg.target_world_x)
        if int(self.cfg.target_world_y) > 0:
            params["targetWorldY"] = int(self.cfg.target_world_y)
        if int(self.cfg.target_max_distance) > 0:
            params["targetMaxDistance"] = int(self.cfg.target_max_distance)
        if int(self.cfg.max_chase_distance) > 0:
            params["maxChaseDistance"] = int(self.cfg.max_chase_distance)

        return [
            Intent(
                intent_key=f"{ACTIVITY_NAME}:ATTACK_NPC:NEAREST",
                activity=ACTIVITY_NAME,
                kind=IntentKind.ATTACK_NPC,
                target={"targetCategory": "NEAREST_ATTACKABLE"},
                params=params,
                policy_key="combat_attack",
                reason="combat_attack_nearest_npc",
            )
        ]

    def _resolve_eat_threshold(self, snapshot: Snapshot) -> int:
        base_threshold = int(self.cfg.eat_at_hitpoints)
        if base_threshold <= 0:
            self._rolled_eat_threshold = None
            return -1

        randomized_pct = self._bounded_randomized_pct()
        if randomized_pct <= 0.0:
            self._rolled_eat_threshold = None
            return base_threshold

        current_hp = snapshot.hitpoints_current
        if (
            self._rolled_eat_threshold is not None
            and current_hp is not None
            and int(current_hp)
            >= int(self._rolled_eat_threshold) + int(self._profile.eat_rearm_buffer_hitpoints)
        ):
            # Re-arm a fresh randomized threshold once we've clearly recovered after eating.
            self._rolled_eat_threshold = None

        if self._rolled_eat_threshold is None:
            spread = randomized_pct / 100.0
            low = max(1, int(round(base_threshold * (1.0 - spread))))
            high = max(low, int(round(base_threshold * (1.0 + spread))))
            hp_max = snapshot.hitpoints_max
            if hp_max is not None and int(hp_max) > 0:
                bounded_hp_max = int(hp_max)
                high = min(high, bounded_hp_max)
                low = min(low, high)
            self._rolled_eat_threshold = random.randint(low, high)
        return int(self._rolled_eat_threshold)

    def _bounded_randomized_pct(self) -> float:
        try:
            raw = float(self.cfg.eat_randomized_pct)
        except (TypeError, ValueError):
            raw = 0.0
        return max(0.0, min(95.0, raw))


def parse_npc_ids(raw: object) -> list[int]:
    text = "" if raw is None else str(raw).strip()
    if not text:
        return []
    normalized = text.replace("[", " ").replace("]", " ").replace(";", ",")
    out: list[int] = []
    for token in normalized.replace(",", " ").split():
        try:
            npc_id = int(token)
        except ValueError:
            continue
        if npc_id > 0 and npc_id not in out:
            out.append(npc_id)
    return out


def build_strategy(args: argparse.Namespace) -> RuntimeStrategy:
    target_npc_ids = parse_npc_ids(getattr(args, "combat_target_npc_id", -1))
    target_npc_id = target_npc_ids[0] if target_npc_ids else -1
    return CombatStrategy(
        cfg=CombatConfig(
            target_npc_id=target_npc_id,
            target_npc_ids=tuple(target_npc_ids),
            encounter_profile=str(getattr(args, "combat_encounter_profile", "none") or "none").strip().lower(),
            target_world_x=int(getattr(args, "combat_target_world_x", -1)),
            target_world_y=int(getattr(args, "combat_target_world_y", -1)),
            target_max_distance=max(1, int(getattr(args, "combat_target_max_distance", 8))),
            max_chase_distance=max(1, int(getattr(args, "combat_max_chase_distance", 8))),
            eat_at_hitpoints=int(getattr(args, "combat_eat_at_hitpoints", -1)),
            eat_randomized_pct=float(getattr(args, "combat_eat_randomized_pct", 0.0)),
            food_item_id=int(getattr(args, "combat_food_item_id", -1)),
            tuning_profile=str(
                getattr(args, "combat_tuning_profile", COMBAT_PROFILE_DB_PARITY) or COMBAT_PROFILE_DB_PARITY
            ),
        )
    )


def add_args(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--combat-target-npc-id", type=str, default="-1")
    parser.add_argument(
        "--combat-tuning-profile",
        type=str,
        default=COMBAT_PROFILE_DB_PARITY,
        choices=(COMBAT_PROFILE_DB_PARITY,),
    )
    parser.add_argument("--combat-encounter-profile", type=str, default="none")
    parser.add_argument("--combat-target-world-x", type=int, default=-1)
    parser.add_argument("--combat-target-world-y", type=int, default=-1)
    parser.add_argument("--combat-target-max-distance", type=int, default=8)
    parser.add_argument("--combat-max-chase-distance", type=int, default=8)
    parser.add_argument("--combat-eat-at-hitpoints", type=int, default=-1)
    parser.add_argument("--combat-eat-randomized-pct", type=float, default=0.0)
    parser.add_argument("--combat-food-item-id", type=int, default=-1)

