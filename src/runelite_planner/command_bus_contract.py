from __future__ import annotations

import base64
import hashlib
import hmac
import json
import time
import uuid
from typing import Any, Mapping

from .command_policy import is_supported_command_type, normalize_command_type
from .models import RuntimeCommand


def build_command_bus_record(
    command: RuntimeCommand,
    *,
    default_source: str,
    command_id: str | None = None,
    created_at_unix_ms: int | None = None,
    emit_command_envelope: bool = True,
    command_envelope_signing_key: str = "",
    command_envelope_session_id: str = "",
    command_envelope_canonical_method: str = "COMMAND",
    command_envelope_canonical_path: str = "/v1/planner/decision",
) -> tuple[dict[str, Any], str]:
    """
    Build one plugin-consumable COMMAND bus row.

    The output shape intentionally matches the existing plugin row parser.
    """
    source = str(command.source or default_source or "").strip() or "xptool.planner"
    command_type = normalize_command_type(command.command_type)
    if not is_supported_command_type(command_type):
        raise ValueError(f"unsupported_command_type:{command_type}")

    command_payload: Mapping[str, Any]
    if isinstance(command.payload, Mapping):
        command_payload = dict(command.payload)
    else:
        command_payload = {}

    reason: str | None
    if command.reason is None:
        reason = None
    else:
        reason = str(command.reason)

    generated_command_id = str(command_id or uuid.uuid4())
    now_ms = int(created_at_unix_ms) if created_at_unix_ms is not None else int(time.time() * 1000)
    tick = int(command.tick) if command.tick is not None else -1
    if emit_command_envelope:
        command_payload = _with_command_envelope(
            command_payload,
            command_id=generated_command_id,
            command_type=command_type,
            issued_at_unix_ms=now_ms,
            session_id=str(command_envelope_session_id or "").strip(),
            signing_key=str(command_envelope_signing_key or "").strip(),
            canonical_method=str(command_envelope_canonical_method or "COMMAND"),
            canonical_path=str(command_envelope_canonical_path or "/v1/planner/decision"),
        )

    record = {
        "type": "COMMAND",
        "tick": tick,
        "source": source,
        "payload": {
            "commandId": generated_command_id,
            "createdAtUnixMillis": now_ms,
            "commandType": command_type,
            "commandPayload": command_payload,
            "reason": reason,
        },
    }
    validate_command_bus_record(record)
    return record, generated_command_id


def validate_command_bus_record(record: Mapping[str, Any]) -> None:
    if str(record.get("type") or "").strip().upper() != "COMMAND":
        raise ValueError("invalid_command_record_type")
    source = str(record.get("source") or "").strip()
    if not source:
        raise ValueError("invalid_command_record_source")

    tick_raw = record.get("tick")
    try:
        int(tick_raw)
    except (TypeError, ValueError) as exc:
        raise ValueError("invalid_command_record_tick") from exc

    payload = record.get("payload")
    if not isinstance(payload, Mapping):
        raise ValueError("invalid_command_record_payload")

    command_id = str(payload.get("commandId") or "").strip()
    if not command_id:
        raise ValueError("invalid_command_payload_command_id")

    created_at = payload.get("createdAtUnixMillis")
    try:
        created_at_int = int(created_at)
    except (TypeError, ValueError) as exc:
        raise ValueError("invalid_command_payload_created_at") from exc
    if created_at_int < 0:
        raise ValueError("invalid_command_payload_created_at_negative")

    command_type = normalize_command_type(str(payload.get("commandType") or ""))
    if not command_type:
        raise ValueError("invalid_command_payload_command_type")
    if not is_supported_command_type(command_type):
        raise ValueError(f"unsupported_command_type:{command_type}")

    command_payload = payload.get("commandPayload")
    if command_payload is not None and not isinstance(command_payload, Mapping):
        raise ValueError("invalid_command_payload_command_payload")


def _with_command_envelope(
    command_payload: Mapping[str, Any],
    *,
    command_id: str,
    command_type: str,
    issued_at_unix_ms: int,
    session_id: str,
    signing_key: str,
    canonical_method: str,
    canonical_path: str,
) -> dict[str, Any]:
    payload = dict(command_payload)
    existing = payload.get("commandEnvelope")
    if isinstance(existing, Mapping):
        return payload
    nonce = uuid.uuid4().hex
    envelope_payload = dict(payload)
    signature = ""
    if signing_key:
        signature = _sign_command_envelope(
            method=canonical_method,
            path=canonical_path,
            body=_stable_json_bytes(envelope_payload),
            timestamp_unix_ms=int(issued_at_unix_ms),
            nonce=nonce,
            session_id=session_id,
            signing_key=signing_key,
        )
    payload["commandEnvelope"] = {
        "commandId": str(command_id),
        "sessionId": session_id,
        "issuedAtUnixMs": int(issued_at_unix_ms),
        "nonce": nonce,
        "commandType": str(command_type),
        "payload": envelope_payload,
        "signatureBase64": signature,
    }
    return payload


def _stable_json_bytes(payload: Mapping[str, Any]) -> bytes:
    return json.dumps(dict(payload), separators=(",", ":"), sort_keys=True).encode("utf-8")


def _sign_command_envelope(
    *,
    method: str,
    path: str,
    body: bytes,
    timestamp_unix_ms: int,
    nonce: str,
    session_id: str,
    signing_key: str,
) -> str:
    canonical = "\n".join(
        [
            str(method or "COMMAND").strip().upper(),
            str(path or "").strip(),
            str(int(timestamp_unix_ms)),
            str(nonce or "").strip(),
            str(session_id or "").strip(),
            _sha256_hex(body),
        ]
    )
    digest = hmac.new(
        str(signing_key or "").encode("utf-8"),
        canonical.encode("utf-8"),
        hashlib.sha256,
    ).digest()
    return base64.urlsafe_b64encode(digest).decode("ascii").rstrip("=")


def _sha256_hex(body: bytes) -> str:
    return hashlib.sha256(body).hexdigest()
