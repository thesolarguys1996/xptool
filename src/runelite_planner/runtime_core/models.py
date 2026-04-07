from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum
from typing import Any, Mapping, Optional, Sequence


class IntentKind(str, Enum):
    WALK_TO_WORLDPOINT = "WALK_TO_WORLDPOINT"
    SCENE_OBJECT_ACTION = "SCENE_OBJECT_ACTION"
    GROUND_ITEM_ACTION = "GROUND_ITEM_ACTION"
    CHOP_TREE = "CHOP_TREE"
    MINE_ROCK = "MINE_ROCK"
    FISH_SPOT = "FISH_SPOT"
    ATTACK_NPC = "ATTACK_NPC"
    NPC_CONTEXT_ACTION = "NPC_CONTEXT_ACTION"
    EAT_FOOD = "EAT_FOOD"
    OPEN_BANK = "OPEN_BANK"
    WITHDRAW_ITEM = "WITHDRAW_ITEM"
    DEPOSIT_ITEM = "DEPOSIT_ITEM"
    CLOSE_BANK = "CLOSE_BANK"
    SHOP_BUY_ITEM = "SHOP_BUY_ITEM"
    WORLD_HOP = "WORLD_HOP"
    DROP_ITEM = "DROP_ITEM"
    START_DROP_SESSION = "START_DROP_SESSION"
    STOP_DROP_SESSION = "STOP_DROP_SESSION"


@dataclass(frozen=True)
class Intent:
    intent_key: str
    activity: str
    kind: IntentKind
    target: Mapping[str, Any]
    params: Mapping[str, Any]
    policy_key: str
    reason: str


@dataclass(frozen=True)
class ResolvedIntent:
    selected: Intent
    snapshot_tick: int
    resolver_reason: str
    dropped_intent_keys: Sequence[str] = field(default_factory=tuple)


class ActionPhase(str, Enum):
    QUEUED = "QUEUED"
    READY_TO_DISPATCH = "READY_TO_DISPATCH"
    DISPATCHING = "DISPATCHING"
    AWAITING_CONFIRMATION = "AWAITING_CONFIRMATION"
    RETRY_WAIT = "RETRY_WAIT"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    CANCELLED = "CANCELLED"


@dataclass
class ActionTicket:
    ticket_id: str
    intent: Intent
    phase: ActionPhase
    created_tick: int
    updated_tick: int
    phase_entered_tick: int
    attempt_count: int = 0
    next_eligible_tick: int = 0
    confirmation_deadline_tick: Optional[int] = None
    dispatch_requested: bool = False
    last_command_id: Optional[str] = None
    last_event_id: Optional[str] = None
    phase_reason: str = ""
    context: dict[str, Any] = field(default_factory=dict)


class ExecutionEventKind(str, Enum):
    DISPATCH_ENQUEUED = "DISPATCH_ENQUEUED"
    EXECUTOR_DISPATCHED = "EXECUTOR_DISPATCHED"
    EXECUTOR_DEFERRED = "EXECUTOR_DEFERRED"
    EXECUTOR_FAILED_RETRYABLE = "EXECUTOR_FAILED_RETRYABLE"
    EXECUTOR_FAILED_TERMINAL = "EXECUTOR_FAILED_TERMINAL"
    EFFECT_OBSERVED = "EFFECT_OBSERVED"
    EFFECT_MISSING_TIMEOUT = "EFFECT_MISSING_TIMEOUT"
    INTENT_REPLACED = "INTENT_REPLACED"


@dataclass(frozen=True)
class ExecutionEvent:
    event_id: str
    tick: int
    source: str
    kind: ExecutionEventKind
    ticket_id: Optional[str] = None
    command_id: Optional[str] = None
    retryable: Optional[bool] = None
    details: Mapping[str, Any] = field(default_factory=dict)
    reason: str = ""


@dataclass(frozen=True)
class PhaseTransition:
    ticket_id: str
    from_phase: ActionPhase
    to_phase: ActionPhase
    tick: int
    reason: str
    data: Mapping[str, Any] = field(default_factory=dict)


class GateVerdict(str, Enum):
    ALLOW = "ALLOW"
    HOLD = "HOLD"
    DENY = "DENY"


@dataclass(frozen=True)
class GateDecision:
    verdict: GateVerdict
    current_phase: ActionPhase
    proposed_phase: ActionPhase
    effective_phase: ActionPhase
    reason: str


@dataclass(frozen=True)
class ActionQueueView:
    active_ticket: Optional[ActionTicket]
    recent_events: Sequence[ExecutionEvent]
