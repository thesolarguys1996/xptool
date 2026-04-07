from __future__ import annotations

from dataclasses import dataclass

PROFILE_DB_PARITY = "DB_PARITY"


MINING_PROFILE_DB_PARITY = PROFILE_DB_PARITY


@dataclass(frozen=True)
class MiningBehaviorProfile:
    profile_key: str
    inventory_full_slots: int


MINING_BEHAVIOR_PROFILE_DB_PARITY = MiningBehaviorProfile(
    profile_key=MINING_PROFILE_DB_PARITY,
    inventory_full_slots=28,
)


def resolve_mining_behavior_profile(value: object) -> MiningBehaviorProfile:
    _ = value
    return MINING_BEHAVIOR_PROFILE_DB_PARITY


WOODCUTTING_PROFILE_DB_PARITY = PROFILE_DB_PARITY


@dataclass(frozen=True)
class WoodcuttingBehaviorProfile:
    profile_key: str
    inventory_full_slots: int


WOODCUTTING_BEHAVIOR_PROFILE_DB_PARITY = WoodcuttingBehaviorProfile(
    profile_key=WOODCUTTING_PROFILE_DB_PARITY,
    inventory_full_slots=28,
)


def resolve_woodcutting_behavior_profile(value: object) -> WoodcuttingBehaviorProfile:
    _ = value
    return WOODCUTTING_BEHAVIOR_PROFILE_DB_PARITY


COMBAT_PROFILE_DB_PARITY = PROFILE_DB_PARITY


@dataclass(frozen=True)
class CombatBehaviorProfile:
    profile_key: str
    eat_rearm_buffer_hitpoints: int


COMBAT_BEHAVIOR_PROFILE_DB_PARITY = CombatBehaviorProfile(
    profile_key=COMBAT_PROFILE_DB_PARITY,
    eat_rearm_buffer_hitpoints=4,
)


def resolve_combat_behavior_profile(value: object) -> CombatBehaviorProfile:
    _ = value
    return COMBAT_BEHAVIOR_PROFILE_DB_PARITY


FISHING_PROFILE_DB_PARITY = PROFILE_DB_PARITY


@dataclass(frozen=True)
class FishingBehaviorProfile:
    profile_key: str
    drop_cadence_profile_key: str
    fish_hold_min_ticks: int
    fish_hold_max_ticks: int
    fish_reclick_min_ticks: int
    fish_reclick_max_ticks: int
    fish_walk_suppress_after_cast_min_ticks: int
    fish_walk_suppress_after_cast_max_ticks: int
    fish_outside_streak_threshold: int
    fish_engagement_force_walk_streak_threshold: int
    bank_reopen_cooldown_after_close_min_ticks: int
    bank_reopen_cooldown_after_close_max_ticks: int
    bank_open_pending_hold_min_ticks: int
    bank_open_pending_hold_max_ticks: int
    bank_inside_entry_chance_pct: int
    inventory_full_slots: int
    inventory_full_confirm_ticks: int


FISHING_BEHAVIOR_PROFILE_DB_PARITY = FishingBehaviorProfile(
    profile_key=FISHING_PROFILE_DB_PARITY,
    drop_cadence_profile_key=PROFILE_DB_PARITY,
    fish_hold_min_ticks=6,
    fish_hold_max_ticks=12,
    fish_reclick_min_ticks=2,
    fish_reclick_max_ticks=5,
    fish_walk_suppress_after_cast_min_ticks=8,
    fish_walk_suppress_after_cast_max_ticks=16,
    fish_outside_streak_threshold=3,
    fish_engagement_force_walk_streak_threshold=5,
    bank_reopen_cooldown_after_close_min_ticks=7,
    bank_reopen_cooldown_after_close_max_ticks=13,
    bank_open_pending_hold_min_ticks=6,
    bank_open_pending_hold_max_ticks=12,
    bank_inside_entry_chance_pct=22,
    inventory_full_slots=28,
    inventory_full_confirm_ticks=2,
)


def resolve_fishing_behavior_profile(value: object) -> FishingBehaviorProfile:
    _ = value
    return FISHING_BEHAVIOR_PROFILE_DB_PARITY
