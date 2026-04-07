import json
import tempfile
import time
import unittest
import urllib.error
from unittest.mock import patch

from runelite_planner.control_plane import (
    ControlPlaneAuditPolicy,
    ControlPlaneAuditSink,
    ControlPlaneSecuritySettings,
    ControlPlanePolicySnapshot,
    ControlPlaneRuntimeSettings,
    ControlPlaneSession,
    FileControlPlaneClient,
    HttpControlPlaneClient,
    policy_snapshot_from_mapping,
)
from runelite_planner.models import RuntimeCommand, Snapshot
from runelite_planner.remote_planner import (
    RemoteCommandSpec,
    RemotePlannerDecision,
    RemotePlannerSettings,
)
from runelite_planner.runner import RuntimeRunner
from runelite_planner.runtime_core import DispatchRequest


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


class _KillSwitchControlPlaneClient:
    def start_session(
        self,
        *,
        runner_id: str,
        strategy_name: str,
        writer_path: str,
        dry_run: bool,
    ) -> ControlPlaneSession:
        _ = (runner_id, strategy_name, writer_path, dry_run)
        return ControlPlaneSession.create(session_id="cp-test")

    def refresh_policy(
        self,
        *,
        session: ControlPlaneSession,
        strategy_name: str,
        tick: int,
    ) -> ControlPlanePolicySnapshot:
        _ = (session, strategy_name, tick)
        return ControlPlanePolicySnapshot(
            kill_switch_global=True,
            disabled_activities=tuple(),
            feature_flags={},
            reason="test_kill_switch",
            fetched_at_unix_ms=1,
        )

    def close_session(self, *, session: ControlPlaneSession, reason: str) -> None:
        _ = (session, reason)


class _StartFailControlPlaneClient:
    def start_session(
        self,
        *,
        runner_id: str,
        strategy_name: str,
        writer_path: str,
        dry_run: bool,
    ) -> ControlPlaneSession:
        _ = (runner_id, strategy_name, writer_path, dry_run)
        raise RuntimeError("start_failed")

    def refresh_policy(
        self,
        *,
        session: ControlPlaneSession,
        strategy_name: str,
        tick: int,
    ) -> ControlPlanePolicySnapshot:
        _ = (session, strategy_name, tick)
        return ControlPlanePolicySnapshot.default()

    def close_session(self, *, session: ControlPlaneSession, reason: str) -> None:
        _ = (session, reason)


class _RefreshFailControlPlaneClient:
    def start_session(
        self,
        *,
        runner_id: str,
        strategy_name: str,
        writer_path: str,
        dry_run: bool,
    ) -> ControlPlaneSession:
        _ = (runner_id, strategy_name, writer_path, dry_run)
        return ControlPlaneSession.create(session_id="cp-refresh-fail")

    def refresh_policy(
        self,
        *,
        session: ControlPlaneSession,
        strategy_name: str,
        tick: int,
    ) -> ControlPlanePolicySnapshot:
        _ = (session, strategy_name, tick)
        raise RuntimeError("refresh_failed")

    def close_session(self, *, session: ControlPlaneSession, reason: str) -> None:
        _ = (session, reason)


class _UnsupportedDispatchCore:
    def __init__(self) -> None:
        self.failed = []

    def on_snapshot(self, _snapshot: Snapshot, _intents):
        return [
            DispatchRequest(
                ticket_id="ticket-unsupported",
                command=RuntimeCommand(
                    command_type="UNSUPPORTED_FAKE_COMMAND",
                    payload={},
                    reason="test_unsupported",
                    tick=1,
                    source="test",
                ),
            )
        ]

    def on_dispatch_enqueued(self, *, ticket_id: str, command_id: str, tick: int) -> None:
        _ = (ticket_id, command_id, tick)

    def on_dispatch_failed(self, *, ticket_id: str, tick: int, reason: str) -> None:
        self.failed.append((ticket_id, tick, reason))

    def on_executor_row(self, row: dict) -> None:
        _ = row

    def telemetry_snapshot(self) -> dict:
        return {}


class _RemotePlannerCommandClient:
    def decide(
        self,
        *,
        snapshot: Snapshot,
        strategy_activity: str,
        strategy_name: str,
        session_id: str,
    ) -> RemotePlannerDecision:
        _ = (snapshot, strategy_activity, strategy_name, session_id)
        return RemotePlannerDecision(
            status="ok",
            decision_id="remote-decision-1",
            commands=(
                RemoteCommandSpec(
                    command_type="LOGOUT_SAFE",
                    payload={"plannerTag": "remote_test"},
                    reason="remote_decision_test",
                    source="xptool.remote",
                ),
            ),
        )


class _RemotePlannerFailureClient:
    def decide(
        self,
        *,
        snapshot: Snapshot,
        strategy_activity: str,
        strategy_name: str,
        session_id: str,
    ) -> RemotePlannerDecision:
        _ = (snapshot, strategy_activity, strategy_name, session_id)
        raise RuntimeError("remote_planner_down")


class _RemotePlannerEnvelopeCommandClient:
    def decide(
        self,
        *,
        snapshot: Snapshot,
        strategy_activity: str,
        strategy_name: str,
        session_id: str,
    ) -> RemotePlannerDecision:
        _ = (snapshot, strategy_activity, strategy_name, session_id)
        return RemotePlannerDecision(
            status="ok",
            decision_id="remote-decision-envelope",
            commands=(
                RemoteCommandSpec(
                    command_type="LOGOUT_SAFE",
                    payload={"plannerTag": "remote_test"},
                    reason="remote_decision_test",
                    source="xptool.remote",
                    envelope={
                        "commandId": "env-1",
                        "sessionId": "sess-x",
                        "issuedAtUnixMs": 1,
                        "nonce": "nonce-1",
                        "commandType": "LOGOUT_SAFE",
                        "payload": {"plannerTag": "remote_test"},
                        "signatureBase64": "sig",
                    },
                ),
            ),
        )


class _RemotePlannerNoActionClient:
    def decide(
        self,
        *,
        snapshot: Snapshot,
        strategy_activity: str,
        strategy_name: str,
        session_id: str,
    ) -> RemotePlannerDecision:
        _ = (snapshot, strategy_activity, strategy_name, session_id)
        return RemotePlannerDecision(status="no_action", decision_id="remote-precheck-ok")


class _RemotePlannerUnsupportedCommandClient:
    def decide(
        self,
        *,
        snapshot: Snapshot,
        strategy_activity: str,
        strategy_name: str,
        session_id: str,
    ) -> RemotePlannerDecision:
        _ = (snapshot, strategy_activity, strategy_name, session_id)
        return RemotePlannerDecision(
            status="ok",
            decision_id="remote-decision-2",
            commands=(
                RemoteCommandSpec(
                    command_type="UNSUPPORTED_FAKE_COMMAND",
                    payload={},
                    reason="remote_unsupported",
                    source="xptool.remote",
                ),
            ),
        )


def _snapshot(tick: int) -> Snapshot:
    return Snapshot(
        tick=tick,
        logged_in=True,
        bank_open=False,
        inventory_counts={},
        bank_counts={},
        inventory_slots_used=0,
        player_animation=0,
        raw={},
    )


class _FakeHttpResponse:
    def __init__(self, body: dict, headers: dict[str, str]) -> None:
        self._body = json.dumps(body).encode("utf-8")
        self.headers = headers

    def read(self) -> bytes:
        return self._body

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        _ = (exc_type, exc, tb)
        return False


class ControlPlaneTests(unittest.TestCase):
    class _StaticTokenProvider:
        def __init__(self, token: str) -> None:
            self._token = token

        def token(self) -> str:
            return self._token

    def test_file_control_plane_policy_parsing(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            policy_path = f"{tmpdir}/policy.json"
            payload = {
                "killSwitch": {
                    "global": False,
                    "activities": ["WOODCUTTING"],
                },
                "featureFlags": {
                    "break_scheduler_enabled": False,
                    "activity.mining.enabled": True,
                },
            }
            with open(policy_path, "w", encoding="utf-8") as fh:
                json.dump(payload, fh)
            client = FileControlPlaneClient(policy_path)
            session = client.start_session(
                runner_id="r",
                strategy_name="WoodcuttingStrategy",
                writer_path="w",
                dry_run=False,
            )
            policy = client.refresh_policy(
                session=session,
                strategy_name="WoodcuttingStrategy",
                tick=100,
            )
            self.assertFalse(policy.kill_switch_global)
            self.assertFalse(policy.is_activity_enabled("woodcutting"))
            self.assertTrue(policy.is_activity_enabled("mining"))
            self.assertFalse(policy.feature_enabled("break_scheduler_enabled", True))

    def test_runner_global_kill_switch_forces_stop_and_skips_strategy(self) -> None:
        strategy = _NoopStrategy()
        writer = _CaptureWriter()
        runner = RuntimeRunner(
            strategy=strategy,
            writer=writer,
            dry_run=False,
            runtime_callback=lambda _message: None,
            control_plane_client=_KillSwitchControlPlaneClient(),
            control_plane_settings=ControlPlaneRuntimeSettings(enabled=True, poll_interval_seconds=0.25),
        )

        runner._initialize_control_plane()
        runner._process_snapshot(_snapshot(50))

        self.assertEqual(0, strategy.calls)
        self.assertEqual(
            ["STOP_ALL_RUNTIME", "DROP_STOP_SESSION"],
            [command.command_type for command in writer.commands],
        )

    def test_audit_sink_redacts_sensitive_keys(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            audit_path = f"{tmpdir}/audit.ndjson"
            sink = ControlPlaneAuditSink(
                audit_path,
                policy=ControlPlaneAuditPolicy(
                    retention_days=14.0,
                    max_event_bytes=8192,
                    max_string_chars=24,
                    prune_interval_seconds=0.0,
                ),
            )
            sink.emit(
                "dispatch_sent",
                {
                    "token": "top-secret-token",
                    "username": "player@example.com",
                    "nested": {"accessToken": "abc123", "safeField": "safe-value"},
                    "message": "x" * 400,
                },
            )
            with open(audit_path, "r", encoding="utf-8") as fh:
                line = fh.readline().strip()
            payload = json.loads(line)
            details = payload.get("details", {})
            self.assertEqual("***REDACTED***", details.get("token"))
            self.assertEqual("***REDACTED***", details.get("username"))
            self.assertEqual("***REDACTED***", details.get("nested", {}).get("accessToken"))
            self.assertEqual("safe-value", details.get("nested", {}).get("safeField"))
            self.assertTrue(str(details.get("message", "")).endswith("...[truncated]"))

    def test_audit_sink_caps_large_payloads(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            audit_path = f"{tmpdir}/audit.ndjson"
            sink = ControlPlaneAuditSink(
                audit_path,
                policy=ControlPlaneAuditPolicy(
                    retention_days=14.0,
                    max_event_bytes=420,
                    max_string_chars=512,
                    prune_interval_seconds=0.0,
                ),
            )
            sink.emit(
                "dispatch_sent",
                {"blob": "y" * 6000, "extra": "z" * 6000},
            )
            with open(audit_path, "r", encoding="utf-8") as fh:
                line = fh.readline().strip()
            payload = json.loads(line)
            details = payload.get("details", {})
            self.assertTrue(details.get("truncated"))
            self.assertIn("maxEventBytes", details)

    def test_audit_sink_prunes_expired_records(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            audit_path = f"{tmpdir}/audit.ndjson"
            with open(audit_path, "w", encoding="utf-8") as fh:
                fh.write(
                    json.dumps(
                        {
                            "type": "CONTROL_PLANE_AUDIT",
                            "eventType": "old_event",
                            "capturedAtUnixMillis": 1,
                            "details": {"ok": True},
                        },
                        separators=(",", ":"),
                    )
                    + "\n"
                )
            sink = ControlPlaneAuditSink(
                audit_path,
                policy=ControlPlaneAuditPolicy(
                    retention_days=0.0,
                    max_event_bytes=8192,
                    prune_interval_seconds=0.0,
                ),
            )
            sink.emit("new_event", {"ok": True})
            with open(audit_path, "r", encoding="utf-8") as fh:
                rows = [json.loads(line) for line in fh if line.strip()]
            self.assertEqual(1, len(rows))
            self.assertEqual("new_event", rows[0].get("eventType"))

    def test_http_client_emits_signed_headers(self) -> None:
        captured_headers: dict[str, str] = {}
        now_ms = int(time.time() * 1000)

        def fake_urlopen(request, timeout=0.0):
            _ = timeout
            captured_headers.update({k.lower(): v for k, v in request.header_items()})
            return _FakeHttpResponse(
                {"sessionId": "sess-1", "accessToken": "tok"},
                {
                    "x-xptool-timestamp": str(now_ms),
                    "x-xptool-nonce": "nonce-response-1",
                },
            )

        client = HttpControlPlaneClient(
            base_url="https://control.example",
            security_settings=ControlPlaneSecuritySettings(
                signing_key="test-signing-secret",
                signing_key_id="key-1",
                replay_window_seconds=300.0,
                require_response_replay_fields=True,
            ),
        )
        with patch("runelite_planner.control_plane.urllib.request.urlopen", side_effect=fake_urlopen):
            session = client.start_session(
                runner_id="runner-1",
                strategy_name="WoodcuttingStrategy",
                writer_path="bus.ndjson",
                dry_run=False,
            )
        self.assertEqual("sess-1", session.session_id)
        self.assertIn("x-xptool-signature", captured_headers)
        self.assertIn("x-xptool-timestamp", captured_headers)
        self.assertIn("x-xptool-nonce", captured_headers)
        self.assertEqual("key-1", captured_headers.get("x-xptool-signing-key-id"))

    def test_http_client_rejects_duplicate_response_nonce(self) -> None:
        now_ms = int(time.time() * 1000)
        responses = [
            _FakeHttpResponse(
                {"sessionId": "sess-2"},
                {
                    "x-xptool-timestamp": str(now_ms),
                    "x-xptool-nonce": "dup-nonce",
                },
            ),
            _FakeHttpResponse(
                {},
                {
                    "x-xptool-timestamp": str(now_ms),
                    "x-xptool-nonce": "dup-nonce",
                },
            ),
        ]

        def fake_urlopen(_request, timeout=0.0):
            _ = timeout
            return responses.pop(0)

        client = HttpControlPlaneClient(
            base_url="https://control.example",
            security_settings=ControlPlaneSecuritySettings(
                signing_key="another-secret",
                replay_window_seconds=300.0,
                require_response_replay_fields=True,
            ),
        )
        with patch("runelite_planner.control_plane.urllib.request.urlopen", side_effect=fake_urlopen):
            session = client.start_session(
                runner_id="runner-2",
                strategy_name="MiningStrategy",
                writer_path="bus.ndjson",
                dry_run=False,
            )
            with self.assertRaisesRegex(RuntimeError, "control_plane_response_replay_rejected"):
                client.refresh_policy(
                    session=session,
                    strategy_name="MiningStrategy",
                    tick=120,
                )

    def test_http_client_timeout_surfaces_runtime_error(self) -> None:
        def fake_urlopen(_request, timeout=0.0):
            _ = timeout
            raise urllib.error.URLError("timeout")

        client = HttpControlPlaneClient(base_url="https://control.example")
        with patch("runelite_planner.control_plane.urllib.request.urlopen", side_effect=fake_urlopen):
            with self.assertRaisesRegex(RuntimeError, "control_plane_request_failed"):
                client.start_session(
                    runner_id="runner-timeout",
                    strategy_name="FishingStrategy",
                    writer_path="bus.ndjson",
                    dry_run=False,
                )

    def test_http_client_rejects_insecure_http_base_url(self) -> None:
        with self.assertRaisesRegex(ValueError, "control_plane_https_required"):
            HttpControlPlaneClient(base_url="http://control.example")

    def test_http_client_allows_localhost_http_when_enforced(self) -> None:
        client = HttpControlPlaneClient(base_url="http://localhost:8080")
        self.assertEqual("http://localhost:8080", client.base_url)

    def test_http_client_request_contract_contains_required_fields(self) -> None:
        requests = []
        now_ms = int(time.time() * 1000)

        def fake_urlopen(request, timeout=0.0):
            _ = timeout
            requests.append(
                {
                    "url": request.full_url,
                    "body": json.loads((request.data or b"{}").decode("utf-8")),
                }
            )
            return _FakeHttpResponse(
                {"sessionId": "sess-contract"},
                {
                    "x-xptool-timestamp": str(now_ms),
                    "x-xptool-nonce": f"nonce-{len(requests)}",
                },
            )

        client = HttpControlPlaneClient(base_url="https://control.example")
        with patch("runelite_planner.control_plane.urllib.request.urlopen", side_effect=fake_urlopen):
            session = client.start_session(
                runner_id="runner-contract",
                strategy_name="WoodcuttingStrategy",
                writer_path="cmd.ndjson",
                dry_run=True,
            )
            client.refresh_policy(
                session=session,
                strategy_name="WoodcuttingStrategy",
                tick=77,
            )
        self.assertEqual(2, len(requests))
        self.assertIn("/v1/planner/session/start", requests[0]["url"])
        self.assertIn("/v1/planner/session/refresh", requests[1]["url"])
        self.assertEqual(
            {"runnerId", "strategyName", "writerPath", "dryRun", "contractVersion", "clientBuild"},
            set(requests[0]["body"].keys()),
        )
        self.assertEqual(
            {"sessionId", "strategyName", "tick", "contractVersion", "clientBuild"},
            set(requests[1]["body"].keys()),
        )

    def test_http_client_requires_decision_id_when_enabled(self) -> None:
        now_ms = int(time.time() * 1000)

        def fake_urlopen(_request, timeout=0.0):
            _ = timeout
            return _FakeHttpResponse(
                {"killSwitchGlobal": False},
                {
                    "x-xptool-timestamp": str(now_ms),
                    "x-xptool-nonce": "nonce-decision-missing",
                },
            )

        client = HttpControlPlaneClient(
            base_url="https://control.example",
            security_settings=ControlPlaneSecuritySettings(
                require_decision_id=True,
                replay_window_seconds=300.0,
                require_response_replay_fields=True,
            ),
        )
        with patch("runelite_planner.control_plane.urllib.request.urlopen", side_effect=fake_urlopen):
            session = ControlPlaneSession.create(session_id="sess-req")
            with self.assertRaisesRegex(RuntimeError, "control_plane_decision_id_missing"):
                client.refresh_policy(
                    session=session,
                    strategy_name="WoodcuttingStrategy",
                    tick=88,
                )

    def test_http_client_uses_env_token_when_session_token_expired(self) -> None:
        captured_headers: dict[str, str] = {}
        now_ms = int(time.time() * 1000)

        def fake_urlopen(request, timeout=0.0):
            _ = timeout
            captured_headers.update({k.lower(): v for k, v in request.header_items()})
            return _FakeHttpResponse(
                {
                    "decisionId": "decision-1",
                    "killSwitchGlobal": False,
                    "responseTimestampUnixMillis": now_ms,
                    "responseNonce": "nonce-token-fallback",
                },
                {},
            )

        client = HttpControlPlaneClient(
            base_url="https://control.example",
            token_provider=self._StaticTokenProvider("env-token"),
            security_settings=ControlPlaneSecuritySettings(
                require_decision_id=True,
                replay_window_seconds=300.0,
                require_response_replay_fields=True,
            ),
        )
        expired_session = ControlPlaneSession(
            session_id="sess-expired",
            access_token="session-token",
            issued_at_unix_ms=now_ms - 60000,
            expires_at_unix_ms=now_ms - 1000,
        )
        with patch("runelite_planner.control_plane.urllib.request.urlopen", side_effect=fake_urlopen):
            client.refresh_policy(
                session=expired_session,
                strategy_name="WoodcuttingStrategy",
                tick=90,
            )
        self.assertEqual("Bearer env-token", captured_headers.get("authorization"))

    def test_policy_snapshot_response_contract_defaults(self) -> None:
        policy = policy_snapshot_from_mapping({})
        self.assertFalse(policy.kill_switch_global)
        self.assertEqual(tuple(), policy.disabled_activities)
        self.assertTrue(policy.is_activity_enabled("woodcutting"))
        self.assertTrue(policy.feature_enabled("missing_flag", True))

    def test_runner_control_plane_start_failure_falls_back(self) -> None:
        strategy = _NoopStrategy()
        runner = RuntimeRunner(
            strategy=strategy,
            writer=None,
            dry_run=True,
            runtime_callback=lambda _message: None,
            control_plane_client=_StartFailControlPlaneClient(),
            control_plane_settings=ControlPlaneRuntimeSettings(enabled=True, poll_interval_seconds=0.25),
        )
        runner._initialize_control_plane()
        runner._process_snapshot(_snapshot(10))
        self.assertEqual(1, strategy.calls)

    def test_runner_control_plane_refresh_failure_falls_back(self) -> None:
        strategy = _NoopStrategy()
        runner = RuntimeRunner(
            strategy=strategy,
            writer=None,
            dry_run=True,
            runtime_callback=lambda _message: None,
            control_plane_client=_RefreshFailControlPlaneClient(),
            control_plane_settings=ControlPlaneRuntimeSettings(enabled=True, poll_interval_seconds=0.25),
        )
        runner._initialize_control_plane()
        runner._process_snapshot(_snapshot(11))
        self.assertEqual(1, strategy.calls)

    def test_runner_rejects_unsupported_dispatch_command_type(self) -> None:
        strategy = _NoopStrategy()
        writer = _CaptureWriter()
        runner = RuntimeRunner(
            strategy=strategy,
            writer=writer,
            dry_run=False,
            runtime_callback=lambda _message: None,
        )
        fake_core = _UnsupportedDispatchCore()
        runner.core = fake_core  # Inject deterministic unsupported dispatch.
        runner._process_snapshot(_snapshot(12))
        self.assertEqual(0, len(writer.commands))
        self.assertEqual(1, len(fake_core.failed))
        self.assertIn("unsupported_command_type", fake_core.failed[0][2])

    def test_runner_request_stop_dry_run_does_not_raise(self) -> None:
        strategy = _NoopStrategy()
        runner = RuntimeRunner(
            strategy=strategy,
            writer=None,
            dry_run=True,
            runtime_callback=lambda _message: None,
        )
        runner._last_snapshot_tick = 123
        runner.request_stop(source="unit-test")

    def test_runner_remote_planner_dispatch_skips_local_strategy(self) -> None:
        strategy = _NoopStrategy()
        writer = _CaptureWriter()
        runner = RuntimeRunner(
            strategy=strategy,
            writer=writer,
            dry_run=False,
            runtime_callback=lambda _message: None,
            remote_planner_client=_RemotePlannerCommandClient(),
            remote_planner_settings=RemotePlannerSettings(
                enabled=True,
                fallback_to_local=True,
            ),
        )
        runner._process_snapshot(_snapshot(200))
        self.assertEqual(0, strategy.calls)
        self.assertEqual(1, len(writer.commands))
        self.assertEqual("LOGOUT_SAFE", writer.commands[0].command_type)

    def test_runner_remote_planner_dispatch_includes_command_envelope(self) -> None:
        strategy = _NoopStrategy()
        writer = _CaptureWriter()
        runner = RuntimeRunner(
            strategy=strategy,
            writer=writer,
            dry_run=False,
            runtime_callback=lambda _message: None,
            remote_planner_client=_RemotePlannerEnvelopeCommandClient(),
            remote_planner_settings=RemotePlannerSettings(
                enabled=True,
                fallback_to_local=True,
            ),
        )
        runner._process_snapshot(_snapshot(205))
        self.assertEqual(0, strategy.calls)
        self.assertEqual(1, len(writer.commands))
        payload = dict(writer.commands[0].payload)
        self.assertIn("commandEnvelope", payload)
        self.assertEqual("env-1", payload["commandEnvelope"].get("commandId"))

    def test_runner_remote_planner_failure_falls_back_to_local_strategy(self) -> None:
        strategy = _NoopStrategy()
        runner = RuntimeRunner(
            strategy=strategy,
            writer=None,
            dry_run=True,
            runtime_callback=lambda _message: None,
            remote_planner_client=_RemotePlannerFailureClient(),
            remote_planner_settings=RemotePlannerSettings(
                enabled=True,
                fallback_to_local=True,
            ),
        )
        runner._process_snapshot(_snapshot(201))
        self.assertEqual(1, strategy.calls)

    def test_runner_remote_planner_failure_without_fallback_blocks_local_strategy(self) -> None:
        strategy = _NoopStrategy()
        runner = RuntimeRunner(
            strategy=strategy,
            writer=None,
            dry_run=True,
            runtime_callback=lambda _message: None,
            remote_planner_client=_RemotePlannerFailureClient(),
            remote_planner_settings=RemotePlannerSettings(
                enabled=True,
                fallback_to_local=False,
            ),
        )
        runner._process_snapshot(_snapshot(202))
        self.assertEqual(0, strategy.calls)

    def test_runner_remote_planner_rejects_unsupported_command(self) -> None:
        strategy = _NoopStrategy()
        writer = _CaptureWriter()
        runner = RuntimeRunner(
            strategy=strategy,
            writer=writer,
            dry_run=False,
            runtime_callback=lambda _message: None,
            remote_planner_client=_RemotePlannerUnsupportedCommandClient(),
            remote_planner_settings=RemotePlannerSettings(
                enabled=True,
                fallback_to_local=True,
            ),
        )
        runner._process_snapshot(_snapshot(203))
        self.assertEqual(0, strategy.calls)
        self.assertEqual(0, len(writer.commands))

    def test_runner_remote_startup_precheck_failure_refuses_run(self) -> None:
        strategy = _NoopStrategy()
        with tempfile.TemporaryDirectory() as tmpdir:
            log_path = f"{tmpdir}/client.log"
            with open(log_path, "w", encoding="utf-8") as fh:
                fh.write("")
            runner = RuntimeRunner(
                strategy=strategy,
                writer=None,
                dry_run=True,
                runtime_callback=lambda _message: None,
                remote_planner_client=_RemotePlannerFailureClient(),
                remote_planner_settings=RemotePlannerSettings(
                    enabled=True,
                    fallback_to_local=False,
                    require_startup_precheck=True,
                ),
            )
            exit_code = runner.run(log_path=log_path, follow=False)
        self.assertEqual(2, exit_code)
        self.assertEqual(0, strategy.calls)

    def test_runner_remote_startup_precheck_success_allows_run(self) -> None:
        strategy = _NoopStrategy()
        with tempfile.TemporaryDirectory() as tmpdir:
            log_path = f"{tmpdir}/client.log"
            with open(log_path, "w", encoding="utf-8") as fh:
                fh.write("")
            runner = RuntimeRunner(
                strategy=strategy,
                writer=None,
                dry_run=True,
                runtime_callback=lambda _message: None,
                remote_planner_client=_RemotePlannerNoActionClient(),
                remote_planner_settings=RemotePlannerSettings(
                    enabled=True,
                    fallback_to_local=False,
                    require_startup_precheck=True,
                ),
            )
            exit_code = runner.run(log_path=log_path, follow=False)
        self.assertEqual(0, exit_code)
        self.assertEqual(0, strategy.calls)


if __name__ == "__main__":
    unittest.main()
