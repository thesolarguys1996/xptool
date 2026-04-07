import unittest
from unittest.mock import patch

from runelite_planner.models import Snapshot
from runelite_planner.runner import RuntimeRunner, BreakSettings


class _NoopStrategy:
    def __init__(self) -> None:
        self.calls = 0

    def intents(self, _snapshot: Snapshot):
        self.calls += 1
        return []


class _CaptureWriter:
    source = "xptool.planner"
    path = "test.ndjson"

    def __init__(self) -> None:
        self.commands = []

    def write_command(self, command):
        self.commands.append(command)
        return f"cmd-{len(self.commands)}"


def _snapshot(tick: int, logged_in: bool) -> Snapshot:
    return Snapshot(
        tick=tick,
        logged_in=logged_in,
        bank_open=False,
        inventory_counts={},
        bank_counts={},
        inventory_slots_used=0,
        player_animation=0,
        raw={},
    )


class RunnerBreakSchedulerTests(unittest.TestCase):
    def test_breaks_emit_logout_then_login_and_pause_strategy(self) -> None:
        strategy = _NoopStrategy()
        writer = _CaptureWriter()
        runner = RuntimeRunner(
            strategy=strategy,
            writer=writer,
            dry_run=False,
            runtime_callback=lambda _msg: None,
            break_settings=BreakSettings(
                enabled=True,
                work_minutes_min=120.0,
                work_minutes_max=120.0,
                break_minutes_min=0.01,
                break_minutes_max=0.01,
                command_retry_seconds=0.5,
            ),
        )

        with patch(
            "runelite_planner.runner.time.monotonic",
            side_effect=[0.0, 1.0, 2.0, 2.2, 18.0, 18.1, 18.2],
        ):
            runner._initialize_break_scheduler()
            runner._work_window_end_monotonic = 0.0
            runner._process_snapshot(_snapshot(1, True))
            runner._process_snapshot(_snapshot(2, False))
            runner._process_snapshot(_snapshot(3, False))
            runner._process_snapshot(_snapshot(4, False))
            runner._process_snapshot(_snapshot(5, True))
            runner._process_snapshot(_snapshot(6, True))

        self.assertEqual(["LOGOUT_SAFE", "LOGIN_START_TEST"], [cmd.command_type for cmd in writer.commands])
        self.assertTrue(bool(writer.commands[1].payload.get("prefilled")))
        self.assertEqual(1, strategy.calls)

    def test_breaks_disable_when_writer_missing(self) -> None:
        strategy = _NoopStrategy()
        runner = RuntimeRunner(
            strategy=strategy,
            writer=None,
            dry_run=False,
            runtime_callback=lambda _msg: None,
            break_settings=BreakSettings(
                enabled=True,
                work_minutes_min=1.0,
                work_minutes_max=1.0,
                break_minutes_min=1.0,
                break_minutes_max=1.0,
            ),
        )

        with patch("runelite_planner.runner.time.monotonic", return_value=0.0):
            runner._initialize_break_scheduler()
            runner._process_snapshot(_snapshot(1, True))

        self.assertEqual("INACTIVE", runner._break_state)
        self.assertEqual(1, strategy.calls)

    def test_breaks_advance_from_logout_safe_completion_without_snapshots(self) -> None:
        strategy = _NoopStrategy()
        writer = _CaptureWriter()
        runner = RuntimeRunner(
            strategy=strategy,
            writer=writer,
            dry_run=False,
            runtime_callback=lambda _msg: None,
            break_settings=BreakSettings(
                enabled=True,
                work_minutes_min=120.0,
                work_minutes_max=120.0,
                break_minutes_min=0.01,
                break_minutes_max=0.01,
                command_retry_seconds=0.5,
            ),
        )

        with patch("runelite_planner.runner.time.monotonic", return_value=0.0):
            runner._initialize_break_scheduler()

        runner._break_cycle_id = 1
        runner._break_state = "LOGOUT_PENDING"
        runner._last_snapshot_tick = 140
        runner._last_snapshot_logged_in = True

        runner._update_break_login_state_from_execution_row(
            {
                "eventType": "DISPATCHED",
                "reason": "logout_safe_complete",
                "commandType": "LOGOUT_SAFE",
                "details": {"gameState": "LOGIN_SCREEN"},
            }
        )
        self.assertFalse(bool(runner._last_snapshot_logged_in))

        with patch("runelite_planner.runner.time.monotonic", return_value=2.0):
            runner._maybe_advance_break_cycle_without_snapshot()
        self.assertEqual("BREAKING", runner._break_state)

        runner._break_window_end_monotonic = 0.0
        with patch("runelite_planner.runner.time.monotonic", return_value=3.0):
            runner._maybe_advance_break_cycle_without_snapshot()

        self.assertEqual(["LOGIN_START_TEST"], [cmd.command_type for cmd in writer.commands])
        self.assertEqual("LOGIN_PENDING", runner._break_state)

    def test_breaks_advance_on_logout_pending_snapshot_stall(self) -> None:
        strategy = _NoopStrategy()
        writer = _CaptureWriter()
        runner = RuntimeRunner(
            strategy=strategy,
            writer=writer,
            dry_run=False,
            runtime_callback=lambda _msg: None,
            break_settings=BreakSettings(
                enabled=True,
                work_minutes_min=120.0,
                work_minutes_max=120.0,
                break_minutes_min=0.01,
                break_minutes_max=0.01,
                command_retry_seconds=0.5,
            ),
        )

        with patch("runelite_planner.runner.time.monotonic", return_value=0.0):
            runner._initialize_break_scheduler()

        runner._break_cycle_id = 1
        runner._break_state = "LOGOUT_PENDING"
        runner._last_snapshot_tick = 140
        runner._last_snapshot_logged_in = True
        runner._last_snapshot_seen_at_monotonic = 1.0

        with patch("runelite_planner.runner.time.monotonic", return_value=4.0):
            runner._maybe_advance_break_cycle_without_snapshot()
        self.assertEqual("BREAKING", runner._break_state)
        self.assertFalse(bool(runner._last_snapshot_logged_in))

        runner._break_window_end_monotonic = 0.0
        with patch("runelite_planner.runner.time.monotonic", return_value=5.0):
            runner._maybe_advance_break_cycle_without_snapshot()

        self.assertEqual(["LOGIN_START_TEST"], [cmd.command_type for cmd in writer.commands])
        self.assertEqual("LOGIN_PENDING", runner._break_state)


if __name__ == "__main__":
    unittest.main()

