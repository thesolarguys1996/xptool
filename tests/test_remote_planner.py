import argparse
import json
import time
import unittest
from unittest.mock import patch

from runelite_planner.main import (
    build_remote_planner_components,
    strict_remote_policy_violations,
)
from runelite_planner.models import Snapshot
from runelite_planner.remote_planner import (
    HttpRemotePlannerClient,
    RemotePlannerSecuritySettings,
    RemotePlannerSettings,
)
from runelite_planner.control_plane import HmacRequestSigner


class _FakeHttpResponse:
    def __init__(self, body: dict, headers: dict | None = None) -> None:
        self._body = json.dumps(body, separators=(",", ":")).encode("utf-8")
        self.headers = headers or {}

    def read(self) -> bytes:
        return self._body

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        _ = (exc_type, exc, tb)
        return False


class _TokenProvider:
    def __init__(self, token: str) -> None:
        self._token = token

    def token(self) -> str:
        return self._token


def _snapshot() -> Snapshot:
    return Snapshot(
        tick=77,
        logged_in=True,
        bank_open=False,
        inventory_counts={},
        bank_counts={},
        inventory_slots_used=0,
        player_animation=0,
        raw={},
    )


class RemotePlannerTests(unittest.TestCase):
    def test_http_remote_planner_rejects_insecure_http_base_url(self) -> None:
        with self.assertRaisesRegex(ValueError, "remote_planner_https_required"):
            HttpRemotePlannerClient(base_url="http://planner.example")

    def test_http_remote_planner_allows_localhost_http(self) -> None:
        client = HttpRemotePlannerClient(base_url="http://localhost:8787")
        self.assertEqual("http://localhost:8787", client.base_url)

    def test_http_remote_planner_request_contract_and_response_parse(self) -> None:
        now_ms = int(time.time() * 1000)
        captured = {}

        def fake_urlopen(request, timeout=0.0):
            _ = timeout
            captured["url"] = request.full_url
            captured["headers"] = {k.lower(): v for k, v in request.header_items()}
            captured["body"] = json.loads((request.data or b"{}").decode("utf-8"))
            return _FakeHttpResponse(
                {
                    "status": "ok",
                    "decisionId": "decision-123",
                    "reason": "ok",
                    "commands": [
                        {
                            "commandType": "LOGOUT_SAFE",
                            "payload": {"plannerTag": "remote"},
                            "reason": "test",
                            "source": "xptool.remote",
                        }
                    ],
                    "capturedAtUnixMillis": now_ms,
                }
            )

        client = HttpRemotePlannerClient(
            base_url="https://planner.example",
            token_provider=_TokenProvider("token-abc"),
            settings=RemotePlannerSettings(
                enabled=True,
                contract_version="1.2",
                client_build="xptool-test-build",
                max_commands_per_decision=2,
            ),
        )
        with patch("runelite_planner.remote_planner.urllib.request.urlopen", side_effect=fake_urlopen):
            decision = client.decide(
                snapshot=_snapshot(),
                strategy_activity="woodcutting",
                strategy_name="WoodcuttingStrategy",
                session_id="sess-1",
            )
        self.assertEqual("https://planner.example/v1/planner/decision", captured["url"])
        self.assertEqual("Bearer token-abc", captured["headers"].get("authorization"))
        self.assertEqual("1.2", captured["body"].get("contractVersion"))
        self.assertEqual("xptool-test-build", captured["body"].get("clientBuild"))
        self.assertEqual("decision-123", decision.decision_id)
        self.assertEqual("ok", decision.status)
        self.assertEqual(1, len(decision.commands))

    def test_http_remote_planner_requires_decision_id_when_enabled(self) -> None:
        def fake_urlopen(_request, timeout=0.0):
            _ = timeout
            return _FakeHttpResponse({"status": "ok", "commands": []})

        client = HttpRemotePlannerClient(
            base_url="https://planner.example",
            settings=RemotePlannerSettings(
                enabled=True,
                require_decision_id=True,
            ),
        )
        with patch("runelite_planner.remote_planner.urllib.request.urlopen", side_effect=fake_urlopen):
            with self.assertRaisesRegex(RuntimeError, "remote_planner_decision_id_missing"):
                client.decide(
                    snapshot=_snapshot(),
                    strategy_activity="woodcutting",
                    strategy_name="WoodcuttingStrategy",
                    session_id="sess-2",
                )

    def test_http_remote_planner_rejects_duplicate_response_nonce(self) -> None:
        now_ms = int(time.time() * 1000)
        responses = [
            _FakeHttpResponse(
                {"status": "no_action"},
                headers={
                    "X-XPTool-Timestamp": str(now_ms),
                    "X-XPTool-Nonce": "dup-nonce",
                },
            ),
            _FakeHttpResponse(
                {"status": "no_action"},
                headers={
                    "X-XPTool-Timestamp": str(now_ms),
                    "X-XPTool-Nonce": "dup-nonce",
                },
            ),
        ]

        def fake_urlopen(_request, timeout=0.0):
            _ = timeout
            return responses.pop(0)

        client = HttpRemotePlannerClient(
            base_url="https://planner.example",
            settings=RemotePlannerSettings(enabled=True),
            security_settings=RemotePlannerSecuritySettings(
                replay_window_seconds=300.0,
                require_response_replay_fields=True,
            ),
        )
        with patch("runelite_planner.remote_planner.urllib.request.urlopen", side_effect=fake_urlopen):
            client.decide(
                snapshot=_snapshot(),
                strategy_activity="woodcutting",
                strategy_name="WoodcuttingStrategy",
                session_id="sess-3",
            )
            with self.assertRaisesRegex(RuntimeError, "remote_planner_response_replay_rejected"):
                client.decide(
                    snapshot=_snapshot(),
                    strategy_activity="woodcutting",
                    strategy_name="WoodcuttingStrategy",
                    session_id="sess-3",
                )

    def test_http_remote_planner_verifies_response_signature(self) -> None:
        now_ms = int(time.time() * 1000)
        nonce = "nonce-response-remote-1"
        session_id = "sess-4"
        body = {
            "status": "ok",
            "decisionId": "decision-signed",
            "commands": [
                {
                    "commandType": "LOGOUT_SAFE",
                    "payload": {"plannerTag": "remote"},
                }
            ],
        }
        signer = HmacRequestSigner(signing_key="remote-signing-secret", signing_key_id="rk-1")
        raw_bytes = json.dumps(body, separators=(",", ":")).encode("utf-8")
        signature = signer.sign(
            method="RESPONSE",
            path="/v1/planner/decision",
            body=raw_bytes,
            timestamp_unix_ms=now_ms,
            nonce=nonce,
            session_id=session_id,
        )

        def fake_urlopen(_request, timeout=0.0):
            _ = timeout
            return _FakeHttpResponse(
                body,
                headers={
                    "X-XPTool-Timestamp": str(now_ms),
                    "X-XPTool-Nonce": nonce,
                    "X-XPTool-Signature": signature,
                },
            )

        client = HttpRemotePlannerClient(
            base_url="https://planner.example",
            settings=RemotePlannerSettings(enabled=True),
            security_settings=RemotePlannerSecuritySettings(
                signing_key="remote-signing-secret",
                signing_key_id="rk-1",
                require_response_signature=True,
                require_response_replay_fields=True,
                replay_window_seconds=300.0,
            ),
        )
        with patch("runelite_planner.remote_planner.urllib.request.urlopen", side_effect=fake_urlopen):
            decision = client.decide(
                snapshot=_snapshot(),
                strategy_activity="woodcutting",
                strategy_name="WoodcuttingStrategy",
                session_id=session_id,
            )
        self.assertEqual("decision-signed", decision.decision_id)

    def test_http_remote_planner_rejects_missing_required_response_signature(self) -> None:
        now_ms = int(time.time() * 1000)

        def fake_urlopen(_request, timeout=0.0):
            _ = timeout
            return _FakeHttpResponse(
                {"status": "no_action"},
                headers={
                    "X-XPTool-Timestamp": str(now_ms),
                    "X-XPTool-Nonce": "nonce-nosig",
                },
            )

        client = HttpRemotePlannerClient(
            base_url="https://planner.example",
            settings=RemotePlannerSettings(enabled=True),
            security_settings=RemotePlannerSecuritySettings(
                signing_key="remote-signing-secret",
                require_response_signature=True,
                require_response_replay_fields=True,
                replay_window_seconds=300.0,
            ),
        )
        with patch("runelite_planner.remote_planner.urllib.request.urlopen", side_effect=fake_urlopen):
            with self.assertRaisesRegex(RuntimeError, "remote_planner_response_signature_missing"):
                client.decide(
                    snapshot=_snapshot(),
                    strategy_activity="woodcutting",
                    strategy_name="WoodcuttingStrategy",
                    session_id="sess-5",
                )

    def test_build_remote_planner_components_enforces_strict_defaults(self) -> None:
        args = argparse.Namespace(
            remote_planner_url="https://planner.example",
            remote_planner_timeout_seconds=0.5,
            remote_planner_max_commands=3,
            remote_planner_contract_version="1.0",
            remote_planner_client_build="xptool-test",
            remote_planner_token_env="XPTOOL_REMOTE_PLANNER_TOKEN",
        )
        _client, settings = build_remote_planner_components(args)
        self.assertFalse(settings.fallback_to_local)
        self.assertTrue(settings.require_startup_precheck)
        self.assertTrue(settings.require_decision_id)
        self.assertTrue(settings.enforce_https)

    def test_strict_remote_policy_requires_remote_url(self) -> None:
        args = argparse.Namespace(
            remote_planner_url="",
            remote_planner_timeout_seconds=0.5,
            remote_planner_max_commands=3,
            remote_planner_contract_version="1.0",
            remote_planner_client_build="xptool-test",
            remote_planner_token_env="XPTOOL_REMOTE_PLANNER_TOKEN",
        )
        _client, settings = build_remote_planner_components(args)
        violations = strict_remote_policy_violations(remote_planner_settings=settings)
        self.assertIn("remote_planner_url_required", violations)


if __name__ == "__main__":
    unittest.main()
