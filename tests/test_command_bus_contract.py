import unittest

from runelite_planner.command_bus_contract import (
    build_command_bus_record,
    validate_command_bus_record,
)
from runelite_planner.models import RuntimeCommand


class CommandBusContractTests(unittest.TestCase):
    def test_build_command_bus_record_normalizes_supported_command(self) -> None:
        command = RuntimeCommand(
            command_type="logout_safe",
            payload={"plannerTag": "test"},
            reason="unit_test",
            tick=42,
            source="xptool.test",
        )
        record, command_id = build_command_bus_record(
            command,
            default_source="xptool.planner",
            command_id="cmd-1",
            created_at_unix_ms=123,
        )
        self.assertEqual("cmd-1", command_id)
        self.assertEqual("COMMAND", record["type"])
        self.assertEqual("xptool.test", record["source"])
        self.assertEqual(42, record["tick"])
        payload = record["payload"]
        self.assertEqual("cmd-1", payload["commandId"])
        self.assertEqual(123, payload["createdAtUnixMillis"])
        self.assertEqual("LOGOUT_SAFE", payload["commandType"])
        command_payload = payload["commandPayload"]
        self.assertEqual("test", command_payload["plannerTag"])
        self.assertIn("commandEnvelope", command_payload)
        envelope = command_payload["commandEnvelope"]
        self.assertEqual("cmd-1", envelope["commandId"])
        self.assertEqual("LOGOUT_SAFE", envelope["commandType"])
        self.assertEqual(123, envelope["issuedAtUnixMs"])
        self.assertEqual({"plannerTag": "test"}, envelope["payload"])
        self.assertEqual("", envelope["signatureBase64"])
        self.assertEqual("unit_test", payload["reason"])

    def test_build_command_bus_record_rejects_unsupported_command(self) -> None:
        command = RuntimeCommand(command_type="NOT_REAL", payload={})
        with self.assertRaisesRegex(ValueError, "unsupported_command_type"):
            build_command_bus_record(command, default_source="xptool.planner")

    def test_build_command_bus_record_preserves_existing_command_envelope(self) -> None:
        command = RuntimeCommand(
            command_type="logout_safe",
            payload={
                "plannerTag": "test",
                "commandEnvelope": {
                    "commandId": "existing-id",
                    "sessionId": "sess-existing",
                    "issuedAtUnixMs": 99,
                    "nonce": "nonce-existing",
                    "commandType": "LOGOUT_SAFE",
                    "payload": {"plannerTag": "test"},
                    "signatureBase64": "existing-signature",
                },
            },
        )
        record, _ = build_command_bus_record(
            command,
            default_source="xptool.planner",
            command_id="cmd-2",
            created_at_unix_ms=456,
        )
        envelope = record["payload"]["commandPayload"]["commandEnvelope"]
        self.assertEqual("existing-id", envelope["commandId"])
        self.assertEqual("existing-signature", envelope["signatureBase64"])

    def test_build_command_bus_record_signs_command_envelope_when_key_is_set(self) -> None:
        command = RuntimeCommand(
            command_type="logout_safe",
            payload={"plannerTag": "test"},
        )
        record, _ = build_command_bus_record(
            command,
            default_source="xptool.planner",
            command_id="cmd-3",
            created_at_unix_ms=789,
            command_envelope_signing_key="test-signing-key",
            command_envelope_session_id="sess-sign",
        )
        envelope = record["payload"]["commandPayload"]["commandEnvelope"]
        self.assertTrue(bool(envelope["signatureBase64"]))
        self.assertEqual("sess-sign", envelope["sessionId"])

    def test_validate_command_bus_record_rejects_missing_source(self) -> None:
        with self.assertRaisesRegex(ValueError, "invalid_command_record_source"):
            validate_command_bus_record(
                {
                    "type": "COMMAND",
                    "tick": 1,
                    "source": "",
                    "payload": {
                        "commandId": "id-1",
                        "createdAtUnixMillis": 10,
                        "commandType": "LOGOUT_SAFE",
                        "commandPayload": {},
                        "reason": "test",
                    },
                }
            )


if __name__ == "__main__":
    unittest.main()
