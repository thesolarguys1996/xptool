from __future__ import annotations

import argparse
from dataclasses import dataclass, field
from typing import Dict, Mapping, Optional, Sequence

from .activity_profiles import (
    WOODCUTTING_PROFILE_DB_PARITY,
    WoodcuttingBehaviorProfile,
    resolve_woodcutting_behavior_profile,
)
from .runtime_strategy import RuntimeStrategy
from .runtime_core.models import Intent, IntentKind
from .drop_service import DropSessionController
from .drop_metrics_tuning import resolve_drop_cadence_tuning_payload
from .idle_metrics_tuning import resolve_idle_cadence_tuning_payload
from .models import Snapshot

ACTIVITY_NAME = "woodcutting"
COMMON_LOG_ITEM_IDS: tuple[int, ...] = (
    1511,  # logs
    1521,  # oak logs
    1519,  # willow logs
    1517,  # maple logs
    1515,  # yew logs
    1513,  # magic logs
    6333,  # teak logs
    6332,  # mahogany logs
)
BEGINNER_CLUE_SCROLL_ITEM_IDS: tuple[int, ...] = (
    23182,  # clue scroll (beginner)
    24361,  # scroll box (beginner)
)
WOODCUTTING_DROP_TUNING_BOUNDS: dict[str, tuple[int, int]] = {
    "localCooldownMinMs": (0, 500),
    "localCooldownMaxMs": (0, 700),
    "secondDispatchChancePercent": (0, 100),
    "rhythmPauseChanceMinPercent": (0, 100),
    "rhythmPauseChanceMaxPercent": (0, 100),
    "rhythmPauseRampStartDispatches": (1, 60),
    "sessionCooldownBiasMs": (-40, 100),
    "targetCycleClicksMedian": (1, 99),
    "targetCycleDurationMsMedian": (1000, 60000),
}
WOODCUTTING_DROP_LOCAL_COOLDOWN_MIN_SCALE = 0.78
WOODCUTTING_DROP_LOCAL_COOLDOWN_MAX_SCALE = 0.76
WOODCUTTING_DROP_SECOND_DISPATCH_BONUS_PERCENT = 8
WOODCUTTING_DROP_SESSION_COOLDOWN_BIAS_DELTA_MS = -6
WOODCUTTING_DROP_RHYTHM_PAUSE_MAX_DELTA_PERCENT = -1
WOODCUTTING_DROP_TARGET_CYCLE_DURATION_SCALE = 0.82
WOODCUTTING_DROP_MISSING_ITEM_STOP_CONFIRM_TICKS = 4


@dataclass
class WoodcuttingConfig:
    auto_drop_when_full: bool = True
    log_item_id: int = 1519
    target_category: str = "SELECTED"
    target_world_x: int = -1
    target_world_y: int = -1
    target_max_distance: int = 12
    tuning_profile: str = WOODCUTTING_PROFILE_DB_PARITY


@dataclass
class WoodcuttingStrategy:
    cfg: WoodcuttingConfig
    _drop: DropSessionController = field(default_factory=DropSessionController)
    _profile: WoodcuttingBehaviorProfile = field(init=False)
    _drop_cadence_tuning_payload: dict[str, int] | None = field(init=False, default=None)
    _idle_cadence_tuning_payload: dict[str, int] | None = field(init=False, default=None)
    _runtime_warnings: list[str] = field(init=False, default_factory=list)
    _drop_tuning_block_warning_latched: bool = field(init=False, default=False)
    _missing_drop_item_streak: int = field(init=False, default=0)

    def consume_runtime_warnings(self) -> tuple[str, ...]:
        if not self._runtime_warnings:
            return ()
        warnings = tuple(self._runtime_warnings)
        self._runtime_warnings.clear()
        return warnings

    def _queue_drop_tuning_missing_warning_once(self) -> None:
        if self._drop_tuning_block_warning_latched:
            return
        self._drop_tuning_block_warning_latched = True
        self._runtime_warnings.append(
            "strict_drop_tuning: blocked woodcutting drop start because dropCadenceTuning is missing"
        )

    def __post_init__(self) -> None:
        self._profile = resolve_woodcutting_behavior_profile(self.cfg.tuning_profile)
        profile_key = str(self.cfg.tuning_profile or WOODCUTTING_PROFILE_DB_PARITY).strip().upper()
        if profile_key == WOODCUTTING_PROFILE_DB_PARITY:
            self._drop_cadence_tuning_payload = resolve_drop_cadence_tuning_payload(
                activity_key=ACTIVITY_NAME,
                user_key="default_user",
            )
            self._drop_cadence_tuning_payload = apply_woodcutting_drop_speed_bias(self._drop_cadence_tuning_payload)
            self._idle_cadence_tuning_payload = resolve_idle_cadence_tuning_payload(
                activity_key=ACTIVITY_NAME,
                user_key="default_user",
            )
        else:
            self._drop_cadence_tuning_payload = None
            self._idle_cadence_tuning_payload = None

    @staticmethod
    def _normalize_target_category(value: object) -> str:
        category = str(value or "SELECTED").strip().upper()
        if category in ("SELECTED", "NORMAL", "OAK", "WILLOW"):
            return category
        return "SELECTED"

    def _resolve_drop_item_id(self, inventory_counts: Dict[int, int]) -> Optional[int]:
        preferred = max(1, int(self.cfg.log_item_id))
        if int(inventory_counts.get(preferred, 0)) > 0:
            return preferred
        for item_id in COMMON_LOG_ITEM_IDS:
            if int(inventory_counts.get(item_id, 0)) > 0:
                return item_id
        return None

    def _resolve_drop_item_ids(self, inventory_counts: Dict[int, int]) -> tuple[int, ...]:
        ordered_ids: list[int] = []
        seen: set[int] = set()

        preferred = max(1, int(self.cfg.log_item_id))
        if int(inventory_counts.get(preferred, 0)) > 0:
            ordered_ids.append(preferred)
            seen.add(preferred)

        for item_id in COMMON_LOG_ITEM_IDS:
            candidate = int(item_id)
            if candidate in seen:
                continue
            if int(inventory_counts.get(candidate, 0)) <= 0:
                continue
            seen.add(candidate)
            ordered_ids.append(candidate)

        if ordered_ids:
            return tuple(ordered_ids)
        return (preferred,)

    @staticmethod
    def _has_beginner_clue_scroll(inventory_counts: Dict[int, int]) -> bool:
        for item_id in BEGINNER_CLUE_SCROLL_ITEM_IDS:
            if int(inventory_counts.get(item_id, 0)) > 0:
                return True
        return False

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
        slots_used = self._inventory_slots_used(snapshot)
        inv_full = slots_used is not None and slots_used >= int(self._profile.inventory_full_slots)

        drop_item_id = self._resolve_drop_item_id(snapshot.inventory_counts)
        active_drop_item_id = drop_item_id
        if self._drop.session_active:
            session_item_id = int(self._drop.session_item_id or -1)
            session_item_remaining = session_item_id > 0 and int(snapshot.inventory_counts.get(session_item_id, 0)) > 0
            clue_remaining = self._has_beginner_clue_scroll(snapshot.inventory_counts)
            if session_item_remaining or clue_remaining:
                active_drop_item_id = session_item_id if session_item_id > 0 else drop_item_id
            if active_drop_item_id is None and session_item_id > 0:
                self._missing_drop_item_streak += 1
                if self._missing_drop_item_streak < WOODCUTTING_DROP_MISSING_ITEM_STOP_CONFIRM_TICKS:
                    active_drop_item_id = session_item_id
            else:
                self._missing_drop_item_streak = 0
        else:
            self._missing_drop_item_streak = 0
        if not self.cfg.auto_drop_when_full:
            forced_stop = self._drop.stop_session(
                activity=ACTIVITY_NAME,
                policy_key="woodcut_drop_session",
                reason="woodcutting_stop_drop_session_disabled",
            )
            if forced_stop:
                return forced_stop
        drop_target_item_ids = self._resolve_drop_item_ids(snapshot.inventory_counts)
        drop_start_requested = bool(self.cfg.auto_drop_when_full and inv_full and drop_item_id is not None)
        drop_tuning_available = bool(self._drop_cadence_tuning_payload)
        if not drop_start_requested or drop_tuning_available:
            self._drop_tuning_block_warning_latched = False
        if drop_start_requested and not drop_tuning_available:
            self._queue_drop_tuning_missing_warning_once()
            forced_stop = self._drop.stop_session(
                activity=ACTIVITY_NAME,
                policy_key="woodcut_drop_session",
                reason="woodcutting_stop_drop_session_missing_drop_cadence_tuning",
            )
            if forced_stop:
                return forced_stop
            return []
        drop_start_extra_params = {
            "itemIds": [int(v) for v in drop_target_item_ids if int(v) > 0],
            "dropCadenceProfile": str(self.cfg.tuning_profile or WOODCUTTING_PROFILE_DB_PARITY).strip().upper(),
        }
        if self._drop_cadence_tuning_payload:
            drop_start_extra_params["dropCadenceTuning"] = dict(self._drop_cadence_tuning_payload)
        if self._idle_cadence_tuning_payload:
            drop_start_extra_params["idleCadenceTuning"] = dict(self._idle_cadence_tuning_payload)
        drop_intents = self._drop.step(
            snapshot,
            activity=ACTIVITY_NAME,
            policy_key="woodcut_drop_session",
            start_condition=bool(
                drop_tuning_available
                and self.cfg.auto_drop_when_full
                and inv_full
                and drop_item_id is not None
            ),
            candidate_item_id=active_drop_item_id,
            start_reason="woodcutting_start_drop_session",
            stop_reason_prefix="woodcutting_stop_drop_session",
            switch_item_on_change=True,
            start_extra_params=drop_start_extra_params,
        )
        if drop_intents:
            return drop_intents
        if self._drop.session_active:
            return []

        if inv_full:
            return []

        if not self._is_animation_active(snapshot):
            target_category = self._normalize_target_category(self.cfg.target_category)
            params: dict[str, int | str] = {"targetCategory": target_category}
            if int(self.cfg.target_world_x) > 0:
                params["targetWorldX"] = int(self.cfg.target_world_x)
            if int(self.cfg.target_world_y) > 0:
                params["targetWorldY"] = int(self.cfg.target_world_y)
            if int(self.cfg.target_max_distance) > 0:
                params["targetMaxDistance"] = int(self.cfg.target_max_distance)
            return [
                Intent(
                    intent_key=f"{ACTIVITY_NAME}:CHOP_TREE:{target_category}",
                    activity=ACTIVITY_NAME,
                    kind=IntentKind.CHOP_TREE,
                    target={"targetCategory": target_category},
                    params=params,
                    policy_key="woodcut_chop",
                    reason="woodcutting_chop_tree",
                )
            ]
        return []


def build_strategy(args: argparse.Namespace) -> RuntimeStrategy:
    return WoodcuttingStrategy(
        cfg=WoodcuttingConfig(
            auto_drop_when_full=bool(getattr(args, "woodcutting_auto_drop_when_full", True)),
            log_item_id=max(1, int(getattr(args, "woodcutting_log_item_id", 1519))),
            target_category=str(getattr(args, "woodcutting_target_category", "SELECTED") or "SELECTED"),
            target_world_x=int(getattr(args, "woodcutting_target_world_x", -1)),
            target_world_y=int(getattr(args, "woodcutting_target_world_y", -1)),
            target_max_distance=max(1, int(getattr(args, "woodcutting_target_max_distance", 12))),
            tuning_profile=str(
                getattr(args, "woodcutting_tuning_profile", WOODCUTTING_PROFILE_DB_PARITY)
                or WOODCUTTING_PROFILE_DB_PARITY
            ),
        )
    )


def add_args(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--woodcutting-log-item-id", type=int, default=1519)
    parser.add_argument(
        "--woodcutting-tuning-profile",
        type=str,
        default=WOODCUTTING_PROFILE_DB_PARITY,
        choices=(WOODCUTTING_PROFILE_DB_PARITY,),
    )
    parser.add_argument("--woodcutting-target-category", type=str, default="SELECTED")
    parser.add_argument("--woodcutting-target-world-x", type=int, default=-1)
    parser.add_argument("--woodcutting-target-world-y", type=int, default=-1)
    parser.add_argument("--woodcutting-target-max-distance", type=int, default=12)
    parser.add_argument("--woodcutting-auto-drop-when-full", action="store_true", default=True)
    parser.add_argument(
        "--woodcutting-no-auto-drop-when-full",
        action="store_false",
        dest="woodcutting_auto_drop_when_full",
    )


def add_woodcutting_args(parser: argparse.ArgumentParser) -> None:
    add_args(parser)


def build_woodcutting_strategy(args: argparse.Namespace) -> RuntimeStrategy:
    return build_strategy(args)


def _clamp_woodcutting_drop_tuning_value(key: str, value: int) -> int:
    bounds = WOODCUTTING_DROP_TUNING_BOUNDS.get(key)
    if bounds is None:
        return int(value)
    min_value, max_value = bounds
    lo = int(min(min_value, max_value))
    hi = int(max(min_value, max_value))
    return max(lo, min(hi, int(value)))


def apply_woodcutting_drop_speed_bias(raw: Mapping[str, object] | None) -> dict[str, int] | None:
    if not isinstance(raw, Mapping):
        return None
    tuned: dict[str, int] = {}
    for key, value in raw.items():
        try:
            tuned[str(key)] = int(value)
        except (TypeError, ValueError):
            continue
    if not tuned:
        return None

    if "localCooldownMinMs" in tuned:
        tuned["localCooldownMinMs"] = _clamp_woodcutting_drop_tuning_value(
            "localCooldownMinMs",
            int(round(tuned["localCooldownMinMs"] * WOODCUTTING_DROP_LOCAL_COOLDOWN_MIN_SCALE)),
        )
    if "localCooldownMaxMs" in tuned:
        tuned["localCooldownMaxMs"] = _clamp_woodcutting_drop_tuning_value(
            "localCooldownMaxMs",
            int(round(tuned["localCooldownMaxMs"] * WOODCUTTING_DROP_LOCAL_COOLDOWN_MAX_SCALE)),
        )
    if "localCooldownMinMs" in tuned and "localCooldownMaxMs" in tuned:
        tuned["localCooldownMaxMs"] = max(tuned["localCooldownMinMs"], tuned["localCooldownMaxMs"])

    if "secondDispatchChancePercent" in tuned:
        tuned["secondDispatchChancePercent"] = _clamp_woodcutting_drop_tuning_value(
            "secondDispatchChancePercent",
            tuned["secondDispatchChancePercent"] + WOODCUTTING_DROP_SECOND_DISPATCH_BONUS_PERCENT,
        )
    if "sessionCooldownBiasMs" in tuned:
        tuned["sessionCooldownBiasMs"] = _clamp_woodcutting_drop_tuning_value(
            "sessionCooldownBiasMs",
            tuned["sessionCooldownBiasMs"] + WOODCUTTING_DROP_SESSION_COOLDOWN_BIAS_DELTA_MS,
        )
    if "rhythmPauseChanceMaxPercent" in tuned:
        tuned["rhythmPauseChanceMaxPercent"] = _clamp_woodcutting_drop_tuning_value(
            "rhythmPauseChanceMaxPercent",
            tuned["rhythmPauseChanceMaxPercent"] + WOODCUTTING_DROP_RHYTHM_PAUSE_MAX_DELTA_PERCENT,
        )
        if "rhythmPauseChanceMinPercent" in tuned:
            tuned["rhythmPauseChanceMaxPercent"] = max(
                tuned["rhythmPauseChanceMinPercent"],
                tuned["rhythmPauseChanceMaxPercent"],
            )
    if "targetCycleDurationMsMedian" in tuned:
        tuned["targetCycleDurationMsMedian"] = _clamp_woodcutting_drop_tuning_value(
            "targetCycleDurationMsMedian",
            int(round(tuned["targetCycleDurationMsMedian"] * WOODCUTTING_DROP_TARGET_CYCLE_DURATION_SCALE)),
        )

    return tuned

