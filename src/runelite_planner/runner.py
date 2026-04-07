from __future__ import annotations

from dataclasses import dataclass
import random
import threading
import time
import uuid
from typing import Any, Callable, Optional, Sequence

from .runtime_strategy import RuntimeStrategy
from .bridge import CommandBusWriter, parse_execution_line, parse_snapshot_line
from .command_policy import is_supported_command_type
from .control_plane import (
    ControlPlaneAuditSink,
    ControlPlaneClient,
    ControlPlanePolicySnapshot,
    ControlPlaneRuntimeSettings,
    ControlPlaneSession,
    NoopControlPlaneClient,
)
from .models import RuntimeCommand, Snapshot
from .remote_planner import (
    NoopRemotePlannerClient,
    RemotePlannerClient,
    RemotePlannerSettings,
)

RuntimeCallback = Callable[[str], None]
FOLLOW_IDLE_SLEEP_SECONDS = 0.01
NO_SNAPSHOT_WARNING_INITIAL_SECONDS = 8.0
NO_SNAPSHOT_WARNING_REPEAT_SECONDS = 20.0
BREAK_NO_SNAPSHOT_ADVANCE_MIN_SECONDS = 0.20
BREAK_LOGOUT_PENDING_NO_SNAPSHOT_ASSUME_LOGGED_OUT_SECONDS = 2.5


class NoopRuntimeCore:
    def on_snapshot(self, snapshot: Snapshot, intents: list[Any]) -> list[Any]:
        _ = (snapshot, intents)
        return []

    def on_dispatch_enqueued(self, *, ticket_id: str, command_id: str, tick: int) -> None:
        _ = (ticket_id, command_id, tick)

    def on_dispatch_failed(self, *, ticket_id: str, tick: int, reason: str) -> None:
        _ = (ticket_id, tick, reason)

    def on_executor_row(self, row: dict) -> None:
        _ = row

    def telemetry_snapshot(self) -> dict:
        return {}


@dataclass(frozen=True)
class BreakSettings:
    enabled: bool = False
    work_minutes_min: float = 35.0
    work_minutes_max: float = 55.0
    break_minutes_min: float = 4.0
    break_minutes_max: float = 9.0
    command_retry_seconds: float = 4.0

    def normalized(self) -> "BreakSettings":
        work_min = max(0.25, float(self.work_minutes_min))
        work_max = max(0.25, float(self.work_minutes_max))
        break_min = max(0.25, float(self.break_minutes_min))
        break_max = max(0.25, float(self.break_minutes_max))
        if work_min > work_max:
            work_min, work_max = work_max, work_min
        if break_min > break_max:
            break_min, break_max = break_max, break_min
        retry_seconds = max(0.5, float(self.command_retry_seconds))
        return BreakSettings(
            enabled=bool(self.enabled),
            work_minutes_min=work_min,
            work_minutes_max=work_max,
            break_minutes_min=break_min,
            break_minutes_max=break_max,
            command_retry_seconds=retry_seconds,
        )


class RuntimeRunner:
    """
    Core runtime loop:

      - Reads client.log (once or follow mode).
      - For each snapshot tick, asks strategy for intents.
      - Delegates lifecycle/timing/retries to RuntimeCore.
      - Writes dispatches to command bus.
      - Feeds xptool.execution rows back into RuntimeCore.
    """

    def __init__(
        self,
        strategy: RuntimeStrategy,
        *,
        command_writer: Optional[CommandBusWriter] = None,
        writer: Optional[CommandBusWriter] = None,
        dry_run: bool = False,
        on_runtime: Optional[RuntimeCallback] = None,
        runtime_callback: Optional[RuntimeCallback] = None,
        stop_event: Optional[threading.Event] = None,
        log_snapshots: bool = False,
        log_idle_decisions: bool = False,
        break_settings: Optional[BreakSettings] = None,
        control_plane_client: Optional[ControlPlaneClient] = None,
        control_plane_settings: Optional[ControlPlaneRuntimeSettings] = None,
        control_plane_audit_sink: Optional[ControlPlaneAuditSink] = None,
        remote_planner_client: Optional[RemotePlannerClient] = None,
        remote_planner_settings: Optional[RemotePlannerSettings] = None,
        strategy_name_override: str = "",
        strategy_activity_override: str = "",
    ) -> None:
        self.strategy = strategy
        self.writer = command_writer if command_writer is not None else writer
        self.dry_run = dry_run
        self.runtime_callback = on_runtime if on_runtime is not None else runtime_callback
        self.stop_event = stop_event
        self.log_snapshots = log_snapshots
        self.log_idle_decisions = log_idle_decisions
        self.remote_planner_client = remote_planner_client or NoopRemotePlannerClient()
        self.remote_planner_settings = (
            remote_planner_settings.normalized()
            if remote_planner_settings is not None
            else RemotePlannerSettings(enabled=False).normalized()
        )
        if self.remote_planner_settings.enabled and not self.remote_planner_settings.fallback_to_local:
            self.core = NoopRuntimeCore()
        else:
            from .runtime_core import RuntimeCore

            self.core = RuntimeCore()
        self._last_snapshot_tick: int = -1
        self._last_snapshot_logged_in: Optional[bool] = None
        self.break_settings = (
            break_settings.normalized() if break_settings is not None else BreakSettings().normalized()
        )
        self._break_state: str = "INACTIVE"
        self._break_cycle_id: int = 0
        self._work_window_end_monotonic: float = 0.0
        self._break_window_end_monotonic: float = 0.0
        self._next_break_command_allowed_monotonic: float = 0.0
        self._forced_stop_emitted = False
        self._stop_lock = threading.Lock()
        self._agility_capture_seen_candidates: set[str] = set()
        self._agility_capture_last_no_candidate_tick_by_step: dict[str, int] = {}
        self.control_plane_client = control_plane_client or NoopControlPlaneClient()
        self.control_plane_settings = (
            control_plane_settings.normalized()
            if control_plane_settings is not None
            else ControlPlaneRuntimeSettings(enabled=False).normalized()
        )
        self.control_plane_audit_sink = control_plane_audit_sink
        self._control_plane_session: Optional[ControlPlaneSession] = None
        self._control_plane_policy: ControlPlanePolicySnapshot = ControlPlanePolicySnapshot.default()
        self._next_control_plane_poll_monotonic: float = 0.0
        self._control_plane_kill_switch_emitted: bool = False
        self._strategy_name_override = str(strategy_name_override or "").strip()
        self._strategy_activity_override = str(strategy_activity_override or "").strip().lower()
        self._break_idle_cadence_tuning_payload = self._resolve_break_idle_cadence_tuning_payload()
        self._started_at_monotonic: float = 0.0
        self._last_snapshot_seen_at_monotonic: float = 0.0
        self._next_no_snapshot_warning_at_monotonic: float = 0.0
        self._last_break_no_snapshot_advance_at_monotonic: float = 0.0
        self._break_logout_pending_stall_assumed_logged_out: bool = False

    def _breaks_enabled(self) -> bool:
        return self._break_state != "INACTIVE"

    def _control_plane_enabled(self) -> bool:
        return bool(self.control_plane_settings.enabled)

    def _remote_planner_enabled(self) -> bool:
        return bool(self.remote_planner_settings.enabled)

    def _strategy_name(self) -> str:
        if self._strategy_name_override:
            return self._strategy_name_override
        return self.strategy.__class__.__name__.strip() or "UnknownStrategy"

    def _strategy_activity_key(self) -> str:
        if self._strategy_activity_override:
            return self._strategy_activity_override
        name = self._strategy_name()
        if name.lower().endswith("strategy"):
            name = name[:-8]
        return name.strip().lower() or "unknown"

    def _resolve_break_idle_cadence_tuning_payload(self) -> dict[str, int] | None:
        strategy = self.strategy
        payload = None
        getter = getattr(strategy, "break_idle_cadence_tuning_payload", None)
        if callable(getter):
            try:
                payload = getter()
            except Exception:
                payload = None
        if payload is None:
            payload = getattr(strategy, "_idle_cadence_tuning_payload", None)
        if not isinstance(payload, dict):
            return None
        normalized: dict[str, int] = {}
        for key, value in payload.items():
            try:
                normalized[str(key)] = int(value)
            except (TypeError, ValueError):
                continue
        return normalized or None

    def _augment_break_payload_with_manual_metrics(self, payload: dict[str, Any]) -> dict[str, Any]:
        merged = dict(payload or {})
        tuning = self._break_idle_cadence_tuning_payload
        if isinstance(tuning, dict) and tuning:
            merged["idleCadenceTuning"] = dict(tuning)
        return merged

    def _emit_control_plane_audit(self, event_type: str, **details: Any) -> None:
        sink = self.control_plane_audit_sink
        if sink is None:
            return
        payload = dict(details)
        payload.setdefault("strategy", self._strategy_name())
        payload.setdefault("runnerDryRun", bool(self.dry_run))
        try:
            sink.emit(event_type, payload)
        except Exception as exc:
            self._emit_runtime(f"[WARN] control-plane audit write failed: {exc!r}")

    def _initialize_control_plane(self) -> None:
        if not self._control_plane_enabled():
            return
        writer_path = getattr(self.writer, "path", "") if self.writer is not None else ""
        try:
            self._control_plane_session = self.control_plane_client.start_session(
                runner_id=f"xptool-{uuid.uuid4()}",
                strategy_name=self._strategy_name(),
                writer_path=str(writer_path or ""),
                dry_run=bool(self.dry_run),
            )
            session_id = self._control_plane_session.session_id if self._control_plane_session is not None else ""
            self._emit_runtime(f"[CONTROL] session_started session_id={session_id}")
            self._emit_control_plane_audit(
                "session_started",
                sessionId=session_id,
                writerPath=str(writer_path or ""),
            )
        except Exception as exc:
            self._emit_runtime(f"[WARN] control-plane session start failed: {exc!r}")
            self._emit_control_plane_audit("session_start_failed", error=repr(exc))
            self._control_plane_session = None
            return
        self._next_control_plane_poll_monotonic = 0.0
        self._refresh_control_plane(tick=self._last_snapshot_tick, force=True)

    def _refresh_control_plane(self, *, tick: int, force: bool) -> None:
        if not self._control_plane_enabled():
            return
        if self._control_plane_session is None:
            return
        now = time.monotonic()
        if not force and now < self._next_control_plane_poll_monotonic:
            return
        try:
            policy = self.control_plane_client.refresh_policy(
                session=self._control_plane_session,
                strategy_name=self._strategy_name(),
                tick=int(tick),
            )
            self._control_plane_policy = policy
            self._emit_control_plane_audit(
                "policy_refreshed",
                sessionId=self._control_plane_session.session_id,
                tick=int(tick),
                globalKillSwitch=bool(policy.kill_switch_global),
                disabledActivities=list(policy.disabled_activities),
                featureFlags=dict(policy.feature_flags),
                reason=policy.reason,
            )
        except Exception as exc:
            self._emit_runtime(f"[WARN] control-plane policy refresh failed: {exc!r}")
            self._emit_control_plane_audit(
                "policy_refresh_failed",
                sessionId=self._control_plane_session.session_id,
                tick=int(tick),
                error=repr(exc),
            )
        finally:
            self._next_control_plane_poll_monotonic = (
                now + self.control_plane_settings.poll_interval_seconds
            )

    def _close_control_plane_session(self, *, reason: str) -> None:
        if not self._control_plane_enabled():
            return
        session = self._control_plane_session
        self._control_plane_session = None
        if session is None:
            return
        try:
            self.control_plane_client.close_session(session=session, reason=reason)
            self._emit_control_plane_audit(
                "session_closed",
                sessionId=session.session_id,
                reason=reason,
            )
        except Exception as exc:
            self._emit_runtime(f"[WARN] control-plane session close failed: {exc!r}")
            self._emit_control_plane_audit(
                "session_close_failed",
                sessionId=session.session_id,
                reason=reason,
                error=repr(exc),
            )

    def _roll_minutes(self, minimum: float, maximum: float) -> float:
        low = min(float(minimum), float(maximum))
        high = max(float(minimum), float(maximum))
        if high <= low:
            return low
        return random.uniform(low, high)

    def _schedule_next_work_window(self, *, now: float, initial: bool) -> None:
        minutes = self._roll_minutes(
            self.break_settings.work_minutes_min,
            self.break_settings.work_minutes_max,
        )
        self._work_window_end_monotonic = now + (minutes * 60.0)
        window_label = "initial_work_window" if initial else "next_work_window"
        self._emit_runtime(f"[BREAK] {window_label}_minutes={minutes:.2f}")

    def _start_break_window(self, *, now: float) -> None:
        minutes = self._roll_minutes(
            self.break_settings.break_minutes_min,
            self.break_settings.break_minutes_max,
        )
        self._break_window_end_monotonic = now + (minutes * 60.0)
        self._emit_runtime(
            f"[BREAK] cycle={self._break_cycle_id} break_started_minutes={minutes:.2f}"
        )

    def _initialize_break_scheduler(self) -> None:
        settings = self.break_settings
        if not settings.enabled:
            self._break_state = "INACTIVE"
            return
        if self.writer is None and not self.dry_run:
            self._break_state = "INACTIVE"
            self._emit_runtime(
                "[WARN] breaks enabled but no writer is configured; break system disabled"
            )
            return
        self._break_state = "ACTIVE"
        self._break_cycle_id = 0
        self._next_break_command_allowed_monotonic = 0.0
        self._schedule_next_work_window(now=time.monotonic(), initial=True)
        self._emit_runtime(
            "[BREAK] enabled "
            f"work={settings.work_minutes_min:.2f}-{settings.work_minutes_max:.2f}m "
            f"break={settings.break_minutes_min:.2f}-{settings.break_minutes_max:.2f}m"
        )

    def _emit_break_command(
        self,
        *,
        command_type: str,
        payload: dict,
        reason: str,
        tick: int,
    ) -> bool:
        if not is_supported_command_type(command_type):
            self._emit_runtime(
                f"[ERROR] unsupported break command type rejected: {command_type}"
            )
            self._emit_control_plane_audit(
                "break_command_rejected_unsupported",
                commandType=command_type,
                tick=int(tick),
                reason=reason,
            )
            return False
        cmd = RuntimeCommand(
            command_type=command_type,
            payload=self._augment_break_payload_with_manual_metrics(payload),
            reason=reason,
            tick=int(tick),
            source="xptool.breaks",
        )
        if self.dry_run or self.writer is None:
            self._emit_runtime(
                f"[BREAK][DRY] {command_type} tick={tick} reason={reason} payload={payload}"
            )
            self._emit_control_plane_audit(
                "break_command_dry_run",
                commandType=command_type,
                tick=int(tick),
                reason=reason,
            )
            return True
        try:
            command_id = self.writer.write_command(cmd)
            self._emit_runtime(f"[BREAK][SENT] {command_type} tick={tick} reason={reason}")
            self._emit_runtime(
                f"[BREAK][EMIT] command_id={command_id} type={command_type} tick={tick}"
            )
            self._emit_control_plane_audit(
                "break_command_sent",
                commandId=command_id,
                commandType=command_type,
                tick=int(tick),
                reason=reason,
            )
            return True
        except Exception as exc:
            self._emit_runtime(
                f"[ERROR] failed to write break command {command_type}: {exc!r}"
            )
            self._emit_control_plane_audit(
                "break_command_failed",
                commandType=command_type,
                tick=int(tick),
                reason=reason,
                error=repr(exc),
            )
            return False

    def _handle_break_cycle(self, snapshot: Snapshot) -> bool:
        if not self._breaks_enabled():
            return False

        now = time.monotonic()
        tick = int(snapshot.tick)

        if self._break_state == "ACTIVE" and now >= self._work_window_end_monotonic:
            self._break_cycle_id += 1
            self._break_state = "LOGOUT_PENDING"
            self._next_break_command_allowed_monotonic = 0.0
            self._break_logout_pending_stall_assumed_logged_out = False
            self._emit_runtime(f"[BREAK] cycle={self._break_cycle_id} work_window_complete")

        if self._break_state == "LOGOUT_PENDING":
            if not bool(snapshot.logged_in):
                self._start_break_window(now=now)
                self._break_state = "BREAKING"
                self._break_logout_pending_stall_assumed_logged_out = False
                return True
            if now >= self._next_break_command_allowed_monotonic:
                sent = self._emit_break_command(
                    command_type="LOGOUT_SAFE",
                    payload={"plannerTag": "break_scheduler"},
                    reason="break_cycle_logout",
                    tick=tick,
                )
                if sent:
                    self._emit_runtime(
                        f"[BREAK] cycle={self._break_cycle_id} logout_command_sent"
                    )
                self._next_break_command_allowed_monotonic = (
                    now + self.break_settings.command_retry_seconds
                )
            return True

        if self._break_state == "BREAKING":
            if now < self._break_window_end_monotonic:
                return True
            self._break_state = "LOGIN_PENDING"
            self._next_break_command_allowed_monotonic = 0.0
            self._emit_runtime(f"[BREAK] cycle={self._break_cycle_id} break_window_complete")

        if self._break_state == "LOGIN_PENDING":
            if bool(snapshot.logged_in):
                self._break_state = "ACTIVE"
                self._next_break_command_allowed_monotonic = 0.0
                self._break_logout_pending_stall_assumed_logged_out = False
                self._schedule_next_work_window(now=now, initial=False)
                self._emit_runtime(f"[BREAK] cycle={self._break_cycle_id} login_confirmed")
                return True
            if now >= self._next_break_command_allowed_monotonic:
                sent = self._emit_break_command(
                    command_type="LOGIN_START_TEST",
                    payload={
                        "prefilled": True,
                        "plannerTag": "break_scheduler",
                    },
                    reason="break_cycle_login",
                    tick=tick,
                )
                if sent:
                    self._emit_runtime(
                        f"[BREAK] cycle={self._break_cycle_id} login_command_sent"
                    )
                self._next_break_command_allowed_monotonic = (
                    now + self.break_settings.command_retry_seconds
                )
            return True

        return False

    def _emit_runtime(self, message: str) -> None:
        text = message.rstrip("\n")
        if self.runtime_callback is not None:
            self.runtime_callback(text)
        else:
            print(text)

    @staticmethod
    def _int_or_default(value: Any, default: int = -1) -> int:
        try:
            return int(value)
        except (TypeError, ValueError):
            return default

    @staticmethod
    def _extract_step_label(intent_key: str) -> str:
        key = str(intent_key or "")
        marker = ":STEP_"
        idx = key.rfind(marker)
        if idx < 0:
            return "unknown"
        return key[idx + 1 :]

    def _emit_agility_obstacle_capture(self, snapshot: Snapshot, intents: Sequence[Any]) -> None:
        raw = snapshot.raw if isinstance(snapshot.raw, dict) else {}
        nearby_objects = raw.get("nearbyObjects")
        if not isinstance(nearby_objects, list) or not nearby_objects:
            return

        player = raw.get("player")
        player_x = -1
        player_y = -1
        player_plane = -1
        if isinstance(player, dict):
            player_x = self._int_or_default(player.get("worldX"), -1)
            player_y = self._int_or_default(player.get("worldY"), -1)
            player_plane = self._int_or_default(player.get("plane"), -1)

        tick = int(snapshot.tick)
        for intent in intents:
            activity = str(getattr(intent, "activity", "") or "").strip().lower()
            if activity != "agility":
                continue
            kind = getattr(getattr(intent, "kind", None), "value", "")
            if str(kind) != "SCENE_OBJECT_ACTION":
                continue

            params = getattr(intent, "params", None)
            if not isinstance(params, dict):
                continue

            min_x = self._int_or_default(params.get("minWorldX"), -1)
            max_x = self._int_or_default(params.get("maxWorldX"), -1)
            min_y = self._int_or_default(params.get("minWorldY"), -1)
            max_y = self._int_or_default(params.get("maxWorldY"), -1)
            target_plane = self._int_or_default(params.get("targetPlane"), -1)
            target_object_id = self._int_or_default(params.get("targetObjectId"), -1)
            target_name_filter = str(params.get("targetObjectNameContains", "") or "").strip().lower()
            option_keywords = params.get("optionKeywords")
            if not isinstance(option_keywords, list):
                option_keywords = []
            option_keywords_text = ",".join(
                str(x).strip() for x in option_keywords if str(x).strip()
            )

            step_label = self._extract_step_label(str(getattr(intent, "intent_key", "") or ""))
            if min_x <= 0 or max_x <= 0 or min_y <= 0 or max_y <= 0:
                continue

            candidates: list[dict[str, Any]] = []
            for row in nearby_objects:
                if not isinstance(row, dict):
                    continue
                obj_x = self._int_or_default(row.get("worldX"), -1)
                obj_y = self._int_or_default(row.get("worldY"), -1)
                obj_plane = self._int_or_default(row.get("plane"), -1)
                if obj_x < min_x or obj_x > max_x or obj_y < min_y or obj_y > max_y:
                    continue
                if target_plane >= 0 and obj_plane != target_plane:
                    continue
                obj_id = self._int_or_default(row.get("id"), -1)
                if target_object_id > 0 and obj_id != target_object_id:
                    continue
                obj_name = str(row.get("name", "") or "").strip().lower()
                if target_name_filter and target_name_filter not in obj_name:
                    continue
                if player_x > 0 and player_y > 0 and player_plane == obj_plane:
                    dist = max(abs(obj_x - player_x), abs(obj_y - player_y))
                else:
                    dist = -1
                candidates.append(
                    {
                        "id": obj_id,
                        "name": obj_name,
                        "x": obj_x,
                        "y": obj_y,
                        "plane": obj_plane,
                        "dist": dist,
                    }
                )

            candidates.sort(
                key=lambda c: (
                    c["dist"] if c["dist"] >= 0 else 9999,
                    c["x"],
                    c["y"],
                    c["id"],
                )
            )

            if not candidates:
                last_no_candidate_tick = self._agility_capture_last_no_candidate_tick_by_step.get(
                    step_label,
                    -9999,
                )
                if (tick - last_no_candidate_tick) >= 12:
                    self._agility_capture_last_no_candidate_tick_by_step[step_label] = tick
                    self._emit_runtime(
                        "[AGILITY_CAPTURE] "
                        f"tick={tick} step={step_label} candidates=0 "
                        f"filterId={target_object_id} filterName={target_name_filter!r} "
                        f"bounds=({min_x}..{max_x},{min_y}..{max_y},p={target_plane}) "
                        f"options={option_keywords_text!r}"
                    )
                continue

            for candidate in candidates[:6]:
                signature = (
                    f"{step_label}:{candidate['id']}:{candidate['x']}:{candidate['y']}:"
                    f"{candidate['plane']}:{target_object_id}:{target_name_filter}"
                )
                if signature in self._agility_capture_seen_candidates:
                    continue
                self._agility_capture_seen_candidates.add(signature)
                self._emit_runtime(
                    "[AGILITY_CAPTURE] "
                    f"tick={tick} step={step_label} "
                    f"candidateId={candidate['id']} candidateName={candidate['name']!r} "
                    f"world=({candidate['x']},{candidate['y']},{candidate['plane']}) "
                    f"dist={candidate['dist']} "
                    f"filterId={target_object_id} filterName={target_name_filter!r} "
                    f"bounds=({min_x}..{max_x},{min_y}..{max_y},p={target_plane}) "
                    f"options={option_keywords_text!r}"
                )

    def _process_remote_decision(
        self,
        *,
        snapshot: Snapshot,
        strategy_activity: str,
    ) -> tuple[str, int]:
        if not self._remote_planner_enabled():
            return ("disabled", 0)
        session_id = ""
        if self._control_plane_session is not None:
            session_id = str(self._control_plane_session.session_id or "")
        try:
            decision = self.remote_planner_client.decide(
                snapshot=snapshot,
                strategy_activity=strategy_activity,
                strategy_name=self._strategy_name(),
                session_id=session_id,
            )
        except Exception as exc:
            self._emit_runtime(f"[WARN] remote planner decision failed: {exc!r}")
            self._emit_control_plane_audit(
                "remote_decision_failed",
                tick=int(snapshot.tick),
                activity=strategy_activity,
                error=repr(exc),
            )
            if self.remote_planner_settings.fallback_to_local:
                self._emit_control_plane_audit(
                    "remote_decision_fallback_local",
                    tick=int(snapshot.tick),
                    activity=strategy_activity,
                )
                return ("fallback_local", 0)
            return ("blocked", 0)

        status = str(decision.status or "").strip().lower()
        if status not in {"ok", "no_action", "reject", "error"}:
            status = "no_action"
        command_specs = list(decision.commands)
        self._emit_control_plane_audit(
            "remote_decision_received",
            tick=int(snapshot.tick),
            activity=strategy_activity,
            status=status,
            decisionId=decision.decision_id,
            commandCount=len(command_specs),
            reason=decision.reason,
        )
        if status != "ok" or not command_specs:
            return ("no_action", 0)

        emitted = 0
        for spec in command_specs:
            command_type = str(spec.command_type or "").strip().upper()
            try:
                payload = dict(spec.payload)
            except Exception:
                payload = {}
            try:
                envelope = dict(spec.envelope)
            except Exception:
                envelope = {}
            if envelope:
                payload["commandEnvelope"] = envelope
            reason = str(spec.reason or decision.reason or "remote_planner_decision")
            source = str(spec.source or "xptool.remote")

            if not is_supported_command_type(command_type):
                self._emit_runtime(
                    f"[ERROR] unsupported remote command type rejected: {command_type}"
                )
                self._emit_control_plane_audit(
                    "remote_dispatch_rejected_unsupported",
                    tick=int(snapshot.tick),
                    commandType=command_type,
                    decisionId=decision.decision_id,
                    reason=reason,
                )
                continue

            cmd = RuntimeCommand(
                command_type=command_type,
                payload=payload,
                reason=reason,
                tick=int(snapshot.tick),
                source=source,
            )
            if self.dry_run or self.writer is None:
                command_id = f"dry-{uuid.uuid4()}"
                emitted += 1
                self._emit_runtime(
                    f"[DRY][REMOTE] {cmd.command_type} tick={cmd.tick} payload={cmd.payload} reason={cmd.reason}"
                )
                self._emit_control_plane_audit(
                    "remote_dispatch_dry_run",
                    tick=int(snapshot.tick),
                    commandId=command_id,
                    commandType=cmd.command_type,
                    decisionId=decision.decision_id,
                    reason=cmd.reason or "",
                )
                continue

            try:
                command_id = self.writer.write_command(cmd)
                emitted += 1
                self._emit_runtime(
                    f"[SENT][REMOTE] {cmd.command_type} tick={cmd.tick} reason={cmd.reason}"
                )
                self._emit_runtime(
                    "[EMIT][REMOTE] "
                    f"command_id={command_id} "
                    f"type={cmd.command_type} "
                    f"tick={cmd.tick}"
                )
                self._emit_control_plane_audit(
                    "remote_dispatch_sent",
                    tick=int(snapshot.tick),
                    commandId=command_id,
                    commandType=cmd.command_type,
                    decisionId=decision.decision_id,
                    reason=cmd.reason or "",
                )
            except Exception as exc:
                self._emit_runtime(
                    f"[ERROR] failed to write remote command {cmd.command_type}: {exc!r}"
                )
                self._emit_control_plane_audit(
                    "remote_dispatch_failed",
                    tick=int(snapshot.tick),
                    commandType=cmd.command_type,
                    decisionId=decision.decision_id,
                    error=repr(exc),
                )
        if emitted > 0:
            return ("dispatch", emitted)
        return ("no_action", 0)

    def _remote_planner_startup_precheck(self) -> bool:
        if not self._remote_planner_enabled():
            return True
        if not self.remote_planner_settings.require_startup_precheck:
            return True
        session_id = ""
        if self._control_plane_session is not None:
            session_id = str(self._control_plane_session.session_id or "")
        probe_snapshot = Snapshot(
            tick=-1,
            logged_in=False,
            bank_open=False,
            shop_open=False,
            world_id=None,
            inventory_counts={},
            bank_counts={},
            shop_counts={},
            inventory_slots_used=0,
            player_animation=0,
            raw={},
        )
        strategy_activity = self._strategy_activity_key()
        try:
            decision = self.remote_planner_client.decide(
                snapshot=probe_snapshot,
                strategy_activity=strategy_activity,
                strategy_name=self._strategy_name(),
                session_id=session_id,
            )
        except Exception as exc:
            self._emit_runtime(
                f"[ERROR] remote planner startup precheck failed: {exc!r}"
            )
            self._emit_control_plane_audit(
                "remote_startup_precheck_failed",
                activity=strategy_activity,
                error=repr(exc),
            )
            return False

        self._emit_control_plane_audit(
            "remote_startup_precheck_passed",
            activity=strategy_activity,
            status=str(decision.status or ""),
            decisionId=str(decision.decision_id or ""),
            commandCount=len(decision.commands),
        )
        self._emit_runtime(
            "[INFO] remote planner startup precheck passed "
            f"status={decision.status} decision_id={decision.decision_id or '-'}"
        )
        return True

    def _process_snapshot(self, snapshot: Snapshot) -> None:
        decision_started = time.perf_counter()
        decision_outcome = "unknown"
        intents_count = 0
        dispatch_count = 0
        self._last_snapshot_tick = int(snapshot.tick)
        self._last_snapshot_logged_in = bool(snapshot.logged_in)
        self._refresh_control_plane(tick=int(snapshot.tick), force=False)
        try:
            if self.log_snapshots:
                self._emit_runtime(
                    "[SNAPSHOT] "
                    f"tick={snapshot.tick} "
                    f"logged_in={snapshot.logged_in} "
                    f"bank_open={snapshot.bank_open} "
                    f"animation={snapshot.player_animation}"
                )

            if self._control_plane_policy.kill_switch_global:
                decision_outcome = "kill_switch"
                if not self._control_plane_kill_switch_emitted:
                    self._control_plane_kill_switch_emitted = True
                    self._emit_runtime("[CONTROL] global_kill_switch_active; forcing runtime stop")
                    self._emit_control_plane_audit(
                        "global_kill_switch_triggered",
                        tick=int(snapshot.tick),
                        sessionId=self._control_plane_session.session_id
                        if self._control_plane_session is not None
                        else "",
                    )
                    self._emit_forced_drop_stop()
                if self.log_idle_decisions:
                    self._emit_runtime(f"[CORE] tick={snapshot.tick} intents=0 dispatches=0 (kill_switch)")
                return

            self._control_plane_kill_switch_emitted = False
            if not self._control_plane_policy.feature_enabled("break_scheduler_enabled", True):
                if self._break_state != "INACTIVE":
                    self._emit_runtime("[CONTROL] break scheduler disabled by control-plane policy")
                    self._emit_control_plane_audit(
                        "break_scheduler_disabled_by_policy",
                        tick=int(snapshot.tick),
                    )
                self._break_state = "INACTIVE"

            strategy_activity = self._strategy_activity_key()
            if not self._control_plane_policy.is_activity_enabled(strategy_activity):
                decision_outcome = "strategy_activity_disabled"
                if self.log_idle_decisions:
                    self._emit_runtime(
                        f"[CORE] tick={snapshot.tick} intents=0 dispatches=0 (activity_disabled:{strategy_activity})"
                    )
                self._emit_control_plane_audit(
                    "strategy_activity_disabled",
                    tick=int(snapshot.tick),
                    activity=strategy_activity,
                )
                return

            if self._handle_break_cycle(snapshot):
                decision_outcome = "break_cycle"
                if self.log_idle_decisions:
                    self._emit_runtime(f"[CORE] tick={snapshot.tick} intents=0 dispatches=0 (break)")
                return

            remote_outcome, remote_dispatches = self._process_remote_decision(
                snapshot=snapshot,
                strategy_activity=strategy_activity,
            )
            if remote_outcome == "dispatch":
                decision_outcome = "remote_dispatch"
                dispatch_count = int(remote_dispatches)
                if self.log_idle_decisions:
                    self._emit_runtime(
                        f"[CORE] tick={snapshot.tick} intents=0 dispatches={dispatch_count} (remote)"
                    )
                return
            if remote_outcome == "no_action":
                decision_outcome = "remote_no_action"
                if self.log_idle_decisions:
                    self._emit_runtime(
                        f"[CORE] tick={snapshot.tick} intents=0 dispatches=0 (remote_no_action)"
                    )
                return
            if remote_outcome == "blocked":
                decision_outcome = "remote_blocked"
                if self.log_idle_decisions:
                    self._emit_runtime(
                        f"[CORE] tick={snapshot.tick} intents=0 dispatches=0 (remote_blocked)"
                    )
                return

            intents = list(self.strategy.intents(snapshot))
            consume_runtime_warnings = getattr(self.strategy, "consume_runtime_warnings", None)
            if callable(consume_runtime_warnings):
                try:
                    pending_warnings = consume_runtime_warnings()
                except Exception as exc:
                    self._emit_runtime(f"[WARN] failed to consume strategy runtime warnings: {exc!r}")
                    pending_warnings = ()
                for warning in pending_warnings:
                    warning_text = str(warning or "").strip()
                    if warning_text:
                        self._emit_runtime(f"[WARN] {warning_text}")
            intents_count = len(intents)
            if intents:
                filtered_intents = []
                dropped = 0
                for intent in intents:
                    intent_activity = str(getattr(intent, "activity", "") or "").strip().lower()
                    if self._control_plane_policy.is_activity_enabled(intent_activity):
                        filtered_intents.append(intent)
                        continue
                    dropped += 1
                if dropped > 0:
                    self._emit_control_plane_audit(
                        "intents_dropped_by_policy",
                        tick=int(snapshot.tick),
                        dropped=dropped,
                    )
                intents = filtered_intents
                intents_count = len(intents)
            self._emit_agility_obstacle_capture(snapshot, intents)
            dispatches = self.core.on_snapshot(snapshot, intents)
            dispatch_count = len(dispatches)
            if not dispatches:
                decision_outcome = "no_dispatch"
                if self.log_idle_decisions:
                    self._emit_runtime(
                        f"[CORE] tick={snapshot.tick} intents={len(intents)} dispatches=0"
                    )
                return

            decision_outcome = "dispatch"
            for dispatch in dispatches:
                cmd = dispatch.command
                if not is_supported_command_type(cmd.command_type):
                    self._emit_runtime(
                        f"[ERROR] unsupported command type rejected: {cmd.command_type}"
                    )
                    self._emit_control_plane_audit(
                        "dispatch_rejected_unsupported",
                        ticketId=dispatch.ticket_id,
                        commandType=cmd.command_type,
                        tick=int(snapshot.tick),
                        reason=cmd.reason or "",
                    )
                    self.core.on_dispatch_failed(
                        ticket_id=dispatch.ticket_id,
                        tick=int(snapshot.tick),
                        reason=f"unsupported_command_type:{cmd.command_type}",
                    )
                    continue
                if self.dry_run or self.writer is None:
                    command_id = f"dry-{uuid.uuid4()}"
                    self._emit_runtime(
                        f"[DRY] {cmd.command_type} tick={cmd.tick} payload={cmd.payload} reason={cmd.reason}"
                    )
                    self._emit_control_plane_audit(
                        "dispatch_dry_run",
                        ticketId=dispatch.ticket_id,
                        commandId=command_id,
                        commandType=cmd.command_type,
                        tick=int(snapshot.tick),
                        reason=cmd.reason or "",
                    )
                    self.core.on_dispatch_enqueued(
                        ticket_id=dispatch.ticket_id,
                        command_id=command_id,
                        tick=int(snapshot.tick),
                    )
                    continue

                try:
                    command_id = self.writer.write_command(cmd)
                    self._emit_runtime(
                        f"[SENT] {cmd.command_type} tick={cmd.tick} reason={cmd.reason}"
                    )
                    self._emit_runtime(
                        "[EMIT] "
                        f"command_id={command_id} "
                        f"type={cmd.command_type} "
                        f"tick={cmd.tick}"
                    )
                    self._emit_control_plane_audit(
                        "dispatch_sent",
                        ticketId=dispatch.ticket_id,
                        commandId=command_id,
                        commandType=cmd.command_type,
                        tick=int(snapshot.tick),
                        reason=cmd.reason or "",
                    )
                    self.core.on_dispatch_enqueued(
                        ticket_id=dispatch.ticket_id,
                        command_id=command_id,
                        tick=int(snapshot.tick),
                    )
                except Exception as exc:  # defensive
                    self._emit_runtime(
                        f"[ERROR] failed to write command {cmd.command_type}: {exc!r}"
                    )
                    self._emit_control_plane_audit(
                        "dispatch_failed",
                        ticketId=dispatch.ticket_id,
                        commandType=cmd.command_type,
                        tick=int(snapshot.tick),
                        error=repr(exc),
                    )
                    self.core.on_dispatch_failed(
                        ticket_id=dispatch.ticket_id,
                        tick=int(snapshot.tick),
                        reason=f"writer_error:{exc!r}",
                    )
        finally:
            latency_ms = max(0, int(round((time.perf_counter() - decision_started) * 1000.0)))
            self._emit_control_plane_audit(
                "decision_latency_sample",
                tick=int(snapshot.tick),
                latencyMs=latency_ms,
                intentsCount=int(intents_count),
                dispatchCount=int(dispatch_count),
                outcome=decision_outcome,
            )

    def _emit_forced_drop_stop(self) -> None:
        with self._stop_lock:
            if self._forced_stop_emitted:
                return
            self._forced_stop_emitted = True
        tick = self._last_snapshot_tick if self._last_snapshot_tick >= 0 else -1
        if self.dry_run or self.writer is None:
            self._emit_runtime("[INFO] stop requested; forced runtime stop skipped (no writer)")
            self._emit_control_plane_audit(
                "forced_stop_skipped",
                reason="no_writer_or_dry_run",
                tick=int(tick),
            )
            return
        source = getattr(self.writer, "source", "xptool.planner")
        forced_commands = (
            ("STOP_ALL_RUNTIME", "runner_stop_force_all_runtime"),
            ("DROP_STOP_SESSION", "runner_stop_force_drop_session"),
        )
        for command_type, reason in forced_commands:
            if not is_supported_command_type(command_type):
                self._emit_runtime(
                    f"[WARN] forced stop command type unsupported and skipped: {command_type}"
                )
                self._emit_control_plane_audit(
                    "forced_stop_command_rejected_unsupported",
                    commandType=command_type,
                    tick=int(tick),
                    reason=reason,
                )
                continue
            stop_cmd = RuntimeCommand(
                command_type=command_type,
                payload={},
                reason=reason,
                tick=tick,
                source=source,
            )
            try:
                command_id = self.writer.write_command(stop_cmd)
                self._emit_runtime(
                    f"[SENT] {stop_cmd.command_type} tick={stop_cmd.tick} reason={stop_cmd.reason}"
                )
                self._emit_runtime(
                    "[EMIT] "
                    f"command_id={command_id} "
                    f"type={stop_cmd.command_type} "
                    f"tick={stop_cmd.tick}"
                )
                self._emit_control_plane_audit(
                    "forced_stop_command_sent",
                    commandId=command_id,
                    commandType=stop_cmd.command_type,
                    tick=int(stop_cmd.tick),
                    reason=stop_cmd.reason or "",
                )
            except Exception as exc:
                self._emit_runtime(
                    f"[WARN] failed to emit forced stop command {command_type}: {exc!r}"
                )
                self._emit_control_plane_audit(
                    "forced_stop_command_failed",
                    commandType=command_type,
                    tick=int(stop_cmd.tick),
                    error=repr(exc),
                )

    def request_stop(self, *, source: str = "external") -> None:
        if self.stop_event is not None:
            self.stop_event.set()
        self._break_state = "INACTIVE"
        self._break_window_end_monotonic = 0.0
        self._work_window_end_monotonic = 0.0
        self._next_break_command_allowed_monotonic = 0.0
        self._emit_runtime(f"[INFO] stop requested ({source})")
        self._emit_control_plane_audit(
            "stop_requested",
            source=source,
            tick=int(self._last_snapshot_tick),
        )
        self._emit_forced_drop_stop()

    def process_line(self, line: str) -> None:
        snapshot = parse_snapshot_line(line)
        if snapshot is not None:
            self._last_snapshot_seen_at_monotonic = time.monotonic()
            self._process_snapshot(snapshot)
            return
        execution_row = parse_execution_line(line)
        if execution_row is not None:
            self._update_break_login_state_from_execution_row(execution_row)
            self.core.on_executor_row(execution_row)
            if self.log_idle_decisions:
                status = execution_row.get("status")
                reason = execution_row.get("reason")
                self._emit_runtime(f"[EXEC] status={status} reason={reason}")

    def _update_break_login_state_from_execution_row(self, execution_row: dict[str, Any]) -> None:
        if not isinstance(execution_row, dict):
            return
        event_type = str(execution_row.get("eventType") or "").strip().upper()
        reason = str(execution_row.get("reason") or "").strip().lower()
        command_type = str(execution_row.get("commandType") or "").strip().upper()
        details_raw = execution_row.get("details")
        details = details_raw if isinstance(details_raw, dict) else {}
        game_state = str(details.get("gameState") or details.get("game_state") or "").strip().upper()
        has_game_state = bool(game_state)
        is_logged_in_state = game_state in {"LOGGED_IN", "LOGGING_IN"}
        is_logged_out_state = has_game_state and not is_logged_in_state

        if event_type == "LOGOUT" and reason in {
            "logout_success",
            "logout_already_logged_out",
            "logout_noop_not_logged_in",
        }:
            self._last_snapshot_logged_in = False
            return
        if event_type == "LOGOUT" and is_logged_out_state:
            self._last_snapshot_logged_in = False
            return
        if event_type == "LOGIN" and reason in {
            "login_success",
            "login_already_logged_in",
        }:
            self._last_snapshot_logged_in = True
            return
        if event_type == "LOGIN" and is_logged_in_state:
            self._last_snapshot_logged_in = True
            return

        if command_type == "LOGOUT_SAFE":
            if reason in {
                "logout_safe_complete",
                "logout_safe_ignored_not_logged_in",
            }:
                self._last_snapshot_logged_in = False
                return
            if is_logged_out_state:
                self._last_snapshot_logged_in = False
                return

        if command_type == "LOGIN_START_TEST":
            if reason == "login_start_test_ignored_logged_in":
                self._last_snapshot_logged_in = True
                return
            if is_logged_in_state:
                self._last_snapshot_logged_in = True

    def _maybe_advance_break_cycle_without_snapshot(self) -> None:
        if not self._breaks_enabled():
            return
        if self._last_snapshot_tick < 0:
            return
        if self._last_snapshot_logged_in is None:
            return
        now = time.monotonic()
        if (
            self._break_state == "LOGOUT_PENDING"
            and bool(self._last_snapshot_logged_in)
            and self._last_snapshot_seen_at_monotonic > 0.0
            and (now - self._last_snapshot_seen_at_monotonic)
            >= BREAK_LOGOUT_PENDING_NO_SNAPSHOT_ASSUME_LOGGED_OUT_SECONDS
        ):
            self._last_snapshot_logged_in = False
            if not self._break_logout_pending_stall_assumed_logged_out:
                self._break_logout_pending_stall_assumed_logged_out = True
                no_snapshot_for_s = now - self._last_snapshot_seen_at_monotonic
                self._emit_runtime(
                    "[BREAK][WARN] "
                    f"cycle={self._break_cycle_id} "
                    "logout_pending_without_fresh_snapshot; "
                    f"assuming_logged_out_after={no_snapshot_for_s:.2f}s"
                )
        if (now - self._last_break_no_snapshot_advance_at_monotonic) < BREAK_NO_SNAPSHOT_ADVANCE_MIN_SECONDS:
            return
        synthetic_snapshot = Snapshot(
            tick=int(self._last_snapshot_tick),
            logged_in=bool(self._last_snapshot_logged_in),
            bank_open=False,
            shop_open=False,
        )
        self._last_break_no_snapshot_advance_at_monotonic = now
        self._handle_break_cycle(synthetic_snapshot)

    def _maybe_emit_no_snapshot_warning(self, *, log_path: str, follow: bool) -> None:
        if not follow:
            return
        now = time.monotonic()
        if self._started_at_monotonic <= 0.0:
            self._started_at_monotonic = now
        if self._last_snapshot_seen_at_monotonic > 0.0:
            return
        elapsed = now - self._started_at_monotonic
        if elapsed < NO_SNAPSHOT_WARNING_INITIAL_SECONDS:
            return
        if now < self._next_no_snapshot_warning_at_monotonic:
            return
        self._next_no_snapshot_warning_at_monotonic = now + NO_SNAPSHOT_WARNING_REPEAT_SECONDS
        self._emit_runtime(
            "[WARN] no xptool.snapshot lines observed yet "
            f"(elapsed={elapsed:.1f}s log={log_path!r} follow={follow}); "
            "loop cannot start until snapshots arrive. "
            "Check that backend log path points to RuneLite client.log, bridge runtime is active, and you are logged in."
        )

    def _emit_stabilization_telemetry(self) -> None:
        telemetry = self.core.telemetry_snapshot()
        self._emit_runtime("[TELEM] stabilization summary begin")

        outcomes = telemetry.get("outcomes_by_activity", {})
        if isinstance(outcomes, dict) and outcomes:
            for activity in sorted(outcomes):
                counts = outcomes.get(activity) or {}
                dispatched = int(counts.get("DISPATCHED", 0))
                deferred = int(counts.get("DEFERRED", 0))
                failed = int(counts.get("FAILED", 0))
                self._emit_runtime(
                    f"[TELEM] outcomes activity={activity} "
                    f"dispatched={dispatched} deferred={deferred} failed={failed}"
                )
        else:
            self._emit_runtime("[TELEM] outcomes none")

        deferred_reasons = telemetry.get("deferred_reasons_top", [])
        if isinstance(deferred_reasons, list) and deferred_reasons:
            for row in deferred_reasons[:5]:
                reason = str(row.get("reason", ""))
                count = int(row.get("count", 0))
                self._emit_runtime(f"[TELEM] deferred_top reason={reason} count={count}")
        else:
            self._emit_runtime("[TELEM] deferred_top none")

        dispatch_latency = telemetry.get("dispatch_to_effect_avg_ticks_by_activity", {})
        if isinstance(dispatch_latency, dict) and dispatch_latency:
            for activity in sorted(dispatch_latency):
                row = dispatch_latency.get(activity) or {}
                avg_ticks = float(row.get("avg_ticks", 0.0))
                samples = int(row.get("samples", 0))
                self._emit_runtime(
                    f"[TELEM] dispatch_to_effect activity={activity} "
                    f"avg_ticks={avg_ticks:.2f} samples={samples}"
                )
        else:
            self._emit_runtime("[TELEM] dispatch_to_effect none")

        retry_total = int(telemetry.get("retry_transition_total", 0))
        retry_non_scheduler_total = int(telemetry.get("retry_transition_non_scheduler_total", 0))
        self._emit_runtime(
            f"[TELEM] retry_transitions total={retry_total} "
            f"non_scheduler={retry_non_scheduler_total}"
        )
        retry_reasons = telemetry.get("retry_transition_reasons_top", [])
        if isinstance(retry_reasons, list):
            for row in retry_reasons[:5]:
                reason = str(row.get("reason", ""))
                count = int(row.get("count", 0))
                self._emit_runtime(f"[TELEM] retry_reason reason={reason} count={count}")

        behavioral_timing = telemetry.get("behavioral_timing_deferred_reasons_top", [])
        if isinstance(behavioral_timing, list) and behavioral_timing:
            for row in behavioral_timing[:5]:
                reason = str(row.get("reason", ""))
                count = int(row.get("count", 0))
                self._emit_runtime(f"[TELEM] behavioral_timing_deferred reason={reason} count={count}")
        else:
            self._emit_runtime("[TELEM] behavioral_timing_deferred none")
        self._emit_runtime("[TELEM] stabilization summary end")

    def run(self, *, log_path: str, follow: bool) -> int:
        self._started_at_monotonic = time.monotonic()
        self._last_snapshot_seen_at_monotonic = 0.0
        self._next_no_snapshot_warning_at_monotonic = 0.0
        self._last_break_no_snapshot_advance_at_monotonic = 0.0
        self._last_snapshot_logged_in = None
        self._emit_runtime(
            f"[INFO] strategy={self._strategy_name()}"
        )
        self._emit_runtime(f"[INFO] log_path={log_path} follow={follow}")
        writer_path = getattr(self.writer, "path", None) if self.writer is not None else None
        writer_name = self.writer.__class__.__name__ if self.writer is not None else "None"
        self._emit_runtime(
            "[INFO] writer="
            f"{writer_name} "
            f"path={writer_path if writer_path else '-'} "
            f"dry_run={self.dry_run}"
        )
        self._emit_runtime(
            "[INFO] control_plane="
            f"{self.control_plane_client.__class__.__name__} "
            f"enabled={self._control_plane_enabled()} "
            f"poll_s={self.control_plane_settings.poll_interval_seconds:.2f}"
        )
        self._emit_runtime(
            "[INFO] remote_planner="
            f"{self.remote_planner_client.__class__.__name__} "
            f"enabled={self._remote_planner_enabled()} "
            f"fallback_local={self.remote_planner_settings.fallback_to_local} "
            f"startup_precheck={self.remote_planner_settings.require_startup_precheck} "
            f"timeout_s={self.remote_planner_settings.timeout_seconds:.2f}"
        )
        self._initialize_control_plane()
        if not self._remote_planner_startup_precheck():
            self._close_control_plane_session(reason="runner_remote_startup_precheck_failed")
            return 2
        self._initialize_break_scheduler()
        if not self.dry_run and self.writer is None:
            self._emit_runtime(
                "[WARN] no command writer configured; commands will not be written"
            )
        try:
            fh = open(log_path, "r", encoding="utf-8", errors="replace")
        except OSError as exc:
            self._emit_runtime(
                f"[ERROR] failed to open log file {log_path!r}: {exc}"
            )
            self._close_control_plane_session(reason="runner_open_log_failed")
            return 1

        try:
            with fh:
                if follow:
                    fh.seek(0, 2)

                while True:
                    if self.stop_event is not None and self.stop_event.is_set():
                        self._emit_runtime("[INFO] stop requested")
                        self._emit_forced_drop_stop()
                        break

                    self._maybe_emit_no_snapshot_warning(log_path=log_path, follow=follow)
                    line = fh.readline()
                    if not line:
                        if not follow:
                            break
                        self._maybe_advance_break_cycle_without_snapshot()
                        time.sleep(FOLLOW_IDLE_SLEEP_SECONDS)
                        continue

                    self.process_line(line)
        finally:
            self._close_control_plane_session(reason="runner_exit")

        self._emit_stabilization_telemetry()
        return 0

