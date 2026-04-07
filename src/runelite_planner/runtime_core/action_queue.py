from __future__ import annotations

from collections import deque
from dataclasses import dataclass, field
import uuid

from .models import (
    ActionPhase,
    ActionQueueView,
    ActionTicket,
    ExecutionEvent,
    ExecutionEventKind,
    GateDecision,
    GateVerdict,
    PhaseTransition,
    ResolvedIntent,
)


@dataclass
class ActionQueue:
    """
    Single source of execution state.
    """

    max_recent_events: int = 256
    _active_ticket: ActionTicket | None = None
    _recent_events: deque[ExecutionEvent] = field(default_factory=deque)

    def view(self) -> ActionQueueView:
        return ActionQueueView(
            active_ticket=self._active_ticket,
            recent_events=tuple(self._recent_events),
        )

    def active_ticket(self) -> ActionTicket | None:
        return self._active_ticket

    def upsert_from_resolved_intent(self, resolved: ResolvedIntent) -> ActionTicket:
        current = self._active_ticket
        selected = resolved.selected
        tick = int(resolved.snapshot_tick)

        if current is None:
            self._active_ticket = self._new_ticket_from_intent(selected, tick)
            return self._active_ticket

        if current.phase in (ActionPhase.COMPLETED, ActionPhase.FAILED, ActionPhase.CANCELLED):
            self._active_ticket = self._new_ticket_from_intent(selected, tick)
            return self._active_ticket

        if current.intent.intent_key == selected.intent_key:
            return current

        # Explicit cancellation event when resolver switches intent.
        replacement_event = ExecutionEvent(
            event_id=str(uuid.uuid4()),
            tick=tick,
            source="intent_resolver",
            kind=ExecutionEventKind.INTENT_REPLACED,
            ticket_id=current.ticket_id,
            details={
                "fromIntentKey": current.intent.intent_key,
                "toIntentKey": selected.intent_key,
            },
            reason="intent_replaced_by_resolver",
        )
        self.record_event(replacement_event)
        current.phase = ActionPhase.CANCELLED
        current.updated_tick = tick
        current.phase_entered_tick = tick
        current.phase_reason = "intent_replaced_by_resolver"
        current.last_event_id = replacement_event.event_id
        self._active_ticket = self._new_ticket_from_intent(selected, tick)
        return self._active_ticket

    def apply_transition(self, transition: PhaseTransition, gate: GateDecision) -> ActionTicket:
        ticket = self._active_ticket
        if ticket is None:
            raise RuntimeError("no_active_ticket")
        if ticket.ticket_id != transition.ticket_id:
            raise RuntimeError("transition_ticket_mismatch")
        if gate.verdict != GateVerdict.ALLOW:
            return ticket

        ticket.phase = transition.to_phase
        ticket.updated_tick = int(transition.tick)
        ticket.phase_entered_tick = int(transition.tick)
        ticket.phase_reason = str(transition.reason)

        if transition.to_phase == ActionPhase.DISPATCHING:
            ticket.attempt_count += 1
            ticket.dispatch_requested = False
            ticket.confirmation_deadline_tick = None
            ticket.last_command_id = None
        if transition.to_phase == ActionPhase.AWAITING_CONFIRMATION:
            raw_deadline = transition.data.get("confirmation_deadline_tick")
            ticket.confirmation_deadline_tick = int(raw_deadline) if raw_deadline is not None else None
        if transition.to_phase == ActionPhase.RETRY_WAIT:
            raw_next = transition.data.get("next_eligible_tick")
            ticket.next_eligible_tick = int(raw_next) if raw_next is not None else int(ticket.updated_tick)
            if str(transition.reason).endswith("timeout"):
                timeout_event = ExecutionEvent(
                    event_id=str(uuid.uuid4()),
                    tick=int(transition.tick),
                    source="scheduler",
                    kind=ExecutionEventKind.EFFECT_MISSING_TIMEOUT,
                    ticket_id=ticket.ticket_id,
                    reason=transition.reason,
                )
                self.record_event(timeout_event)
                ticket.last_event_id = timeout_event.event_id
        if transition.to_phase == ActionPhase.COMPLETED:
            raw_next = transition.data.get("next_eligible_tick")
            if raw_next is not None:
                ticket.next_eligible_tick = int(raw_next)
        if transition.to_phase in (ActionPhase.FAILED, ActionPhase.CANCELLED):
            ticket.confirmation_deadline_tick = None

        return ticket

    def mark_dispatch_requested(self, *, ticket_id: str, tick: int, context: dict | None = None) -> ActionTicket:
        ticket = self._active_ticket
        if ticket is None or ticket.ticket_id != ticket_id:
            raise RuntimeError("no_matching_ticket")
        if ticket.phase != ActionPhase.DISPATCHING:
            raise RuntimeError("dispatch_requested_outside_dispatching")
        ticket.dispatch_requested = True
        ticket.updated_tick = int(tick)
        if context:
            ticket.context.update(context)
        return ticket

    def bind_command_id(self, *, ticket_id: str, command_id: str) -> None:
        ticket = self._active_ticket
        if ticket is None or ticket.ticket_id != ticket_id:
            return
        ticket.last_command_id = str(command_id)

    def record_event(self, event: ExecutionEvent) -> None:
        self._recent_events.append(event)
        while len(self._recent_events) > int(self.max_recent_events):
            self._recent_events.popleft()
        ticket = self._active_ticket
        if ticket is not None and event.ticket_id == ticket.ticket_id:
            ticket.last_event_id = event.event_id
            ticket.updated_tick = max(ticket.updated_tick, int(event.tick))

    @staticmethod
    def _new_ticket_from_intent(intent, tick: int) -> ActionTicket:
        return ActionTicket(
            ticket_id=str(uuid.uuid4()),
            intent=intent,
            phase=ActionPhase.QUEUED,
            created_tick=int(tick),
            updated_tick=int(tick),
            phase_entered_tick=int(tick),
            next_eligible_tick=int(tick),
            phase_reason="intent_queued",
        )
