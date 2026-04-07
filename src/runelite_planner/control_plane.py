from __future__ import annotations

import base64
from dataclasses import dataclass, field
import hashlib
import hmac
import json
import os
from pathlib import Path
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from typing import Any, Mapping, Optional, Protocol


def _now_unix_ms() -> int:
    return int(time.time() * 1000)


def _normalize_activity(activity: str) -> str:
    return str(activity or "").strip().lower()


def _as_bool(value: Any, default: bool = False) -> bool:
    if isinstance(value, bool):
        return value
    if isinstance(value, str):
        raw = value.strip().lower()
        if raw in {"true", "1", "yes", "on"}:
            return True
        if raw in {"false", "0", "no", "off"}:
            return False
        return default
    if isinstance(value, (int, float)):
        return bool(value)
    return default


def _as_float(value: Any, default: float) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return float(default)


def _as_mapping(value: Any) -> Mapping[str, Any]:
    return value if isinstance(value, Mapping) else {}


def _as_sequence(value: Any) -> list[Any]:
    return value if isinstance(value, list) else []


def _normalize_key(value: str) -> str:
    raw = str(value or "").strip().lower()
    if not raw:
        return ""
    out = []
    for ch in raw:
        if ("a" <= ch <= "z") or ("0" <= ch <= "9"):
            out.append(ch)
    return "".join(out)


def _hash_body_sha256_hex(body: bytes) -> str:
    return hashlib.sha256(body).hexdigest()


@dataclass(frozen=True)
class ControlPlaneRuntimeSettings:
    enabled: bool = False
    poll_interval_seconds: float = 3.0

    def normalized(self) -> "ControlPlaneRuntimeSettings":
        poll_seconds = _as_float(self.poll_interval_seconds, 3.0)
        poll_seconds = min(60.0, max(0.25, poll_seconds))
        return ControlPlaneRuntimeSettings(
            enabled=bool(self.enabled),
            poll_interval_seconds=poll_seconds,
        )


@dataclass(frozen=True)
class ControlPlaneSecuritySettings:
    signing_key: str = ""
    signing_key_id: str = ""
    replay_window_seconds: float = 30.0
    require_response_replay_fields: bool = False
    enforce_https: bool = True
    allow_insecure_http_localhost: bool = True
    contract_version: str = "1.0"
    client_build: str = "xptool-local"
    require_decision_id: bool = False

    def normalized(self) -> "ControlPlaneSecuritySettings":
        replay_window_seconds = _as_float(self.replay_window_seconds, 30.0)
        replay_window_seconds = min(300.0, max(1.0, replay_window_seconds))
        contract_version = str(self.contract_version or "").strip() or "1.0"
        client_build = str(self.client_build or "").strip() or "xptool-local"
        return ControlPlaneSecuritySettings(
            signing_key=str(self.signing_key or "").strip(),
            signing_key_id=str(self.signing_key_id or "").strip(),
            replay_window_seconds=replay_window_seconds,
            require_response_replay_fields=bool(self.require_response_replay_fields),
            enforce_https=bool(self.enforce_https),
            allow_insecure_http_localhost=bool(self.allow_insecure_http_localhost),
            contract_version=contract_version,
            client_build=client_build,
            require_decision_id=bool(self.require_decision_id),
        )

    def signing_enabled(self) -> bool:
        return bool(self.signing_key)


@dataclass(frozen=True)
class ControlPlaneAuditPolicy:
    retention_days: float = 14.0
    max_event_bytes: int = 8192
    max_string_chars: int = 512
    max_list_items: int = 64
    max_dict_items: int = 128
    max_depth: int = 6
    prune_interval_seconds: float = 900.0

    def normalized(self) -> "ControlPlaneAuditPolicy":
        retention_days = min(365.0, max(0.0, _as_float(self.retention_days, 14.0)))
        max_event_bytes = int(min(262_144, max(1024, int(self.max_event_bytes))))
        max_string_chars = int(min(8192, max(32, int(self.max_string_chars))))
        max_list_items = int(min(2048, max(4, int(self.max_list_items))))
        max_dict_items = int(min(2048, max(4, int(self.max_dict_items))))
        max_depth = int(min(20, max(2, int(self.max_depth))))
        prune_interval_seconds = min(86_400.0, max(0.0, _as_float(self.prune_interval_seconds, 900.0)))
        return ControlPlaneAuditPolicy(
            retention_days=retention_days,
            max_event_bytes=max_event_bytes,
            max_string_chars=max_string_chars,
            max_list_items=max_list_items,
            max_dict_items=max_dict_items,
            max_depth=max_depth,
            prune_interval_seconds=prune_interval_seconds,
        )


@dataclass(frozen=True)
class ControlPlaneSession:
    session_id: str
    issued_at_unix_ms: int = 0
    expires_at_unix_ms: int = 0
    access_token: str = ""

    @staticmethod
    def create(*, session_id: str = "", access_token: str = "") -> "ControlPlaneSession":
        sid = str(session_id or "").strip() or str(uuid.uuid4())
        return ControlPlaneSession(
            session_id=sid,
            issued_at_unix_ms=_now_unix_ms(),
            access_token=str(access_token or ""),
        )


@dataclass(frozen=True)
class ControlPlanePolicySnapshot:
    kill_switch_global: bool = False
    disabled_activities: tuple[str, ...] = field(default_factory=tuple)
    feature_flags: Mapping[str, bool] = field(default_factory=dict)
    reason: str = ""
    fetched_at_unix_ms: int = 0

    @staticmethod
    def default() -> "ControlPlanePolicySnapshot":
        return ControlPlanePolicySnapshot(
            kill_switch_global=False,
            disabled_activities=tuple(),
            feature_flags={},
            reason="default_allow",
            fetched_at_unix_ms=_now_unix_ms(),
        )

    def is_activity_enabled(self, activity: str) -> bool:
        normalized = _normalize_activity(activity)
        if not normalized:
            return True
        if self.kill_switch_global:
            return False
        if normalized in self.disabled_activities:
            return False
        key = f"activity.{normalized}.enabled"
        if key in self.feature_flags:
            return bool(self.feature_flags.get(key))
        return True

    def feature_enabled(self, key: str, default: bool = False) -> bool:
        normalized = str(key or "").strip().lower()
        if not normalized:
            return default
        if normalized not in self.feature_flags:
            return default
        return bool(self.feature_flags[normalized])


def policy_snapshot_from_mapping(raw: Mapping[str, Any]) -> ControlPlanePolicySnapshot:
    payload = _as_mapping(raw)
    kill_switch = _as_mapping(payload.get("killSwitch"))
    global_kill_switch = _as_bool(
        payload.get("killSwitchGlobal", kill_switch.get("global", False)),
        False,
    )

    disabled_raw = payload.get("disabledActivities")
    if not isinstance(disabled_raw, list):
        disabled_raw = kill_switch.get("activities")
    disabled = tuple(
        sorted(
            {
                _normalize_activity(activity)
                for activity in _as_sequence(disabled_raw)
                if _normalize_activity(activity)
            }
        )
    )

    raw_feature_flags = _as_mapping(payload.get("featureFlags"))
    feature_flags: dict[str, bool] = {}
    for key, value in raw_feature_flags.items():
        normalized_key = str(key or "").strip().lower()
        if not normalized_key:
            continue
        feature_flags[normalized_key] = _as_bool(value, False)

    reason = str(payload.get("reason", "") or "")
    fetched_at = payload.get("fetchedAtUnixMillis", payload.get("fetchedAtMs", _now_unix_ms()))
    try:
        fetched_at_unix_ms = int(fetched_at)
    except (TypeError, ValueError):
        fetched_at_unix_ms = _now_unix_ms()

    return ControlPlanePolicySnapshot(
        kill_switch_global=global_kill_switch,
        disabled_activities=disabled,
        feature_flags=feature_flags,
        reason=reason,
        fetched_at_unix_ms=fetched_at_unix_ms,
    )


class ControlPlaneClient(Protocol):
    def start_session(
        self,
        *,
        runner_id: str,
        strategy_name: str,
        writer_path: str,
        dry_run: bool,
    ) -> ControlPlaneSession:
        ...

    def refresh_policy(
        self,
        *,
        session: ControlPlaneSession,
        strategy_name: str,
        tick: int,
    ) -> ControlPlanePolicySnapshot:
        ...

    def close_session(self, *, session: ControlPlaneSession, reason: str) -> None:
        ...


class ControlPlaneTokenProvider(Protocol):
    def token(self) -> str:
        ...


class ControlPlaneSecretProvider(Protocol):
    def secret(self) -> str:
        ...


class EnvTokenProvider:
    def __init__(self, env_var: str = "XPTOOL_CONTROL_PLANE_TOKEN") -> None:
        self.env_var = str(env_var or "XPTOOL_CONTROL_PLANE_TOKEN")

    def token(self) -> str:
        return str(os.environ.get(self.env_var, "") or "").strip()


class EnvSecretProvider:
    def __init__(self, env_var: str = "XPTOOL_CONTROL_PLANE_SIGNING_KEY") -> None:
        self.env_var = str(env_var or "XPTOOL_CONTROL_PLANE_SIGNING_KEY")

    def secret(self) -> str:
        return str(os.environ.get(self.env_var, "") or "").strip()


class NoopControlPlaneClient:
    def start_session(
        self,
        *,
        runner_id: str,
        strategy_name: str,
        writer_path: str,
        dry_run: bool,
    ) -> ControlPlaneSession:
        _ = (runner_id, strategy_name, writer_path, dry_run)
        return ControlPlaneSession.create()

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


class FileControlPlaneClient:
    """
    Local control-plane policy source for development/staging.

    Expected file shape:
    {
      "killSwitchGlobal": false,
      "disabledActivities": ["woodcutting"],
      "featureFlags": {
        "break_scheduler_enabled": true,
        "activity.woodcutting.enabled": true
      }
    }
    """

    def __init__(self, policy_path: str) -> None:
        self.policy_path = str(policy_path or "").strip()

    def start_session(
        self,
        *,
        runner_id: str,
        strategy_name: str,
        writer_path: str,
        dry_run: bool,
    ) -> ControlPlaneSession:
        _ = (runner_id, strategy_name, writer_path, dry_run)
        payload = self._read_policy_payload()
        session_id = str(payload.get("sessionId", "") or "").strip()
        return ControlPlaneSession.create(session_id=session_id)

    def refresh_policy(
        self,
        *,
        session: ControlPlaneSession,
        strategy_name: str,
        tick: int,
    ) -> ControlPlanePolicySnapshot:
        _ = (session, strategy_name, tick)
        payload = self._read_policy_payload()
        return policy_snapshot_from_mapping(payload)

    def close_session(self, *, session: ControlPlaneSession, reason: str) -> None:
        _ = (session, reason)

    def _read_policy_payload(self) -> Mapping[str, Any]:
        if not self.policy_path:
            return {}
        path = Path(self.policy_path)
        if not path.exists():
            return {}
        try:
            payload = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            return {}
        return _as_mapping(payload)


class ReplayWindowGuard:
    def __init__(self, *, replay_window_seconds: float, max_entries: int = 4096) -> None:
        self.replay_window_ms = int(max(1.0, replay_window_seconds) * 1000.0)
        self.max_entries = max(128, int(max_entries))
        self._lock = threading.Lock()
        self._seen: dict[str, int] = {}

    def register(self, *, nonce: str, timestamp_unix_ms: int, now_unix_ms: Optional[int] = None) -> bool:
        nonce_key = str(nonce or "").strip()
        if not nonce_key:
            return False
        try:
            ts = int(timestamp_unix_ms)
        except (TypeError, ValueError):
            return False
        current = _now_unix_ms() if now_unix_ms is None else int(now_unix_ms)
        if abs(current - ts) > self.replay_window_ms:
            return False
        with self._lock:
            self._prune_locked(now_unix_ms=current)
            if nonce_key in self._seen:
                return False
            self._seen[nonce_key] = ts
            if len(self._seen) > self.max_entries:
                oldest = sorted(self._seen.items(), key=lambda item: item[1])[: len(self._seen) - self.max_entries]
                for key, _ in oldest:
                    self._seen.pop(key, None)
        return True

    def _prune_locked(self, *, now_unix_ms: int) -> None:
        cutoff = now_unix_ms - self.replay_window_ms
        expired = [key for key, ts in self._seen.items() if ts < cutoff]
        for key in expired:
            self._seen.pop(key, None)


class HmacRequestSigner:
    def __init__(self, *, signing_key: str, signing_key_id: str = "") -> None:
        secret = str(signing_key or "")
        if not secret:
            raise ValueError("signing_key_required")
        self._secret = secret.encode("utf-8")
        self.signing_key_id = str(signing_key_id or "").strip()

    def sign(
        self,
        *,
        method: str,
        path: str,
        body: bytes,
        timestamp_unix_ms: int,
        nonce: str,
        session_id: str,
    ) -> str:
        canonical = "\n".join(
            [
                str(method or "POST").strip().upper(),
                str(path or "").strip(),
                str(int(timestamp_unix_ms)),
                str(nonce or "").strip(),
                str(session_id or "").strip(),
                _hash_body_sha256_hex(body),
            ]
        )
        digest = hmac.new(self._secret, canonical.encode("utf-8"), hashlib.sha256).digest()
        return base64.urlsafe_b64encode(digest).decode("ascii").rstrip("=")


class HttpControlPlaneClient:
    HEADER_TIMESTAMP = "X-XPTool-Timestamp"
    HEADER_NONCE = "X-XPTool-Nonce"
    HEADER_SIGNATURE = "X-XPTool-Signature"
    HEADER_SIGNING_KEY_ID = "X-XPTool-Signing-Key-Id"
    HEADER_SESSION_ID = "X-XPTool-Session-Id"
    LOCALHOST_HOSTNAMES = {"127.0.0.1", "localhost", "::1"}

    def __init__(
        self,
        *,
        base_url: str,
        token_provider: Optional[ControlPlaneTokenProvider] = None,
        secret_provider: Optional[ControlPlaneSecretProvider] = None,
        security_settings: Optional[ControlPlaneSecuritySettings] = None,
        timeout_seconds: float = 2.0,
    ) -> None:
        self.base_url = str(base_url or "").strip().rstrip("/")
        if not self.base_url:
            raise ValueError("control_plane_base_url_missing")
        self.token_provider = token_provider
        self.secret_provider = secret_provider
        self.timeout_seconds = min(15.0, max(0.25, _as_float(timeout_seconds, 2.0)))
        self.security_settings = (
            security_settings.normalized()
            if security_settings is not None
            else ControlPlaneSecuritySettings().normalized()
        )
        self._validate_base_url_security()
        self._request_replay_guard = ReplayWindowGuard(
            replay_window_seconds=self.security_settings.replay_window_seconds,
        )
        self._response_replay_guard = ReplayWindowGuard(
            replay_window_seconds=self.security_settings.replay_window_seconds,
        )
        self._request_signer: Optional[HmacRequestSigner] = None
        signing_key = self.security_settings.signing_key
        if not signing_key and self.secret_provider is not None:
            signing_key = str(self.secret_provider.secret() or "").strip()
        if signing_key:
            self._request_signer = HmacRequestSigner(
                signing_key=signing_key,
                signing_key_id=self.security_settings.signing_key_id,
            )

    def start_session(
        self,
        *,
        runner_id: str,
        strategy_name: str,
        writer_path: str,
        dry_run: bool,
    ) -> ControlPlaneSession:
        payload = self._request_json(
            "/v1/planner/session/start",
            {
                "runnerId": str(runner_id or ""),
                "strategyName": str(strategy_name or ""),
                "writerPath": str(writer_path or ""),
                "dryRun": bool(dry_run),
            },
        )
        session_id = str(payload.get("sessionId", "") or "").strip()
        access_token = str(payload.get("accessToken", "") or "").strip()
        issued_at = payload.get("issuedAtUnixMillis", _now_unix_ms())
        expires_at = payload.get("expiresAtUnixMillis", 0)
        try:
            issued_at_unix_ms = int(issued_at)
        except (TypeError, ValueError):
            issued_at_unix_ms = _now_unix_ms()
        try:
            expires_at_unix_ms = int(expires_at)
        except (TypeError, ValueError):
            expires_at_unix_ms = 0
        return ControlPlaneSession(
            session_id=session_id or str(uuid.uuid4()),
            access_token=access_token,
            issued_at_unix_ms=issued_at_unix_ms,
            expires_at_unix_ms=expires_at_unix_ms,
        )

    def refresh_policy(
        self,
        *,
        session: ControlPlaneSession,
        strategy_name: str,
        tick: int,
    ) -> ControlPlanePolicySnapshot:
        payload = self._request_json(
            "/v1/planner/session/refresh",
            {
                "sessionId": str(session.session_id or ""),
                "strategyName": str(strategy_name or ""),
                "tick": int(tick),
            },
            session=session,
            require_decision_id=self.security_settings.require_decision_id,
        )
        return policy_snapshot_from_mapping(payload)

    def close_session(self, *, session: ControlPlaneSession, reason: str) -> None:
        self._request_json(
            "/v1/planner/session/close",
            {
                "sessionId": str(session.session_id or ""),
                "reason": str(reason or ""),
            },
            session=session,
        )

    def _request_json(
        self,
        path: str,
        payload: Mapping[str, Any],
        *,
        session: Optional[ControlPlaneSession] = None,
        require_decision_id: bool = False,
    ) -> Mapping[str, Any]:
        request_payload = dict(payload)
        request_payload.setdefault("contractVersion", self.security_settings.contract_version)
        request_payload.setdefault("clientBuild", self.security_settings.client_build)
        body = json.dumps(request_payload, separators=(",", ":")).encode("utf-8")
        url = f"{self.base_url}{path}"
        request = urllib.request.Request(url=url, data=body, method="POST")
        request.add_header("Content-Type", "application/json")
        request_timestamp = _now_unix_ms()
        request_nonce = uuid.uuid4().hex
        if not self._request_replay_guard.register(
            nonce=request_nonce,
            timestamp_unix_ms=request_timestamp,
            now_unix_ms=request_timestamp,
        ):
            raise RuntimeError("control_plane_request_nonce_replay_rejected")
        request.add_header(self.HEADER_TIMESTAMP, str(request_timestamp))
        request.add_header(self.HEADER_NONCE, request_nonce)
        if session is not None and session.session_id:
            request.add_header(self.HEADER_SESSION_ID, str(session.session_id))
        if self._request_signer is not None:
            session_id = session.session_id if session is not None else ""
            signature = self._request_signer.sign(
                method="POST",
                path=path,
                body=body,
                timestamp_unix_ms=request_timestamp,
                nonce=request_nonce,
                session_id=session_id,
            )
            request.add_header(self.HEADER_SIGNATURE, signature)
            if self._request_signer.signing_key_id:
                request.add_header(self.HEADER_SIGNING_KEY_ID, self._request_signer.signing_key_id)
        token = self._session_access_token(session)
        if not token and self.token_provider is not None:
            token = str(self.token_provider.token() or "").strip()
        if token:
            request.add_header("Authorization", f"Bearer {token}")
        try:
            with urllib.request.urlopen(request, timeout=self.timeout_seconds) as response:
                raw = response.read().decode("utf-8", errors="replace")
                response_headers = {
                    str(key).lower(): str(value)
                    for key, value in response.headers.items()
                }
        except urllib.error.URLError as exc:
            raise RuntimeError(f"control_plane_request_failed:{exc!r}") from exc
        try:
            decoded = json.loads(raw) if raw else {}
        except json.JSONDecodeError as exc:
            raise RuntimeError("control_plane_response_invalid_json") from exc
        payload_mapping = _as_mapping(decoded)
        self._validate_response_replay(
            payload=payload_mapping,
            response_headers=response_headers,
        )
        if require_decision_id:
            decision_id = str(payload_mapping.get("decisionId", "") or "").strip()
            if not decision_id:
                raise RuntimeError("control_plane_decision_id_missing")
        return payload_mapping

    def _validate_base_url_security(self) -> None:
        if not self.security_settings.enforce_https:
            return
        parsed = urllib.parse.urlsplit(self.base_url)
        scheme = str(parsed.scheme or "").strip().lower()
        host = str(parsed.hostname or "").strip().lower()
        if scheme == "https":
            return
        if (
            scheme == "http"
            and self.security_settings.allow_insecure_http_localhost
            and host in self.LOCALHOST_HOSTNAMES
        ):
            return
        raise ValueError("control_plane_https_required")

    def _session_access_token(self, session: Optional[ControlPlaneSession]) -> str:
        if session is None:
            return ""
        token = str(session.access_token or "").strip()
        if not token:
            return ""
        try:
            expires_at = int(session.expires_at_unix_ms)
        except (TypeError, ValueError):
            expires_at = 0
        if expires_at <= 0:
            return token
        if _now_unix_ms() >= expires_at:
            return ""
        return token

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

        require_fields = self.security_settings.require_response_replay_fields
        if require_fields and (not nonce or timestamp_unix_ms is None):
            raise RuntimeError("control_plane_response_replay_fields_missing")
        if not nonce or timestamp_unix_ms is None:
            return
        if not self._response_replay_guard.register(
            nonce=nonce,
            timestamp_unix_ms=timestamp_unix_ms,
            now_unix_ms=_now_unix_ms(),
        ):
            raise RuntimeError("control_plane_response_replay_rejected")


class ControlPlaneAuditSink:
    REDACTED_VALUE = "***REDACTED***"
    SENSITIVE_KEY_TOKENS = (
        "password",
        "passwd",
        "secret",
        "token",
        "authorization",
        "apikey",
        "accesstoken",
        "refreshtoken",
        "username",
        "email",
        "accountname",
        "displayname",
        "playername",
        "cookie",
        "sessionkey",
        "privatekey",
    )

    def __init__(
        self,
        path: str,
        *,
        source: str = "xptool.control_plane",
        policy: Optional[ControlPlaneAuditPolicy] = None,
    ) -> None:
        self.path = str(path or "").strip()
        self.source = str(source or "xptool.control_plane")
        self.policy = (policy or ControlPlaneAuditPolicy()).normalized()
        self._lock = threading.Lock()
        self._last_prune_monotonic = 0.0

    def emit(self, event_type: str, details: Mapping[str, Any]) -> None:
        if not self.path:
            return
        sanitized_details = self._sanitize_value(
            _as_mapping(details),
            depth=0,
        )
        payload = {
            "type": "CONTROL_PLANE_AUDIT",
            "eventType": str(event_type or "").strip() or "UNKNOWN",
            "source": self.source,
            "eventId": str(uuid.uuid4()),
            "capturedAtUnixMillis": _now_unix_ms(),
            "details": sanitized_details,
        }
        payload = self._cap_payload_bytes(payload)
        path = Path(self.path)
        with self._lock:
            path.parent.mkdir(parents=True, exist_ok=True)
            self._maybe_prune_locked(path, payload["capturedAtUnixMillis"])
            with open(path, "a", encoding="utf-8") as fh:
                fh.write(json.dumps(payload, separators=(",", ":")) + "\n")

    def _sanitize_value(self, value: Any, *, depth: int, key_name: str = "") -> Any:
        if depth >= self.policy.max_depth:
            return "[TRUNCATED_DEPTH]"
        if self._is_sensitive_key(key_name):
            return self.REDACTED_VALUE
        if value is None:
            return None
        if isinstance(value, bool):
            return value
        if isinstance(value, int):
            return value
        if isinstance(value, float):
            return value
        if isinstance(value, str):
            if len(value) <= self.policy.max_string_chars:
                return value
            return value[: self.policy.max_string_chars] + "...[truncated]"
        if isinstance(value, bytes):
            return f"<bytes:{len(value)}>"
        if isinstance(value, Mapping):
            out: dict[str, Any] = {}
            for idx, (k, v) in enumerate(value.items()):
                if idx >= self.policy.max_dict_items:
                    out["__truncated_dict_items__"] = True
                    break
                key = str(k)
                out[key] = self._sanitize_value(v, depth=depth + 1, key_name=key)
            return out
        if isinstance(value, (list, tuple, set)):
            out_list: list[Any] = []
            for idx, entry in enumerate(value):
                if idx >= self.policy.max_list_items:
                    out_list.append("[TRUNCATED_LIST_ITEMS]")
                    break
                out_list.append(self._sanitize_value(entry, depth=depth + 1))
            return out_list
        return self._sanitize_value(str(value), depth=depth + 1)

    def _is_sensitive_key(self, key_name: str) -> bool:
        normalized = _normalize_key(key_name)
        if not normalized:
            return False
        for token in self.SENSITIVE_KEY_TOKENS:
            if token in normalized:
                return True
        return False

    def _cap_payload_bytes(self, payload: Mapping[str, Any]) -> Mapping[str, Any]:
        try:
            encoded = json.dumps(payload, separators=(",", ":")).encode("utf-8")
        except Exception:
            return payload
        if len(encoded) <= self.policy.max_event_bytes:
            return payload
        details = _as_mapping(payload.get("details"))
        preview_keys = list(details.keys())[:16]
        capped = dict(payload)
        capped["details"] = {
            "truncated": True,
            "reason": "max_event_bytes_exceeded",
            "maxEventBytes": int(self.policy.max_event_bytes),
            "detailKeysPreview": preview_keys,
        }
        try:
            encoded_capped = json.dumps(capped, separators=(",", ":")).encode("utf-8")
        except Exception:
            return capped
        if len(encoded_capped) <= self.policy.max_event_bytes:
            return capped
        hard_capped = dict(capped)
        hard_capped["details"] = {
            "truncated": True,
            "reason": "max_event_bytes_exceeded_hard",
            "maxEventBytes": int(self.policy.max_event_bytes),
        }
        return hard_capped

    def _maybe_prune_locked(self, path: Path, now_unix_ms: int) -> None:
        if self.policy.retention_days <= 0.0:
            cutoff_unix_ms = now_unix_ms
        else:
            cutoff_unix_ms = now_unix_ms - int(self.policy.retention_days * 24 * 60 * 60 * 1000)
        now_monotonic = time.monotonic()
        if self.policy.prune_interval_seconds > 0.0:
            if (now_monotonic - self._last_prune_monotonic) < self.policy.prune_interval_seconds:
                return
        self._last_prune_monotonic = now_monotonic
        self._prune_locked(path, cutoff_unix_ms)

    def _prune_locked(self, path: Path, cutoff_unix_ms: int) -> None:
        if not path.exists():
            return
        tmp_path = path.with_suffix(path.suffix + ".tmp")
        try:
            with open(path, "r", encoding="utf-8", errors="replace") as src, open(
                tmp_path, "w", encoding="utf-8"
            ) as dst:
                for raw_line in src:
                    line = raw_line.rstrip("\n")
                    if not line:
                        continue
                    keep = True
                    try:
                        parsed = json.loads(line)
                    except json.JSONDecodeError:
                        parsed = None
                    if isinstance(parsed, Mapping):
                        captured_raw = parsed.get("capturedAtUnixMillis")
                        try:
                            captured_unix_ms = int(captured_raw)
                        except (TypeError, ValueError):
                            captured_unix_ms = 0
                        if captured_unix_ms > 0 and captured_unix_ms < cutoff_unix_ms:
                            keep = False
                    if keep:
                        dst.write(line + "\n")
            os.replace(tmp_path, path)
        except OSError:
            try:
                if tmp_path.exists():
                    tmp_path.unlink()
            except OSError:
                pass
