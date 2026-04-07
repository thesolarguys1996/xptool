from __future__ import annotations

from dataclasses import dataclass

from ..activity_profiles import (
    COMBAT_PROFILE_DB_PARITY,
    MINING_PROFILE_DB_PARITY,
    WOODCUTTING_PROFILE_DB_PARITY,
)
from ..bank_probe import BankProbeConfig, BankProbeStrategy
from ..combat import CombatConfig, CombatStrategy, parse_npc_ids
from ..mining import MiningConfig, MiningStrategy
from ..runtime_strategy import RuntimeStrategy
from ..woodcutting import WoodcuttingConfig, WoodcuttingStrategy
from .combat_presets import encounter_profile_from_label
from .registry import default_profile_for_activity


class ActivityBuildError(ValueError):
    pass


@dataclass(frozen=True)
class ActivityBuildOutcome:
    strategy: RuntimeStrategy
    profile: str | None
    info_messages: tuple[str, ...] = ()


@dataclass(frozen=True)
class GuiActivityInputs:
    woodcut_target_category: str
    woodcut_target_x: str
    woodcut_target_y: str
    woodcut_target_max_distance: str
    woodcut_tuning_profile: str
    mining_tuning_profile: str
    combat_npc_id: str
    combat_target_x: str
    combat_target_y: str
    combat_target_max_distance: str
    combat_max_chase_distance: str
    combat_eat_hp: str
    combat_eat_randomized_pct: str
    combat_food_item_id: str
    combat_encounter_label: str
    combat_tuning_profile: str
    bank_probe_target_x: str
    bank_probe_target_y: str
    bank_probe_item_id: str
    bank_probe_withdraw_qty: str
    bank_probe_deposit_qty: str


def build_activity_strategy(activity: str, *, inputs: GuiActivityInputs) -> ActivityBuildOutcome:
    normalized = str(activity or "").strip().lower()
    if normalized == "woodcutting":
        return _build_woodcutting(inputs)
    if normalized == "mining":
        return _build_mining(inputs)
    if normalized == "combat":
        return _build_combat(inputs)
    if normalized == "bank_probe":
        return _build_bank_probe(inputs)
    raise ActivityBuildError(f"Unknown activity: {normalized!r}")


def _require_profile(activity: str, raw_profile: str) -> str:
    default_profile = default_profile_for_activity(activity)
    token = str(raw_profile or "").strip().upper()
    if default_profile is None:
        return token
    if token != default_profile:
        raise ActivityBuildError(f"{activity.capitalize()} profile must be {default_profile}")
    return default_profile


def _build_woodcutting(inputs: GuiActivityInputs) -> ActivityBuildOutcome:
    try:
        target_world_x = int(inputs.woodcut_target_x.strip() or "-1")
        target_world_y = int(inputs.woodcut_target_y.strip() or "-1")
        target_max_distance = max(1, int(inputs.woodcut_target_max_distance.strip() or "12"))
    except ValueError as exc:
        raise ActivityBuildError("Woodcut area X/Y/range must be integers") from exc
    profile = _require_profile("woodcutting", inputs.woodcut_tuning_profile or WOODCUTTING_PROFILE_DB_PARITY)
    strategy = WoodcuttingStrategy(
        cfg=WoodcuttingConfig(
            auto_drop_when_full=True,
            log_item_id=1519,
            target_category=str(inputs.woodcut_target_category).strip().upper() or "SELECTED",
            target_world_x=target_world_x,
            target_world_y=target_world_y,
            target_max_distance=target_max_distance,
            tuning_profile=profile,
        )
    )
    return ActivityBuildOutcome(strategy=strategy, profile=profile, info_messages=(f"[INFO] woodcutting profile={profile}",))


def _build_mining(inputs: GuiActivityInputs) -> ActivityBuildOutcome:
    profile = _require_profile("mining", inputs.mining_tuning_profile or MINING_PROFILE_DB_PARITY)
    strategy = MiningStrategy(
        cfg=MiningConfig(
            auto_drop_when_full=True,
            ore_item_id=440,
            stop_when_inventory_full=True,
            tuning_profile=profile,
        )
    )
    return ActivityBuildOutcome(strategy=strategy, profile=profile, info_messages=(f"[INFO] mining profile={profile}",))


def _build_combat(inputs: GuiActivityInputs) -> ActivityBuildOutcome:
    try:
        target_npc_ids = parse_npc_ids(inputs.combat_npc_id.strip() or "-1")
        target_npc_id = target_npc_ids[0] if target_npc_ids else -1
        target_world_x = int(inputs.combat_target_x.strip() or "-1")
        target_world_y = int(inputs.combat_target_y.strip() or "-1")
        target_max_distance = max(1, int(inputs.combat_target_max_distance.strip() or "8"))
        max_chase_distance = max(1, int(inputs.combat_max_chase_distance.strip() or "8"))
        eat_at_hitpoints = int(inputs.combat_eat_hp.strip() or "-1")
        eat_randomized_pct = float(inputs.combat_eat_randomized_pct.strip() or "0")
        food_item_id = int(inputs.combat_food_item_id.strip() or "-1")
    except ValueError as exc:
        raise ActivityBuildError(
            "Combat NPC ID must be integer or comma-separated integers; X/Y/range/chase/eat HP/food ID must be integers and eat random % must be numeric"
        ) from exc
    profile = _require_profile("combat", inputs.combat_tuning_profile or COMBAT_PROFILE_DB_PARITY)
    eat_randomized_pct = max(0.0, min(95.0, eat_randomized_pct))
    encounter_profile = encounter_profile_from_label(inputs.combat_encounter_label)
    strategy = CombatStrategy(
        cfg=CombatConfig(
            target_npc_id=target_npc_id,
            target_npc_ids=tuple(target_npc_ids),
            target_world_x=target_world_x,
            target_world_y=target_world_y,
            target_max_distance=target_max_distance,
            max_chase_distance=max_chase_distance,
            eat_at_hitpoints=eat_at_hitpoints,
            eat_randomized_pct=eat_randomized_pct,
            food_item_id=food_item_id,
            encounter_profile=encounter_profile,
            tuning_profile=profile,
        )
    )
    return ActivityBuildOutcome(strategy=strategy, profile=profile, info_messages=(f"[INFO] combat profile={profile}",))


def _build_bank_probe(inputs: GuiActivityInputs) -> ActivityBuildOutcome:
    try:
        target_x = int(inputs.bank_probe_target_x.strip())
        target_y = int(inputs.bank_probe_target_y.strip())
        item_id = int(inputs.bank_probe_item_id.strip())
    except ValueError as exc:
        raise ActivityBuildError("Bank probe X/Y/item ID must be integers") from exc
    if target_x <= 0 or target_y <= 0:
        raise ActivityBuildError("Bank probe target X/Y must be > 0")
    if item_id <= 0:
        raise ActivityBuildError("Bank probe item ID must be > 0")
    strategy = BankProbeStrategy(
        cfg=BankProbeConfig(
            target_world_x=target_x,
            target_world_y=target_y,
            item_id=item_id,
            withdraw_quantity=str(inputs.bank_probe_withdraw_qty).strip() or "1",
            deposit_quantity=str(inputs.bank_probe_deposit_qty).strip() or "ALL",
        )
    )
    return ActivityBuildOutcome(strategy=strategy, profile=None, info_messages=())
