from __future__ import annotations

import argparse
import json
import math
import random
from dataclasses import dataclass, field
from typing import Dict, Mapping, Optional, Sequence

from .activity_profiles import (
    FISHING_PROFILE_DB_PARITY,
    FishingBehaviorProfile,
    resolve_fishing_behavior_profile,
)
from .runtime_strategy import RuntimeStrategy
from .runtime_core.models import Intent, IntentKind
from .drop_service import DropSessionController
from .drop_metrics_tuning import resolve_drop_cadence_tuning_payload
from .idle_metrics_tuning import resolve_idle_cadence_tuning_payload
from .models import Snapshot

ACTIVITY_NAME = "fishing"
COMMON_FISH_ITEM_IDS: tuple[int, ...] = (
    317,   # shrimp
    321,   # anchovies
    327,   # sardine
    345,   # herring
    335,   # trout
    331,   # salmon
    359,   # tuna
    377,   # lobster
    371,   # swordfish
    383,   # shark
)
FISH_FAR_OVERRIDE_TILES = 4
FISH_NEAR_ZONE_SUPPRESS_BUFFER_TILES = 4
FISH_ENGAGEMENT_SUPPRESS_EXTRA_DISTANCE_TILES = 6
FISH_RECENT_COMMAND_WALK_SUPPRESS_TICKS = 18
FISH_REACQUIRE_IN_PLACE_TICKS = 34
FISH_REACQUIRE_EXTRA_DISTANCE_TILES = 5
BANK_OUTSIDE_INTERACT_WORLD_X = 3100
BANK_OUTSIDE_INTERACT_WORLD_Y = 3482
BANK_OUTSIDE_APPROACH_RADIUS_TILES = 2
BANK_SCENE_ONLY_BUFFER_TILES = 2
SCENE_ONLY_NEAR_DISTANCE_TILES = 6
WAYPOINT_REISSUE_GAP_MIN_TICKS = 5
WAYPOINT_REISSUE_GAP_MAX_TICKS = 9
SCENE_ONLY_WAYPOINT_REISSUE_GAP_MIN_TICKS = 8
SCENE_ONLY_WAYPOINT_REISSUE_GAP_MAX_TICKS = 13
FISHING_SCENE_ONLY_WALK_POINTS: set[tuple[int, int]] = {
    (3100, 3439),
    (3099, 3460),
    (3102, 3482),
    (3093, 3493),
}
# Per-waypoint tuning for sticky route points.
WAYPOINT_REISSUE_GAP_OVERRIDES_TICKS: dict[tuple[int, int], tuple[int, int]] = {
    # Bank-approach pivot; hold longer before re-click to avoid spam retry loops.
    (3099, 3460): (18, 28),
    # Final approach to fish spot; hold longer before reissue.
    (3100, 3439): (16, 24),
    # Mid-route anchor where movement confirmation can lag.
    (3102, 3482): (14, 22),
}
WAYPOINT_ARRIVE_DISTANCE_OVERRIDES_TILES: dict[tuple[int, int], int] = {
    # Treat as arrived earlier to let route advance and reduce repeated dispatches.
    (3099, 3460): 3,
    (3100, 3439): 2,
}
WAYPOINT_MAX_DISPATCHES_PER_LEG_DEFAULT = 3
WAYPOINT_MAX_DISPATCHES_PER_LEG_OVERRIDES: dict[tuple[int, int], int] = {
    (3099, 3460): 3,
    (3100, 3439): 3,
    (3102, 3482): 4,
}
WAYPOINT_CONSECUTIVE_DISPATCH_HARD_CAP_DEFAULT = 4
WAYPOINT_CONSECUTIVE_DISPATCH_HARD_CAP_OVERRIDES: dict[tuple[int, int], int] = {
    (3099, 3460): 4,
    (3100, 3439): 4,
    (3102, 3482): 5,
}

FISHING_DROP_TUNING_BOUNDS: dict[str, tuple[int, int]] = {
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
FISHING_DROP_TUNING_KEYS: tuple[str, ...] = tuple(FISHING_DROP_TUNING_BOUNDS.keys())
FISHING_DROP_LOCAL_COOLDOWN_MIN_SCALE = 0.86
FISHING_DROP_LOCAL_COOLDOWN_MAX_SCALE = 0.84
FISHING_DROP_SECOND_DISPATCH_BONUS_PERCENT = 4
FISHING_DROP_SESSION_COOLDOWN_BIAS_DELTA_MS = -2
FISHING_DROP_RHYTHM_PAUSE_MAX_DELTA_PERCENT = -1
FISHING_MOTOR_TUNING_BOUNDS: dict[str, tuple[int, int]] = {
    "moveAccelPercent": (0, 100),
    "moveDecelPercent": (0, 100),
    "terminalSlowdownRadiusPx": (0, 260),
}
FISHING_MOTOR_TUNING_KEYS: tuple[str, ...] = tuple(FISHING_MOTOR_TUNING_BOUNDS.keys())


@dataclass
class FishingConfig:
    auto_drop_when_full: bool = True
    fish_item_ids: tuple[int, ...] = (335,)
    stop_when_inventory_full: bool = True
    target_npc_ids: tuple[int, ...] = ()
    enable_banking_loop: bool = False
    fishing_world_x: int = -1
    fishing_world_y: int = -1
    fishing_plane: int = 0
    fishing_area_radius_tiles: int = 3
    bank_world_x: int = -1
    bank_world_y: int = -1
    bank_plane: int = 0
    bank_area_radius_tiles: int = 3
    arrive_distance_tiles: int = 1
    waypoint_step_tiles: int = 6
    route_anchors: tuple[tuple[int, int, int], ...] = ()
    route_anchor_radius_tiles: int = 1
    tuning_profile: str = FISHING_PROFILE_DB_PARITY
    fishing_drop_tuning: dict[str, int] | None = None
    fishing_motor_tuning: dict[str, int] | None = None


@dataclass(frozen=True)
class RoutePoint:
    world_x: int
    world_y: int
    plane: int = 0


@dataclass
class FishingStrategy:
    cfg: FishingConfig
    _drop: DropSessionController = field(default_factory=DropSessionController)
    _drop_chain_active: bool = False
    _active_walk_leg: str = field(init=False, default="")
    _active_route: tuple[RoutePoint, ...] = field(init=False, default=())
    _active_destination: RoutePoint | None = field(init=False, default=None)
    _waypoint_index: int = field(init=False, default=0)
    _walk_arrive_distance: int = field(init=False, default=1)
    _walk_last_waypoint: RoutePoint | None = field(init=False, default=None)
    _walk_last_distance: int = field(init=False, default=9999)
    _walk_no_progress_ticks: int = field(init=False, default=0)
    _walk_no_progress_limit: int = field(init=False, default=5)
    _walk_no_progress_tick_limit: int = field(init=False, default=16)
    _walk_last_progress_tick: int = field(init=False, default=-1)
    _walk_next_issue_tick: int = field(init=False, default=-1)
    _walk_recovery_attempts: int = field(init=False, default=0)
    _walk_waypoint_budget_key: tuple[int, int, int] | None = field(init=False, default=None)
    _walk_waypoint_click_budget: int = field(init=False, default=1)
    _walk_waypoint_clicks_issued: int = field(init=False, default=0)
    _walk_last_dispatched_waypoint_key: tuple[int, int, int] | None = field(init=False, default=None)
    _walk_consecutive_dispatches: int = field(init=False, default=0)
    _walk_waypoint_total_dispatch_counts: Dict[tuple[int, int, int], int] = field(
        init=False,
        default_factory=dict,
    )
    _walk_waypoint_reissue_cooldown_until_tick: Dict[tuple[int, int, int], int] = field(
        init=False,
        default_factory=dict,
    )
    _fish_hold_until_tick: int = field(init=False, default=-1)
    _fish_outside_streak: int = field(init=False, default=0)
    _last_fish_command_tick: int = field(init=False, default=-1)
    _next_fish_retry_tick: int = field(init=False, default=-1)
    _suppress_walk_to_fish_until_tick: int = field(init=False, default=-1)
    _inventory_full_streak: int = field(init=False, default=0)
    _bank_reopen_block_until_tick: int = field(init=False, default=-1)
    _bank_open_pending_until_tick: int = field(init=False, default=-1)
    _bank_trip_use_inside_entry: bool | None = field(init=False, default=None)
    _fishing_center: RoutePoint = field(init=False)
    _bank_center: RoutePoint = field(init=False)
    _bank_outside_interact_point: RoutePoint | None = field(init=False, default=None)
    _route_anchors: tuple[RoutePoint, ...] = field(init=False, default=())
    _profile: FishingBehaviorProfile = field(init=False)
    _drop_cadence_tuning_payload: dict[str, int] | None = field(init=False, default=None)
    _idle_cadence_tuning_payload: dict[str, int] | None = field(init=False, default=None)
    _motor_tuning_payload: dict[str, int] | None = field(init=False, default=None)
    _runtime_warnings: list[str] = field(init=False, default_factory=list)
    _drop_tuning_block_warning_latched: bool = field(init=False, default=False)

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
            "strict_drop_tuning: blocked fishing drop start because dropCadenceTuning is missing"
        )

    def __post_init__(self) -> None:
        self._profile = resolve_fishing_behavior_profile(self.cfg.tuning_profile)
        if str(self._profile.drop_cadence_profile_key).strip().upper() == FISHING_PROFILE_DB_PARITY:
            self._drop_cadence_tuning_payload = resolve_drop_cadence_tuning_payload(
                activity_key=ACTIVITY_NAME,
                user_key="default_user",
            )
            self._drop_cadence_tuning_payload = apply_fishing_drop_speed_bias(self._drop_cadence_tuning_payload)
            self._idle_cadence_tuning_payload = resolve_idle_cadence_tuning_payload(
                activity_key=ACTIVITY_NAME,
                user_key="default_user",
            )
        else:
            self._drop_cadence_tuning_payload = None
            self._idle_cadence_tuning_payload = None
        drop_tuning_overrides = parse_drop_cadence_tuning_overrides(self.cfg.fishing_drop_tuning)
        if drop_tuning_overrides:
            merged_drop_tuning = dict(self._drop_cadence_tuning_payload or {})
            merged_drop_tuning.update(drop_tuning_overrides)
            self._drop_cadence_tuning_payload = merged_drop_tuning
        self._motor_tuning_payload = parse_fishing_motor_tuning_overrides(self.cfg.fishing_motor_tuning)
        self._fishing_center = RoutePoint(
            world_x=int(self.cfg.fishing_world_x),
            world_y=int(self.cfg.fishing_world_y),
            plane=max(0, int(self.cfg.fishing_plane)),
        )
        self._bank_center = RoutePoint(
            world_x=int(self.cfg.bank_world_x),
            world_y=int(self.cfg.bank_world_y),
            plane=max(0, int(self.cfg.bank_plane)),
        )
        outside_world_x = int(BANK_OUTSIDE_INTERACT_WORLD_X)
        outside_world_y = int(BANK_OUTSIDE_INTERACT_WORLD_Y)
        outside_plane = self._bank_center.plane
        if outside_world_x > 0 and outside_world_y > 0:
            self._bank_outside_interact_point = RoutePoint(
                world_x=outside_world_x,
                world_y=outside_world_y,
                plane=outside_plane,
            )
        else:
            self._bank_outside_interact_point = None
        anchors: list[RoutePoint] = []
        for raw_anchor in tuple(self.cfg.route_anchors):
            try:
                x_raw, y_raw, plane_raw = raw_anchor
                anchor_x = int(x_raw)
                anchor_y = int(y_raw)
                anchor_plane = max(0, int(plane_raw))
            except (TypeError, ValueError):
                continue
            if anchor_x <= 0 or anchor_y <= 0:
                continue
            anchor = RoutePoint(world_x=anchor_x, world_y=anchor_y, plane=anchor_plane)
            if not anchors or anchors[-1] != anchor:
                anchors.append(anchor)
        self._route_anchors = tuple(anchors)

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
        for configured_id in self.cfg.fish_item_ids:
            preferred = max(1, int(configured_id))
            if int(inventory_counts.get(preferred, 0)) > 0:
                return preferred
        for item_id in COMMON_FISH_ITEM_IDS:
            if int(inventory_counts.get(item_id, 0)) > 0:
                return item_id
        return None

    def _resolve_drop_item_ids(self, inventory_counts: Dict[int, int]) -> tuple[int, ...]:
        ordered_ids: list[int] = []
        seen: set[int] = set()

        for configured_id in self.cfg.fish_item_ids:
            candidate = max(1, int(configured_id))
            if candidate in seen:
                continue
            if int(inventory_counts.get(candidate, 0)) <= 0:
                continue
            seen.add(candidate)
            ordered_ids.append(candidate)

        for candidate in COMMON_FISH_ITEM_IDS:
            fish_id = int(candidate)
            if fish_id in seen:
                continue
            if int(inventory_counts.get(fish_id, 0)) <= 0:
                continue
            seen.add(fish_id)
            ordered_ids.append(fish_id)

        if ordered_ids:
            return tuple(ordered_ids)

        fallback: list[int] = []
        for configured_id in self.cfg.fish_item_ids:
            candidate = max(1, int(configured_id))
            if candidate in fallback:
                continue
            fallback.append(candidate)
        return tuple(fallback)

    @staticmethod
    def _inventory_slots_used(snapshot: Snapshot) -> Optional[int]:
        inventory_rows = snapshot.raw.get("inventory") if isinstance(snapshot.raw, dict) else None
        if isinstance(inventory_rows, list):
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
        if snapshot.inventory_slots_used is not None:
            return snapshot.inventory_slots_used
        return None

    @staticmethod
    def _is_animation_active(snapshot: Snapshot) -> bool:
        return snapshot.player_animation not in (None, -1, 0)

    @staticmethod
    def _player_world_position(snapshot: Snapshot) -> RoutePoint | None:
        raw = snapshot.raw if isinstance(snapshot.raw, dict) else {}
        player = raw.get("player")
        if not isinstance(player, dict):
            return None
        try:
            world_x = int(player.get("worldX", -1))
            world_y = int(player.get("worldY", -1))
            plane = int(player.get("plane", -1))
        except (TypeError, ValueError):
            return None
        if world_x <= 0 or world_y <= 0 or plane < 0:
            return None
        return RoutePoint(world_x=world_x, world_y=world_y, plane=plane)

    @staticmethod
    def _distance_chebyshev(a: RoutePoint, b: RoutePoint) -> int:
        if a.plane != b.plane:
            return 9999
        return max(abs(a.world_x - b.world_x), abs(a.world_y - b.world_y))

    @staticmethod
    def _is_within_circle(*, point: RoutePoint, center: RoutePoint, radius_tiles: int) -> bool:
        if point.plane != center.plane:
            return False
        radius = max(0, int(radius_tiles))
        dx = point.world_x - center.world_x
        dy = point.world_y - center.world_y
        return (dx * dx) + (dy * dy) <= (radius * radius)

    @staticmethod
    def _sample_destination_within_circle(*, center: RoutePoint, radius: int) -> RoutePoint:
        if radius <= 0:
            return center
        max_radius = max(0, int(radius))
        for _ in range(40):
            dx = random.randint(-max_radius, max_radius)
            dy = random.randint(-max_radius, max_radius)
            if (dx * dx) + (dy * dy) > (max_radius * max_radius):
                continue
            return RoutePoint(
                world_x=center.world_x + dx,
                world_y=center.world_y + dy,
                plane=center.plane,
            )
        return center

    @staticmethod
    def _build_route(*, start: RoutePoint, end: RoutePoint, step_tiles: int) -> tuple[RoutePoint, ...]:
        if start.world_x <= 0 or start.world_y <= 0 or end.world_x <= 0 or end.world_y <= 0:
            return ()
        if start.plane != end.plane:
            return (end,)
        dx = end.world_x - start.world_x
        dy = end.world_y - start.world_y
        max_axis = max(abs(dx), abs(dy))
        if max_axis <= 0:
            return (end,)
        step = max(1, int(step_tiles))
        segment_count = max(1, int(math.ceil(max_axis / float(step))))
        points: list[RoutePoint] = []
        for i in range(1, segment_count + 1):
            t = i / float(segment_count)
            x = int(round(start.world_x + (dx * t)))
            y = int(round(start.world_y + (dy * t)))
            point = RoutePoint(world_x=x, world_y=y, plane=start.plane)
            if not points or points[-1] != point:
                points.append(point)
        return tuple(points)

    def _clear_walk_route(self) -> None:
        self._active_walk_leg = ""
        self._active_route = ()
        self._active_destination = None
        self._waypoint_index = 0
        self._walk_arrive_distance = max(1, int(self.cfg.arrive_distance_tiles))
        self._walk_last_waypoint = None
        self._walk_last_distance = 9999
        self._walk_no_progress_ticks = 0
        self._walk_no_progress_limit = 5
        self._walk_no_progress_tick_limit = 16
        self._walk_last_progress_tick = -1
        self._walk_next_issue_tick = -1
        self._walk_recovery_attempts = 0
        self._walk_waypoint_budget_key = None
        self._walk_waypoint_click_budget = 1
        self._walk_waypoint_clicks_issued = 0
        self._walk_last_dispatched_waypoint_key = None
        self._walk_consecutive_dispatches = 0
        self._walk_waypoint_total_dispatch_counts.clear()

    def _sample_walk_arrive_distance(self) -> int:
        base = max(1, int(self.cfg.arrive_distance_tiles))
        jitter = random.randint(0, 1)
        return max(1, min(base + 1, base + jitter))

    def _reset_fish_engagement_state(self) -> None:
        self._fish_hold_until_tick = -1
        self._fish_outside_streak = 0
        self._last_fish_command_tick = -1
        self._next_fish_retry_tick = -1
        self._suppress_walk_to_fish_until_tick = -1

    def _set_fish_hold_window(self, tick: int) -> None:
        hold_ticks = random.randint(
            int(self._profile.fish_hold_min_ticks),
            int(self._profile.fish_hold_max_ticks),
        )
        hold_until = int(tick) + hold_ticks
        if hold_until > self._fish_hold_until_tick:
            self._fish_hold_until_tick = hold_until

    def _set_post_cast_walk_suppression_window(self, tick: int) -> None:
        suppress_ticks = random.randint(
            int(self._profile.fish_walk_suppress_after_cast_min_ticks),
            int(self._profile.fish_walk_suppress_after_cast_max_ticks),
        )
        suppress_until = int(tick) + suppress_ticks
        if suppress_until > self._suppress_walk_to_fish_until_tick:
            self._suppress_walk_to_fish_until_tick = suppress_until

    def _set_next_fish_retry_tick(self, tick: int) -> None:
        retry_gap = random.randint(
            int(self._profile.fish_reclick_min_ticks),
            int(self._profile.fish_reclick_max_ticks),
        )
        self._next_fish_retry_tick = int(tick) + retry_gap

    def _set_bank_reopen_block_window(self, tick: int) -> None:
        cooldown_ticks = random.randint(
            int(self._profile.bank_reopen_cooldown_after_close_min_ticks),
            int(self._profile.bank_reopen_cooldown_after_close_max_ticks),
        )
        block_until = int(tick) + cooldown_ticks
        if block_until > self._bank_reopen_block_until_tick:
            self._bank_reopen_block_until_tick = block_until

    def _set_bank_open_pending_window(self, tick: int) -> None:
        hold_ticks = random.randint(
            int(self._profile.bank_open_pending_hold_min_ticks),
            int(self._profile.bank_open_pending_hold_max_ticks),
        )
        pending_until = int(tick) + hold_ticks
        if pending_until > self._bank_open_pending_until_tick:
            self._bank_open_pending_until_tick = pending_until

    def _clear_bank_open_pending_window(self) -> None:
        self._bank_open_pending_until_tick = -1

    def _reset_bank_trip_entry_mode(self) -> None:
        self._bank_trip_use_inside_entry = None

    def _ensure_bank_trip_entry_mode(self) -> None:
        if self._bank_trip_use_inside_entry is not None:
            return
        inside_chance = max(0, min(100, int(self._profile.bank_inside_entry_chance_pct)))
        self._bank_trip_use_inside_entry = random.randint(1, 100) <= inside_chance

    def _outside_bank_entry_active(self) -> bool:
        return self._bank_trip_use_inside_entry is False and self._bank_outside_interact_point is not None

    def _has_any_fish_items(self, inventory_counts: Dict[int, int]) -> bool:
        seen_ids: set[int] = set()
        for item_id in tuple(self.cfg.fish_item_ids) + COMMON_FISH_ITEM_IDS:
            fish_id = int(item_id)
            if fish_id <= 0 or fish_id in seen_ids:
                continue
            seen_ids.add(fish_id)
            if int(inventory_counts.get(fish_id, 0)) > 0:
                return True
        return False

    @staticmethod
    def _sample_walk_issue_gap_ticks() -> int:
        # Avoid metronomic spam while keeping movement responsive.
        return random.randint(4, 9)

    @staticmethod
    def _sample_waypoint_click_budget() -> int:
        return 1

    def _sync_waypoint_click_budget(self, waypoint: RoutePoint) -> None:
        key = (int(waypoint.world_x), int(waypoint.world_y), int(waypoint.plane))
        if self._walk_waypoint_budget_key == key:
            return
        self._walk_waypoint_budget_key = key
        self._walk_waypoint_click_budget = self._sample_waypoint_click_budget()
        self._walk_waypoint_clicks_issued = 0

    @staticmethod
    def _resolve_waypoint_arrive_distance(waypoint: RoutePoint, base_arrive_distance: int) -> int:
        base = max(1, int(base_arrive_distance))
        override = WAYPOINT_ARRIVE_DISTANCE_OVERRIDES_TILES.get((int(waypoint.world_x), int(waypoint.world_y)))
        if override is None:
            return base
        return max(base, int(override))

    @staticmethod
    def _sample_waypoint_reissue_gap_ticks(waypoint: RoutePoint) -> int:
        key = (int(waypoint.world_x), int(waypoint.world_y))
        override = WAYPOINT_REISSUE_GAP_OVERRIDES_TICKS.get(key)
        if override is not None:
            min_ticks = max(1, int(override[0]))
            max_ticks = max(min_ticks, int(override[1]))
            return random.randint(min_ticks, max_ticks)
        if key in FISHING_SCENE_ONLY_WALK_POINTS:
            return random.randint(
                SCENE_ONLY_WAYPOINT_REISSUE_GAP_MIN_TICKS,
                SCENE_ONLY_WAYPOINT_REISSUE_GAP_MAX_TICKS,
            )
        return random.randint(
            WAYPOINT_REISSUE_GAP_MIN_TICKS,
            WAYPOINT_REISSUE_GAP_MAX_TICKS,
        )

    @staticmethod
    def _waypoint_max_dispatches_per_leg(waypoint: RoutePoint) -> int:
        key = (int(waypoint.world_x), int(waypoint.world_y))
        override = WAYPOINT_MAX_DISPATCHES_PER_LEG_OVERRIDES.get(key)
        if override is None:
            return max(1, int(WAYPOINT_MAX_DISPATCHES_PER_LEG_DEFAULT))
        return max(1, int(override))

    @staticmethod
    def _waypoint_consecutive_dispatch_hard_cap(waypoint: RoutePoint) -> int:
        key = (int(waypoint.world_x), int(waypoint.world_y))
        override = WAYPOINT_CONSECUTIVE_DISPATCH_HARD_CAP_OVERRIDES.get(key)
        if override is None:
            return max(1, int(WAYPOINT_CONSECUTIVE_DISPATCH_HARD_CAP_DEFAULT))
        return max(1, int(override))

    def _reset_walk_waypoint_runtime(self, tick: int) -> None:
        self._walk_last_waypoint = None
        self._walk_no_progress_ticks = 0
        self._walk_last_progress_tick = int(tick)
        self._walk_next_issue_tick = int(tick)
        self._walk_waypoint_budget_key = None
        self._walk_waypoint_click_budget = 1
        self._walk_waypoint_clicks_issued = 0

    @staticmethod
    def _combine_routes(first: tuple[RoutePoint, ...], second: tuple[RoutePoint, ...]) -> tuple[RoutePoint, ...]:
        if not first:
            return second
        if not second:
            return first
        out: list[RoutePoint] = list(first)
        for point in second:
            if out[-1] != point:
                out.append(point)
        return tuple(out)

    def _sample_point_near_anchor(self, anchor: RoutePoint) -> RoutePoint:
        radius = max(0, int(self.cfg.route_anchor_radius_tiles))
        if radius <= 0:
            return anchor
        return self._sample_destination_within_circle(center=anchor, radius=radius)

    def _anchor_chain_for_leg(self, *, leg: str, player: RoutePoint, target_center: RoutePoint) -> tuple[RoutePoint, ...]:
        if not self._route_anchors:
            return ()
        ordered = self._route_anchors if leg == "TO_BANK" else tuple(reversed(self._route_anchors))
        player_distance = self._distance_chebyshev(player, target_center)
        out: list[RoutePoint] = []
        for anchor in ordered:
            if anchor.plane != target_center.plane:
                continue
            if leg == "TO_BANK" and self._outside_bank_entry_active():
                bank_anchor_radius = max(1, int(self.cfg.bank_area_radius_tiles) + 1)
                if self._is_within_circle(
                    point=anchor,
                    center=self._bank_center,
                    radius_tiles=bank_anchor_radius,
                ):
                    continue
            anchor_distance = self._distance_chebyshev(anchor, target_center)
            # Keep anchors that move us generally toward the destination.
            if anchor_distance <= player_distance:
                sampled = self._sample_point_near_anchor(anchor)
                if not out or out[-1] != sampled:
                    out.append(sampled)
        return tuple(out)

    def _build_route_through_waypoints(
        self,
        *,
        start: RoutePoint,
        waypoints: Sequence[RoutePoint],
        step_tiles: int,
    ) -> tuple[RoutePoint, ...]:
        route: tuple[RoutePoint, ...] = ()
        cursor = start
        for waypoint in waypoints:
            segment = self._build_route(start=cursor, end=waypoint, step_tiles=step_tiles)
            route = self._combine_routes(route, segment)
            cursor = waypoint
        return route

    @staticmethod
    def _direct_route_through_waypoints(waypoints: Sequence[RoutePoint]) -> tuple[RoutePoint, ...]:
        out: list[RoutePoint] = []
        for waypoint in waypoints:
            if not out or out[-1] != waypoint:
                out.append(waypoint)
        return tuple(out)

    def _sample_detour_point(self, *, player: RoutePoint, center: RoutePoint) -> RoutePoint | None:
        current_distance = self._distance_chebyshev(player, center)
        for _ in range(24):
            dx = random.randint(-4, 4)
            dy = random.randint(-4, 4)
            if max(abs(dx), abs(dy)) < 2:
                continue
            detour = RoutePoint(
                world_x=player.world_x + dx,
                world_y=player.world_y + dy,
                plane=player.plane,
            )
            # Prefer a slight side-step that does not pull too far off-route.
            if self._distance_chebyshev(detour, center) <= current_distance + 3:
                return detour
        return None

    def _prune_backtracking_route(
        self,
        *,
        route: tuple[RoutePoint, ...],
        target_center: RoutePoint,
        start_distance: int,
    ) -> tuple[RoutePoint, ...]:
        if not route:
            return ()
        max_distance = max(0, int(start_distance))
        out: list[RoutePoint] = []
        for point in route:
            distance = self._distance_chebyshev(point, target_center)
            # Keep progression monotonic toward destination to avoid backward hops.
            if distance > max_distance:
                continue
            if not out or out[-1] != point:
                out.append(point)
            max_distance = distance
        if out:
            return tuple(out)
        # If all points were pruned, keep the closest route point to preserve forward progress.
        best = min(route, key=lambda p: self._distance_chebyshev(p, target_center))
        return (best,)

    def _plan_walk_route(
        self,
        *,
        player: RoutePoint,
        leg: str,
        target_center: RoutePoint,
        target_radius: int,
        tick: int,
        recovering: bool,
    ) -> None:
        if not recovering:
            self._walk_recovery_attempts = 0
        else:
            self._walk_recovery_attempts += 1

        self._active_walk_leg = leg
        self._active_destination = self._sample_destination_within_circle(
            center=target_center,
            radius=target_radius,
        )
        destination = self._active_destination
        if destination is None:
            self._active_route = ()
            return

        step_tiles = max(1, int(self.cfg.waypoint_step_tiles))
        if self._walk_recovery_attempts >= 2:
            step_tiles = max(1, step_tiles - 1)

        anchor_chain = self._anchor_chain_for_leg(
            leg=leg,
            player=player,
            target_center=target_center,
        )
        waypoints = list(anchor_chain) + [destination]
        has_anchor_route = bool(anchor_chain)
        if has_anchor_route:
            route = self._direct_route_through_waypoints(waypoints)
        else:
            route = self._build_route_through_waypoints(
                start=player,
                waypoints=waypoints,
                step_tiles=step_tiles,
            )
        if recovering and not has_anchor_route:
            detour = self._sample_detour_point(player=player, center=target_center)
            if detour is not None:
                detour_waypoints = [detour] + waypoints
                if has_anchor_route:
                    route = self._direct_route_through_waypoints(detour_waypoints)
                else:
                    detour_step = max(1, step_tiles - 1)
                    route = self._build_route_through_waypoints(
                        start=player,
                        waypoints=detour_waypoints,
                        step_tiles=detour_step,
                    )
        route = self._prune_backtracking_route(
            route=route,
            target_center=target_center,
            start_distance=self._distance_chebyshev(player, target_center),
        )

        self._active_route = route
        self._waypoint_index = 0
        self._walk_arrive_distance = self._sample_walk_arrive_distance()
        self._walk_last_waypoint = None
        self._walk_last_distance = 9999
        self._walk_no_progress_ticks = 0
        if has_anchor_route:
            self._walk_no_progress_limit = random.randint(6, 10)
            self._walk_no_progress_tick_limit = random.randint(20, 30)
        else:
            self._walk_no_progress_limit = random.randint(4, 7)
            self._walk_no_progress_tick_limit = random.randint(12, 18)
        self._walk_last_progress_tick = int(tick)
        self._walk_next_issue_tick = int(tick)
        self._walk_waypoint_budget_key = None
        self._walk_waypoint_click_budget = 1
        self._walk_waypoint_clicks_issued = 0

    def _walk_intents_for_leg(self, *, snapshot: Snapshot, player: RoutePoint, leg: str) -> Sequence[Intent]:
        if leg == "TO_BANK" and self._outside_bank_entry_active():
            target_center = self._bank_outside_interact_point or self._bank_center
            target_radius = max(1, int(BANK_OUTSIDE_APPROACH_RADIUS_TILES))
        else:
            target_center = self._bank_center if leg == "TO_BANK" else self._fishing_center
            target_radius = max(
                0,
                int(
                    self.cfg.bank_area_radius_tiles if leg == "TO_BANK" else self.cfg.fishing_area_radius_tiles
                ),
            )
        tick = int(snapshot.tick)
        if self._active_walk_leg != leg:
            self._walk_waypoint_total_dispatch_counts.clear()
            self._walk_last_dispatched_waypoint_key = None
            self._walk_consecutive_dispatches = 0
        if (
            self._active_walk_leg != leg
            or not self._active_route
            or self._active_destination is None
            or self._active_destination.plane != target_center.plane
        ):
            self._plan_walk_route(
                player=player,
                leg=leg,
                target_center=target_center,
                target_radius=target_radius,
                tick=tick,
                recovering=False,
            )

        if not self._active_route:
            self._clear_walk_route()
            return []

        current = self._active_route[self._waypoint_index]
        arrive_distance = self._resolve_waypoint_arrive_distance(current, self._walk_arrive_distance)
        if self._distance_chebyshev(player, current) <= arrive_distance:
            if self._waypoint_index + 1 < len(self._active_route):
                self._waypoint_index += 1
                current = self._active_route[self._waypoint_index]
                arrive_distance = self._resolve_waypoint_arrive_distance(current, self._walk_arrive_distance)
                self._reset_walk_waypoint_runtime(tick)
            else:
                self._clear_walk_route()
                return []

        distance_now = self._distance_chebyshev(player, current)
        if self._walk_last_waypoint is None or self._walk_last_waypoint != current:
            self._walk_last_waypoint = current
            self._walk_last_distance = distance_now
            self._walk_no_progress_ticks = 0
            self._walk_last_progress_tick = tick
        else:
            if distance_now < self._walk_last_distance:
                self._walk_last_distance = distance_now
                self._walk_no_progress_ticks = 0
                self._walk_last_progress_tick = tick
            else:
                self._walk_last_distance = distance_now
                self._walk_no_progress_ticks += 1

        no_progress_for = 0
        if self._walk_last_progress_tick >= 0:
            no_progress_for = tick - self._walk_last_progress_tick
        should_recover = (
            self._walk_no_progress_ticks >= self._walk_no_progress_limit
            or no_progress_for >= self._walk_no_progress_tick_limit
        )
        if should_recover:
            self._plan_walk_route(
                player=player,
                leg=leg,
                target_center=target_center,
                target_radius=target_radius,
                tick=tick,
                recovering=True,
            )
            if not self._active_route:
                self._clear_walk_route()
                return []
            current = self._active_route[self._waypoint_index]
            arrive_distance = self._resolve_waypoint_arrive_distance(current, self._walk_arrive_distance)

        if tick < self._walk_next_issue_tick:
            return []

        self._sync_waypoint_click_budget(current)
        current_key = (int(current.world_x), int(current.world_y), int(current.plane))
        consecutive_cap = self._waypoint_consecutive_dispatch_hard_cap(current)
        if (
            self._walk_last_dispatched_waypoint_key == current_key
            and self._walk_consecutive_dispatches >= consecutive_cap
        ):
            if leg == "TO_FISH" and self._waypoint_index + 1 < len(self._active_route):
                self._waypoint_index += 1
                current = self._active_route[self._waypoint_index]
                self._reset_walk_waypoint_runtime(tick)
                return []
            self._plan_walk_route(
                player=player,
                leg=leg,
                target_center=target_center,
                target_radius=target_radius,
                tick=tick,
                recovering=True,
            )
            return []
        current_dispatch_count = int(self._walk_waypoint_total_dispatch_counts.get(current_key, 0))
        current_dispatch_cap = self._waypoint_max_dispatches_per_leg(current)
        if current_dispatch_count >= current_dispatch_cap:
            if (
                leg == "TO_FISH"
                and self._waypoint_index + 1 < len(self._active_route)
            ):
                self._waypoint_index += 1
                current = self._active_route[self._waypoint_index]
                self._reset_walk_waypoint_runtime(tick)
                return []
            self._plan_walk_route(
                player=player,
                leg=leg,
                target_center=target_center,
                target_radius=target_radius,
                tick=tick,
                recovering=True,
            )
            return []
        reissue_cooldown_until = int(self._walk_waypoint_reissue_cooldown_until_tick.get(current_key, -1))
        if tick < reissue_cooldown_until:
            self._walk_next_issue_tick = max(self._walk_next_issue_tick, reissue_cooldown_until)
            return []
        if self._walk_waypoint_clicks_issued >= self._walk_waypoint_click_budget:
            self._walk_next_issue_tick = tick + random.randint(4, 7)
            return []

        self._walk_waypoint_clicks_issued += 1
        self._walk_waypoint_total_dispatch_counts[current_key] = current_dispatch_count + 1
        if self._walk_last_dispatched_waypoint_key == current_key:
            self._walk_consecutive_dispatches += 1
        else:
            self._walk_last_dispatched_waypoint_key = current_key
            self._walk_consecutive_dispatches = 1
        self._walk_next_issue_tick = tick + self._sample_walk_issue_gap_ticks()
        reissue_gap = self._sample_waypoint_reissue_gap_ticks(current)
        self._walk_waypoint_reissue_cooldown_until_tick[current_key] = tick + reissue_gap
        destination = self._active_destination
        destination_key = (
            f"{destination.world_x}:{destination.world_y}:{destination.plane}"
            if destination is not None
            else "unknown"
        )
        intent_key = f"{ACTIVITY_NAME}:WALK:{leg}:{destination_key}:{self._waypoint_index}"
        bank_scene_radius = max(
            1,
            int(self.cfg.bank_area_radius_tiles) + max(1, int(self.cfg.arrive_distance_tiles)) + BANK_SCENE_ONLY_BUFFER_TILES,
        )
        player_in_bank_scene_zone = self._is_within_circle(
            point=player,
            center=self._bank_center,
            radius_tiles=bank_scene_radius,
        )
        use_scene_click = (
            (
                (int(current.world_x), int(current.world_y)) in FISHING_SCENE_ONLY_WALK_POINTS
                and distance_now <= SCENE_ONLY_NEAR_DISTANCE_TILES
            )
            or player_in_bank_scene_zone
        )
        walk_click_mode = "SCENE" if use_scene_click else "MINIMAP"
        minimap_click_chance_pct = 0 if use_scene_click else 100
        return [
            Intent(
                intent_key=intent_key,
                activity=ACTIVITY_NAME,
                kind=IntentKind.WALK_TO_WORLDPOINT,
                target={"worldX": current.world_x, "worldY": current.world_y, "plane": current.plane},
                params={
                    "targetWorldX": current.world_x,
                    "targetWorldY": current.world_y,
                    "targetPlane": current.plane,
                    "arriveDistanceTiles": arrive_distance,
                    "walkClickMode": walk_click_mode,
                    "minimapClickChancePct": minimap_click_chance_pct,
                },
                policy_key="fishing_walk",
                reason="fishing_walk_waypoint",
            )
        ]

    def _banking_targets_valid(self) -> bool:
        return (
            int(self._fishing_center.world_x) > 0
            and int(self._fishing_center.world_y) > 0
            and int(self._bank_center.world_x) > 0
            and int(self._bank_center.world_y) > 0
        )

    def _banking_loop_intents(
        self,
        snapshot: Snapshot,
        inventory_counts: Dict[int, int],
        inv_full: bool,
    ) -> Sequence[Intent]:
        tick = int(snapshot.tick)
        player = self._player_world_position(snapshot)
        if player is None:
            return []
        if not self._banking_targets_valid():
            return []

        arrive_distance = max(0, int(self.cfg.arrive_distance_tiles))
        fish_radius = max(0, int(self.cfg.fishing_area_radius_tiles))
        bank_radius = max(0, int(self.cfg.bank_area_radius_tiles))

        if snapshot.bank_open:
            self._clear_bank_open_pending_window()
            self._clear_walk_route()
            self._reset_fish_engagement_state()
            seen_ids: set[int] = set()
            for item_id in tuple(self.cfg.fish_item_ids) + COMMON_FISH_ITEM_IDS:
                fish_id = int(item_id)
                if fish_id <= 0 or fish_id in seen_ids:
                    continue
                seen_ids.add(fish_id)
                if fish_id > 0 and int(inventory_counts.get(fish_id, 0)) > 0:
                    return [
                        Intent(
                            intent_key=f"{ACTIVITY_NAME}:DEPOSIT_ITEM:{fish_id}",
                            activity=ACTIVITY_NAME,
                            kind=IntentKind.DEPOSIT_ITEM,
                            target={"itemId": fish_id},
                            params={"itemId": fish_id, "quantity": "ALL"},
                            policy_key="fishing_bank_deposit",
                            reason="fishing_deposit_fish_item",
                        )
                    ]
            self._set_bank_reopen_block_window(tick)
            return [
                Intent(
                    intent_key=f"{ACTIVITY_NAME}:CLOSE_BANK",
                    activity=ACTIVITY_NAME,
                    kind=IntentKind.CLOSE_BANK,
                    target={},
                    params={},
                    policy_key="fishing_bank_close",
                    reason="fishing_close_bank_after_deposit",
                )
                    ]

        if inv_full and tick <= self._bank_open_pending_until_tick:
            return []
        if not inv_full:
            self._clear_bank_open_pending_window()

        if inv_full:
            self._reset_fish_engagement_state()
            self._ensure_bank_trip_entry_mode()
            fish_items_remaining = self._has_any_fish_items(inventory_counts)
            if self._outside_bank_entry_active():
                outside_point = self._bank_outside_interact_point
                if outside_point is not None:
                    outside_open_radius = max(1, int(BANK_OUTSIDE_APPROACH_RADIUS_TILES) + arrive_distance)
                    if not self._is_within_circle(
                        point=player,
                        center=outside_point,
                        radius_tiles=outside_open_radius,
                    ):
                        return self._walk_intents_for_leg(snapshot=snapshot, player=player, leg="TO_BANK")
                    if tick <= self._bank_reopen_block_until_tick:
                        if fish_items_remaining:
                            return []
                        self._reset_bank_trip_entry_mode()
                        return self._walk_intents_for_leg(snapshot=snapshot, player=player, leg="TO_FISH")
                    if not fish_items_remaining:
                        self._reset_bank_trip_entry_mode()
                        return self._walk_intents_for_leg(snapshot=snapshot, player=player, leg="TO_FISH")
                    self._clear_walk_route()
                    self._set_bank_open_pending_window(tick)
                    return [
                        Intent(
                            intent_key=f"{ACTIVITY_NAME}:OPEN_BANK:{self._bank_center.world_x}:{self._bank_center.world_y}",
                            activity=ACTIVITY_NAME,
                            kind=IntentKind.OPEN_BANK,
                            target={
                                "targetWorldX": self._bank_center.world_x,
                                "targetWorldY": self._bank_center.world_y,
                            },
                            params={
                                "targetWorldX": self._bank_center.world_x,
                                "targetWorldY": self._bank_center.world_y,
                            },
                            policy_key="fishing_bank_open",
                            reason="fishing_open_bank_from_outside_when_full",
                        )
                    ]
            if not self._is_within_circle(
                point=player,
                center=self._bank_center,
                radius_tiles=bank_radius + arrive_distance,
            ):
                return self._walk_intents_for_leg(snapshot=snapshot, player=player, leg="TO_BANK")
            if tick <= self._bank_reopen_block_until_tick:
                if fish_items_remaining:
                    return []
                self._reset_bank_trip_entry_mode()
                return self._walk_intents_for_leg(snapshot=snapshot, player=player, leg="TO_FISH")
            if not fish_items_remaining:
                self._reset_bank_trip_entry_mode()
                return self._walk_intents_for_leg(snapshot=snapshot, player=player, leg="TO_FISH")
            self._clear_walk_route()
            self._set_bank_open_pending_window(tick)
            return [
                Intent(
                    intent_key=f"{ACTIVITY_NAME}:OPEN_BANK:{self._bank_center.world_x}:{self._bank_center.world_y}",
                    activity=ACTIVITY_NAME,
                    kind=IntentKind.OPEN_BANK,
                    target={
                        "targetWorldX": self._bank_center.world_x,
                        "targetWorldY": self._bank_center.world_y,
                    },
                    params={
                        "targetWorldX": self._bank_center.world_x,
                        "targetWorldY": self._bank_center.world_y,
                    },
                    policy_key="fishing_bank_open",
                    reason="fishing_open_bank_when_full",
                )
            ]
        self._reset_bank_trip_entry_mode()

        fish_travel_radius = fish_radius + arrive_distance
        in_fish_zone = self._is_within_circle(
            point=player,
            center=self._fishing_center,
            radius_tiles=fish_travel_radius,
        )
        fish_distance = self._distance_chebyshev(player, self._fishing_center)
        far_override_distance = max(1, fish_travel_radius + FISH_FAR_OVERRIDE_TILES)
        hold_active = tick <= self._fish_hold_until_tick
        cast_settle_active = tick <= self._suppress_walk_to_fish_until_tick
        recent_fish_command_active = (
            self._last_fish_command_tick >= 0
            and (tick - self._last_fish_command_tick) <= FISH_RECENT_COMMAND_WALK_SUPPRESS_TICKS
        )
        recent_fish_reacquire_active = (
            self._last_fish_command_tick >= 0
            and (tick - self._last_fish_command_tick) <= FISH_REACQUIRE_IN_PLACE_TICKS
        )
        reacquire_distance = fish_travel_radius + FISH_REACQUIRE_EXTRA_DISTANCE_TILES
        can_reacquire_in_place = (
            recent_fish_reacquire_active
            and fish_distance <= reacquire_distance
        )
        fish_engagement_active = hold_active or cast_settle_active or recent_fish_command_active
        # Avoid fish/walk flapping: if a fish attempt was recent, fish owns the loop
        # and any stale TO_FISH route is dropped before it can re-dispatch.
        if self._active_walk_leg == "TO_FISH" and self._active_route:
            if fish_engagement_active or can_reacquire_in_place:
                self._clear_walk_route()
            else:
                terminal_waypoint = self._active_route[-1]
                terminal_arrive_distance = self._resolve_waypoint_arrive_distance(
                    terminal_waypoint,
                    self._walk_arrive_distance,
                )
                terminal_distance = self._distance_chebyshev(player, terminal_waypoint)
                if terminal_distance > terminal_arrive_distance:
                    walk_intents = self._walk_intents_for_leg(snapshot=snapshot, player=player, leg="TO_FISH")
                    if walk_intents:
                        return walk_intents
        if self._is_animation_active(snapshot):
            self._set_fish_hold_window(tick)
            self._fish_outside_streak = 0
            self._clear_walk_route()
            return []

        if in_fish_zone:
            self._fish_outside_streak = 0
        else:
            self._fish_outside_streak += 1
            should_force_walk = fish_distance > far_override_distance
            engagement_suppress_distance = (
                fish_travel_radius
                + FISH_NEAR_ZONE_SUPPRESS_BUFFER_TILES
                + FISH_ENGAGEMENT_SUPPRESS_EXTRA_DISTANCE_TILES
            )
            if fish_engagement_active and fish_distance <= engagement_suppress_distance:
                return []
            if should_force_walk:
                if (
                    fish_engagement_active
                    and self._fish_outside_streak < int(self._profile.fish_engagement_force_walk_streak_threshold)
                ):
                    return []
            elif fish_engagement_active or self._fish_outside_streak < int(self._profile.fish_outside_streak_threshold):
                return []
            if can_reacquire_in_place:
                self._clear_walk_route()
            else:
                return self._walk_intents_for_leg(snapshot=snapshot, player=player, leg="TO_FISH")

        self._clear_walk_route()
        if tick < self._next_fish_retry_tick:
            return []
        params = {"targetCategory": "NEAREST_FISHING_SPOT"}
        if self.cfg.target_npc_ids:
            params["targetNpcIds"] = [int(v) for v in self.cfg.target_npc_ids if int(v) > 0]
        self._last_fish_command_tick = tick
        self._set_fish_hold_window(tick)
        self._set_post_cast_walk_suppression_window(tick)
        self._set_next_fish_retry_tick(tick)
        return [
            Intent(
                intent_key=f"{ACTIVITY_NAME}:FISH_SPOT:NEAREST",
                activity=ACTIVITY_NAME,
                kind=IntentKind.FISH_SPOT,
                target={"targetCategory": "NEAREST_FISHING_SPOT"},
                params=params,
                policy_key="fishing_fish",
                reason="fishing_fish_nearest_spot",
            )
        ]

    def intents(self, snapshot: Snapshot) -> Sequence[Intent]:
        if not snapshot.logged_in:
            self._clear_walk_route()
            self._reset_fish_engagement_state()
            forced_stop = self._drop.stop_session(
                activity=ACTIVITY_NAME,
                policy_key="fishing_drop_session",
                reason="fishing_stop_drop_session_logged_out",
            )
            if forced_stop:
                return forced_stop
            return []
        slots_used = self._inventory_slots_used(snapshot)
        inventory_counts = self._inventory_counts(snapshot)
        if slots_used is None:
            slots_used = sum(1 for _, qty in inventory_counts.items() if int(qty) > 0)
        inv_full_raw = slots_used >= int(self._profile.inventory_full_slots)
        if inv_full_raw:
            self._inventory_full_streak += 1
        else:
            self._inventory_full_streak = 0
        inv_full = inv_full_raw and self._inventory_full_streak >= max(1, int(self._profile.inventory_full_confirm_ticks))

        if self.cfg.enable_banking_loop and self._banking_targets_valid():
            self._drop_chain_active = False
            forced_stop = self._drop.stop_session(
                activity=ACTIVITY_NAME,
                policy_key="fishing_drop_session",
                reason="fishing_stop_drop_session_banking_enabled",
            )
            if forced_stop:
                return forced_stop
            return self._banking_loop_intents(
                snapshot=snapshot,
                inventory_counts=inventory_counts,
                inv_full=inv_full,
            )

        if snapshot.bank_open:
            forced_stop = self._drop.stop_session(
                activity=ACTIVITY_NAME,
                policy_key="fishing_drop_session",
                reason="fishing_stop_drop_session_bank_open",
            )
            if forced_stop:
                return forced_stop
            return []

        drop_item_id = self._resolve_drop_item_id(inventory_counts)
        if bool(self.cfg.auto_drop_when_full and inv_full and drop_item_id is not None):
            self._drop_chain_active = True
        elif drop_item_id is None:
            self._drop_chain_active = False
        active_drop_item_id = drop_item_id
        if self._drop.session_active:
            session_item_id = int(self._drop.session_item_id or -1)
            session_item_remaining = session_item_id > 0 and int(inventory_counts.get(session_item_id, 0)) > 0
            if session_item_remaining:
                active_drop_item_id = session_item_id

        if not self.cfg.auto_drop_when_full:
            self._drop_chain_active = False
            forced_stop = self._drop.stop_session(
                activity=ACTIVITY_NAME,
                policy_key="fishing_drop_session",
                reason="fishing_stop_drop_session_disabled",
            )
            if forced_stop:
                return forced_stop

        drop_target_item_ids = self._resolve_drop_item_ids(inventory_counts)
        drop_start_requested = bool(
            self.cfg.auto_drop_when_full
            and drop_item_id is not None
            and (inv_full or self._drop_chain_active)
        )
        drop_tuning_available = bool(self._drop_cadence_tuning_payload)
        if not drop_start_requested or drop_tuning_available:
            self._drop_tuning_block_warning_latched = False
        if drop_start_requested and not drop_tuning_available:
            self._queue_drop_tuning_missing_warning_once()
            self._drop_chain_active = False
            forced_stop = self._drop.stop_session(
                activity=ACTIVITY_NAME,
                policy_key="fishing_drop_session",
                reason="fishing_stop_drop_session_missing_drop_cadence_tuning",
            )
            if forced_stop:
                return forced_stop
            if inv_full:
                return []
        drop_start_extra_params = {
            "itemIds": [int(v) for v in drop_target_item_ids if int(v) > 0],
            "dropCadenceProfile": str(self._profile.drop_cadence_profile_key).strip().upper(),
        }
        if self._drop_cadence_tuning_payload:
            drop_start_extra_params["dropCadenceTuning"] = dict(self._drop_cadence_tuning_payload)
        if self._idle_cadence_tuning_payload:
            drop_start_extra_params["idleCadenceTuning"] = dict(self._idle_cadence_tuning_payload)
        drop_intents = self._drop.step(
            snapshot,
            activity=ACTIVITY_NAME,
            policy_key="fishing_drop_session",
            start_condition=bool(
                drop_tuning_available
                and
                self.cfg.auto_drop_when_full
                and drop_item_id is not None
                and (inv_full or self._drop_chain_active)
            ),
            candidate_item_id=active_drop_item_id,
            start_reason="fishing_start_drop_session",
            stop_reason_prefix="fishing_stop_drop_session",
            switch_item_on_change=True,
            start_extra_params=drop_start_extra_params,
        )
        if drop_intents:
            return drop_intents
        if self._drop.session_active:
            return []

        if self.cfg.stop_when_inventory_full and inv_full:
            return []

        if not self._is_animation_active(snapshot):
            params = {"targetCategory": "NEAREST_FISHING_SPOT"}
            if self.cfg.target_npc_ids:
                params["targetNpcIds"] = [int(v) for v in self.cfg.target_npc_ids if int(v) > 0]
            if self._motor_tuning_payload:
                params.update(self._motor_tuning_payload)
            return [
                Intent(
                    intent_key=f"{ACTIVITY_NAME}:FISH_SPOT:NEAREST",
                    activity=ACTIVITY_NAME,
                    kind=IntentKind.FISH_SPOT,
                    target={"targetCategory": "NEAREST_FISHING_SPOT"},
                    params=params,
                    policy_key="fishing_fish",
                    reason="fishing_fish_nearest_spot",
                )
            ]
        return []


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


def _clamp_fishing_drop_tuning_value(key: str, value: int) -> int:
    bounds = FISHING_DROP_TUNING_BOUNDS.get(key)
    if bounds is None:
        return int(value)
    min_value, max_value = bounds
    lo = int(min(min_value, max_value))
    hi = int(max(min_value, max_value))
    return max(lo, min(hi, int(value)))


def apply_fishing_drop_speed_bias(raw: Mapping[str, object] | None) -> dict[str, int] | None:
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
        tuned["localCooldownMinMs"] = _clamp_fishing_drop_tuning_value(
            "localCooldownMinMs",
            int(round(tuned["localCooldownMinMs"] * FISHING_DROP_LOCAL_COOLDOWN_MIN_SCALE)),
        )
    if "localCooldownMaxMs" in tuned:
        tuned["localCooldownMaxMs"] = _clamp_fishing_drop_tuning_value(
            "localCooldownMaxMs",
            int(round(tuned["localCooldownMaxMs"] * FISHING_DROP_LOCAL_COOLDOWN_MAX_SCALE)),
        )
    if "localCooldownMinMs" in tuned and "localCooldownMaxMs" in tuned:
        tuned["localCooldownMaxMs"] = max(tuned["localCooldownMinMs"], tuned["localCooldownMaxMs"])

    if "secondDispatchChancePercent" in tuned:
        tuned["secondDispatchChancePercent"] = _clamp_fishing_drop_tuning_value(
            "secondDispatchChancePercent",
            tuned["secondDispatchChancePercent"] + FISHING_DROP_SECOND_DISPATCH_BONUS_PERCENT,
        )
    if "sessionCooldownBiasMs" in tuned:
        tuned["sessionCooldownBiasMs"] = _clamp_fishing_drop_tuning_value(
            "sessionCooldownBiasMs",
            tuned["sessionCooldownBiasMs"] + FISHING_DROP_SESSION_COOLDOWN_BIAS_DELTA_MS,
        )
    if "rhythmPauseChanceMaxPercent" in tuned:
        tuned["rhythmPauseChanceMaxPercent"] = _clamp_fishing_drop_tuning_value(
            "rhythmPauseChanceMaxPercent",
            tuned["rhythmPauseChanceMaxPercent"] + FISHING_DROP_RHYTHM_PAUSE_MAX_DELTA_PERCENT,
        )
        if "rhythmPauseChanceMinPercent" in tuned:
            tuned["rhythmPauseChanceMaxPercent"] = max(
                tuned["rhythmPauseChanceMinPercent"],
                tuned["rhythmPauseChanceMaxPercent"],
            )

    return tuned


def parse_item_ids(raw: object) -> list[int]:
    text = "" if raw is None else str(raw).strip()
    if not text:
        return []
    normalized = text.replace("[", " ").replace("]", " ").replace(";", ",")
    out: list[int] = []
    for token in normalized.replace(",", " ").split():
        try:
            item_id = int(token)
        except ValueError:
            continue
        if item_id > 0 and item_id not in out:
            out.append(item_id)
    return out


def parse_route_anchors(raw: object, *, default_plane: int = 0) -> list[tuple[int, int, int]]:
    text = "" if raw is None else str(raw).strip()
    if not text:
        return []
    normalized = text.replace("|", ";").replace("\n", ";")
    out: list[tuple[int, int, int]] = []
    seen: set[tuple[int, int, int]] = set()
    for chunk in normalized.split(";"):
        token = chunk.strip()
        if not token:
            continue
        values: list[int] = []
        for part in token.replace(",", " ").split():
            try:
                values.append(int(part))
            except ValueError:
                values = []
                break
        if len(values) < 2:
            continue
        x = int(values[0])
        y = int(values[1])
        plane = max(0, int(values[2])) if len(values) >= 3 else max(0, int(default_plane))
        if x <= 0 or y <= 0:
            continue
        point = (x, y, plane)
        if point in seen:
            continue
        seen.add(point)
        out.append(point)
    return out


def parse_drop_cadence_tuning_overrides(raw: object) -> dict[str, int] | None:
    if raw is None:
        return None
    if isinstance(raw, Mapping):
        return _sanitize_drop_cadence_tuning_mapping(raw)
    text = str(raw).strip()
    if not text:
        return None
    mapping: Mapping[str, object]
    if text.startswith("{"):
        try:
            parsed = json.loads(text)
        except json.JSONDecodeError as exc:
            raise ValueError(f"Invalid JSON fishing drop tuning block: {exc.msg}") from exc
        if not isinstance(parsed, dict):
            raise ValueError("Fishing drop tuning JSON must be an object")
        mapping = parsed
    else:
        parsed_pairs: dict[str, object] = {}
        for token in text.replace(";", ",").split(","):
            part = token.strip()
            if not part:
                continue
            if "=" not in part:
                raise ValueError(f"Invalid fishing drop tuning token: {part!r}")
            key, value = part.split("=", 1)
            parsed_pairs[key.strip()] = value.strip()
        mapping = parsed_pairs
    return _sanitize_drop_cadence_tuning_mapping(mapping)


def _sanitize_drop_cadence_tuning_mapping(raw: Mapping[str, object]) -> dict[str, int] | None:
    out: dict[str, int] = {}
    unknown_keys = sorted(set(str(key) for key in raw.keys()) - set(FISHING_DROP_TUNING_KEYS))
    if unknown_keys:
        raise ValueError(
            "Unsupported fishing drop tuning key(s): " + ", ".join(unknown_keys)
        )
    for key, bounds in FISHING_DROP_TUNING_BOUNDS.items():
        if key not in raw:
            continue
        min_value, max_value = bounds
        try:
            parsed = int(raw[key])
        except (TypeError, ValueError) as exc:
            raise ValueError(f"Fishing drop tuning key {key!r} must be an integer") from exc
        out[key] = max(min_value, min(max_value, parsed))
    if "localCooldownMinMs" in out and "localCooldownMaxMs" in out:
        out["localCooldownMaxMs"] = max(out["localCooldownMinMs"], out["localCooldownMaxMs"])
    if "rhythmPauseChanceMinPercent" in out and "rhythmPauseChanceMaxPercent" in out:
        out["rhythmPauseChanceMaxPercent"] = max(
            out["rhythmPauseChanceMinPercent"],
            out["rhythmPauseChanceMaxPercent"],
        )
    return out or None


def parse_fishing_motor_tuning_overrides(raw: object) -> dict[str, int] | None:
    if raw is None:
        return None
    if isinstance(raw, Mapping):
        return _sanitize_fishing_motor_tuning_mapping(raw)
    text = str(raw).strip()
    if not text:
        return None
    mapping: Mapping[str, object]
    if text.startswith("{"):
        try:
            parsed = json.loads(text)
        except json.JSONDecodeError as exc:
            raise ValueError(f"Invalid JSON fishing motor tuning block: {exc.msg}") from exc
        if not isinstance(parsed, dict):
            raise ValueError("Fishing motor tuning JSON must be an object")
        mapping = parsed
    else:
        parsed_pairs: dict[str, object] = {}
        for token in text.replace(";", ",").split(","):
            part = token.strip()
            if not part:
                continue
            if "=" not in part:
                raise ValueError(f"Invalid fishing motor tuning token: {part!r}")
            key, value = part.split("=", 1)
            parsed_pairs[key.strip()] = value.strip()
        mapping = parsed_pairs
    return _sanitize_fishing_motor_tuning_mapping(mapping)


def _sanitize_fishing_motor_tuning_mapping(raw: Mapping[str, object]) -> dict[str, int] | None:
    out: dict[str, int] = {}
    unknown_keys = sorted(set(str(key) for key in raw.keys()) - set(FISHING_MOTOR_TUNING_KEYS))
    if unknown_keys:
        raise ValueError("Unsupported fishing motor tuning key(s): " + ", ".join(unknown_keys))
    for key, bounds in FISHING_MOTOR_TUNING_BOUNDS.items():
        if key not in raw:
            continue
        min_value, max_value = bounds
        try:
            parsed = int(raw[key])
        except (TypeError, ValueError) as exc:
            raise ValueError(f"Fishing motor tuning key {key!r} must be an integer") from exc
        out[key] = max(min_value, min(max_value, parsed))
    return out or None


def build_strategy(args: argparse.Namespace) -> RuntimeStrategy:
    target_npc_ids = parse_npc_ids(getattr(args, "fishing_target_npc_id", ""))
    fish_item_ids = parse_item_ids(getattr(args, "fishing_fish_item_ids", ""))
    fishing_plane = max(0, int(getattr(args, "fishing_plane", 0)))
    route_anchors = parse_route_anchors(
        getattr(args, "fishing_route_anchors", ""),
        default_plane=fishing_plane,
    )
    drop_tuning_overrides = parse_drop_cadence_tuning_overrides(
        getattr(args, "fishing_drop_tuning", ""),
    )
    motor_tuning_overrides = parse_fishing_motor_tuning_overrides(
        getattr(args, "fishing_motor_tuning", ""),
    )
    if not fish_item_ids:
        fish_item_ids = [max(1, int(getattr(args, "fishing_fish_item_id", 335)))]
    return FishingStrategy(
        cfg=FishingConfig(
            auto_drop_when_full=bool(getattr(args, "fishing_auto_drop_when_full", True)),
            fish_item_ids=tuple(fish_item_ids),
            stop_when_inventory_full=bool(getattr(args, "fishing_stop_when_inventory_full", True)),
            target_npc_ids=tuple(target_npc_ids),
            enable_banking_loop=bool(getattr(args, "fishing_enable_banking_loop", False)),
            fishing_world_x=int(getattr(args, "fishing_world_x", -1)),
            fishing_world_y=int(getattr(args, "fishing_world_y", -1)),
            fishing_plane=fishing_plane,
            fishing_area_radius_tiles=max(0, int(getattr(args, "fishing_area_radius_tiles", 3))),
            bank_world_x=int(getattr(args, "fishing_bank_world_x", -1)),
            bank_world_y=int(getattr(args, "fishing_bank_world_y", -1)),
            bank_plane=max(0, int(getattr(args, "fishing_bank_plane", 0))),
            bank_area_radius_tiles=max(0, int(getattr(args, "fishing_bank_area_radius_tiles", 3))),
            arrive_distance_tiles=max(0, int(getattr(args, "fishing_arrive_distance_tiles", 1))),
            waypoint_step_tiles=max(1, int(getattr(args, "fishing_waypoint_step_tiles", 6))),
            route_anchors=tuple(route_anchors),
            route_anchor_radius_tiles=max(0, int(getattr(args, "fishing_route_anchor_radius_tiles", 1))),
            tuning_profile=str(
                getattr(args, "fishing_tuning_profile", FISHING_PROFILE_DB_PARITY) or FISHING_PROFILE_DB_PARITY
            ),
            fishing_drop_tuning=drop_tuning_overrides,
            fishing_motor_tuning=motor_tuning_overrides,
        )
    )


def add_args(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--fishing-fish-item-id", type=int, default=335)
    parser.add_argument("--fishing-fish-item-ids", type=str, default="")
    parser.add_argument("--fishing-target-npc-id", type=str, default="")
    parser.add_argument(
        "--fishing-tuning-profile",
        type=str,
        default=FISHING_PROFILE_DB_PARITY,
        choices=(FISHING_PROFILE_DB_PARITY,),
    )
    parser.add_argument("--fishing-auto-drop-when-full", action="store_true", default=True)
    parser.add_argument(
        "--fishing-no-auto-drop-when-full",
        action="store_false",
        dest="fishing_auto_drop_when_full",
    )
    parser.add_argument("--fishing-stop-when-inventory-full", action="store_true", default=True)
    parser.add_argument(
        "--fishing-no-stop-when-inventory-full",
        action="store_false",
        dest="fishing_stop_when_inventory_full",
    )
    parser.add_argument("--fishing-enable-banking-loop", action="store_true", default=False)
    parser.add_argument("--fishing-world-x", type=int, default=-1)
    parser.add_argument("--fishing-world-y", type=int, default=-1)
    parser.add_argument("--fishing-plane", type=int, default=0)
    parser.add_argument("--fishing-area-radius-tiles", type=int, default=3)
    parser.add_argument("--fishing-bank-world-x", type=int, default=-1)
    parser.add_argument("--fishing-bank-world-y", type=int, default=-1)
    parser.add_argument("--fishing-bank-plane", type=int, default=0)
    parser.add_argument("--fishing-bank-area-radius-tiles", type=int, default=3)
    parser.add_argument("--fishing-arrive-distance-tiles", type=int, default=1)
    parser.add_argument("--fishing-waypoint-step-tiles", type=int, default=6)
    parser.add_argument("--fishing-route-anchors", type=str, default="")
    parser.add_argument("--fishing-route-anchor-radius-tiles", type=int, default=1)
    parser.add_argument(
        "--fishing-drop-tuning",
        type=str,
        default="",
        help=(
            "Fishing-only drop cadence overrides as JSON object or key=value list. "
            f"Supported keys: {', '.join(FISHING_DROP_TUNING_KEYS)}"
        ),
    )
    parser.add_argument(
        "--fishing-motor-tuning",
        type=str,
        default="",
        help=(
            "Fishing-only motor easing overrides as JSON object or key=value list. "
            f"Supported keys: {', '.join(FISHING_MOTOR_TUNING_KEYS)}"
        ),
    )

