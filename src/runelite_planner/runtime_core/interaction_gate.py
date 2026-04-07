from __future__ import annotations

from dataclasses import dataclass

from .models import ActionPhase, ActionTicket, GateDecision, GateVerdict, PhaseTransition

ALLOWED_TRANSITIONS: set[tuple[ActionPhase, ActionPhase]] = {
    (ActionPhase.QUEUED, ActionPhase.READY_TO_DISPATCH),
    (ActionPhase.READY_TO_DISPATCH, ActionPhase.DISPATCHING),
    (ActionPhase.DISPATCHING, ActionPhase.AWAITING_CONFIRMATION),
    (ActionPhase.DISPATCHING, ActionPhase.RETRY_WAIT),
    (ActionPhase.DISPATCHING, ActionPhase.FAILED),
    (ActionPhase.AWAITING_CONFIRMATION, ActionPhase.COMPLETED),
    (ActionPhase.AWAITING_CONFIRMATION, ActionPhase.RETRY_WAIT),
    (ActionPhase.AWAITING_CONFIRMATION, ActionPhase.FAILED),
    (ActionPhase.RETRY_WAIT, ActionPhase.READY_TO_DISPATCH),
    (ActionPhase.QUEUED, ActionPhase.CANCELLED),
    (ActionPhase.READY_TO_DISPATCH, ActionPhase.CANCELLED),
    (ActionPhase.DISPATCHING, ActionPhase.CANCELLED),
    (ActionPhase.AWAITING_CONFIRMATION, ActionPhase.CANCELLED),
    (ActionPhase.RETRY_WAIT, ActionPhase.CANCELLED),
}


@dataclass(frozen=True)
class InteractionGate:
    """
    Validates phase transitions only.
    """

    def evaluate(self, *, ticket: ActionTicket, proposed: PhaseTransition) -> GateDecision:
        if proposed.ticket_id != ticket.ticket_id:
            return GateDecision(
                verdict=GateVerdict.DENY,
                current_phase=ticket.phase,
                proposed_phase=proposed.to_phase,
                effective_phase=ticket.phase,
                reason="ticket_mismatch",
            )
        if proposed.from_phase != ticket.phase:
            return GateDecision(
                verdict=GateVerdict.HOLD,
                current_phase=ticket.phase,
                proposed_phase=proposed.to_phase,
                effective_phase=ticket.phase,
                reason="stale_transition",
            )
        if (proposed.from_phase, proposed.to_phase) not in ALLOWED_TRANSITIONS:
            return GateDecision(
                verdict=GateVerdict.DENY,
                current_phase=ticket.phase,
                proposed_phase=proposed.to_phase,
                effective_phase=ticket.phase,
                reason="invalid_transition",
            )
        return GateDecision(
            verdict=GateVerdict.ALLOW,
            current_phase=ticket.phase,
            proposed_phase=proposed.to_phase,
            effective_phase=proposed.to_phase,
            reason="allowed",
        )
