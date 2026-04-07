from __future__ import annotations

import hmac
from dataclasses import dataclass, field
import json
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from typing import Any, Mapping, Optional, Protocol

from .command_policy import SUPPORTED_COMMAND_TYPES
from .control_plane import EnvSecretProvider, EnvTokenProvider, HmacRequestSigner, ReplayWindowGuard
from .models import Snapshot


def _now_unix_ms() -> int:
    return int(time.time() * 1000)


def _as_mapping(value: Any) -> Mapping[str, Any]:
    return value if isinstance(value, Mapping) else {}


def _to_int(value: Any, default: int = 0) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return int(default)


def _to_bool(value: Any, default: bool = False) -> bool:
    if isinstance(value, bool):
        return value
    if isinstance(value, str):
        raw = value.strip().lower()
        if raw in {"1", "true", "yes", "on"}:
            return True
        if raw in {"0", "false", "no", "off"}:
            return False
        return default
    if isinstance(value, (int, float)):
        return bool(value)
    return default


def _to_float(value: Any, default: float) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return float(default)


def _stable_json_bytes(payload: Mapping[str, Any]) -> bytes:
    return json.dumps(dict(payload), separators=(",", ":"), sort_keys=True).encode("utf-8")


@dataclass(frozen=True)
class RemotePlannerSettings:
    enabled: bool = False
    timeout_seconds: float = 0.5
    fallback_to_local: bool = True
    require_startup_precheck: bool = False
    max_commands_per_decision: int = 3
    contract_version: str = "1.0"
    client_build: str = "xptool-local"
    enforce_https: bool = True
    allow_insecure_http_localhost: bool = True
    require_decision_id: bool = False

    def normalized(self) -> "RemotePlannerSettings":
        timeout_seconds = min(5.0, max(0.1, _to_float(self.timeout_seconds, 0.5)))
        max_commands = min(16, max(1, int(self.max_commands_per_decision)))
        contract_version = str(self.contract_version or "").strip() or "1.0"
        client_build = str(self.client_build or "").strip() or "xptool-local"
        return RemotePlannerSettings(
            enabled=bool(self.enabled),
            timeout_seconds=timeout_seconds,
            fallback_to_local=bool(self.fallback_to_local),
            require_startup_precheck=bool(self.require_startup_precheck),
            max_commands_per_decision=max_commands,
            contract_version=contract_version,
            client_build=client_build,
            enforce_https=bool(self.enforce_https),
            allow_insecure_http_localhost=bool(self.allow_insecure_http_localhost),
            require_decision_id=bool(self.require_decision_id),
        )


@dataclass(frozen=True)
class RemotePlannerSecuritySettings:
    signing_key: str = ""
    signing_key_id: str = ""
    replay_window_seconds: float = 30.0
    require_response_replay_fields: bool = False
    require_response_signature: bool = False
    verify_command_envelopes: bool = False
    require_command_envelopes: bool = False
    emit_local_command_envelopes: bool = False

    def normalized(self) -> "RemotePlannerSecuritySettings":
        replay_window_seconds = min(300.0, max(1.0, _to_float(self.replay_window_seconds, 30.0)))
        return RemotePlannerSecuritySettings(
            signing_key=str(self.signing_key or "").strip(),
            signing_key_id=str(self.signing_key_id or "").strip(),
            replay_window_seconds=replay_window_seconds,
            require_response_replay_fields=bool(self.require_response_replay_fields),
            require_response_signature=bool(self.require_response_signature),
            verify_command_envelopes=bool(self.verify_command_envelopes),
            require_command_envelopes=bool(self.require_command_envelopes),
            emit_local_command_envelopes=bool(self.emit_local_command_envelopes),
        )

    def signing_enabled(self) -> bool:
        return bool(self.signing_key)


@dataclass(frozen=True)
class RemoteCommandSpec:
    command_type: str
    payload: Mapping[str, Any] = field(default_factory=dict)
    reason: str = ""
    source: str = "xptool.remote"
    envelope: Mapping[str, Any] = field(default_factory=dict)


@dataclass(frozen=True)
class RemotePlannerDecision:
    status: str = "no_action"
    decision_id: str = ""
    reason: str = ""
    commands: tuple[RemoteCommandSpec, ...] = field(default_factory=tuple)


class RemotePlannerClient(Protocol):
    def decide(
        self,
        *,
        snapshot: Snapshot,
        strategy_activity: str,
        strategy_name: str,
        session_id: str,
    ) -> RemotePlannerDecision:
        ...


class NoopRemotePlannerClient:
    def decide(
        self,
        *,
        snapshot: Snapshot,
        strategy_activity: str,
        strategy_name: str,
        session_id: str,
    ) -> RemotePlannerDecision:
        _ = (snapshot, strategy_activity, strategy_name, session_id)
        return RemotePlannerDecision(status="no_action")


class HttpRemotePlannerClient:
    HEADER_TIMESTAMP = "X-XPTool-Timestamp"
    HEADER_NONCE = "X-XPTool-Nonce"
    HEADER_SIGNATURE = "X-XPTool-Signature"
    HEADER_SIGNING_KEY_ID = "X-XPTool-Signing-Key-Id"
    LOCALHOST_HOSTNAMES = {"127.0.0.1", "localhost", "::1"}

    def __init__(
        self,
        *,
        base_url: str,
        token_provider: Optional[EnvTokenProvider] = None,
        secret_provider: Optional[EnvSecretProvider] = None,
        settings: Optional[RemotePlannerSettings] = None,
        security_settings: Optional[RemotePlannerSecuritySettings] = None,
    ) -> None:
        self.base_url = str(base_url or "").strip().rstrip("/")
        if not self.base_url:
            raise ValueError("remote_planner_base_url_missing")
        self.settings = (settings or RemotePlannerSettings()).normalized()
        self.token_provider = token_provider
        self.secret_provider = secret_provider
        self.security_settings = (
            security_settings.normalized()
            if security_settings is not None
            else RemotePlannerSecuritySettings().normalized()
        )
        signing_key = self.security_settings.signing_key
        if not signing_key and self.secret_provider is not None:
            signing_key = str(self.secret_provider.secret() or "").strip()
        if signing_key:
            self._signer: Optional[HmacRequestSigner] = HmacRequestSigner(
                signing_key=signing_key,
                signing_key_id=self.security_settings.signing_key_id,
            )
        else:
            self._signer = None
        self._request_replay_guard = ReplayWindowGuard(
            replay_window_seconds=self.security_settings.replay_window_seconds,
        )
        self._response_replay_guard = ReplayWindowGuard(
            replay_window_seconds=self.security_settings.replay_window_seconds,
        )
        self._command_envelope_replay_guard = ReplayWindowGuard(
            replay_window_seconds=self.security_settings.replay_window_seconds,
        )
        self._validate_base_url_security()

    def decide(
        self,
        *,
        snapshot: Snapshot,
        strategy_activity: str,
        strategy_name: str,
        session_id: str,
    ) -> RemotePlannerDecision:
        payload = {
            "requestId": str(uuid.uuid4()),
            "capturedAtUnixMillis": _now_unix_ms(),
            "sessionId": str(session_id or ""),
            "strategyActivity": str(strategy_activity or ""),
            "strategyName": str(strategy_name or ""),
            "tick": int(snapshot.tick),
            "contractVersion": self.settings.contract_version,
            "clientBuild": self.settings.client_build,
            "snapshot": _snapshot_payload(snapshot),
            "capabilities": {
                "supportedCommandTypes": sorted(SUPPORTED_COMMAND_TYPES),
            },
        }
        response = self._post_json(
            "/v1/planner/decision",
            payload,
            session_id=str(session_id or ""),
        )
        decision = _decision_from_payload(
            response,
            max_commands=self.settings.max_commands_per_decision,
        )
        decision = self._with_command_envelopes(
            decision=decision,
            session_id=str(session_id or ""),
        )
        self._validate_decision_command_envelopes(
            decision=decision,
            session_id=str(session_id or ""),
        )
        if self.settings.require_decision_id and not decision.decision_id:
            raise RuntimeError("remote_planner_decision_id_missing")
        return decision

    def _post_json(self, path: str, payload: Mapping[str, Any], *, session_id: str) -> Mapping[str, Any]:
        body = json.dumps(dict(payload), separators=(",", ":")).encode("utf-8")
        request_timestamp = _now_unix_ms()
        request_nonce = uuid.uuid4().hex
        if not self._request_replay_guard.register(
            nonce=request_nonce,
            timestamp_unix_ms=request_timestamp,
            now_unix_ms=request_timestamp,
        ):
            raise RuntimeError("remote_planner_request_nonce_replay_rejected")
        request = urllib.request.Request(
            url=f"{self.base_url}{path}",
            data=body,
            method="POST",
        )
        request.add_header("Content-Type", "application/json")
        request.add_header(self.HEADER_TIMESTAMP, str(request_timestamp))
        request.add_header(self.HEADER_NONCE, request_nonce)
        if self._signer is not None:
            signature = self._signer.sign(
                method="POST",
                path=path,
                body=body,
                timestamp_unix_ms=request_timestamp,
                nonce=request_nonce,
                session_id=str(session_id or ""),
            )
            request.add_header(self.HEADER_SIGNATURE, signature)
            if self._signer.signing_key_id:
                request.add_header(self.HEADER_SIGNING_KEY_ID, self._signer.signing_key_id)
        token = ""
        if self.token_provider is not None:
            token = str(self.token_provider.token() or "").strip()
        if token:
            request.add_header("Authorization", f"Bearer {token}")
        try:
            with urllib.request.urlopen(request, timeout=self.settings.timeout_seconds) as response:
                raw_bytes = response.read()
                raw = raw_bytes.decode("utf-8", errors="replace")
                response_headers = {
                    str(k).lower(): str(v)
                    for k, v in response.headers.items()
                }
        except urllib.error.URLError as exc:
            raise RuntimeError(f"remote_planner_request_failed:{exc!r}") from exc
        try:
            decoded = json.loads(raw) if raw else {}
        except json.JSONDecodeError as exc:
            raise RuntimeError("remote_planner_response_invalid_json") from exc
        payload_mapping = _as_mapping(decoded)
        self._validate_response_replay(
            payload=payload_mapping,
            response_headers=response_headers,
        )
        self._validate_response_signature(
            path=path,
            body=raw_bytes,
            payload=payload_mapping,
            response_headers=response_headers,
            session_id=str(session_id or ""),
        )
        return payload_mapping

    def _validate_base_url_security(self) -> None:
        if not self.settings.enforce_https:
            return
        parsed = urllib.parse.urlsplit(self.base_url)
        scheme = str(parsed.scheme or "").strip().lower()
        host = str(parsed.hostname or "").strip().lower()
        if scheme == "https":
            return
        if (
            scheme == "http"
            and self.settings.allow_insecure_http_localhost
            and host in self.LOCALHOST_HOSTNAMES
        ):
            return
        raise ValueError("remote_planner_https_required")

    def _validate_response_replay(
        self,
        *,
        payload: Mapping[str, Any],
        response_headers: Mapping[str, str],
    ) -> None:
        header_ts = str(response_headers.get(self.HEADER_TIMESTAMP.lower(), "")).strip()
        header_nonce = str(response_headers.get(self.HEADER_NONCE.lower(), "")).strip()
        body_nonce = str(payload.get("responseNonce", payload.get("nonce", "")) or "").strip()
        body_ts = payload.get("responseTimestampUnixMillis", payload.get("timestampUnixMillis"))

        nonce = header_nonce or body_nonce
        timestamp_unix_ms: Optional[int]
        if header_ts:
            try:
                timestamp_unix_ms = int(header_ts)
            except (TypeError, ValueError):
                timestamp_unix_ms = None
        else:
            try:
                timestamp_unix_ms = int(body_ts)
            except (TypeError, ValueError):
                timestamp_unix_ms = None

        if self.security_settings.require_response_replay_fields and (not nonce or timestamp_unix_ms is None):
            raise RuntimeError("remote_planner_response_replay_fields_missing")
        if not nonce or timestamp_unix_ms is None:
            return
        if not self._response_replay_guard.register(
            nonce=nonce,
            timestamp_unix_ms=timestamp_unix_ms,
            now_unix_ms=_now_unix_ms(),
        ):
            raise RuntimeError("remote_planner_response_replay_rejected")

    def _validate_response_signature(
        self,
        *,
        path: str,
        body: bytes,
        payload: Mapping[str, Any],
        response_headers: Mapping[str, str],
        session_id: str,
    ) -> None:
        header_signature = str(response_headers.get(self.HEADER_SIGNATURE.lower(), "")).strip()
        body_signature = str(payload.get("responseSignature", payload.get("signature", "")) or "").strip()
        signature = header_signature or body_signature
        if self.security_settings.require_response_signature and not signature:
            raise RuntimeError("remote_planner_response_signature_missing")
        if not signature:
            return
        if self._signer is None:
            raise RuntimeError("remote_planner_response_signature_unconfigured")

        header_ts = str(response_headers.get(self.HEADER_TIMESTAMP.lower(), "")).strip()
        body_ts = payload.get("responseTimestampUnixMillis", payload.get("timestampUnixMillis", _now_unix_ms()))
        header_nonce = str(response_headers.get(self.HEADER_NONCE.lower(), "")).strip()
        body_nonce = str(payload.get("responseNonce", payload.get("nonce", "")) or "").strip()
        nonce = header_nonce or body_nonce
        if not nonce:
            raise RuntimeError("remote_planner_response_signature_nonce_missing")
        try:
            timestamp_unix_ms = int(header_ts) if header_ts else int(body_ts)
        except (TypeError, ValueError) as exc:
            raise RuntimeError("remote_planner_response_signature_timestamp_invalid") from exc
        expected = self._signer.sign(
            method="RESPONSE",
            path=path,
            body=body,
            timestamp_unix_ms=timestamp_unix_ms,
            nonce=nonce,
            session_id=str(session_id or ""),
        )
        if not hmac.compare_digest(signature, expected):
            raise RuntimeError("remote_planner_response_signature_mismatch")

    def _with_command_envelopes(
        self,
        *,
        decision: RemotePlannerDecision,
        session_id: str,
    ) -> RemotePlannerDecision:
        if not self.security_settings.emit_local_command_envelopes or self._signer is None:
            return decision
        commands: list[RemoteCommandSpec] = []
        for spec in decision.commands:
            if isinstance(spec.envelope, Mapping) and spec.envelope:
                commands.append(spec)
                continue
            issued_at_unix_ms = _now_unix_ms()
            nonce = uuid.uuid4().hex
            envelope_payload = {
                "commandId": str(uuid.uuid4()),
                "sessionId": str(session_id or ""),
                "issuedAtUnixMs": issued_at_unix_ms,
                "nonce": nonce,
                "commandType": str(spec.command_type or "").strip().upper(),
                "payload": dict(_as_mapping(spec.payload)),
                "signatureBase64": "",
            }
            signature = self._signer.sign(
                method="COMMAND",
                path="/v1/planner/decision",
                body=_stable_json_bytes(_as_mapping(envelope_payload.get("payload"))),
                timestamp_unix_ms=issued_at_unix_ms,
                nonce=nonce,
                session_id=str(session_id or ""),
            )
            envelope_payload["signatureBase64"] = signature
            commands.append(
                RemoteCommandSpec(
                    command_type=spec.command_type,
                    payload=dict(_as_mapping(spec.payload)),
                    reason=spec.reason,
                    source=spec.source,
                    envelope=envelope_payload,
                )
            )
        return RemotePlannerDecision(
            status=decision.status,
            decision_id=decision.decision_id,
            reason=decision.reason,
            commands=tuple(commands),
        )

    def _validate_decision_command_envelopes(
        self,
        *,
        decision: RemotePlannerDecision,
        session_id: str,
    ) -> None:
        if not (self.security_settings.verify_command_envelopes or self.security_settings.require_command_envelopes):
            return
        for spec in decision.commands:
            envelope = _as_mapping(spec.envelope)
            if not envelope:
                if self.security_settings.require_command_envelopes:
                    raise RuntimeError("remote_planner_command_envelope_missing")
                continue
            self._validate_single_command_envelope(
                envelope=envelope,
                command=spec,
                session_id=str(session_id or ""),
            )

    def _validate_single_command_envelope(
        self,
        *,
        envelope: Mapping[str, Any],
        command: RemoteCommandSpec,
        session_id: str,
    ) -> None:
        envelope_type = str(envelope.get("commandType", "") or "").strip().upper()
        command_type = str(command.command_type or "").strip().upper()
        if envelope_type != command_type:
            raise RuntimeError("remote_planner_command_envelope_command_type_mismatch")

        envelope_session_id = str(envelope.get("sessionId", "") or "").strip()
        if session_id and envelope_session_id and envelope_session_id != session_id:
            raise RuntimeError("remote_planner_command_envelope_session_mismatch")
        nonce = str(envelope.get("nonce", "") or "").strip()
        if not nonce:
            raise RuntimeError("remote_planner_command_envelope_nonce_missing")
        issued_at = envelope.get("issuedAtUnixMs")
        try:
            issued_at_unix_ms = int(issued_at)
        except (TypeError, ValueError) as exc:
            raise RuntimeError("remote_planner_command_envelope_timestamp_invalid") from exc
        if not self._command_envelope_replay_guard.register(
            nonce=nonce,
            timestamp_unix_ms=issued_at_unix_ms,
            now_unix_ms=_now_unix_ms(),
        ):
            raise RuntimeError("remote_planner_command_envelope_replay_rejected")

        if not self.security_settings.verify_command_envelopes:
            return
        if self._signer is None:
            raise RuntimeError("remote_planner_command_envelope_signature_unconfigured")
        signature = str(envelope.get("signatureBase64", envelope.get("signature", "")) or "").strip()
        if not signature:
            raise RuntimeError("remote_planner_command_envelope_signature_missing")
        envelope_payload = dict(_as_mapping(envelope.get("payload")))
        command_payload = dict(_as_mapping(command.payload))
        if envelope_payload != command_payload:
            raise RuntimeError("remote_planner_command_envelope_payload_mismatch")
        expected = self._signer.sign(
            method="COMMAND",
            path="/v1/planner/decision",
            body=_stable_json_bytes(envelope_payload),
            timestamp_unix_ms=issued_at_unix_ms,
            nonce=nonce,
            session_id=envelope_session_id if envelope_session_id else str(session_id or ""),
        )
        if not hmac.compare_digest(signature, expected):
            raise RuntimeError("remote_planner_command_envelope_signature_mismatch")


def _decision_from_payload(payload: Mapping[str, Any], *, max_commands: int) -> RemotePlannerDecision:
    decision_id = str(payload.get("decisionId", "") or "").strip()
    status = str(payload.get("status", "") or "").strip().lower() or "no_action"
    reason = str(payload.get("reason", "") or "")
    raw_commands = payload.get("commands")
    if not isinstance(raw_commands, list):
        raw_commands = []
    commands: list[RemoteCommandSpec] = []
    for row in raw_commands:
        if not isinstance(row, Mapping):
            continue
        command_type = str(row.get("commandType", "") or "").strip().upper()
        if not command_type:
            continue
        payload_mapping = _as_mapping(row.get("payload"))
        command_reason = str(row.get("reason", reason) or "")
        source = str(row.get("source", "xptool.remote") or "xptool.remote")
        envelope = _as_mapping(row.get("commandEnvelope"))
        commands.append(
            RemoteCommandSpec(
                command_type=command_type,
                payload=dict(payload_mapping),
                reason=command_reason,
                source=source,
                envelope=dict(envelope),
            )
        )
        if len(commands) >= max(1, int(max_commands)):
            break
    if status == "ok" and not commands:
        status = "no_action"
    return RemotePlannerDecision(
        status=status,
        decision_id=decision_id,
        reason=reason,
        commands=tuple(commands),
    )


def _snapshot_payload(snapshot: Snapshot) -> dict[str, Any]:
    raw = snapshot.raw if isinstance(snapshot.raw, dict) else {}
    payload = {
        "tick": int(snapshot.tick),
        "loggedIn": bool(snapshot.logged_in),
        "bankOpen": bool(snapshot.bank_open),
        "shopOpen": bool(snapshot.shop_open),
        "worldId": snapshot.world_id,
        "playerAnimation": snapshot.player_animation,
        "inventoryCounts": dict(snapshot.inventory_counts),
        "bankCounts": dict(snapshot.bank_counts),
        "shopCounts": dict(snapshot.shop_counts),
        "inventorySlotsUsed": snapshot.inventory_slots_used,
        "hitpointsCurrent": snapshot.hitpoints_current,
        "hitpointsMax": snapshot.hitpoints_max,
        "nearestTree": {
            "id": snapshot.nearest_tree_id,
            "worldX": snapshot.nearest_tree_world_x,
            "worldY": snapshot.nearest_tree_world_y,
            "distance": snapshot.nearest_tree_distance,
            "interactable": snapshot.nearest_tree_interactable,
        },
    }
    # Keep only compact collections required for backend decisioning.
    if isinstance(raw.get("nearbyObjects"), list):
        payload["nearbyObjects"] = raw.get("nearbyObjects")
    if isinstance(raw.get("nearbyNpcs"), list):
        payload["nearbyNpcs"] = raw.get("nearbyNpcs")
    if isinstance(raw.get("nearbyGroundItems"), list):
        payload["nearbyGroundItems"] = raw.get("nearbyGroundItems")
    if isinstance(raw.get("player"), Mapping):
        payload["player"] = dict(_as_mapping(raw.get("player")))
    payload["loggedIn"] = _to_bool(payload.get("loggedIn"), False)
    payload["bankOpen"] = _to_bool(payload.get("bankOpen"), False)
    payload["shopOpen"] = _to_bool(payload.get("shopOpen"), False)
    payload["tick"] = _to_int(payload.get("tick"), 0)
    return payload
