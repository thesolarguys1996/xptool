from __future__ import annotations

from dataclasses import dataclass
import random

from .models import (
    ActionPhase,
    ActionQueueView,
    ActionTicket,
    ExecutionEventKind,
    IntentKind,
    PhaseTransition,
)


@dataclass(frozen=True)
class SchedulerConfig:
    dispatch_cooldown_ticks: int = 1
    dispatch_cooldown_jitter_ticks: int = 1
    retry_backoff_ticks: int = 2
    retry_backoff_jitter_ticks: int = 1
    deferred_retry_backoff_ticks: int = 1
    deferred_retry_backoff_jitter_ticks: int = 1
    confirmation_timeout_ticks: int = 4
    confirmation_timeout_jitter_ticks: int = 1


@dataclass
class Scheduler:
    cfg: SchedulerConfig = SchedulerConfig()

    def propose(self, *, tick: int, ticket: ActionTicket, queue_view: ActionQueueView) -> PhaseTransition | None:
        if ticket.phase == ActionPhase.QUEUED:
            if tick >= int(ticket.next_eligible_tick):
                return PhaseTransition(
                    ticket_id=ticket.ticket_id,
                    from_phase=ticket.phase,
                    to_phase=ActionPhase.READY_TO_DISPATCH,
                    tick=tick,
                    reason="scheduler_ready_window_open",
                )
            return None

        if ticket.phase == ActionPhase.READY_TO_DISPATCH:
            if tick >= int(ticket.next_eligible_tick):
                return PhaseTransition(
                    ticket_id=ticket.ticket_id,
                    from_phase=ticket.phase,
                    to_phase=ActionPhase.DISPATCHING,
                    tick=tick,
                    reason="scheduler_dispatch_window_open",
                )
            return None

        if ticket.phase == ActionPhase.DISPATCHING:
            if self._has_event(
                queue_view,
                ticket.ticket_id,
                ExecutionEventKind.EXECUTOR_FAILED_TERMINAL,
                since_tick=ticket.phase_entered_tick,
            ):
                return PhaseTransition(
                    ticket_id=ticket.ticket_id,
                    from_phase=ticket.phase,
                    to_phase=ActionPhase.FAILED,
                    tick=tick,
                    reason="scheduler_dispatch_terminal_failure",
                )
            if self._has_event(
                queue_view,
                ticket.ticket_id,
                ExecutionEventKind.EXECUTOR_FAILED_RETRYABLE,
                since_tick=ticket.phase_entered_tick,
            ):
                return PhaseTransition(
                    ticket_id=ticket.ticket_id,
                    from_phase=ticket.phase,
                    to_phase=ActionPhase.RETRY_WAIT,
                    tick=tick,
                    reason="scheduler_dispatch_retryable_failure",
                    data={"next_eligible_tick": tick + self._retry_backoff_ticks_for_ticket(ticket)},
                )
            if self._has_event(
                queue_view,
                ticket.ticket_id,
                ExecutionEventKind.DISPATCH_ENQUEUED,
                since_tick=ticket.phase_entered_tick,
            ):
                return PhaseTransition(
                    ticket_id=ticket.ticket_id,
                    from_phase=ticket.phase,
                    to_phase=ActionPhase.AWAITING_CONFIRMATION,
                    tick=tick,
                    reason="scheduler_dispatch_recorded",
                    data={
                        "confirmation_deadline_tick": tick + self._confirmation_timeout_ticks_for_ticket(ticket),
                    },
                )
            return None

        if ticket.phase == ActionPhase.AWAITING_CONFIRMATION:
            if self._has_event(
                queue_view,
                ticket.ticket_id,
                ExecutionEventKind.EXECUTOR_FAILED_TERMINAL,
                since_tick=ticket.phase_entered_tick,
            ):
                return PhaseTransition(
                    ticket_id=ticket.ticket_id,
                    from_phase=ticket.phase,
                    to_phase=ActionPhase.FAILED,
                    tick=tick,
                    reason="scheduler_terminal_failure",
                )
            # Drop motor intents should only fast-complete after executor confirms
            # dispatch. Enqueue-only fast path can hide deferred outcomes (for
            # example motor_lock_unavailable) and strand planner state.
            if self._is_drop_motor_intent(ticket.intent.kind) and self._has_event(
                queue_view,
                ticket.ticket_id,
                ExecutionEventKind.EXECUTOR_DISPATCHED,
                since_tick=ticket.phase_entered_tick,
            ):
                cooldown_ticks = self._cooldown_ticks_for_ticket(ticket)
                return PhaseTransition(
                    ticket_id=ticket.ticket_id,
                    from_phase=ticket.phase,
                    to_phase=ActionPhase.COMPLETED,
                    tick=tick,
                    reason="scheduler_drop_dispatched_fast_path",
                    data={"next_eligible_tick": tick + cooldown_ticks},
                )
            if ticket.intent.kind == IntentKind.SCENE_OBJECT_ACTION and self._has_event(
                queue_view,
                ticket.ticket_id,
                ExecutionEventKind.EXECUTOR_DISPATCHED,
                since_tick=ticket.phase_entered_tick,
            ):
                cooldown_ticks = self._cooldown_ticks_for_ticket(ticket)
                return PhaseTransition(
                    ticket_id=ticket.ticket_id,
                    from_phase=ticket.phase,
                    to_phase=ActionPhase.COMPLETED,
                    tick=tick,
                    reason="scheduler_scene_object_dispatched_fast_path",
                    data={"next_eligible_tick": tick + cooldown_ticks},
                )
            if ticket.intent.kind == IntentKind.GROUND_ITEM_ACTION and self._has_event(
                queue_view,
                ticket.ticket_id,
                ExecutionEventKind.EXECUTOR_DISPATCHED,
                since_tick=ticket.phase_entered_tick,
            ):
                cooldown_ticks = self._cooldown_ticks_for_ticket(ticket)
                return PhaseTransition(
                    ticket_id=ticket.ticket_id,
                    from_phase=ticket.phase,
                    to_phase=ActionPhase.COMPLETED,
                    tick=tick,
                    reason="scheduler_ground_item_dispatched_fast_path",
                    data={"next_eligible_tick": tick + cooldown_ticks},
                )
            if self._has_event(
                queue_view,
                ticket.ticket_id,
                ExecutionEventKind.EFFECT_OBSERVED,
                since_tick=ticket.phase_entered_tick,
            ):
                cooldown_ticks = self._cooldown_ticks_for_ticket(ticket)
                return PhaseTransition(
                    ticket_id=ticket.ticket_id,
                    from_phase=ticket.phase,
                    to_phase=ActionPhase.COMPLETED,
                    tick=tick,
                    reason="scheduler_effect_confirmed",
                    data={"next_eligible_tick": tick + cooldown_ticks},
                )
            if self._has_event(
                queue_view,
                ticket.ticket_id,
                ExecutionEventKind.EXECUTOR_DEFERRED,
                since_tick=ticket.phase_entered_tick,
            ):
                return PhaseTransition(
                    ticket_id=ticket.ticket_id,
                    from_phase=ticket.phase,
                    to_phase=ActionPhase.RETRY_WAIT,
                    tick=tick,
                    reason="scheduler_executor_deferred",
                    data={
                        "next_eligible_tick": tick + self._deferred_retry_backoff_ticks_for_ticket(ticket),
                    },
                )
            if self._has_event(
                queue_view,
                ticket.ticket_id,
                ExecutionEventKind.EXECUTOR_FAILED_RETRYABLE,
                since_tick=ticket.phase_entered_tick,
            ):
                return PhaseTransition(
                    ticket_id=ticket.ticket_id,
                    from_phase=ticket.phase,
                    to_phase=ActionPhase.RETRY_WAIT,
                    tick=tick,
                    reason="scheduler_retryable_failure",
                    data={"next_eligible_tick": tick + self._retry_backoff_ticks_for_ticket(ticket)},
                )
            deadline = ticket.confirmation_deadline_tick
            if deadline is not None and tick >= int(deadline):
                return PhaseTransition(
                    ticket_id=ticket.ticket_id,
                    from_phase=ticket.phase,
                    to_phase=ActionPhase.RETRY_WAIT,
                    tick=tick,
                    reason="scheduler_confirmation_timeout",
                    data={"next_eligible_tick": tick + self._retry_backoff_ticks_for_ticket(ticket)},
                )
            return None

        if ticket.phase == ActionPhase.RETRY_WAIT:
            if tick >= int(ticket.next_eligible_tick):
                return PhaseTransition(
                    ticket_id=ticket.ticket_id,
                    from_phase=ticket.phase,
                    to_phase=ActionPhase.READY_TO_DISPATCH,
                    tick=tick,
                    reason="scheduler_retry_window_open",
                )
            return None

        return None

    @staticmethod
    def _has_event(
        queue_view: ActionQueueView,
        ticket_id: str,
        kind: ExecutionEventKind,
        *,
        since_tick: int,
    ) -> bool:
        for event in reversed(queue_view.recent_events):
            if event.ticket_id != ticket_id:
                continue
            if int(event.tick) < int(since_tick):
                continue
            if event.kind == kind:
                return True
        return False

    def _cooldown_ticks_for_ticket(self, ticket: ActionTicket) -> int:
        # Drops are driven as an executor session; keep planner-side cooldown at zero
        # so subsequent drop intents can flow on the next eligible snapshot tick.
        if self._is_drop_motor_intent(ticket.intent.kind):
            return 0
        return self._sample_ticks(
            base_ticks=self.cfg.dispatch_cooldown_ticks,
            jitter_ticks=self.cfg.dispatch_cooldown_jitter_ticks,
            minimum_ticks=0,
        )

    def _retry_backoff_ticks_for_ticket(self, ticket: ActionTicket) -> int:
        base_ticks = self.cfg.retry_backoff_ticks
        jitter_ticks = self.cfg.retry_backoff_jitter_ticks
        minimum_ticks = 1
        if ticket.intent.kind == IntentKind.WALK_TO_WORLDPOINT:
            # Walking retries should breathe longer before another click attempt.
            base_ticks += 2
            jitter_ticks = max(jitter_ticks, 2)
            minimum_ticks = 2
        elif ticket.intent.kind == IntentKind.SCENE_OBJECT_ACTION:
            # Object interactions should not spam retries while player/path settles.
            base_ticks += 2
            jitter_ticks = max(jitter_ticks, 2)
            minimum_ticks = 2
        elif ticket.intent.kind == IntentKind.GROUND_ITEM_ACTION:
            # Ground-item take attempts are especially noisy during rooftop transitions.
            base_ticks += 4
            jitter_ticks = max(jitter_ticks, 3)
            minimum_ticks = 3
        return self._sample_ticks(
            base_ticks=base_ticks,
            jitter_ticks=jitter_ticks,
            minimum_ticks=minimum_ticks,
        )

    def _deferred_retry_backoff_ticks_for_ticket(self, ticket: ActionTicket) -> int:
        base_ticks = self.cfg.deferred_retry_backoff_ticks
        jitter_ticks = self.cfg.deferred_retry_backoff_jitter_ticks
        minimum_ticks = 1
        if ticket.intent.kind == IntentKind.WALK_TO_WORLDPOINT:
            # Deferred walk dispatches should not immediately hammer the same target.
            base_ticks += 2
            jitter_ticks = max(jitter_ticks, 2)
            minimum_ticks = 2
        elif ticket.intent.kind == IntentKind.SCENE_OBJECT_ACTION:
            # Scene object retries should breathe to avoid rapid re-click spam.
            base_ticks += 3
            jitter_ticks = max(jitter_ticks, 2)
            minimum_ticks = 2
        elif ticket.intent.kind == IntentKind.GROUND_ITEM_ACTION:
            # Marks can be visible on destination roof while still pathing.
            # Back off longer so we wait for natural traversal completion.
            base_ticks += 5
            jitter_ticks = max(jitter_ticks, 3)
            minimum_ticks = 4
        return self._sample_ticks(
            base_ticks=base_ticks,
            jitter_ticks=jitter_ticks,
            minimum_ticks=minimum_ticks,
        )

    def _confirmation_timeout_ticks(self) -> int:
        return self._sample_ticks(
            base_ticks=self.cfg.confirmation_timeout_ticks,
            jitter_ticks=self.cfg.confirmation_timeout_jitter_ticks,
            minimum_ticks=1,
        )

    def _confirmation_timeout_ticks_for_ticket(self, ticket: ActionTicket) -> int:
        timeout_ticks = self._confirmation_timeout_ticks()
        if ticket.intent.kind == IntentKind.CHOP_TREE:
            # Woodcut often has an initial walk/target acquisition delay before animation flips.
            return timeout_ticks + 2
        if ticket.intent.kind == IntentKind.WALK_TO_WORLDPOINT:
            # Walking may require traversing several tiles before arrival is observable.
            return timeout_ticks + 6
        if ticket.intent.kind == IntentKind.NPC_CONTEXT_ACTION:
            # NPC interaction can require a short menu/title refresh before shop open is visible.
            return timeout_ticks + 4
        if ticket.intent.kind == IntentKind.WORLD_HOP:
            # World hops require interface click + transfer + relog confirmation.
            return timeout_ticks + 12
        return timeout_ticks

    @staticmethod
    def _sample_ticks(*, base_ticks: int, jitter_ticks: int, minimum_ticks: int) -> int:
        base = max(int(minimum_ticks), int(base_ticks))
        jitter = max(0, int(jitter_ticks))
        if jitter <= 0:
            return base
        sampled = base + random.randint(-jitter, jitter)
        return max(int(minimum_ticks), sampled)

    @staticmethod
    def _is_drop_motor_intent(kind: IntentKind) -> bool:
        return kind in (
            IntentKind.DROP_ITEM,
            IntentKind.START_DROP_SESSION,
            IntentKind.STOP_DROP_SESSION,
        )
