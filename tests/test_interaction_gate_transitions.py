import unittest

from runelite_planner.runtime_core.interaction_gate import ALLOWED_TRANSITIONS, InteractionGate
from runelite_planner.runtime_core.models import (
    ActionPhase,
    ActionTicket,
    GateVerdict,
    Intent,
    IntentKind,
    PhaseTransition,
)


class InteractionGateTransitionTests(unittest.TestCase):
    def _ticket(self) -> ActionTicket:
        intent = Intent(
            intent_key="intent-1",
            activity="woodcutting",
            kind=IntentKind.CHOP_TREE,
            target={},
            params={},
            policy_key="policy",
            reason="test",
        )
        return ActionTicket(
            ticket_id="ticket-1",
            intent=intent,
            phase=ActionPhase.DISPATCHING,
            created_tick=10,
            updated_tick=10,
            phase_entered_tick=10,
        )

    def test_allowed_transition_dispatching_to_retry_wait(self) -> None:
        self.assertIn((ActionPhase.DISPATCHING, ActionPhase.RETRY_WAIT), ALLOWED_TRANSITIONS)

        gate = InteractionGate()
        ticket = self._ticket()
        proposed = PhaseTransition(
            ticket_id=ticket.ticket_id,
            from_phase=ActionPhase.DISPATCHING,
            to_phase=ActionPhase.RETRY_WAIT,
            tick=11,
            reason="scheduler_dispatch_retryable_failure",
        )
        decision = gate.evaluate(ticket=ticket, proposed=proposed)
        self.assertEqual(GateVerdict.ALLOW, decision.verdict)

    def test_allowed_transition_dispatching_to_failed(self) -> None:
        self.assertIn((ActionPhase.DISPATCHING, ActionPhase.FAILED), ALLOWED_TRANSITIONS)

        gate = InteractionGate()
        ticket = self._ticket()
        proposed = PhaseTransition(
            ticket_id=ticket.ticket_id,
            from_phase=ActionPhase.DISPATCHING,
            to_phase=ActionPhase.FAILED,
            tick=11,
            reason="scheduler_dispatch_terminal_failure",
        )
        decision = gate.evaluate(ticket=ticket, proposed=proposed)
        self.assertEqual(GateVerdict.ALLOW, decision.verdict)


if __name__ == "__main__":
    unittest.main()

