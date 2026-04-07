from __future__ import annotations

from collections import Counter, defaultdict
from dataclasses import dataclass
from typing import Mapping
import uuid

from ..models import RuntimeCommand, Snapshot
from .action_queue import ActionQueue
from .intent_resolver import IntentResolver
from .interaction_gate import InteractionGate
from .models import (
    ActionPhase,
    ExecutionEvent,
    ExecutionEventKind,
    GateVerdict,
    Intent,
    IntentKind,
)
from .motion_engine import MotionEngine
from .scheduler import Scheduler


@dataclass(frozen=True)
class DispatchRequest:
    ticket_id: str
    command: RuntimeCommand


@dataclass
class RuntimeCore:
    queue: ActionQueue
    resolver: IntentResolver
    scheduler: Scheduler
    gate: InteractionGate
    motion_engine: MotionEngine

    def __init__(self) -> None:
        self.queue = ActionQueue()
        self.resolver = IntentResolver()
        self.scheduler = Scheduler()
        self.gate = InteractionGate()
        self.motion_engine = MotionEngine()
        self._command_id_to_ticket: dict[str, str] = {}
        self._ticket_activity: dict[str, str] = {}
        self._outcome_counts_by_activity: dict[str, Counter[str]] = defaultdict(Counter)
        self._deferred_reason_counts: Counter[str] = Counter()
        self._behavioral_timing_deferred_reason_counts: Counter[str] = Counter()
        self._last_dispatched_tick_by_ticket: dict[str, int] = {}
        self._dispatch_to_effect_ticks_by_activity: dict[str, list[int]] = defaultdict(list)
        self._retry_transition_reason_counts: Counter[str] = Counter()
        self._woodcut_implicit_area_anchor_by_activity: dict[str, tuple[int, int]] = {}

    def on_snapshot(self, snapshot: Snapshot, intents: list[Intent]) -> list[DispatchRequest]:
        resolved = self.resolver.resolve(snapshot_tick=int(snapshot.tick), intents=intents)
        if resolved is not None:
            self.queue.upsert_from_resolved_intent(resolved)
            ticket = self.queue.active_ticket()
            if ticket is not None:
                self._ticket_activity[str(ticket.ticket_id)] = str(ticket.intent.activity)

        self._record_snapshot_effects(snapshot)
        self._advance_phases(snapshot.tick)
        return self._build_dispatch_requests(snapshot)

    def on_dispatch_enqueued(self, *, ticket_id: str, command_id: str, tick: int) -> None:
        self.queue.bind_command_id(ticket_id=ticket_id, command_id=command_id)
        self._command_id_to_ticket[str(command_id)] = str(ticket_id)
        ticket = self.queue.active_ticket()
        if ticket is not None and str(ticket.ticket_id) == str(ticket_id):
            self._ticket_activity[str(ticket_id)] = str(ticket.intent.activity)
        event = ExecutionEvent(
            event_id=str(uuid.uuid4()),
            tick=int(tick),
            source="runner",
            kind=ExecutionEventKind.DISPATCH_ENQUEUED,
            ticket_id=str(ticket_id),
            command_id=str(command_id),
            reason="command_enqueued",
        )
        self.queue.record_event(event)
        self._advance_phases(int(tick))

    def on_dispatch_failed(self, *, ticket_id: str, tick: int, reason: str) -> None:
        event = ExecutionEvent(
            event_id=str(uuid.uuid4()),
            tick=int(tick),
            source="runner",
            kind=ExecutionEventKind.EXECUTOR_FAILED_RETRYABLE,
            ticket_id=str(ticket_id),
            retryable=True,
            reason=str(reason),
        )
        self.queue.record_event(event)
        self._advance_phases(int(tick))

    def on_executor_row(self, row: dict) -> None:
        command_id = str(row.get("commandId") or "")
        ticket_id = self._command_id_to_ticket.get(command_id)
        if not ticket_id:
            return

        tick = int(row.get("commandTick", -1))
        if tick < 0:
            tick = int(row.get("tick", 0))
        status = str(row.get("status") or "").strip().lower()
        event_type = str(row.get("eventType") or "").strip().upper()
        reason = str(row.get("reason") or "")

        kind: ExecutionEventKind
        retryable = None
        if event_type == "DISPATCHED":
            kind = ExecutionEventKind.EXECUTOR_DISPATCHED
        elif event_type == "DEFERRED":
            kind = ExecutionEventKind.EXECUTOR_DEFERRED
        elif event_type == "FAILED":
            kind = ExecutionEventKind.EXECUTOR_FAILED_RETRYABLE
            retryable = True
        elif event_type == "TERMINAL":
            kind = ExecutionEventKind.EXECUTOR_FAILED_TERMINAL
            retryable = False
        elif status == "executed":
            if reason.endswith("_dispatched") or reason.endswith("_complete") or reason.endswith("_entered"):
                kind = ExecutionEventKind.EXECUTOR_DISPATCHED
            else:
                kind = ExecutionEventKind.EXECUTOR_DEFERRED
        elif status in ("failed", "rejected"):
            kind = ExecutionEventKind.EXECUTOR_FAILED_RETRYABLE
            retryable = True
        else:
            kind = ExecutionEventKind.EXECUTOR_DEFERRED

        event = ExecutionEvent(
            event_id=str(uuid.uuid4()),
            tick=int(tick),
            source="executor",
            kind=kind,
            ticket_id=ticket_id,
            command_id=command_id or None,
            retryable=retryable,
            reason=reason,
            details=row.get("details") if isinstance(row.get("details"), dict) else {},
        )
        self.queue.record_event(event)
        self._record_executor_outcome_telemetry(
            ticket_id=ticket_id,
            tick=int(tick),
            kind=kind,
            reason=reason,
            row_source=str(row.get("source") or ""),
        )
        self._advance_phases(int(tick))

    def telemetry_snapshot(self) -> dict:
        per_activity: dict[str, dict[str, int]] = {}
        for activity in sorted(self._outcome_counts_by_activity):
            counts = self._outcome_counts_by_activity[activity]
            per_activity[activity] = {
                "DISPATCHED": int(counts.get("DISPATCHED", 0)),
                "DEFERRED": int(counts.get("DEFERRED", 0)),
                "FAILED": int(counts.get("FAILED", 0)),
            }

        deferred_top = [
            {"reason": str(reason), "count": int(count)}
            for reason, count in self._deferred_reason_counts.most_common(10)
        ]
        behavioral_timing_deferred_top = [
            {"reason": str(reason), "count": int(count)}
            for reason, count in self._behavioral_timing_deferred_reason_counts.most_common(10)
        ]

        dispatch_to_effect_avg_ticks_by_activity: dict[str, dict[str, float | int]] = {}
        for activity in sorted(self._dispatch_to_effect_ticks_by_activity):
            samples = self._dispatch_to_effect_ticks_by_activity[activity]
            if not samples:
                continue
            dispatch_to_effect_avg_ticks_by_activity[activity] = {
                "avg_ticks": float(sum(samples) / len(samples)),
                "samples": int(len(samples)),
            }

        retry_reasons = [
            {"reason": str(reason), "count": int(count)}
            for reason, count in self._retry_transition_reason_counts.most_common(10)
        ]
        retry_total = int(sum(self._retry_transition_reason_counts.values()))
        retry_non_scheduler = int(
            sum(
                count
                for reason, count in self._retry_transition_reason_counts.items()
                if not str(reason).startswith("scheduler_")
            )
        )

        return {
            "outcomes_by_activity": per_activity,
            "deferred_reasons_top": deferred_top,
            "behavioral_timing_deferred_reasons_top": behavioral_timing_deferred_top,
            "dispatch_to_effect_avg_ticks_by_activity": dispatch_to_effect_avg_ticks_by_activity,
            "retry_transition_total": retry_total,
            "retry_transition_non_scheduler_total": retry_non_scheduler,
            "retry_transition_reasons_top": retry_reasons,
        }

    def _advance_phases(self, tick: int) -> None:
        # Bounded loop to allow same-tick multi-step phase movement.
        for _ in range(8):
            view = self.queue.view()
            ticket = view.active_ticket
            if ticket is None:
                return
            transition = self.scheduler.propose(tick=int(tick), ticket=ticket, queue_view=view)
            if transition is None:
                return
            if transition.to_phase == ActionPhase.RETRY_WAIT:
                self._retry_transition_reason_counts[str(transition.reason)] += 1
            gate = self.gate.evaluate(ticket=ticket, proposed=transition)
            prior = ticket.phase
            self.queue.apply_transition(transition, gate)
            if gate.verdict != GateVerdict.ALLOW:
                return
            if ticket.phase == prior:
                return

    def _build_dispatch_requests(self, snapshot: Snapshot) -> list[DispatchRequest]:
        ticket = self.queue.active_ticket()
        if ticket is None:
            return []
        if ticket.phase != ActionPhase.DISPATCHING or ticket.dispatch_requested:
            return []

        cmd = self._command_for_ticket(ticket, snapshot)
        if cmd is None:
            return []
        self.queue.mark_dispatch_requested(
            ticket_id=ticket.ticket_id,
            tick=int(snapshot.tick),
            context=self._dispatch_context(ticket, snapshot),
        )
        return [DispatchRequest(ticket_id=ticket.ticket_id, command=cmd)]

    def _dispatch_context(self, ticket, snapshot: Snapshot) -> dict:
        if ticket.intent.kind == IntentKind.DROP_ITEM:
            item_id = int(ticket.intent.params.get("itemId", -1))
            count = int(snapshot.inventory_counts.get(item_id, 0))
            return {"drop_item_id": item_id, "drop_count_before": count}
        if ticket.intent.kind == IntentKind.WITHDRAW_ITEM:
            item_id = int(ticket.intent.params.get("itemId", -1))
            count = int(snapshot.inventory_counts.get(item_id, 0))
            return {"bank_probe_item_id": item_id, "inventory_item_count_before": count}
        if ticket.intent.kind == IntentKind.DEPOSIT_ITEM:
            item_id = int(ticket.intent.params.get("itemId", -1))
            count = int(snapshot.inventory_counts.get(item_id, 0))
            return {"bank_probe_item_id": item_id, "inventory_item_count_before": count}
        if ticket.intent.kind == IntentKind.EAT_FOOD:
            item_id = int(ticket.intent.params.get("itemId", -1))
            count = int(snapshot.inventory_counts.get(item_id, 0))
            hp_before = int(snapshot.hitpoints_current) if snapshot.hitpoints_current is not None else -1
            return {
                "food_item_id": item_id,
                "food_count_before": count,
                "hitpoints_before": hp_before,
            }
        if ticket.intent.kind == IntentKind.SHOP_BUY_ITEM:
            item_id = int(ticket.intent.params.get("itemId", -1))
            count = int(snapshot.inventory_counts.get(item_id, 0)) if item_id > 0 else 0
            return {"shop_item_id": item_id, "shop_item_count_before": count}
        if ticket.intent.kind == IntentKind.WORLD_HOP:
            before_world_id = int(snapshot.world_id) if snapshot.world_id is not None else -1
            return {"world_id_before": before_world_id}
        return {}

    def _command_for_ticket(self, ticket, snapshot: Snapshot) -> RuntimeCommand | None:
        intent = ticket.intent
        payload: dict = {}
        if intent.kind == IntentKind.WALK_TO_WORLDPOINT:
            target_world_x = int(intent.params.get("targetWorldX", -1))
            target_world_y = int(intent.params.get("targetWorldY", -1))
            target_plane = int(intent.params.get("targetPlane", 0))
            arrive_distance_tiles = max(0, int(intent.params.get("arriveDistanceTiles", 1)))
            walk_click_mode = str(intent.params.get("walkClickMode", "MIXED")).strip().upper() or "MIXED"
            if walk_click_mode not in {"SCENE", "MINIMAP", "MIXED"}:
                walk_click_mode = "MIXED"
            minimap_click_chance_pct = max(0, min(100, int(intent.params.get("minimapClickChancePct", 95))))
            if target_world_x <= 0 or target_world_y <= 0:
                return None
            payload = {
                "interactionKind": "WALK_TO_WORLDPOINT",
                "interactionMode": "left_click",
                "targetWorldX": target_world_x,
                "targetWorldY": target_world_y,
                "targetPlane": target_plane,
                "arriveDistanceTiles": arrive_distance_tiles,
                "walkClickMode": walk_click_mode,
                "minimapClickChancePct": minimap_click_chance_pct,
                "plannerTag": intent.activity,
            }
            self.motion_engine.decorate_payload(payload)
            return RuntimeCommand(
                command_type="WALK_TO_WORLDPOINT_SAFE",
                payload=payload,
                reason=intent.reason,
                tick=int(snapshot.tick),
                source=intent.activity,
            )

        if intent.kind == IntentKind.SCENE_OBJECT_ACTION:
            payload = {
                "interactionKind": "SCENE_OBJECT_ACTION",
                "interactionMode": "left_click",
                "plannerTag": intent.activity,
            }
            if "targetObjectId" in intent.params:
                payload["targetObjectId"] = int(intent.params["targetObjectId"])
            if "targetObjectNameContains" in intent.params:
                payload["targetObjectNameContains"] = str(intent.params["targetObjectNameContains"])
            if "targetWorldX" in intent.params:
                payload["targetWorldX"] = int(intent.params["targetWorldX"])
            if "targetWorldY" in intent.params:
                payload["targetWorldY"] = int(intent.params["targetWorldY"])
            if "targetPlane" in intent.params:
                payload["targetPlane"] = int(intent.params["targetPlane"])
            if "minWorldX" in intent.params:
                payload["minWorldX"] = int(intent.params["minWorldX"])
            if "maxWorldX" in intent.params:
                payload["maxWorldX"] = int(intent.params["maxWorldX"])
            if "minWorldY" in intent.params:
                payload["minWorldY"] = int(intent.params["minWorldY"])
            if "maxWorldY" in intent.params:
                payload["maxWorldY"] = int(intent.params["maxWorldY"])
            if "maxDistanceTiles" in intent.params:
                payload["maxDistanceTiles"] = int(intent.params["maxDistanceTiles"])
            if "optionKeyword" in intent.params:
                payload["optionKeyword"] = str(intent.params["optionKeyword"])
            if "optionKeywords" in intent.params:
                keywords = []
                for value in intent.params["optionKeywords"]:
                    token = str(value).strip()
                    if token:
                        keywords.append(token)
                if keywords:
                    payload["optionKeywords"] = keywords
            if "routeStepIndex" in intent.params:
                payload["routeStepIndex"] = int(intent.params["routeStepIndex"])
            if "routeStepCount" in intent.params:
                payload["routeStepCount"] = int(intent.params["routeStepCount"])
            self.motion_engine.decorate_payload(payload)
            command_type = "SCENE_OBJECT_ACTION_SAFE"
            if (
                str(intent.activity or "").strip().lower() == "agility"
                and "routeStepIndex" in payload
                and "routeStepCount" in payload
            ):
                command_type = "AGILITY_OBSTACLE_ACTION_SAFE"
            return RuntimeCommand(
                command_type=command_type,
                payload=payload,
                reason=intent.reason,
                tick=int(snapshot.tick),
                source=intent.activity,
            )

        if intent.kind == IntentKind.GROUND_ITEM_ACTION:
            payload = {
                "interactionKind": "GROUND_ITEM_ACTION",
                "interactionMode": "left_click",
                "plannerTag": intent.activity,
            }
            if "targetItemId" in intent.params:
                payload["targetItemId"] = int(intent.params["targetItemId"])
            if "targetItemNameContains" in intent.params:
                payload["targetItemNameContains"] = str(intent.params["targetItemNameContains"])
            if "targetWorldX" in intent.params:
                payload["targetWorldX"] = int(intent.params["targetWorldX"])
            if "targetWorldY" in intent.params:
                payload["targetWorldY"] = int(intent.params["targetWorldY"])
            if "targetPlane" in intent.params:
                payload["targetPlane"] = int(intent.params["targetPlane"])
            if "minWorldX" in intent.params:
                payload["minWorldX"] = int(intent.params["minWorldX"])
            if "maxWorldX" in intent.params:
                payload["maxWorldX"] = int(intent.params["maxWorldX"])
            if "minWorldY" in intent.params:
                payload["minWorldY"] = int(intent.params["minWorldY"])
            if "maxWorldY" in intent.params:
                payload["maxWorldY"] = int(intent.params["maxWorldY"])
            if "maxDistanceTiles" in intent.params:
                payload["maxDistanceTiles"] = int(intent.params["maxDistanceTiles"])
            if "optionKeyword" in intent.params:
                payload["optionKeyword"] = str(intent.params["optionKeyword"])
            if "optionKeywords" in intent.params:
                keywords = []
                for value in intent.params["optionKeywords"]:
                    token = str(value).strip()
                    if token:
                        keywords.append(token)
                if keywords:
                    payload["optionKeywords"] = keywords
            self.motion_engine.decorate_payload(payload)
            return RuntimeCommand(
                command_type="GROUND_ITEM_ACTION_SAFE",
                payload=payload,
                reason=intent.reason,
                tick=int(snapshot.tick),
                source=intent.activity,
            )

        if intent.kind == IntentKind.CHOP_TREE:
            target_category = str(intent.params.get("targetCategory", "SELECTED")).strip().upper() or "SELECTED"
            target_world_x = int(intent.params.get("targetWorldX", -1))
            target_world_y = int(intent.params.get("targetWorldY", -1))
            target_max_distance = int(intent.params.get("targetMaxDistance", -1))
            activity_key = str(intent.activity or "").strip().lower() or "unknown"

            has_explicit_anchor = target_world_x > 0 and target_world_y > 0
            implicit_anchor_mode = (
                not has_explicit_anchor
                and target_category in {"NORMAL", "OAK", "WILLOW"}
                and target_max_distance > 0
            )

            if has_explicit_anchor:
                self._woodcut_implicit_area_anchor_by_activity.pop(activity_key, None)
            elif implicit_anchor_mode:
                anchored = self._woodcut_implicit_area_anchor_by_activity.get(activity_key)
                if anchored is None:
                    player_position = self._snapshot_player_world_position(snapshot)
                    if player_position is not None:
                        anchored = (int(player_position[0]), int(player_position[1]))
                        self._woodcut_implicit_area_anchor_by_activity[activity_key] = anchored
                if anchored is not None:
                    target_world_x = int(anchored[0])
                    target_world_y = int(anchored[1])
            else:
                self._woodcut_implicit_area_anchor_by_activity.pop(activity_key, None)

            payload = {
                "interactionKind": "CHOP_NEAREST_TREE",
                "interactionMode": "left_click",
                "targetCategory": target_category,
                "plannerTag": intent.activity,
            }
            if target_world_x > 0:
                payload["targetWorldX"] = target_world_x
            if target_world_y > 0:
                payload["targetWorldY"] = target_world_y
            if "targetMaxDistance" in intent.params:
                payload["targetMaxDistance"] = int(intent.params["targetMaxDistance"])
            self.motion_engine.decorate_payload(payload)
            return RuntimeCommand(
                command_type="WOODCUT_CHOP_NEAREST_TREE_SAFE",
                payload=payload,
                reason=intent.reason,
                tick=int(snapshot.tick),
                source=intent.activity,
            )

        if intent.kind == IntentKind.MINE_ROCK:
            payload = {
                "interactionKind": "MINE_NEAREST_ROCK",
                "interactionMode": "left_click",
                "targetCategory": "SELECTED",
                "plannerTag": intent.activity,
            }
            if "targetWorldX" in intent.params:
                payload["targetWorldX"] = int(intent.params["targetWorldX"])
            if "targetWorldY" in intent.params:
                payload["targetWorldY"] = int(intent.params["targetWorldY"])
            self.motion_engine.decorate_payload(payload)
            return RuntimeCommand(
                command_type="MINE_NEAREST_ROCK_SAFE",
                payload=payload,
                reason=intent.reason,
                tick=int(snapshot.tick),
                source=intent.activity,
            )

        if intent.kind == IntentKind.FISH_SPOT:
            payload = {
                "interactionKind": "FISH_NEAREST_SPOT",
                "interactionMode": "left_click",
                "targetCategory": str(intent.params.get("targetCategory", "NEAREST_FISHING_SPOT")),
                "plannerTag": intent.activity,
            }
            if "targetNpcIds" in intent.params:
                npc_ids = []
                for value in intent.params["targetNpcIds"]:
                    npc_id = int(value)
                    if npc_id > 0:
                        npc_ids.append(npc_id)
                if npc_ids:
                    payload["targetNpcIds"] = npc_ids
            for key in ("moveAccelPercent", "moveDecelPercent", "terminalSlowdownRadiusPx"):
                if key in intent.params:
                    payload[key] = int(intent.params[key])
            self.motion_engine.decorate_payload(payload)
            return RuntimeCommand(
                command_type="FISH_NEAREST_SPOT_SAFE",
                payload=payload,
                reason=intent.reason,
                tick=int(snapshot.tick),
                source=intent.activity,
            )

        if intent.kind == IntentKind.ATTACK_NPC:
            payload = {
                "interactionKind": "ATTACK_NEAREST_NPC",
                "interactionMode": "left_click",
                "targetCategory": str(intent.params.get("targetCategory", "NEAREST_ATTACKABLE")),
                "plannerTag": intent.activity,
            }
            if "targetNpcIds" in intent.params:
                npc_ids = []
                for value in intent.params["targetNpcIds"]:
                    npc_id = int(value)
                    if npc_id > 0:
                        npc_ids.append(npc_id)
                if npc_ids:
                    payload["targetNpcIds"] = npc_ids
            if "targetNpcId" in intent.params:
                payload["targetNpcId"] = int(intent.params["targetNpcId"])
            if "encounterProfile" in intent.params:
                payload["encounterProfile"] = str(intent.params["encounterProfile"]).strip().lower()
            if "targetWorldX" in intent.params:
                payload["targetWorldX"] = int(intent.params["targetWorldX"])
            if "targetWorldY" in intent.params:
                payload["targetWorldY"] = int(intent.params["targetWorldY"])
            if "targetMaxDistance" in intent.params:
                payload["targetMaxDistance"] = int(intent.params["targetMaxDistance"])
            if "maxChaseDistance" in intent.params:
                payload["maxChaseDistance"] = int(intent.params["maxChaseDistance"])
            self.motion_engine.decorate_payload(payload)
            return RuntimeCommand(
                command_type="COMBAT_ATTACK_NEAREST_NPC_SAFE",
                payload=payload,
                reason=intent.reason,
                tick=int(snapshot.tick),
                source=intent.activity,
            )

        if intent.kind == IntentKind.NPC_CONTEXT_ACTION:
            payload = {
                "interactionKind": "NPC_CONTEXT_ACTION",
                "interactionMode": "left_click",
                "plannerTag": intent.activity,
            }
            if "targetNpcId" in intent.params:
                payload["targetNpcId"] = int(intent.params["targetNpcId"])
            if "targetNpcNameContains" in intent.params:
                payload["targetNpcNameContains"] = str(intent.params["targetNpcNameContains"])
            if "maxDistanceTiles" in intent.params:
                payload["maxDistanceTiles"] = int(intent.params["maxDistanceTiles"])
            if "followupOnly" in intent.params:
                payload["followupOnly"] = bool(intent.params["followupOnly"])
            if "optionKeyword" in intent.params:
                payload["optionKeyword"] = str(intent.params["optionKeyword"])
            if "optionKeywords" in intent.params:
                keywords = []
                for value in intent.params["optionKeywords"]:
                    token = str(value).strip()
                    if token:
                        keywords.append(token)
                if keywords:
                    payload["optionKeywords"] = keywords
            self.motion_engine.decorate_payload(payload)
            return RuntimeCommand(
                command_type="NPC_CONTEXT_MENU_TEST",
                payload=payload,
                reason=intent.reason,
                tick=int(snapshot.tick),
                source=intent.activity,
            )

        if intent.kind == IntentKind.EAT_FOOD:
            item_id = int(intent.params.get("itemId", -1))
            if item_id <= 0:
                return None
            payload = {
                "itemId": item_id,
                "interactionKind": "EAT_FOOD",
                "interactionMode": "left_click",
                "plannerTag": intent.activity,
            }
            self.motion_engine.decorate_payload(payload)
            return RuntimeCommand(
                command_type="EAT_FOOD_SAFE",
                payload=payload,
                reason=intent.reason,
                tick=int(snapshot.tick),
                source=intent.activity,
            )

        if intent.kind == IntentKind.SHOP_BUY_ITEM:
            item_id = int(intent.params.get("itemId", -1))
            quantity = max(1, int(intent.params.get("quantity", 5)))
            if item_id <= 0:
                return None
            payload = {
                "itemId": item_id,
                "quantity": quantity,
                "interactionKind": "SHOP_BUY_ITEM",
                "interactionMode": "left_click",
                "plannerTag": intent.activity,
            }
            self.motion_engine.decorate_payload(payload)
            return RuntimeCommand(
                command_type="SHOP_BUY_ITEM_SAFE",
                payload=payload,
                reason=intent.reason,
                tick=int(snapshot.tick),
                source=intent.activity,
            )

        if intent.kind == IntentKind.WORLD_HOP:
            payload = {
                "interactionKind": "WORLD_HOP",
                "plannerTag": intent.activity,
            }
            if "targetWorld" in intent.params:
                payload["targetWorld"] = int(intent.params["targetWorld"])
            self.motion_engine.decorate_payload(payload)
            return RuntimeCommand(
                command_type="WORLD_HOP_SAFE",
                payload=payload,
                reason=intent.reason,
                tick=int(snapshot.tick),
                source=intent.activity,
            )

        if intent.kind == IntentKind.OPEN_BANK:
            target_world_x = int(intent.params.get("targetWorldX", -1))
            target_world_y = int(intent.params.get("targetWorldY", -1))
            if target_world_x <= 0 or target_world_y <= 0:
                return None
            payload = {
                "targetWorldX": target_world_x,
                "targetWorldY": target_world_y,
                "interactionKind": "OPEN_BANK",
                "interactionMode": "left_click",
                "targetCategory": "BANK_OBJECT",
                "plannerTag": intent.activity,
            }
            self.motion_engine.decorate_payload(payload)
            return RuntimeCommand(
                command_type="BANK_OPEN_SAFE",
                payload=payload,
                reason=intent.reason,
                tick=int(snapshot.tick),
                source=intent.activity,
            )

        if intent.kind == IntentKind.WITHDRAW_ITEM:
            item_id = int(intent.params.get("itemId", -1))
            quantity_raw = str(intent.params.get("quantity", "1")).strip().upper()
            if item_id <= 0:
                return None
            quantity = quantity_raw if quantity_raw else "1"
            payload = {
                "itemId": item_id,
                "quantity": quantity,
                "interactionKind": "WITHDRAW_ITEM",
                "plannerTag": intent.activity,
            }
            self.motion_engine.decorate_payload(payload)
            return RuntimeCommand(
                command_type="WITHDRAW_ITEM",
                payload=payload,
                reason=intent.reason,
                tick=int(snapshot.tick),
                source=intent.activity,
            )

        if intent.kind == IntentKind.DEPOSIT_ITEM:
            item_id = int(intent.params.get("itemId", -1))
            quantity_raw = str(intent.params.get("quantity", "ALL")).strip().upper()
            if item_id <= 0:
                return None
            quantity = quantity_raw if quantity_raw else "ALL"
            payload = {
                "itemId": item_id,
                "quantity": quantity,
                "interactionKind": "DEPOSIT_ITEM",
                "plannerTag": intent.activity,
            }
            self.motion_engine.decorate_payload(payload)
            return RuntimeCommand(
                command_type="DEPOSIT_ITEM",
                payload=payload,
                reason=intent.reason,
                tick=int(snapshot.tick),
                source=intent.activity,
            )

        if intent.kind == IntentKind.CLOSE_BANK:
            payload = {
                "interactionKind": "CLOSE_BANK",
                "plannerTag": intent.activity,
            }
            self.motion_engine.decorate_payload(payload)
            return RuntimeCommand(
                command_type="CLOSE_BANK",
                payload=payload,
                reason=intent.reason,
                tick=int(snapshot.tick),
                source=intent.activity,
            )

        if intent.kind == IntentKind.DROP_ITEM:
            item_id = int(intent.params.get("itemId", -1))
            if item_id <= 0:
                return None
            payload = {
                "itemId": item_id,
                "interactionKind": "DROP_ITEM",
                "interactionMode": "left_click",
                "dropMode": "left_click_dropper",
                "plannerTag": intent.activity,
            }
            self.motion_engine.decorate_payload(payload)
            return RuntimeCommand(
                command_type="DROP_ITEM_SAFE",
                payload=payload,
                reason=intent.reason,
                tick=int(snapshot.tick),
                source=intent.activity,
            )

        if intent.kind == IntentKind.START_DROP_SESSION:
            item_id = int(intent.params.get("itemId", -1))
            if item_id <= 0:
                return None
            payload = {
                "itemId": item_id,
                "interactionKind": "START_DROP_SESSION",
                "dropMode": "left_click_dropper",
                "plannerTag": intent.activity,
            }
            if "itemIds" in intent.params:
                item_ids = []
                for value in intent.params["itemIds"]:
                    parsed = int(value)
                    if parsed > 0:
                        item_ids.append(parsed)
                if item_ids:
                    payload["itemIds"] = item_ids
            if "dropCadenceProfile" in intent.params:
                token = str(intent.params["dropCadenceProfile"]).strip().upper()
                if token == "DB_PARITY":
                    payload["dropCadenceProfile"] = token
            drop_cadence_tuning = intent.params.get("dropCadenceTuning")
            if isinstance(drop_cadence_tuning, Mapping):
                sanitized_tuning: dict[str, int] = {}
                allowed_tuning_keys = (
                    "localCooldownMinMs",
                    "localCooldownMaxMs",
                    "secondDispatchChancePercent",
                    "rhythmPauseChanceMinPercent",
                    "rhythmPauseChanceMaxPercent",
                    "rhythmPauseRampStartDispatches",
                    "sessionTickSkipChancePercent",
                    "sessionBurstPauseThreshold",
                    "sessionBurstPauseChancePercent",
                    "sessionCooldownBiasMs",
                    "targetCycleClicksMedian",
                    "targetCycleDurationMsMedian",
                )
                for tuning_key in allowed_tuning_keys:
                    if tuning_key not in drop_cadence_tuning:
                        continue
                    try:
                        parsed = int(drop_cadence_tuning[tuning_key])
                    except (TypeError, ValueError):
                        continue
                    sanitized_tuning[tuning_key] = parsed
                if sanitized_tuning:
                    payload["dropCadenceTuning"] = sanitized_tuning
            if str(intent.activity).strip().lower() in ("fishing", "woodcutting"):
                if "dropCadenceTuning" not in payload:
                    return None
            idle_cadence_tuning = intent.params.get("idleCadenceTuning")
            if isinstance(idle_cadence_tuning, Mapping):
                sanitized_idle_tuning: dict[str, int] = {}
                allowed_idle_tuning_keys = (
                    "fishingIdleMinIntervalTicks",
                    "fishingIdleMaxIntervalTicks",
                    "fishingIdleRetryMinIntervalTicks",
                    "fishingIdleRetryMaxIntervalTicks",
                    "fishingDbParityIdleMinIntervalTicks",
                    "fishingDbParityIdleMaxIntervalTicks",
                    "fishingDbParityIdleRetryMinIntervalTicks",
                    "fishingDbParityIdleRetryMaxIntervalTicks",
                    "postDropIdleCooldownMinTicks",
                    "postDropIdleCooldownMaxTicks",
                    "postDropIdleDbParityCooldownMinTicks",
                    "postDropIdleDbParityCooldownMaxTicks",
                    "offscreenWindowMarginMinPx",
                    "offscreenWindowMarginMaxPx",
                    "offscreenNearTargetMaxGapPx",
                    "offscreenFarTargetMinGapPx",
                    "offscreenFarTargetMaxGapPx",
                    "profileHoverChancePercent",
                    "profileDriftChancePercent",
                    "profileCameraChancePercent",
                    "profileNoopChancePercent",
                    "profileParkAfterBurstMinActions",
                    "profileParkAfterBurstChancePercent",
                )
                for tuning_key in allowed_idle_tuning_keys:
                    if tuning_key not in idle_cadence_tuning:
                        continue
                    try:
                        parsed = int(idle_cadence_tuning[tuning_key])
                    except (TypeError, ValueError):
                        continue
                    sanitized_idle_tuning[tuning_key] = parsed
                if sanitized_idle_tuning:
                    payload["idleCadenceTuning"] = sanitized_idle_tuning
            self.motion_engine.decorate_payload(payload)
            return RuntimeCommand(
                command_type="DROP_START_SESSION",
                payload=payload,
                reason=intent.reason,
                tick=int(snapshot.tick),
                source=intent.activity,
            )

        if intent.kind == IntentKind.STOP_DROP_SESSION:
            item_id = int(intent.params.get("itemId", -1))
            payload = {
                "interactionKind": "STOP_DROP_SESSION",
                "plannerTag": intent.activity,
            }
            if item_id > 0:
                payload["itemId"] = item_id
            self.motion_engine.decorate_payload(payload)
            return RuntimeCommand(
                command_type="DROP_STOP_SESSION",
                payload=payload,
                reason=intent.reason,
                tick=int(snapshot.tick),
                source=intent.activity,
            )

        return None

    def _record_snapshot_effects(self, snapshot: Snapshot) -> None:
        ticket = self.queue.active_ticket()
        if ticket is None or ticket.phase != ActionPhase.AWAITING_CONFIRMATION:
            return
        if ticket.intent.kind == IntentKind.WALK_TO_WORLDPOINT:
            player_position = self._snapshot_player_world_position(snapshot)
            target_world_x = int(ticket.intent.params.get("targetWorldX", -1))
            target_world_y = int(ticket.intent.params.get("targetWorldY", -1))
            target_plane = int(ticket.intent.params.get("targetPlane", 0))
            arrive_distance_tiles = max(0, int(ticket.intent.params.get("arriveDistanceTiles", 1)))
            if (
                player_position is not None
                and target_world_x > 0
                and target_world_y > 0
                and player_position[2] == target_plane
            ):
                distance = max(
                    abs(player_position[0] - target_world_x),
                    abs(player_position[1] - target_world_y),
                )
                if distance <= arrive_distance_tiles:
                    self.queue.record_event(
                        ExecutionEvent(
                            event_id=str(uuid.uuid4()),
                            tick=int(snapshot.tick),
                            source="snapshot_observer",
                            kind=ExecutionEventKind.EFFECT_OBSERVED,
                            ticket_id=ticket.ticket_id,
                            reason="arrived_at_walk_target",
                            details={
                                "targetWorldX": target_world_x,
                                "targetWorldY": target_world_y,
                                "targetPlane": target_plane,
                                "arriveDistanceTiles": arrive_distance_tiles,
                                "distance": distance,
                            },
                        )
                    )
                    self._record_effect_observed(ticket_id=str(ticket.ticket_id), tick=int(snapshot.tick))
                    return

        if ticket.intent.kind == IntentKind.CHOP_TREE:
            anim = snapshot.player_animation
            if anim not in (None, -1, 0):
                self.queue.record_event(
                    ExecutionEvent(
                        event_id=str(uuid.uuid4()),
                        tick=int(snapshot.tick),
                        source="snapshot_observer",
                        kind=ExecutionEventKind.EFFECT_OBSERVED,
                        ticket_id=ticket.ticket_id,
                        reason="animation_started_after_chop",
                    )
                )
                self._record_effect_observed(ticket_id=str(ticket.ticket_id), tick=int(snapshot.tick))
                return

        if ticket.intent.kind == IntentKind.MINE_ROCK:
            anim = snapshot.player_animation
            if anim not in (None, -1, 0):
                self.queue.record_event(
                    ExecutionEvent(
                        event_id=str(uuid.uuid4()),
                        tick=int(snapshot.tick),
                        source="snapshot_observer",
                        kind=ExecutionEventKind.EFFECT_OBSERVED,
                        ticket_id=ticket.ticket_id,
                        reason="animation_started_after_mine",
                    )
                )
                self._record_effect_observed(ticket_id=str(ticket.ticket_id), tick=int(snapshot.tick))
                return

        if ticket.intent.kind == IntentKind.FISH_SPOT:
            anim = snapshot.player_animation
            if anim not in (None, -1, 0):
                self.queue.record_event(
                    ExecutionEvent(
                        event_id=str(uuid.uuid4()),
                        tick=int(snapshot.tick),
                        source="snapshot_observer",
                        kind=ExecutionEventKind.EFFECT_OBSERVED,
                        ticket_id=ticket.ticket_id,
                        reason="animation_started_after_fish",
                    )
                )
                self._record_effect_observed(ticket_id=str(ticket.ticket_id), tick=int(snapshot.tick))
                return

        if ticket.intent.kind == IntentKind.ATTACK_NPC:
            anim = snapshot.player_animation
            if anim not in (None, -1, 0):
                self.queue.record_event(
                    ExecutionEvent(
                        event_id=str(uuid.uuid4()),
                        tick=int(snapshot.tick),
                        source="snapshot_observer",
                        kind=ExecutionEventKind.EFFECT_OBSERVED,
                        ticket_id=ticket.ticket_id,
                        reason="animation_started_after_attack",
                    )
                )
                self._record_effect_observed(ticket_id=str(ticket.ticket_id), tick=int(snapshot.tick))
                return

        if ticket.intent.kind == IntentKind.NPC_CONTEXT_ACTION:
            expected_effect = str(ticket.intent.params.get("expectedEffect", "")).strip().lower()
            if expected_effect == "shop_open" and bool(snapshot.shop_open):
                self.queue.record_event(
                    ExecutionEvent(
                        event_id=str(uuid.uuid4()),
                        tick=int(snapshot.tick),
                        source="snapshot_observer",
                        kind=ExecutionEventKind.EFFECT_OBSERVED,
                        ticket_id=ticket.ticket_id,
                        reason="shop_open_after_npc_context_action",
                    )
                )
                self._record_effect_observed(ticket_id=str(ticket.ticket_id), tick=int(snapshot.tick))
                return

        if ticket.intent.kind == IntentKind.EAT_FOOD:
            item_id = int(ticket.context.get("food_item_id", -1))
            before_count = int(ticket.context.get("food_count_before", 0))
            after_count = int(snapshot.inventory_counts.get(item_id, 0)) if item_id > 0 else 0
            hp_before = int(ticket.context.get("hitpoints_before", -1))
            hp_after = int(snapshot.hitpoints_current) if snapshot.hitpoints_current is not None else -1
            consumed_food = item_id > 0 and after_count < before_count
            healed = hp_before >= 0 and hp_after >= 0 and hp_after > hp_before
            if consumed_food or healed:
                self.queue.record_event(
                    ExecutionEvent(
                        event_id=str(uuid.uuid4()),
                        tick=int(snapshot.tick),
                        source="snapshot_observer",
                        kind=ExecutionEventKind.EFFECT_OBSERVED,
                        ticket_id=ticket.ticket_id,
                        reason="food_consumed_after_eat",
                        details={
                            "itemId": item_id,
                            "foodBefore": before_count,
                            "foodAfter": after_count,
                            "hitpointsBefore": hp_before,
                            "hitpointsAfter": hp_after,
                        },
                    )
                )
                self._record_effect_observed(ticket_id=str(ticket.ticket_id), tick=int(snapshot.tick))
                return

        if ticket.intent.kind == IntentKind.SHOP_BUY_ITEM:
            item_id = int(ticket.context.get("shop_item_id", -1))
            before = int(ticket.context.get("shop_item_count_before", 0))
            after = int(snapshot.inventory_counts.get(item_id, 0)) if item_id > 0 else 0
            if item_id > 0 and after > before:
                self.queue.record_event(
                    ExecutionEvent(
                        event_id=str(uuid.uuid4()),
                        tick=int(snapshot.tick),
                        source="snapshot_observer",
                        kind=ExecutionEventKind.EFFECT_OBSERVED,
                        ticket_id=ticket.ticket_id,
                        reason="inventory_count_increased_after_shop_buy",
                        details={"itemId": item_id, "before": before, "after": after},
                    )
                )
                self._record_effect_observed(ticket_id=str(ticket.ticket_id), tick=int(snapshot.tick))
                return

        if ticket.intent.kind == IntentKind.WORLD_HOP:
            before_world_id = int(ticket.context.get("world_id_before", -1))
            target_world_id = int(ticket.intent.params.get("targetWorld", -1))
            current_world_id = int(snapshot.world_id) if snapshot.world_id is not None else -1
            if current_world_id <= 0:
                return
            target_reached = target_world_id > 0 and current_world_id == target_world_id
            changed_world = before_world_id > 0 and current_world_id != before_world_id
            if target_reached or (target_world_id <= 0 and changed_world):
                self.queue.record_event(
                    ExecutionEvent(
                        event_id=str(uuid.uuid4()),
                        tick=int(snapshot.tick),
                        source="snapshot_observer",
                        kind=ExecutionEventKind.EFFECT_OBSERVED,
                        ticket_id=ticket.ticket_id,
                        reason="world_changed_after_world_hop",
                        details={
                            "worldBefore": before_world_id,
                            "worldAfter": current_world_id,
                            "targetWorld": target_world_id,
                        },
                    )
                )
                self._record_effect_observed(ticket_id=str(ticket.ticket_id), tick=int(snapshot.tick))
                return

        if ticket.intent.kind == IntentKind.OPEN_BANK:
            if bool(snapshot.bank_open):
                self.queue.record_event(
                    ExecutionEvent(
                        event_id=str(uuid.uuid4()),
                        tick=int(snapshot.tick),
                        source="snapshot_observer",
                        kind=ExecutionEventKind.EFFECT_OBSERVED,
                        ticket_id=ticket.ticket_id,
                        reason="bank_open_after_open_command",
                    )
                )
                self._record_effect_observed(ticket_id=str(ticket.ticket_id), tick=int(snapshot.tick))
                return

        if ticket.intent.kind == IntentKind.WITHDRAW_ITEM:
            item_id = int(ticket.context.get("bank_probe_item_id", -1))
            before = int(ticket.context.get("inventory_item_count_before", 0))
            after = int(snapshot.inventory_counts.get(item_id, 0))
            if item_id > 0 and after > before:
                self.queue.record_event(
                    ExecutionEvent(
                        event_id=str(uuid.uuid4()),
                        tick=int(snapshot.tick),
                        source="snapshot_observer",
                        kind=ExecutionEventKind.EFFECT_OBSERVED,
                        ticket_id=ticket.ticket_id,
                        reason="inventory_count_increased_after_withdraw",
                        details={"itemId": item_id, "before": before, "after": after},
                    )
                )
                self._record_effect_observed(ticket_id=str(ticket.ticket_id), tick=int(snapshot.tick))
                return

        if ticket.intent.kind == IntentKind.DEPOSIT_ITEM:
            item_id = int(ticket.context.get("bank_probe_item_id", -1))
            before = int(ticket.context.get("inventory_item_count_before", 0))
            after = int(snapshot.inventory_counts.get(item_id, 0))
            if item_id > 0 and after < before:
                self.queue.record_event(
                    ExecutionEvent(
                        event_id=str(uuid.uuid4()),
                        tick=int(snapshot.tick),
                        source="snapshot_observer",
                        kind=ExecutionEventKind.EFFECT_OBSERVED,
                        ticket_id=ticket.ticket_id,
                        reason="inventory_count_decreased_after_deposit",
                        details={"itemId": item_id, "before": before, "after": after},
                    )
                )
                self._record_effect_observed(ticket_id=str(ticket.ticket_id), tick=int(snapshot.tick))
                return

        if ticket.intent.kind == IntentKind.CLOSE_BANK:
            if not bool(snapshot.bank_open):
                self.queue.record_event(
                    ExecutionEvent(
                        event_id=str(uuid.uuid4()),
                        tick=int(snapshot.tick),
                        source="snapshot_observer",
                        kind=ExecutionEventKind.EFFECT_OBSERVED,
                        ticket_id=ticket.ticket_id,
                        reason="bank_closed_after_close_command",
                    )
                )
                self._record_effect_observed(ticket_id=str(ticket.ticket_id), tick=int(snapshot.tick))

        if ticket.intent.kind == IntentKind.DROP_ITEM:
            item_id = int(ticket.context.get("drop_item_id", -1))
            before = int(ticket.context.get("drop_count_before", 0))
            after = int(snapshot.inventory_counts.get(item_id, 0))
            if item_id > 0 and after < before:
                self.queue.record_event(
                    ExecutionEvent(
                        event_id=str(uuid.uuid4()),
                        tick=int(snapshot.tick),
                        source="snapshot_observer",
                        kind=ExecutionEventKind.EFFECT_OBSERVED,
                        ticket_id=ticket.ticket_id,
                        reason="inventory_count_decreased_after_drop",
                        details={"itemId": item_id, "before": before, "after": after},
                    )
                )
                self._record_effect_observed(ticket_id=str(ticket.ticket_id), tick=int(snapshot.tick))

    def _record_executor_outcome_telemetry(
        self,
        *,
        ticket_id: str,
        tick: int,
        kind: ExecutionEventKind,
        reason: str,
        row_source: str,
    ) -> None:
        source_activity = str(row_source or "").strip()
        activity = source_activity or self._ticket_activity.get(str(ticket_id), "unknown")
        self._ticket_activity[str(ticket_id)] = activity
        bucket = self._outcome_counts_by_activity[activity]
        if kind == ExecutionEventKind.EXECUTOR_DISPATCHED:
            bucket["DISPATCHED"] += 1
            self._last_dispatched_tick_by_ticket[str(ticket_id)] = int(tick)
            return
        if kind == ExecutionEventKind.EXECUTOR_DEFERRED:
            bucket["DEFERRED"] += 1
            normalized_reason = str(reason or "")
            self._deferred_reason_counts[normalized_reason] += 1
            if self._is_behavioral_timing_reason(normalized_reason):
                self._behavioral_timing_deferred_reason_counts[normalized_reason] += 1
            return
        if kind in (ExecutionEventKind.EXECUTOR_FAILED_RETRYABLE, ExecutionEventKind.EXECUTOR_FAILED_TERMINAL):
            bucket["FAILED"] += 1

    def _record_effect_observed(self, *, ticket_id: str, tick: int) -> None:
        dispatched_tick = self._last_dispatched_tick_by_ticket.pop(str(ticket_id), None)
        if dispatched_tick is None:
            return
        delta = int(tick) - int(dispatched_tick)
        if delta < 0:
            return
        activity = self._ticket_activity.get(str(ticket_id), "unknown")
        self._dispatch_to_effect_ticks_by_activity[activity].append(int(delta))

    @staticmethod
    def _is_behavioral_timing_reason(reason: str) -> bool:
        normalized = str(reason or "").lower()
        if not normalized:
            return False
        timing_tokens = (
            "cooldown",
            "settl",
            "wait",
            "pending",
            "deferred_after_move",
            "timeout",
        )
        return any(token in normalized for token in timing_tokens)

    @staticmethod
    def _snapshot_player_world_position(snapshot: Snapshot) -> tuple[int, int, int] | None:
        raw = snapshot.raw if isinstance(snapshot.raw, dict) else {}
        player = raw.get("player")
        if not isinstance(player, dict):
            return None
        try:
            x = int(player.get("worldX", -1))
            y = int(player.get("worldY", -1))
            plane = int(player.get("plane", -1))
        except (TypeError, ValueError):
            return None
        if x <= 0 or y <= 0 or plane < 0:
            return None
        return (x, y, plane)

