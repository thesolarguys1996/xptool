from __future__ import annotations

import argparse
import random
from dataclasses import dataclass, field
from typing import Sequence

from .runtime_strategy import RuntimeStrategy
from .runtime_core.models import Intent, IntentKind
from .models import Snapshot

ACTIVITY_NAME = "agility"

AGILITY_TUNING_PROFILE_DB_PARITY = "DB_PARITY"
AGILITY_OUTSIDE_BUFFER_TILES = 2
AGILITY_START_RADIUS_TILES_DEFAULT = 6
AGILITY_ROUTE_ADVANCE_DISTANCE_TILES = 6
AGILITY_ROUTE_DISTANCE_BIAS_TILES = 2
AGILITY_STEP_TARGET_RADIUS_TILES_DEFAULT = 3
AGILITY_STEP_APPROACH_EXTRA_DISTANCE_TILES = 6
AGILITY_STEP_ACTION_MAX_DISTANCE_FLOOR = 14
AGILITY_STEP_WALK_TRIGGER_DISTANCE_FLOOR = 16
AGILITY_MARK_OF_GRACE_ITEM_ID = 11849
AGILITY_MARK_NAME_TOKEN = "mark of grace"
AGILITY_MARK_PICKUP_MAX_DISTANCE_TILES = 12
AGILITY_MARK_PICKUP_READY_DISTANCE_TILES = 5
AGILITY_MARK_RETRY_MIN_TICKS = 8
AGILITY_MARK_RETRY_MAX_TICKS = 12


@dataclass(frozen=True)
class AgilityTuning:
    profile_key: str
    outside_buffer_tiles: int
    route_advance_distance_tiles: int
    route_distance_bias_tiles: int
    step_approach_extra_distance_tiles: int
    step_action_max_distance_floor: int
    step_walk_trigger_distance_floor: int
    mark_pickup_ready_distance_tiles: int
    mark_retry_min_ticks: int
    mark_retry_max_ticks: int
    step_retry_block_min_ticks: int
    step_retry_block_max_ticks: int
    step_no_progress_retry_ticks: int


AGILITY_TUNING_DB_PARITY = AgilityTuning(
    profile_key=AGILITY_TUNING_PROFILE_DB_PARITY,
    outside_buffer_tiles=3,
    route_advance_distance_tiles=7,
    route_distance_bias_tiles=3,
    step_approach_extra_distance_tiles=8,
    step_action_max_distance_floor=16,
    step_walk_trigger_distance_floor=18,
    mark_pickup_ready_distance_tiles=6,
    mark_retry_min_ticks=10,
    mark_retry_max_ticks=14,
    step_retry_block_min_ticks=2,
    step_retry_block_max_ticks=4,
    step_no_progress_retry_ticks=5,
)


@dataclass(frozen=True)
class RoutePoint:
    world_x: int
    world_y: int
    plane: int = 0


@dataclass(frozen=True)
class ObstacleStep:
    world_x: int
    world_y: int
    plane: int = 0
    option_keywords: tuple[str, ...] = ()
    object_name_contains: str = ""
    object_id: int = -1
    target_radius_tiles: int = -1


@dataclass(frozen=True)
class GroundItemPoint:
    item_id: int
    item_name: str
    world_x: int
    world_y: int
    plane: int
    distance_tiles: int


@dataclass
class AgilityConfig:
    start_world_x: int = -1
    start_world_y: int = -1
    start_plane: int = 0
    start_radius_tiles: int = AGILITY_START_RADIUS_TILES_DEFAULT
    course_world_x: int = -1
    course_world_y: int = -1
    course_plane: int = 0
    course_radius_tiles: int = 12
    walk_arrive_distance_tiles: int = 2
    walk_click_mode: str = "MIXED"
    minimap_click_chance_pct: int = 93
    walk_to_course_when_outside: bool = True
    max_obstacle_distance_tiles: int = 10
    obstacle_target_radius_tiles: int = AGILITY_STEP_TARGET_RADIUS_TILES_DEFAULT
    obstacle_option_keywords: tuple[str, ...] = (
        "climb",
        "cross",
        "jump",
        "balance",
        "squeeze",
        "swing",
        "vault",
    )
    target_object_name_contains: str = ""
    target_object_id: int = -1
    obstacle_route: tuple[ObstacleStep, ...] = ()
    tuning_profile: str = AGILITY_TUNING_PROFILE_DB_PARITY


@dataclass
class AgilityStrategy:
    cfg: AgilityConfig
    _entry_confirmed: bool = field(init=False, default=False)
    _course_start: RoutePoint | None = field(init=False, default=None)
    _course_center: RoutePoint | None = field(init=False, default=None)
    _obstacle_route: tuple[ObstacleStep, ...] = field(init=False, default=())
    _next_mark_retry_tick: int = field(init=False, default=-1)
    _tuning: AgilityTuning = field(init=False)
    _step_retry_signature: str = field(init=False, default="")
    _step_retry_block_until_tick: int = field(init=False, default=-1)
    _step_no_progress_streak: int = field(init=False, default=0)
    _step_last_distance: int = field(init=False, default=9999)

    def __post_init__(self) -> None:
        self._tuning = _resolve_agility_tuning_profile(self.cfg.tuning_profile)
        start_x = int(self.cfg.start_world_x)
        start_y = int(self.cfg.start_world_y)
        start_plane = max(0, int(self.cfg.start_plane))
        if start_x > 0 and start_y > 0:
            self._course_start = RoutePoint(world_x=start_x, world_y=start_y, plane=start_plane)

        x = int(self.cfg.course_world_x)
        y = int(self.cfg.course_world_y)
        plane = max(0, int(self.cfg.course_plane))
        if x > 0 and y > 0:
            self._course_center = RoutePoint(world_x=x, world_y=y, plane=plane)
        if self._course_start is None:
            self._course_start = self._course_center
        default_keywords = self._bounded_option_keywords(tuple(self.cfg.obstacle_option_keywords))
        self._obstacle_route = self._sanitize_obstacle_route(tuple(self.cfg.obstacle_route), default_keywords)

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
    def _is_animation_active(snapshot: Snapshot) -> bool:
        return snapshot.player_animation not in (None, -1, 0)

    @staticmethod
    def _distance_chebyshev(a: RoutePoint, b: RoutePoint) -> int:
        if a.plane != b.plane:
            return 9999
        return max(abs(a.world_x - b.world_x), abs(a.world_y - b.world_y))

    @staticmethod
    def _distance_2d_chebyshev(a: RoutePoint, b: RoutePoint) -> int:
        return max(abs(a.world_x - b.world_x), abs(a.world_y - b.world_y))

    @staticmethod
    def _is_within_course(*, point: RoutePoint, center: RoutePoint, radius_tiles: int) -> bool:
        if point.plane != center.plane:
            return False
        radius = max(0, int(radius_tiles))
        dx = point.world_x - center.world_x
        dy = point.world_y - center.world_y
        return (dx * dx) + (dy * dy) <= (radius * radius)

    @staticmethod
    def _normalize_walk_click_mode(raw: str) -> str:
        normalized = (raw or "").strip().upper()
        if normalized not in {"MIXED", "SCENE", "MINIMAP"}:
            return "MIXED"
        return normalized

    @staticmethod
    def _bounded_option_keywords(values: tuple[str, ...]) -> tuple[str, ...]:
        out: list[str] = []
        seen: set[str] = set()
        for raw in values:
            token = str(raw).strip().lower()
            if not token or token in seen:
                continue
            seen.add(token)
            out.append(token)
        if not out:
            return ("climb",)
        return tuple(out)

    @staticmethod
    def _sanitize_obstacle_route(
        route: tuple[ObstacleStep, ...],
        default_keywords: tuple[str, ...],
    ) -> tuple[ObstacleStep, ...]:
        out: list[ObstacleStep] = []
        for step in route:
            if step is None:
                continue
            x = int(step.world_x)
            y = int(step.world_y)
            plane = max(0, int(step.plane))
            if x <= 0 or y <= 0:
                continue
            keywords = tuple(step.option_keywords) if step.option_keywords else tuple(default_keywords)
            normalized_keywords = AgilityStrategy._bounded_option_keywords(tuple(keywords))
            out.append(
                ObstacleStep(
                    world_x=x,
                    world_y=y,
                    plane=plane,
                    option_keywords=normalized_keywords,
                    object_name_contains=str(step.object_name_contains or "").strip(),
                    object_id=int(step.object_id),
                    target_radius_tiles=int(step.target_radius_tiles),
                )
            )
        return tuple(out)

    def _set_next_mark_retry_tick(self, tick: int) -> None:
        retry_min = max(0, int(self._tuning.mark_retry_min_ticks))
        retry_max = max(retry_min, int(self._tuning.mark_retry_max_ticks))
        self._next_mark_retry_tick = tick + random.randint(
            retry_min,
            retry_max,
        )

    def _step_point(self, step: ObstacleStep) -> RoutePoint:
        return RoutePoint(world_x=int(step.world_x), world_y=int(step.world_y), plane=max(0, int(step.plane)))

    def _resolve_step_target_radius(self, step: ObstacleStep | None) -> int:
        if step is not None:
            step_radius = int(step.target_radius_tiles)
            if step_radius > 0:
                return max(1, step_radius)
        return max(1, int(self.cfg.obstacle_target_radius_tiles))

    def _should_issue_step_action(
        self,
        *,
        tick: int,
        step: ObstacleStep,
        route_index: int,
        distance_tiles: int,
    ) -> bool:
        signature = f"{route_index}:{int(step.world_x)}:{int(step.world_y)}:{int(step.plane)}"
        if signature != self._step_retry_signature:
            self._step_retry_signature = signature
            self._step_retry_block_until_tick = -1
            self._step_no_progress_streak = 0
            self._step_last_distance = int(distance_tiles)
            return True

        if int(distance_tiles) < int(self._step_last_distance):
            self._step_no_progress_streak = 0
        else:
            self._step_no_progress_streak += 1
        self._step_last_distance = int(distance_tiles)

        if self._step_retry_block_until_tick >= 0 and tick < self._step_retry_block_until_tick:
            retry_after_no_progress = max(0, int(self._tuning.step_no_progress_retry_ticks))
            if self._step_no_progress_streak < retry_after_no_progress:
                return False
        return True

    def _mark_step_action_issued(self, *, tick: int, distance_tiles: int) -> None:
        self._step_no_progress_streak = 0
        self._step_last_distance = int(distance_tiles)
        block_min = max(0, int(self._tuning.step_retry_block_min_ticks))
        block_max = max(block_min, int(self._tuning.step_retry_block_max_ticks))
        if block_max <= 0:
            self._step_retry_block_until_tick = int(tick)
            return
        self._step_retry_block_until_tick = int(tick) + random.randint(block_min, block_max)

    def _nearest_route_step_on_player_plane(self, player: RoutePoint) -> tuple[int, int]:
        if player is None or not self._obstacle_route:
            return (-1, 9999)
        best_idx = -1
        best_dist = 9999
        for idx, step in enumerate(self._obstacle_route):
            step_point = self._step_point(step)
            if step_point.plane != player.plane:
                continue
            dist = self._distance_2d_chebyshev(player, step_point)
            if dist < best_dist:
                best_dist = dist
                best_idx = idx
        return (best_idx, best_dist)

    def _resolve_active_route_step_index(self, player: RoutePoint) -> int:
        if player is None or not self._obstacle_route:
            return -1
        route_count = len(self._obstacle_route)
        nearest_idx, nearest_dist = self._nearest_route_step_on_player_plane(player)
        if nearest_idx >= 0:
            # Stateless forward bias: when we are effectively between two sequential same-plane
            # steps, prefer the next step so dispatch authority stays in the executor.
            next_idx = (nearest_idx + 1) % route_count
            next_step = self._obstacle_route[next_idx]
            if int(next_step.plane) == int(player.plane):
                next_dist = self._distance_2d_chebyshev(player, self._step_point(next_step))
                if (
                    next_dist <= int(self._tuning.route_advance_distance_tiles)
                    and next_dist <= (nearest_dist + int(self._tuning.route_distance_bias_tiles))
                ):
                    return next_idx
            return nearest_idx

        # No same-plane step visible (e.g. rooftop transition). Choose nearest overall.
        best_idx = -1
        best_dist = 9999
        for idx, step in enumerate(self._obstacle_route):
            dist = self._distance_2d_chebyshev(player, self._step_point(step))
            if dist < best_dist:
                best_dist = dist
                best_idx = idx
        return best_idx

    def _walk_to_course_intent(self, target_point: RoutePoint, *, reason: str) -> Intent:
        walk_click_mode = self._normalize_walk_click_mode(self.cfg.walk_click_mode)
        minimap_click_chance_pct = max(0, min(100, int(self.cfg.minimap_click_chance_pct)))
        arrive_distance_tiles = max(0, int(self.cfg.walk_arrive_distance_tiles))
        return Intent(
            intent_key=f"{ACTIVITY_NAME}:WALK_TO_COURSE:{target_point.world_x}:{target_point.world_y}:{target_point.plane}",
            activity=ACTIVITY_NAME,
            kind=IntentKind.WALK_TO_WORLDPOINT,
            target={
                "worldX": target_point.world_x,
                "worldY": target_point.world_y,
                "plane": target_point.plane,
            },
            params={
                "targetWorldX": target_point.world_x,
                "targetWorldY": target_point.world_y,
                "targetPlane": target_point.plane,
                "arriveDistanceTiles": arrive_distance_tiles,
                "walkClickMode": walk_click_mode,
                "minimapClickChancePct": minimap_click_chance_pct,
            },
            policy_key="agility_walk_course",
            reason=reason,
        )

    def _scene_object_intent(self, step: ObstacleStep | None = None, *, step_index: int = -1) -> Intent:
        keywords = self._bounded_option_keywords(tuple(self.cfg.obstacle_option_keywords))
        if step is not None and step.option_keywords:
            keywords = self._bounded_option_keywords(tuple(step.option_keywords))
        params: dict[str, object] = {
            "optionKeywords": list(keywords),
            "maxDistanceTiles": max(1, int(self.cfg.max_obstacle_distance_tiles)),
        }
        target_name_filter = str(self.cfg.target_object_name_contains or "").strip()
        if step is not None and str(step.object_name_contains or "").strip():
            target_name_filter = str(step.object_name_contains or "").strip()
        if target_name_filter:
            params["targetObjectNameContains"] = target_name_filter
        target_object_id = int(self.cfg.target_object_id)
        if step is not None and int(step.object_id) > 0:
            target_object_id = int(step.object_id)
        if target_object_id > 0:
            params["targetObjectId"] = target_object_id
        if step is not None:
            radius = self._resolve_step_target_radius(step)
            params["minWorldX"] = int(step.world_x - radius)
            params["maxWorldX"] = int(step.world_x + radius)
            params["minWorldY"] = int(step.world_y - radius)
            params["maxWorldY"] = int(step.world_y + radius)
            params["targetPlane"] = int(step.plane)
            if step_index >= 0:
                params["routeStepIndex"] = int(step_index)
                params["routeStepCount"] = len(self._obstacle_route)
            params["maxDistanceTiles"] = max(
                int(params["maxDistanceTiles"]),
                int(radius + 8),
                int(self._tuning.step_action_max_distance_floor),
            )
        else:
            center = self._course_center
            if center is not None:
                radius = max(0, int(self.cfg.course_radius_tiles))
                if radius > 0:
                    params["minWorldX"] = int(center.world_x - radius)
                    params["maxWorldX"] = int(center.world_x + radius)
                    params["minWorldY"] = int(center.world_y - radius)
                    params["maxWorldY"] = int(center.world_y + radius)
        intent_suffix = "COURSE" if step is None else f"STEP_{max(0, int(step_index))}"
        reason = "agility_obstacle_action" if step is None else "agility_obstacle_action_route_step"
        return Intent(
            intent_key=f"{ACTIVITY_NAME}:SCENE_OBJECT_ACTION:{intent_suffix}",
            activity=ACTIVITY_NAME,
            kind=IntentKind.SCENE_OBJECT_ACTION,
            target={"targetCategory": "AGILITY_OBSTACLE"},
            params=params,
            policy_key="agility_obstacle",
            reason=reason,
        )

    def _resolve_mark_of_grace_candidate(
        self,
        snapshot: Snapshot,
        player: RoutePoint,
    ) -> GroundItemPoint | None:
        raw = snapshot.raw if isinstance(snapshot.raw, dict) else {}
        nearby_ground_items = raw.get("nearbyGroundItems")
        if not isinstance(nearby_ground_items, list) or not nearby_ground_items:
            return None

        best_candidate: GroundItemPoint | None = None
        for row in nearby_ground_items:
            if not isinstance(row, dict):
                continue
            try:
                item_id = int(row.get("id", row.get("itemId", -1)))
                world_x = int(row.get("worldX", row.get("world_x", -1)))
                world_y = int(row.get("worldY", row.get("world_y", -1)))
                plane = int(row.get("plane", -1))
            except (TypeError, ValueError):
                continue
            if world_x <= 0 or world_y <= 0 or plane < 0:
                continue
            if plane != player.plane:
                # Explicit same-rooftop guard.
                continue
            item_name = str(row.get("name", row.get("itemName", "")) or "").strip().lower()
            is_mark = item_id == AGILITY_MARK_OF_GRACE_ITEM_ID or AGILITY_MARK_NAME_TOKEN in item_name
            if not is_mark:
                continue
            distance_tiles = self._distance_2d_chebyshev(
                player,
                RoutePoint(world_x=world_x, world_y=world_y, plane=plane),
            )
            if distance_tiles > AGILITY_MARK_PICKUP_MAX_DISTANCE_TILES:
                continue
            candidate = GroundItemPoint(
                item_id=item_id,
                item_name=item_name,
                world_x=world_x,
                world_y=world_y,
                plane=plane,
                distance_tiles=distance_tiles,
            )
            if best_candidate is None:
                best_candidate = candidate
                continue
            if (
                candidate.distance_tiles,
                candidate.world_x,
                candidate.world_y,
                candidate.item_id,
            ) < (
                best_candidate.distance_tiles,
                best_candidate.world_x,
                best_candidate.world_y,
                best_candidate.item_id,
            ):
                best_candidate = candidate
        return best_candidate

    def _ground_item_intent(self, item: GroundItemPoint) -> Intent:
        radius_tiles = 1
        return Intent(
            intent_key=(
                f"{ACTIVITY_NAME}:GROUND_ITEM_ACTION:"
                f"{item.world_x}:{item.world_y}:{item.plane}:{max(0, int(item.item_id))}"
            ),
            activity=ACTIVITY_NAME,
            kind=IntentKind.GROUND_ITEM_ACTION,
            target={
                "targetCategory": "MARK_OF_GRACE",
                "worldX": item.world_x,
                "worldY": item.world_y,
                "plane": item.plane,
            },
            params={
                "targetItemId": AGILITY_MARK_OF_GRACE_ITEM_ID,
                "targetItemNameContains": AGILITY_MARK_NAME_TOKEN,
                "targetWorldX": item.world_x,
                "targetWorldY": item.world_y,
                "targetPlane": item.plane,
                "minWorldX": item.world_x - radius_tiles,
                "maxWorldX": item.world_x + radius_tiles,
                "minWorldY": item.world_y - radius_tiles,
                "maxWorldY": item.world_y + radius_tiles,
                "maxDistanceTiles": max(
                    AGILITY_MARK_PICKUP_MAX_DISTANCE_TILES,
                    int(self.cfg.max_obstacle_distance_tiles) + 2,
                ),
                "optionKeywords": ["take"],
            },
            policy_key="agility_mark_of_grace",
            reason="agility_pickup_mark_of_grace",
        )

    def intents(self, snapshot: Snapshot) -> Sequence[Intent]:
        if not snapshot.logged_in:
            self._next_mark_retry_tick = -1
            self._entry_confirmed = False
            self._step_retry_signature = ""
            self._step_retry_block_until_tick = -1
            self._step_no_progress_streak = 0
            self._step_last_distance = 9999
            return []

        tick = int(snapshot.tick)
        player = self._player_world_position(snapshot)
        if player is None:
            self._step_retry_signature = ""
            return []

        if self._is_animation_active(snapshot):
            return []

        start = self._course_start
        if start is not None and not self._entry_confirmed:
            start_radius = max(0, int(self.cfg.start_radius_tiles))
            if self._distance_2d_chebyshev(player, start) <= start_radius:
                self._entry_confirmed = True
            elif bool(self.cfg.walk_to_course_when_outside):
                self._step_retry_signature = ""
                return [self._walk_to_course_intent(start, reason="agility_walk_to_course_start")]
            else:
                return []

        center = self._course_center
        if (
            center is not None
            and bool(self.cfg.walk_to_course_when_outside)
            and self._distance_2d_chebyshev(player, center) > (
                max(0, int(self.cfg.course_radius_tiles)) + int(self._tuning.outside_buffer_tiles)
            )
        ):
            self._step_retry_signature = ""
            return [self._walk_to_course_intent(center, reason="agility_walk_to_course_center")]

        if tick >= self._next_mark_retry_tick:
            mark = self._resolve_mark_of_grace_candidate(snapshot, player)
            route_index_for_mark = self._resolve_active_route_step_index(player)
            if mark is not None and route_index_for_mark >= 0 and self._obstacle_route:
                current_step = self._obstacle_route[int(route_index_for_mark) % len(self._obstacle_route)]
                current_step_dist = self._distance_chebyshev(player, self._step_point(current_step))
                current_step_radius = self._resolve_step_target_radius(current_step)
                if current_step_dist <= (current_step_radius + 2):
                    mark = None
            if mark is not None and int(mark.distance_tiles) <= int(self._tuning.mark_pickup_ready_distance_tiles):
                self._set_next_mark_retry_tick(tick)
                return [self._ground_item_intent(mark)]

        if self._obstacle_route:
            route_index = int(self._resolve_active_route_step_index(player))
            if route_index < 0:
                return []
            step = self._obstacle_route[route_index]
            step_point = self._step_point(step)
            if player.plane != step_point.plane:
                self._step_retry_signature = ""
                return [
                    self._walk_to_course_intent(
                        step_point,
                        reason=f"agility_walk_to_step_{route_index}",
                    )
                ]
            step_dist = self._distance_chebyshev(player, step_point)
            step_radius = self._resolve_step_target_radius(step)
            max_object_dist = max(
                int(self._tuning.step_walk_trigger_distance_floor),
                1,
                int(self.cfg.max_obstacle_distance_tiles) + int(self._tuning.step_approach_extra_distance_tiles),
                int(step_radius + 8),
            )
            if step_dist > max_object_dist:
                self._step_retry_signature = ""
                return [
                    self._walk_to_course_intent(
                        step_point,
                        reason=f"agility_walk_to_step_{route_index}",
                    )
                ]
            if not self._should_issue_step_action(
                tick=tick,
                step=step,
                route_index=route_index,
                distance_tiles=step_dist,
            ):
                return []
            self._mark_step_action_issued(tick=tick, distance_tiles=step_dist)
            return [self._scene_object_intent(step=step, step_index=route_index)]
        return [self._scene_object_intent()]


def parse_keywords(raw: object) -> list[str]:
    text = "" if raw is None else str(raw).strip()
    if not text:
        return []
    normalized = text.replace("|", " ").replace(";", " ").replace(",", " ")
    out: list[str] = []
    seen: set[str] = set()
    for chunk in normalized.split():
        token = chunk.strip().lower()
        if not token or token in seen:
            continue
        seen.add(token)
        out.append(token)
    return out


def _keywords_from_action_text(action_raw: object) -> list[str]:
    action = "" if action_raw is None else str(action_raw).strip().lower()
    if not action:
        return []
    if "jump-up" in action or "jump up" in action:
        return ["jump-up", "jump"]
    if "climb-down" in action:
        return ["climb-down", "climb"]
    if "climb" in action:
        return ["climb"]
    if "cross" in action:
        return ["cross"]
    if "balance" in action:
        return ["balance"]
    if "jump" in action:
        return ["jump"]
    if "squeeze" in action:
        return ["squeeze"]
    if "swing" in action:
        return ["swing"]
    if "vault" in action:
        return ["vault"]
    return parse_keywords(action)


def _object_name_from_action_text(action_raw: object) -> str:
    action = "" if action_raw is None else str(action_raw).strip().lower()
    if not action:
        return ""
    prefixes = (
        "climb-down ",
        "climb down ",
        "climb-up ",
        "climb up ",
        "climb ",
        "cross ",
        "balance ",
        "jump-up ",
        "jump up ",
        "jump ",
        "squeeze-through ",
        "squeeze through ",
        "squeeze ",
        "swing-on ",
        "swing on ",
        "swing ",
        "vault-over ",
        "vault over ",
        "vault ",
        "leap ",
        "walk-across ",
        "walk across ",
    )
    for prefix in prefixes:
        if action.startswith(prefix):
            return action[len(prefix):].strip()
    return ""


def parse_obstacle_route(raw: object) -> list[ObstacleStep]:
    if raw is None:
        return []
    out: list[ObstacleStep] = []
    if isinstance(raw, (list, tuple)):
        for item in raw:
            if not isinstance(item, dict):
                continue
            try:
                x = int(item.get("world_x", item.get("x", -1)))
                y = int(item.get("world_y", item.get("y", -1)))
                plane = max(0, int(item.get("plane", 0)))
            except (TypeError, ValueError):
                continue
            if x <= 0 or y <= 0:
                continue
            action_keywords = parse_keywords(item.get("option_keywords"))
            if not action_keywords:
                action_keywords = _keywords_from_action_text(item.get("action"))
            object_name_contains = str(item.get("object_name_contains", "") or "").strip()
            if not object_name_contains:
                object_name_contains = _object_name_from_action_text(item.get("action"))
            try:
                object_id = int(item.get("object_id", -1))
            except (TypeError, ValueError):
                object_id = -1
            out.append(
                ObstacleStep(
                    world_x=x,
                    world_y=y,
                    plane=plane,
                    option_keywords=tuple(action_keywords),
                    object_name_contains=object_name_contains,
                    object_id=object_id,
                    target_radius_tiles=int(item.get("target_radius_tiles", item.get("radius_tiles", -1))),
                )
            )
        return out
    text = str(raw).strip()
    if not text:
        return []
    normalized = text.replace("|", ";").replace("\n", ";")
    for chunk in normalized.split(";"):
        token = chunk.strip()
        if not token:
            continue
        parts = [p for p in token.replace(",", " ").split() if p]
        if len(parts) < 3:
            continue
        try:
            x = int(parts[0])
            y = int(parts[1])
            plane = max(0, int(parts[2]))
        except ValueError:
            continue
        if x <= 0 or y <= 0:
            continue
        action = " ".join(parts[3:]).strip()
        keywords = _keywords_from_action_text(action)
        out.append(
            ObstacleStep(
                world_x=x,
                world_y=y,
                plane=plane,
                option_keywords=tuple(keywords),
                object_name_contains=_object_name_from_action_text(action),
                target_radius_tiles=-1,
            )
        )
    return out


def _resolve_agility_tuning_profile(value: object) -> AgilityTuning:
    _ = value
    return AGILITY_TUNING_DB_PARITY


def build_strategy(args: argparse.Namespace) -> RuntimeStrategy:
    option_keywords = parse_keywords(getattr(args, "agility_option_keywords", ""))
    if not option_keywords:
        option_keywords = ["climb", "cross", "jump", "balance", "squeeze", "swing", "vault"]
    obstacle_route = parse_obstacle_route(getattr(args, "agility_obstacle_route", ""))
    return AgilityStrategy(
        cfg=AgilityConfig(
            start_world_x=int(getattr(args, "agility_start_world_x", -1)),
            start_world_y=int(getattr(args, "agility_start_world_y", -1)),
            start_plane=max(0, int(getattr(args, "agility_start_plane", 0))),
            start_radius_tiles=max(0, int(getattr(args, "agility_start_radius_tiles", AGILITY_START_RADIUS_TILES_DEFAULT))),
            course_world_x=int(getattr(args, "agility_course_world_x", -1)),
            course_world_y=int(getattr(args, "agility_course_world_y", -1)),
            course_plane=max(0, int(getattr(args, "agility_course_plane", 0))),
            course_radius_tiles=max(0, int(getattr(args, "agility_course_radius_tiles", 12))),
            walk_arrive_distance_tiles=max(0, int(getattr(args, "agility_walk_arrive_distance_tiles", 2))),
            walk_click_mode=str(getattr(args, "agility_walk_click_mode", "MIXED")),
            minimap_click_chance_pct=max(0, min(100, int(getattr(args, "agility_minimap_click_chance_pct", 93)))),
            walk_to_course_when_outside=bool(getattr(args, "agility_walk_to_course_when_outside", True)),
            max_obstacle_distance_tiles=max(1, int(getattr(args, "agility_max_obstacle_distance_tiles", 10))),
            obstacle_target_radius_tiles=max(
                1,
                int(getattr(args, "agility_obstacle_target_radius_tiles", AGILITY_STEP_TARGET_RADIUS_TILES_DEFAULT)),
            ),
            obstacle_option_keywords=tuple(option_keywords),
            target_object_name_contains=str(getattr(args, "agility_target_object_name_contains", "")).strip(),
            target_object_id=int(getattr(args, "agility_target_object_id", -1)),
            obstacle_route=tuple(obstacle_route),
            tuning_profile=str(
                getattr(args, "agility_tuning_profile", AGILITY_TUNING_PROFILE_DB_PARITY)
                or AGILITY_TUNING_PROFILE_DB_PARITY
            ),
        )
    )


def add_args(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--agility-start-world-x", type=int, default=-1)
    parser.add_argument("--agility-start-world-y", type=int, default=-1)
    parser.add_argument("--agility-start-plane", type=int, default=0)
    parser.add_argument("--agility-start-radius-tiles", type=int, default=AGILITY_START_RADIUS_TILES_DEFAULT)
    parser.add_argument("--agility-course-world-x", type=int, default=-1)
    parser.add_argument("--agility-course-world-y", type=int, default=-1)
    parser.add_argument("--agility-course-plane", type=int, default=0)
    parser.add_argument("--agility-course-radius-tiles", type=int, default=12)
    parser.add_argument("--agility-walk-arrive-distance-tiles", type=int, default=2)
    parser.add_argument("--agility-walk-click-mode", type=str, default="MIXED")
    parser.add_argument("--agility-minimap-click-chance-pct", type=int, default=93)
    parser.add_argument("--agility-walk-to-course-when-outside", action="store_true", default=True)
    parser.add_argument(
        "--agility-no-walk-to-course-when-outside",
        action="store_false",
        dest="agility_walk_to_course_when_outside",
    )
    parser.add_argument("--agility-max-obstacle-distance-tiles", type=int, default=10)
    parser.add_argument(
        "--agility-obstacle-target-radius-tiles",
        type=int,
        default=AGILITY_STEP_TARGET_RADIUS_TILES_DEFAULT,
    )
    parser.add_argument(
        "--agility-option-keywords",
        type=str,
        default="climb,cross,jump,balance,squeeze,swing,vault",
    )
    parser.add_argument("--agility-target-object-name-contains", type=str, default="")
    parser.add_argument("--agility-target-object-id", type=int, default=-1)
    parser.add_argument("--agility-obstacle-route", type=str, default="")
    parser.add_argument(
        "--agility-tuning-profile",
        type=str,
        default=AGILITY_TUNING_PROFILE_DB_PARITY,
        choices=(AGILITY_TUNING_PROFILE_DB_PARITY,),
    )
