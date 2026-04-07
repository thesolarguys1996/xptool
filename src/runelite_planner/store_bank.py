from __future__ import annotations

import argparse
import math
import random
from dataclasses import dataclass, field
from typing import ClassVar, Sequence

from .runtime_strategy import RuntimeStrategy
from .runtime_core.models import Intent, IntentKind
from .models import Snapshot

ACTIVITY_NAME = "store_bank"
MODE_WALK_ONLY = "WALK_ONLY"
MODE_THESSALIA_SKIRT_BUYER = "THESSALIA_SKIRT_BUYER"
STORE_BANK_TUNING_PROFILE_DB_PARITY = "DB_PARITY"
SHOP_TRADE_MAX_DISTANCE_TILES = 10


@dataclass(frozen=True)
class StoreBankTuning:
    profile_key: str
    shop_trade_approach_arrive_distance_tiles: int
    explicit_route_intermediate_arrive_distance_tiles: int
    door_interaction_max_distance_tiles: int
    door_traversal_grace_ticks: int
    hop_retry_interval_ticks: int
    hop_max_pending_ticks: int
    hop_retry_cooldown_ticks: int


STORE_BANK_TUNING_DB_PARITY = StoreBankTuning(
    profile_key=STORE_BANK_TUNING_PROFILE_DB_PARITY,
    shop_trade_approach_arrive_distance_tiles=4,
    explicit_route_intermediate_arrive_distance_tiles=3,
    door_interaction_max_distance_tiles=10,
    door_traversal_grace_ticks=8,
    hop_retry_interval_ticks=6,
    hop_max_pending_ticks=150,
    hop_retry_cooldown_ticks=28,
)


@dataclass(frozen=True)
class RoutePoint:
    world_x: int
    world_y: int
    plane: int = 0


@dataclass
class StoreBankConfig:
    store_world_x: int = -1
    store_world_y: int = -1
    store_plane: int = 0
    store_area_radius_tiles: int = 0
    bank_world_x: int = -1
    bank_world_y: int = -1
    bank_plane: int = 0
    bank_area_radius_tiles: int = 0
    arrive_distance_tiles: int = 1
    waypoint_step_tiles: int = 6
    endpoint_hold_ticks: int = 2
    start_leg: str = "TO_BANK"
    mode: str = MODE_WALK_ONLY
    shop_npc_name_contains: str = "thessalia"
    pink_skirt_item_id: int = 1013
    blue_skirt_item_id: int = 1011
    shop_buy_quantity: int = 5
    inventory_full_slots: int = 28
    hop_worlds: tuple[int, ...] = ()
    tuning_profile: str = STORE_BANK_TUNING_PROFILE_DB_PARITY


@dataclass
class StoreBankStrategy:
    cfg: StoreBankConfig
    _delegate: RuntimeStrategy = field(init=False)

    def __post_init__(self) -> None:
        mode = _normalize_mode(self.cfg.mode)
        if mode == MODE_THESSALIA_SKIRT_BUYER:
            self._delegate = ThessaliaSkirtBuyerStrategy(cfg=self.cfg)
            return
        self._delegate = StoreBankWalkerStrategy(cfg=self.cfg)

    def intents(self, snapshot: Snapshot) -> Sequence[Intent]:
        return self._delegate.intents(snapshot)


@dataclass
class StoreBankWalkerStrategy:
    cfg: StoreBankConfig
    _active_leg: str = field(init=False, default="TO_BANK")
    _waypoint_index: int = field(init=False, default=0)
    _hold_until_tick: int = field(init=False, default=-1)
    _store_center: RoutePoint = field(init=False)
    _bank_center: RoutePoint = field(init=False)
    _active_route: tuple[RoutePoint, ...] = field(init=False, default=())
    _active_destination: RoutePoint | None = field(init=False, default=None)

    def __post_init__(self) -> None:
        self._active_leg = self._normalize_leg(self.cfg.start_leg)
        self._store_center = RoutePoint(
            world_x=int(self.cfg.store_world_x),
            world_y=int(self.cfg.store_world_y),
            plane=max(0, int(self.cfg.store_plane)),
        )
        self._bank_center = RoutePoint(
            world_x=int(self.cfg.bank_world_x),
            world_y=int(self.cfg.bank_world_y),
            plane=max(0, int(self.cfg.bank_plane)),
        )

    def intents(self, snapshot: Snapshot) -> Sequence[Intent]:
        if not snapshot.logged_in:
            return []
        if snapshot.bank_open:
            # Let dedicated bank logic own clicks while bank UI is up.
            return []

        player = self._player_world_position(snapshot)
        if player is None:
            return []
        tick = int(snapshot.tick)
        if self._hold_until_tick >= 0 and tick < self._hold_until_tick:
            return []

        route = self._ensure_active_route(player)
        if not route:
            return []
        current = route[self._waypoint_index]
        arrive_distance = max(0, int(self.cfg.arrive_distance_tiles))
        if self._distance_chebyshev(player, current) <= arrive_distance:
            if self._waypoint_index + 1 < len(route):
                self._waypoint_index += 1
                current = route[self._waypoint_index]
            else:
                self._toggle_leg()
                self._active_route = ()
                self._active_destination = None
                self._waypoint_index = 0
                hold_ticks = max(0, int(self.cfg.endpoint_hold_ticks))
                self._hold_until_tick = tick + hold_ticks if hold_ticks > 0 else -1
                return []

        destination = self._active_destination
        destination_key = (
            f"{destination.world_x}:{destination.world_y}:{destination.plane}"
            if destination is not None
            else "unknown"
        )
        intent_key = f"{ACTIVITY_NAME}:WALK:{self._active_leg}:{destination_key}:{self._waypoint_index}"
        return [
            Intent(
                intent_key=intent_key,
                activity=ACTIVITY_NAME,
                kind=IntentKind.WALK_TO_WORLDPOINT,
                target={
                    "worldX": current.world_x,
                    "worldY": current.world_y,
                    "plane": current.plane,
                },
                params={
                    "targetWorldX": current.world_x,
                    "targetWorldY": current.world_y,
                    "targetPlane": current.plane,
                    "arriveDistanceTiles": arrive_distance,
                },
                policy_key="store_bank_walk",
                reason="store_bank_walk_waypoint",
            )
        ]

    def _ensure_active_route(self, player: RoutePoint) -> tuple[RoutePoint, ...]:
        route = self._active_route
        if route and 0 <= self._waypoint_index < len(route):
            return route
        center = self._target_center_for_leg()
        if center is None:
            self._active_route = ()
            self._active_destination = None
            self._waypoint_index = 0
            return ()
        radius = self._target_radius_for_leg()
        destination = _sample_destination_within_circle(center=center, radius=max(0, int(radius)))
        route = _build_route(
            start=player,
            end=destination,
            step_tiles=max(1, int(self.cfg.waypoint_step_tiles)),
        )
        self._active_route = route
        self._active_destination = destination
        self._waypoint_index = 0
        return route

    def _target_center_for_leg(self) -> RoutePoint | None:
        if self._active_leg == "TO_STORE":
            return self._store_center
        if self._active_leg == "TO_BANK":
            return self._bank_center
        return None

    def _target_radius_for_leg(self) -> int:
        if self._active_leg == "TO_STORE":
            return max(0, int(self.cfg.store_area_radius_tiles))
        return max(0, int(self.cfg.bank_area_radius_tiles))

    def _toggle_leg(self) -> None:
        self._active_leg = "TO_STORE" if self._active_leg == "TO_BANK" else "TO_BANK"

    @staticmethod
    def _normalize_leg(value: object) -> str:
        raw = str(value or "TO_BANK").strip().upper()
        if raw in ("TO_STORE", "STORE"):
            return "TO_STORE"
        return "TO_BANK"

    @staticmethod
    def _player_world_position(snapshot: Snapshot) -> RoutePoint | None:
        return _player_world_position(snapshot)

    @staticmethod
    def _distance_chebyshev(a: RoutePoint, b: RoutePoint) -> int:
        return _distance_chebyshev(a, b)


@dataclass
class ThessaliaSkirtBuyerStrategy:
    THESSALIA_DOOR_POINT: ClassVar[RoutePoint] = RoutePoint(world_x=3209, world_y=3415, plane=0)
    THESSALIA_DOOR_SEARCH_MIN_X: ClassVar[int] = 3208
    THESSALIA_DOOR_SEARCH_MAX_X: ClassVar[int] = 3209
    THESSALIA_DOOR_SEARCH_MIN_Y: ClassVar[int] = 3415
    THESSALIA_DOOR_SEARCH_MAX_Y: ClassVar[int] = 3416
    THESSALIA_OPEN_DOOR_OBJECT_IDS: ClassVar[frozenset[int]] = frozenset({11774})
    THESSALIA_TO_BANK_ROUTE: ClassVar[tuple[RoutePoint, ...]] = (
        RoutePoint(world_x=3209, world_y=3415, plane=0),
        RoutePoint(world_x=3211, world_y=3422, plane=0),
        RoutePoint(world_x=3206, world_y=3429, plane=0),
        RoutePoint(world_x=3191, world_y=3431, plane=0),
        RoutePoint(world_x=3182, world_y=3440, plane=0),
    )
    THESSALIA_TO_STORE_ROUTE: ClassVar[tuple[RoutePoint, ...]] = (
        RoutePoint(world_x=3191, world_y=3431, plane=0),
        RoutePoint(world_x=3206, world_y=3429, plane=0),
        RoutePoint(world_x=3211, world_y=3422, plane=0),
        RoutePoint(world_x=3209, world_y=3415, plane=0),
        RoutePoint(world_x=3206, world_y=3416, plane=0),
    )
    THESSALIA_TO_STORE_DOOR_INDEX: ClassVar[int] = 3

    cfg: StoreBankConfig
    _store_center: RoutePoint = field(init=False)
    _bank_center: RoutePoint = field(init=False)
    _active_walk_leg: str = field(init=False, default="")
    _active_route: tuple[RoutePoint, ...] = field(init=False, default=())
    _active_destination: RoutePoint | None = field(init=False, default=None)
    _waypoint_index: int = field(init=False, default=0)
    _hop_index: int = field(init=False, default=0)
    _pending_hop_target_world: int = field(init=False, default=-1)
    _pending_hop_origin_world: int = field(init=False, default=-1)
    _pending_hop_started_tick: int = field(init=False, default=-1)
    _pending_hop_next_issue_tick: int = field(init=False, default=-1)
    _pending_hop_retry_cooldown_until_tick: int = field(init=False, default=-1)
    _door_traversal_grace_until_tick: int = field(init=False, default=-1)
    _tuning: StoreBankTuning = field(init=False)

    def __post_init__(self) -> None:
        self._tuning = _resolve_store_bank_tuning_profile(self.cfg.tuning_profile)
        self._store_center = RoutePoint(
            world_x=int(self.cfg.store_world_x),
            world_y=int(self.cfg.store_world_y),
            plane=max(0, int(self.cfg.store_plane)),
        )
        self._bank_center = RoutePoint(
            world_x=int(self.cfg.bank_world_x),
            world_y=int(self.cfg.bank_world_y),
            plane=max(0, int(self.cfg.bank_plane)),
        )

    def intents(self, snapshot: Snapshot) -> Sequence[Intent]:
        if not snapshot.logged_in:
            self._clear_walk_route()
            return []

        player = _player_world_position(snapshot)
        if player is None:
            return []

        pending_hop_intents = self._pending_hop_intents(snapshot)
        if pending_hop_intents:
            return pending_hop_intents
        if self._hop_pending_active():
            # Keep hop flow exclusive while we wait for the next retry tick.
            return []

        inventory_used = self._inventory_slots_used(snapshot)
        full_slots = max(1, min(28, int(self.cfg.inventory_full_slots)))
        inventory_full = inventory_used >= full_slots

        if bool(snapshot.bank_open) or inventory_full:
            return self._bank_intents(snapshot, player)
        return self._shop_intents(snapshot, player)

    def _hop_pending_active(self) -> bool:
        return self._pending_hop_target_world > 0 or self._pending_hop_origin_world > 0

    def _shop_intents(self, snapshot: Snapshot, player: RoutePoint) -> Sequence[Intent]:
        arrive_distance = max(0, int(self.cfg.arrive_distance_tiles))
        store_radius = max(0, int(self.cfg.store_area_radius_tiles))
        current_world = int(snapshot.world_id) if snapshot.world_id is not None else -1
        door_intents = self._maybe_open_shop_door(snapshot, player)
        if door_intents:
            return door_intents
        if not _is_within_circle(
            point=player,
            center=self._store_center,
            radius_tiles=store_radius + arrive_distance,
        ):
            return self._walk_intents_for_leg(snapshot, player, "TO_STORE")
        if not self._can_cross_shop_door(snapshot, player):
            door_intents = self._maybe_open_shop_door(snapshot, player)
            if door_intents:
                return door_intents
            return self._walk_intents_for_leg(snapshot, player, "TO_STORE")

        self._clear_walk_route()
        shop_npc = _nearest_matching_npc(snapshot, str(self.cfg.shop_npc_name_contains))

        if not bool(snapshot.shop_open):
            if (
                shop_npc is not None
                and shop_npc.distance_tiles > SHOP_TRADE_MAX_DISTANCE_TILES
            ):
                return [
                    Intent(
                        intent_key=(
                            f"{ACTIVITY_NAME}:APPROACH_SHOP_NPC:"
                            f"{shop_npc.world_x}:{shop_npc.world_y}:{shop_npc.plane}"
                        ),
                        activity=ACTIVITY_NAME,
                        kind=IntentKind.WALK_TO_WORLDPOINT,
                        target={
                            "worldX": shop_npc.world_x,
                            "worldY": shop_npc.world_y,
                            "plane": shop_npc.plane,
                        },
                        params={
                            "targetWorldX": shop_npc.world_x,
                            "targetWorldY": shop_npc.world_y,
                            "targetPlane": shop_npc.plane,
                            "arriveDistanceTiles": self._tuning.shop_trade_approach_arrive_distance_tiles,
                        },
                        policy_key="store_bank_walk_shop_npc",
                        reason="store_bank_walk_to_shop_npc",
                    )
                ]
            return [
                Intent(
                    intent_key=f"{ACTIVITY_NAME}:TRADE_SHOP_NPC:{self.cfg.shop_npc_name_contains}",
                    activity=ACTIVITY_NAME,
                    kind=IntentKind.NPC_CONTEXT_ACTION,
                    target={"targetNpcNameContains": str(self.cfg.shop_npc_name_contains)},
                    params={
                        "targetNpcNameContains": str(self.cfg.shop_npc_name_contains),
                        "optionKeywords": ["trade"],
                        "maxDistanceTiles": SHOP_TRADE_MAX_DISTANCE_TILES,
                        "expectedEffect": "shop_open",
                    },
                    policy_key="store_bank_open_shop",
                    reason="store_bank_trade_shop_npc",
                )
            ]

        pink_id = int(self.cfg.pink_skirt_item_id)
        blue_id = int(self.cfg.blue_skirt_item_id)
        pink_stock = int(snapshot.shop_counts.get(pink_id, 0))
        blue_stock = int(snapshot.shop_counts.get(blue_id, 0))
        if pink_stock <= 0 and blue_stock <= 0:
            tick = int(snapshot.tick)
            if (
                self._pending_hop_retry_cooldown_until_tick >= 0
                and tick < self._pending_hop_retry_cooldown_until_tick
            ):
                return []
            if self._pending_hop_target_world <= 0 or self._pending_hop_target_world == current_world:
                self._pending_hop_target_world = self._next_hop_world(snapshot.world_id)
                self._pending_hop_started_tick = tick
                self._pending_hop_next_issue_tick = tick
            if current_world > 0 and self._pending_hop_origin_world <= 0:
                self._pending_hop_origin_world = current_world
            target_world = self._pending_hop_target_world
            params = {}
            if target_world > 0:
                params["targetWorld"] = target_world
            return [
                Intent(
                    intent_key=f"{ACTIVITY_NAME}:WORLD_HOP:{target_world if target_world > 0 else 'next'}",
                    activity=ACTIVITY_NAME,
                    kind=IntentKind.WORLD_HOP,
                    target={"targetWorld": target_world},
                    params=params,
                    policy_key="store_bank_world_hop",
                    reason="store_bank_hop_for_shop_restock",
                )
            ]

        self._pending_hop_target_world = -1
        self._pending_hop_origin_world = -1
        self._pending_hop_started_tick = -1
        self._pending_hop_next_issue_tick = -1
        self._pending_hop_retry_cooldown_until_tick = -1
        buy_item_id = self._choose_shop_item_to_buy(snapshot, pink_id, blue_id, pink_stock, blue_stock)
        quantity = max(1, int(self.cfg.shop_buy_quantity))
        return [
            Intent(
                intent_key=f"{ACTIVITY_NAME}:SHOP_BUY:{buy_item_id}:Q{quantity}",
                activity=ACTIVITY_NAME,
                kind=IntentKind.SHOP_BUY_ITEM,
                target={"itemId": buy_item_id},
                params={"itemId": buy_item_id, "quantity": quantity},
                policy_key="store_bank_shop_buy",
                reason="store_bank_shop_buy_item",
            )
        ]

    def _pending_hop_intents(self, snapshot: Snapshot) -> Sequence[Intent]:
        tick = int(snapshot.tick)
        current_world = int(snapshot.world_id) if snapshot.world_id is not None else -1
        if self._pending_hop_target_world <= 0 and self._pending_hop_origin_world <= 0:
            return []
        if (
            self._pending_hop_target_world > 0
            and current_world > 0
            and current_world == self._pending_hop_target_world
        ):
            self._pending_hop_target_world = -1
            self._pending_hop_origin_world = -1
            self._pending_hop_started_tick = -1
            self._pending_hop_next_issue_tick = -1
            self._pending_hop_retry_cooldown_until_tick = -1
            return []
        if (
            self._pending_hop_target_world <= 0
            and self._pending_hop_origin_world > 0
            and current_world > 0
            and current_world != self._pending_hop_origin_world
        ):
            self._pending_hop_target_world = -1
            self._pending_hop_origin_world = -1
            self._pending_hop_started_tick = -1
            self._pending_hop_next_issue_tick = -1
            self._pending_hop_retry_cooldown_until_tick = -1
            return []
        if self._pending_hop_started_tick < 0:
            self._pending_hop_started_tick = tick
        if tick - self._pending_hop_started_tick > self._tuning.hop_max_pending_ticks:
            self._pending_hop_target_world = -1
            self._pending_hop_origin_world = -1
            self._pending_hop_started_tick = -1
            self._pending_hop_next_issue_tick = -1
            self._pending_hop_retry_cooldown_until_tick = tick + self._tuning.hop_retry_cooldown_ticks
            return []
        if self._pending_hop_next_issue_tick < 0:
            self._pending_hop_next_issue_tick = tick
        if tick < self._pending_hop_next_issue_tick:
            return []
        self._pending_hop_next_issue_tick = tick + self._tuning.hop_retry_interval_ticks
        target_world = self._pending_hop_target_world
        params = {}
        if target_world > 0:
            params["targetWorld"] = target_world
        return [
            Intent(
                intent_key=f"{ACTIVITY_NAME}:WORLD_HOP:{target_world if target_world > 0 else 'next'}",
                activity=ACTIVITY_NAME,
                kind=IntentKind.WORLD_HOP,
                target={"targetWorld": target_world},
                params=params,
                policy_key="store_bank_world_hop",
                reason="store_bank_hop_for_shop_restock",
            )
        ]

    def _bank_intents(self, snapshot: Snapshot, player: RoutePoint) -> Sequence[Intent]:
        arrive_distance = max(0, int(self.cfg.arrive_distance_tiles))
        bank_radius = max(0, int(self.cfg.bank_area_radius_tiles))
        door_intents = self._maybe_open_shop_door(snapshot, player)
        if door_intents:
            return door_intents
        if not bool(snapshot.bank_open) and not _is_within_circle(
            point=player,
            center=self._bank_center,
            radius_tiles=bank_radius + arrive_distance,
        ):
            return self._walk_intents_for_leg(snapshot, player, "TO_BANK")

        self._clear_walk_route()

        if not bool(snapshot.bank_open):
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
                    policy_key="store_bank_open_bank",
                    reason="store_bank_open_bank",
                )
            ]

        pink_id = int(self.cfg.pink_skirt_item_id)
        blue_id = int(self.cfg.blue_skirt_item_id)
        pink_count = int(snapshot.inventory_counts.get(pink_id, 0))
        blue_count = int(snapshot.inventory_counts.get(blue_id, 0))
        if pink_count > 0:
            return [
                Intent(
                    intent_key=f"{ACTIVITY_NAME}:DEPOSIT_ITEM:{pink_id}",
                    activity=ACTIVITY_NAME,
                    kind=IntentKind.DEPOSIT_ITEM,
                    target={"itemId": pink_id},
                    params={"itemId": pink_id, "quantity": "ALL"},
                    policy_key="store_bank_deposit",
                    reason="store_bank_deposit_pink_skirts",
                )
            ]
        if blue_count > 0:
            return [
                Intent(
                    intent_key=f"{ACTIVITY_NAME}:DEPOSIT_ITEM:{blue_id}",
                    activity=ACTIVITY_NAME,
                    kind=IntentKind.DEPOSIT_ITEM,
                    target={"itemId": blue_id},
                    params={"itemId": blue_id, "quantity": "ALL"},
                    policy_key="store_bank_deposit",
                    reason="store_bank_deposit_blue_skirts",
                )
            ]

        return [
            Intent(
                intent_key=f"{ACTIVITY_NAME}:CLOSE_BANK",
                activity=ACTIVITY_NAME,
                kind=IntentKind.CLOSE_BANK,
                target={},
                params={},
                policy_key="store_bank_close_bank",
                reason="store_bank_close_bank",
            )
        ]

    def _maybe_open_shop_door(self, snapshot: Snapshot, player: RoutePoint) -> Sequence[Intent]:
        tick = int(snapshot.tick)
        if (
            self._door_traversal_grace_until_tick >= 0
            and tick < self._door_traversal_grace_until_tick
        ):
            return []
        if bool(snapshot.shop_open):
            return []
        if self._is_on_shop_side_of_door(player):
            return []
        door_object = _nearest_matching_object(
            snapshot,
            name_contains="door",
            min_world_x=self.THESSALIA_DOOR_SEARCH_MIN_X,
            max_world_x=self.THESSALIA_DOOR_SEARCH_MAX_X,
            min_world_y=self.THESSALIA_DOOR_SEARCH_MIN_Y,
            max_world_y=self.THESSALIA_DOOR_SEARCH_MAX_Y,
            plane=self.THESSALIA_DOOR_POINT.plane,
        )
        if door_object is None or player.plane != door_object.plane:
            return []
        door_point = RoutePoint(
            world_x=door_object.world_x,
            world_y=door_object.world_y,
            plane=door_object.plane,
        )
        door_distance = _distance_chebyshev(player, door_point)
        if door_distance > self._tuning.door_interaction_max_distance_tiles:
            return []
        self._door_traversal_grace_until_tick = tick + self._tuning.door_traversal_grace_ticks
        return [
            Intent(
                intent_key=(
                    f"{ACTIVITY_NAME}:OPEN_DOOR:"
                    f"{door_object.world_x}:{door_object.world_y}:{door_object.plane}"
                ),
                activity=ACTIVITY_NAME,
                kind=IntentKind.SCENE_OBJECT_ACTION,
                target={
                    "targetObjectNameContains": "door",
                    "targetPlane": door_object.plane,
                    "minWorldX": self.THESSALIA_DOOR_SEARCH_MIN_X,
                    "maxWorldX": self.THESSALIA_DOOR_SEARCH_MAX_X,
                    "minWorldY": self.THESSALIA_DOOR_SEARCH_MIN_Y,
                    "maxWorldY": self.THESSALIA_DOOR_SEARCH_MAX_Y,
                },
                params={
                    "targetObjectNameContains": "door",
                    "targetPlane": door_object.plane,
                    "minWorldX": self.THESSALIA_DOOR_SEARCH_MIN_X,
                    "maxWorldX": self.THESSALIA_DOOR_SEARCH_MAX_X,
                    "minWorldY": self.THESSALIA_DOOR_SEARCH_MIN_Y,
                    "maxWorldY": self.THESSALIA_DOOR_SEARCH_MAX_Y,
                    "maxDistanceTiles": self._tuning.door_interaction_max_distance_tiles,
                    "optionKeywords": ["open"],
                },
                policy_key="store_bank_open_door",
                reason="store_bank_open_shop_door",
            )
        ]

    def _is_on_shop_side_of_door(self, player: RoutePoint) -> bool:
        if player.plane != self.THESSALIA_DOOR_POINT.plane:
            return False
        return player.world_x < self.THESSALIA_DOOR_SEARCH_MIN_X

    def _can_cross_shop_door(self, snapshot: Snapshot, player: RoutePoint) -> bool:
        return self._is_on_shop_side_of_door(player) or self._shop_door_is_open(snapshot)

    def _shop_door_is_open(self, snapshot: Snapshot) -> bool:
        door_object = _nearest_matching_object(
            snapshot,
            name_contains="door",
            min_world_x=self.THESSALIA_DOOR_SEARCH_MIN_X,
            max_world_x=self.THESSALIA_DOOR_SEARCH_MAX_X,
            min_world_y=self.THESSALIA_DOOR_SEARCH_MIN_Y,
            max_world_y=self.THESSALIA_DOOR_SEARCH_MAX_Y,
            plane=self.THESSALIA_DOOR_POINT.plane,
        )
        if door_object is None:
            return False
        return door_object.object_id in self.THESSALIA_OPEN_DOOR_OBJECT_IDS

    def _walk_intents_for_leg(self, snapshot: Snapshot, player: RoutePoint, leg: str) -> Sequence[Intent]:
        center = self._store_center if leg == "TO_STORE" else self._bank_center
        radius = max(0, int(self.cfg.store_area_radius_tiles if leg == "TO_STORE" else self.cfg.bank_area_radius_tiles))
        arrive_distance = max(0, int(self.cfg.arrive_distance_tiles))
        using_explicit_route = False
        if (
            self._active_walk_leg != leg
            or not self._active_route
            or self._active_destination is None
            or self._active_destination.plane != center.plane
        ):
            self._active_walk_leg = leg
            explicit_route = self._route_points_for_leg(leg)
            if explicit_route:
                using_explicit_route = True
                self._active_destination = explicit_route[-1]
                self._active_route = explicit_route
                self._waypoint_index = self._initial_waypoint_index(
                    player,
                    explicit_route,
                    arrive_distance,
                )
            else:
                self._active_destination = _sample_destination_within_circle(center=center, radius=radius)
                self._active_route = _build_route(
                    start=player,
                    end=self._active_destination,
                    step_tiles=max(1, int(self.cfg.waypoint_step_tiles)),
                )
                self._waypoint_index = 0

        if not self._active_route:
            self._clear_walk_route()
            return []

        if leg == "TO_STORE" and not self._can_cross_shop_door(snapshot, player):
            self._waypoint_index = min(self._waypoint_index, self.THESSALIA_TO_STORE_DOOR_INDEX)

        if not using_explicit_route:
            using_explicit_route = bool(self._route_points_for_leg(leg))
        current = self._active_route[self._waypoint_index]
        current_arrive_distance = arrive_distance
        if using_explicit_route and self._waypoint_index + 1 < len(self._active_route):
            current_arrive_distance = max(
                arrive_distance,
                self._tuning.explicit_route_intermediate_arrive_distance_tiles,
            )
        if _distance_chebyshev(player, current) <= current_arrive_distance:
            if self._waypoint_index + 1 < len(self._active_route):
                next_index = self._waypoint_index + 1
                if leg == "TO_STORE" and not self._can_cross_shop_door(snapshot, player):
                    next_index = min(next_index, self.THESSALIA_TO_STORE_DOOR_INDEX)
                self._waypoint_index = next_index
                current = self._active_route[self._waypoint_index]
                current_arrive_distance = arrive_distance
                if using_explicit_route and self._waypoint_index + 1 < len(self._active_route):
                    current_arrive_distance = max(
                        arrive_distance,
                        self._tuning.explicit_route_intermediate_arrive_distance_tiles,
                    )
            else:
                self._clear_walk_route()
                return []

        destination = self._active_destination
        destination_key = (
            f"{destination.world_x}:{destination.world_y}:{destination.plane}"
            if destination is not None
            else "unknown"
        )
        intent_key = f"{ACTIVITY_NAME}:WALK:{leg}:{destination_key}:{self._waypoint_index}"
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
                    "arriveDistanceTiles": current_arrive_distance,
                },
                policy_key="store_bank_walk",
                reason="store_bank_walk_waypoint",
            )
        ]

    def _clear_walk_route(self) -> None:
        self._active_walk_leg = ""
        self._active_route = ()
        self._active_destination = None
        self._waypoint_index = 0

    def _route_points_for_leg(self, leg: str) -> tuple[RoutePoint, ...]:
        if self._store_center.plane != 0 or self._bank_center.plane != 0:
            return ()
        if leg == "TO_BANK":
            return self.THESSALIA_TO_BANK_ROUTE
        if leg == "TO_STORE":
            return self.THESSALIA_TO_STORE_ROUTE
        return ()

    @staticmethod
    def _initial_waypoint_index(
        player: RoutePoint,
        route: tuple[RoutePoint, ...],
        arrive_distance: int,
    ) -> int:
        if not route:
            return 0
        reached_indices = [
            index for index, point in enumerate(route)
            if _distance_chebyshev(player, point) <= arrive_distance
        ]
        if reached_indices:
            return min(len(route) - 1, max(reached_indices) + 1)
        closest_index = min(
            range(len(route)),
            key=lambda index: _distance_chebyshev(player, route[index]),
        )
        return max(0, closest_index)

    def _inventory_slots_used(self, snapshot: Snapshot) -> int:
        if snapshot.inventory_slots_used is not None:
            return max(0, int(snapshot.inventory_slots_used))
        used = 0
        for quantity in snapshot.inventory_counts.values():
            if int(quantity) > 0:
                used += 1
        return used

    def _choose_shop_item_to_buy(
        self,
        snapshot: Snapshot,
        pink_id: int,
        blue_id: int,
        pink_stock: int,
        blue_stock: int,
    ) -> int:
        if pink_stock > 0 and blue_stock > 0:
            pink_count = int(snapshot.inventory_counts.get(pink_id, 0))
            blue_count = int(snapshot.inventory_counts.get(blue_id, 0))
            return pink_id if pink_count <= blue_count else blue_id
        if pink_stock > 0:
            return pink_id
        return blue_id

    def _next_hop_world(self, current_world: int | None) -> int:
        worlds = tuple(int(world) for world in self.cfg.hop_worlds if int(world) > 0)
        if not worlds:
            return -1
        current = int(current_world) if current_world is not None else -1
        for _ in range(len(worlds)):
            candidate = worlds[self._hop_index % len(worlds)]
            self._hop_index += 1
            if candidate != current:
                return candidate
        return worlds[0]


def _normalize_mode(value: object) -> str:
    raw = str(value or MODE_WALK_ONLY).strip().upper()
    if raw in ("THESSALIA_SKIRT_BUYER", "THESSALIA", "SKIRT_BUYER"):
        return MODE_THESSALIA_SKIRT_BUYER
    return MODE_WALK_ONLY


def _resolve_store_bank_tuning_profile(value: object) -> StoreBankTuning:
    _ = value
    return STORE_BANK_TUNING_DB_PARITY


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


@dataclass(frozen=True)
class NearbyNpcPoint:
    npc_id: int
    npc_index: int
    name: str
    world_x: int
    world_y: int
    plane: int
    distance_tiles: int


@dataclass(frozen=True)
class NearbyObjectPoint:
    object_id: int
    name: str
    world_x: int
    world_y: int
    plane: int
    distance_tiles: int


def _nearest_matching_npc(snapshot: Snapshot, name_contains: str) -> NearbyNpcPoint | None:
    raw = snapshot.raw if isinstance(snapshot.raw, dict) else {}
    nearby_npcs = raw.get("nearbyNpcs")
    if not isinstance(nearby_npcs, list):
        return None
    needle = str(name_contains or "").strip().lower()
    best: NearbyNpcPoint | None = None
    for row in nearby_npcs:
        if not isinstance(row, dict):
            continue
        name = str(row.get("name", "")).strip()
        if needle and needle not in name.lower():
            continue
        try:
            world_x = int(row.get("worldX", -1))
            world_y = int(row.get("worldY", -1))
            plane = int(row.get("plane", -1))
            distance_tiles = int(row.get("distance", 9999))
            npc_id = int(row.get("id", -1))
            npc_index = int(row.get("index", -1))
        except (TypeError, ValueError):
            continue
        if world_x <= 0 or world_y <= 0 or plane < 0:
            continue
        candidate = NearbyNpcPoint(
            npc_id=npc_id,
            npc_index=npc_index,
            name=name,
            world_x=world_x,
            world_y=world_y,
            plane=plane,
            distance_tiles=max(0, distance_tiles),
        )
        if best is None or candidate.distance_tiles < best.distance_tiles:
            best = candidate
    return best


def _nearest_matching_object(
    snapshot: Snapshot,
    name_contains: str,
    *,
    min_world_x: int | None = None,
    max_world_x: int | None = None,
    min_world_y: int | None = None,
    max_world_y: int | None = None,
    plane: int | None = None,
) -> NearbyObjectPoint | None:
    raw = snapshot.raw if isinstance(snapshot.raw, dict) else {}
    nearby_objects = raw.get("nearbyObjects")
    if not isinstance(nearby_objects, list):
        return None
    needle = str(name_contains or "").strip().lower()
    best: NearbyObjectPoint | None = None
    for row in nearby_objects:
        if not isinstance(row, dict):
            continue
        name = str(row.get("name", "")).strip()
        if needle and needle not in name.lower():
            continue
        try:
            world_x = int(row.get("worldX", -1))
            world_y = int(row.get("worldY", -1))
            object_plane = int(row.get("plane", -1))
            distance_tiles = int(row.get("distance", 9999))
            object_id = int(row.get("id", -1))
        except (TypeError, ValueError):
            continue
        if world_x <= 0 or world_y <= 0 or object_plane < 0:
            continue
        candidate = NearbyObjectPoint(
            object_id=object_id,
            name=name,
            world_x=world_x,
            world_y=world_y,
            plane=object_plane,
            distance_tiles=max(0, distance_tiles),
        )
        if min_world_x is not None and world_x < int(min_world_x):
            continue
        if max_world_x is not None and world_x > int(max_world_x):
            continue
        if min_world_y is not None and world_y < int(min_world_y):
            continue
        if max_world_y is not None and world_y > int(max_world_y):
            continue
        if plane is not None and candidate.plane != int(plane):
            continue
        if best is None or candidate.distance_tiles < best.distance_tiles:
            best = candidate
    return best


def _distance_chebyshev(a: RoutePoint, b: RoutePoint) -> int:
    if a.plane != b.plane:
        return 9999
    return max(abs(a.world_x - b.world_x), abs(a.world_y - b.world_y))


def _is_within_circle(*, point: RoutePoint, center: RoutePoint, radius_tiles: int) -> bool:
    if point.plane != center.plane:
        return False
    radius = max(0, int(radius_tiles))
    dx = point.world_x - center.world_x
    dy = point.world_y - center.world_y
    return (dx * dx) + (dy * dy) <= (radius * radius)


def _sample_destination_within_circle(*, center: RoutePoint, radius: int) -> RoutePoint:
    if radius <= 0:
        return center
    max_radius = max(0, int(radius))
    attempts = 40
    for _ in range(attempts):
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


def _build_route(*, start: RoutePoint, end: RoutePoint, step_tiles: int) -> tuple[RoutePoint, ...]:
    if (
        start.world_x <= 0
        or start.world_y <= 0
        or end.world_x <= 0
        or end.world_y <= 0
    ):
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


def _parse_hop_worlds(raw_value: object) -> tuple[int, ...]:
    raw = str(raw_value or "").strip()
    if not raw:
        return ()
    out: list[int] = []
    seen: set[int] = set()
    for token in raw.split(","):
        text = token.strip()
        if not text:
            continue
        try:
            world = int(text)
        except ValueError:
            continue
        if world <= 0 or world in seen:
            continue
        seen.add(world)
        out.append(world)
    return tuple(out)


def build_strategy(args: argparse.Namespace) -> RuntimeStrategy:
    return StoreBankStrategy(
        cfg=StoreBankConfig(
            store_world_x=int(getattr(args, "store_bank_store_world_x", -1)),
            store_world_y=int(getattr(args, "store_bank_store_world_y", -1)),
            store_plane=max(0, int(getattr(args, "store_bank_store_plane", 0))),
            store_area_radius_tiles=max(0, int(getattr(args, "store_bank_store_area_radius_tiles", 0))),
            bank_world_x=int(getattr(args, "store_bank_bank_world_x", -1)),
            bank_world_y=int(getattr(args, "store_bank_bank_world_y", -1)),
            bank_plane=max(0, int(getattr(args, "store_bank_bank_plane", 0))),
            bank_area_radius_tiles=max(0, int(getattr(args, "store_bank_bank_area_radius_tiles", 0))),
            arrive_distance_tiles=max(0, int(getattr(args, "store_bank_arrive_distance_tiles", 1))),
            waypoint_step_tiles=max(1, int(getattr(args, "store_bank_waypoint_step_tiles", 6))),
            endpoint_hold_ticks=max(0, int(getattr(args, "store_bank_endpoint_hold_ticks", 2))),
            start_leg=str(getattr(args, "store_bank_start_leg", "TO_BANK") or "TO_BANK"),
            mode=str(getattr(args, "store_bank_mode", MODE_WALK_ONLY) or MODE_WALK_ONLY),
            shop_npc_name_contains=str(
                getattr(args, "store_bank_shop_npc_name_contains", "thessalia") or "thessalia"
            ),
            pink_skirt_item_id=max(1, int(getattr(args, "store_bank_pink_skirt_item_id", 1013))),
            blue_skirt_item_id=max(1, int(getattr(args, "store_bank_blue_skirt_item_id", 1011))),
            shop_buy_quantity=max(1, int(getattr(args, "store_bank_shop_buy_quantity", 5))),
            inventory_full_slots=max(1, min(28, int(getattr(args, "store_bank_inventory_full_slots", 28)))),
            hop_worlds=_parse_hop_worlds(getattr(args, "store_bank_hop_worlds", "")),
            tuning_profile=str(
                getattr(args, "store_bank_tuning_profile", STORE_BANK_TUNING_PROFILE_DB_PARITY)
                or STORE_BANK_TUNING_PROFILE_DB_PARITY
            ),
        )
    )


def add_args(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--store-bank-store-world-x", type=int, default=-1)
    parser.add_argument("--store-bank-store-world-y", type=int, default=-1)
    parser.add_argument("--store-bank-store-plane", type=int, default=0)
    parser.add_argument("--store-bank-store-area-radius-tiles", type=int, default=0)
    parser.add_argument("--store-bank-bank-world-x", type=int, default=-1)
    parser.add_argument("--store-bank-bank-world-y", type=int, default=-1)
    parser.add_argument("--store-bank-bank-plane", type=int, default=0)
    parser.add_argument("--store-bank-bank-area-radius-tiles", type=int, default=0)
    parser.add_argument("--store-bank-arrive-distance-tiles", type=int, default=1)
    parser.add_argument("--store-bank-waypoint-step-tiles", type=int, default=6)
    parser.add_argument("--store-bank-endpoint-hold-ticks", type=int, default=2)
    parser.add_argument("--store-bank-start-leg", type=str, default="TO_BANK")
    parser.add_argument("--store-bank-mode", type=str, default=MODE_WALK_ONLY)
    parser.add_argument("--store-bank-shop-npc-name-contains", type=str, default="thessalia")
    parser.add_argument("--store-bank-pink-skirt-item-id", type=int, default=1013)
    parser.add_argument("--store-bank-blue-skirt-item-id", type=int, default=1011)
    parser.add_argument("--store-bank-shop-buy-quantity", type=int, default=5)
    parser.add_argument("--store-bank-inventory-full-slots", type=int, default=28)
    parser.add_argument("--store-bank-hop-worlds", type=str, default="")
    parser.add_argument(
        "--store-bank-tuning-profile",
        type=str,
        default=STORE_BANK_TUNING_PROFILE_DB_PARITY,
        choices=(STORE_BANK_TUNING_PROFILE_DB_PARITY,),
    )
