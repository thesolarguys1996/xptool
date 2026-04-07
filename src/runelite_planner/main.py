from __future__ import annotations

import argparse
import sys
from typing import Optional
from .bridge import CommandBusWriter
from .activities.registry import default_headless_activity, supported_headless_activities
from .control_plane import (
    ControlPlaneAuditSink,
    ControlPlaneAuditPolicy,
    ControlPlaneRuntimeSettings,
    ControlPlaneSecuritySettings,
    EnvSecretProvider,
    EnvTokenProvider,
    FileControlPlaneClient,
    HttpControlPlaneClient,
    NoopControlPlaneClient,
)
from .remote_planner import (
    HttpRemotePlannerClient,
    NoopRemotePlannerClient,
    RemotePlannerSecuritySettings,
    RemotePlannerSettings,
)
from .paths import (
    default_command_out_path,
    default_control_plane_audit_path,
    default_control_plane_policy_path,
    default_log_path,
)
from .runner import RuntimeRunner, BreakSettings


ACTIVITY_CHOICES = list(supported_headless_activities())
DEFAULT_ACTIVITY = default_headless_activity()


class _RemoteOnlyStrategy:
    def intents(self, _snapshot):
        return ()


def _activity_to_strategy_name(activity: str) -> str:
    tokens = [token.strip() for token in str(activity or "").split("_") if token.strip()]
    if not tokens:
        return "UnknownStrategy"
    return "".join(token.capitalize() for token in tokens) + "Strategy"


def parse_args(argv: Optional[list[str]] = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        prog="xptool",
        description="Headless RuneLite planner runner",
    )

    parser.add_argument(
        "--log",
        help="Path to RuneLite client.log file (default: runtime/client.log)",
    )
    parser.add_argument(
        "--command-out",
        help="Path to NDJSON command bus output file for the plugin",
    )
    parser.add_argument(
        "--follow",
        action="store_true",
        help="Tail the log file (like tail -f) instead of exiting at EOF",
    )
    parser.add_argument(
        "--replay",
        action="store_true",
        help="Replay historical snapshots from the log (no tailing)",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Do not write commands; just log decisions to stdout",
    )
    parser.add_argument(
        "--activity",
        choices=list(ACTIVITY_CHOICES),
        default=DEFAULT_ACTIVITY,
        help=f"Activity strategy to run (default: {DEFAULT_ACTIVITY})",
    )
    parser.add_argument(
        "--enable-breaks",
        action="store_true",
        help="Enable global break scheduler (logout/login cycles) for every activity",
    )
    parser.add_argument(
        "--break-bot-time-minutes",
        type=float,
        default=50.0,
        help="Base active work window in minutes before taking a break (default: 50)",
    )
    parser.add_argument(
        "--break-time-minutes",
        type=float,
        default=30.0,
        help="Base break window in minutes (default: 30)",
    )
    parser.add_argument(
        "--break-randomized-pct",
        type=float,
        default=15.0,
        help="Percent jitter applied to bot/break times (default: 15)",
    )
    # Backward-compatible legacy break flags.
    parser.add_argument("--break-work-min", type=float, default=None, help=argparse.SUPPRESS)
    parser.add_argument("--break-work-max", type=float, default=None, help=argparse.SUPPRESS)
    parser.add_argument("--break-min", type=float, default=None, help=argparse.SUPPRESS)
    parser.add_argument("--break-max", type=float, default=None, help=argparse.SUPPRESS)
    parser.add_argument("--break-login-username", default="", help=argparse.SUPPRESS)
    parser.add_argument("--break-login-password", default="", help=argparse.SUPPRESS)
    parser.add_argument("--break-login-no-submit", action="store_true", help=argparse.SUPPRESS)
    parser.add_argument(
        "--break-command-retry-seconds",
        type=float,
        default=4.0,
        help="Seconds between repeated break commands while waiting for state changes (default: 4)",
    )
    parser.add_argument(
        "--control-plane-url",
        default="",
        help="Optional control-plane base URL (enables HTTP control-plane mode)",
    )
    parser.add_argument(
        "--control-plane-policy-file",
        default="",
        help=(
            "Optional local JSON policy file for control-plane mode "
            "(default when no URL is set: runtime/xptool-state/control-plane-policy.json)"
        ),
    )
    parser.add_argument(
        "--control-plane-token-env",
        default="XPTOOL_CONTROL_PLANE_TOKEN",
        help="Environment variable name containing control-plane bearer token (default: XPTOOL_CONTROL_PLANE_TOKEN)",
    )
    parser.add_argument(
        "--control-plane-timeout-seconds",
        type=float,
        default=2.0,
        help="HTTP control-plane request timeout in seconds (default: 2.0)",
    )
    parser.add_argument(
        "--control-plane-signing-key-env",
        default="XPTOOL_CONTROL_PLANE_SIGNING_KEY",
        help=(
            "Environment variable name containing control-plane HMAC signing key "
            "(default: XPTOOL_CONTROL_PLANE_SIGNING_KEY)"
        ),
    )
    parser.add_argument(
        "--control-plane-signing-key-id",
        default="",
        help="Optional signing key id header value for control-plane HMAC signing",
    )
    parser.add_argument(
        "--control-plane-replay-window-seconds",
        type=float,
        default=30.0,
        help="Replay protection window in seconds for control-plane request/response nonces (default: 30)",
    )
    parser.add_argument(
        "--control-plane-require-response-replay-fields",
        action="store_true",
        help="Require response nonce/timestamp replay fields from control-plane responses",
    )
    parser.add_argument(
        "--control-plane-allow-insecure-http",
        action="store_true",
        help="Allow non-HTTPS control-plane URL (localhost is always allowed by default)",
    )
    parser.add_argument(
        "--control-plane-contract-version",
        default="1.0",
        help="Planner/control-plane contract version field sent on requests (default: 1.0)",
    )
    parser.add_argument(
        "--control-plane-client-build",
        default="xptool-local",
        help="Client build identifier sent on control-plane requests (default: xptool-local)",
    )
    parser.add_argument(
        "--control-plane-require-decision-id",
        action="store_true",
        help="Require decisionId in control-plane refresh responses",
    )
    parser.add_argument(
        "--control-plane-poll-seconds",
        type=float,
        default=3.0,
        help="Control-plane policy refresh interval in seconds (default: 3.0)",
    )
    parser.add_argument(
        "--control-plane-audit-path",
        default="",
        help=(
            "NDJSON audit sink path for control-plane events "
            "(default: runtime/xptool-state/control-plane-audit.ndjson)"
        ),
    )
    parser.add_argument(
        "--control-plane-disable-audit",
        action="store_true",
        help="Disable local control-plane audit NDJSON output",
    )
    parser.add_argument(
        "--control-plane-audit-retention-days",
        type=float,
        default=14.0,
        help="Retention window for control-plane audit NDJSON entries (default: 14.0 days)",
    )
    parser.add_argument(
        "--control-plane-audit-max-event-bytes",
        type=int,
        default=8192,
        help="Maximum serialized size per control-plane audit event (default: 8192)",
    )
    parser.add_argument(
        "--control-plane-audit-prune-seconds",
        type=float,
        default=900.0,
        help="Minimum interval between control-plane audit prune passes (default: 900s)",
    )
    parser.add_argument(
        "--remote-planner-url",
        default="",
        help="Remote planner base URL (required by strict policy lock)",
    )
    parser.add_argument(
        "--remote-planner-token-env",
        default="XPTOOL_REMOTE_PLANNER_TOKEN",
        help="Environment variable name containing remote planner bearer token",
    )
    parser.add_argument(
        "--remote-planner-timeout-seconds",
        type=float,
        default=0.5,
        help="Remote planner request timeout in seconds (default: 0.5)",
    )
    parser.add_argument(
        "--remote-planner-max-commands",
        type=int,
        default=3,
        help="Maximum commands accepted per remote decision (default: 3)",
    )
    parser.add_argument(
        "--remote-planner-contract-version",
        default="1.0",
        help="Remote planner contract version sent on decision requests (default: 1.0)",
    )
    parser.add_argument(
        "--remote-planner-client-build",
        default="xptool-local",
        help="Client build identifier sent on remote decision requests",
    )
    parser.add_argument(
        "--remote-planner-signing-key-env",
        default="XPTOOL_REMOTE_PLANNER_SIGNING_KEY",
        help="Environment variable name containing remote planner HMAC signing key",
    )
    parser.add_argument(
        "--remote-planner-signing-key-id",
        default="",
        help="Optional signing key id header value for remote planner signatures",
    )
    parser.add_argument(
        "--remote-planner-replay-window-seconds",
        type=float,
        default=30.0,
        help="Replay protection window in seconds for remote planner nonces (default: 30)",
    )
    parser.add_argument(
        "--remote-planner-require-response-replay-fields",
        action="store_true",
        help="Require response nonce/timestamp fields from remote planner responses",
    )
    parser.add_argument(
        "--remote-planner-require-response-signature",
        action="store_true",
        help="Require HMAC response signatures from remote planner",
    )
    parser.add_argument(
        "--remote-planner-verify-command-envelopes",
        action="store_true",
        help="Verify commandEnvelope signatures for each remote command",
    )
    parser.add_argument(
        "--remote-planner-require-command-envelopes",
        action="store_true",
        help="Reject remote decisions that omit per-command commandEnvelope payloads",
    )
    parser.add_argument(
        "--remote-planner-emit-local-command-envelopes",
        action="store_true",
        help="Attach locally signed commandEnvelope metadata when remote commands omit it",
    )
    args = parser.parse_args(argv)

    # follow and replay are mutually exclusive; if both set, prefer replay.
    if args.replay:
        args.follow = False

    return args


def build_control_plane_components(
    args: argparse.Namespace,
):
    base_url = str(getattr(args, "control_plane_url", "") or "").strip()
    policy_file = str(getattr(args, "control_plane_policy_file", "") or "").strip()
    poll_seconds = float(getattr(args, "control_plane_poll_seconds", 3.0))

    enabled = bool(base_url or policy_file)
    security_settings = ControlPlaneSecuritySettings(
        signing_key="",
        signing_key_id=str(getattr(args, "control_plane_signing_key_id", "") or ""),
        replay_window_seconds=float(getattr(args, "control_plane_replay_window_seconds", 30.0)),
        require_response_replay_fields=bool(
            getattr(args, "control_plane_require_response_replay_fields", False)
        ),
        enforce_https=not bool(getattr(args, "control_plane_allow_insecure_http", False)),
        allow_insecure_http_localhost=True,
        contract_version=str(getattr(args, "control_plane_contract_version", "1.0") or "1.0"),
        client_build=str(getattr(args, "control_plane_client_build", "xptool-local") or "xptool-local"),
        require_decision_id=bool(getattr(args, "control_plane_require_decision_id", False)),
    ).normalized()
    if not enabled:
        client = NoopControlPlaneClient()
    elif base_url:
        token_env = str(getattr(args, "control_plane_token_env", "XPTOOL_CONTROL_PLANE_TOKEN") or "")
        signing_env = str(
            getattr(args, "control_plane_signing_key_env", "XPTOOL_CONTROL_PLANE_SIGNING_KEY") or ""
        )
        timeout_seconds = float(getattr(args, "control_plane_timeout_seconds", 2.0))
        client = HttpControlPlaneClient(
            base_url=base_url,
            token_provider=EnvTokenProvider(token_env),
            secret_provider=EnvSecretProvider(signing_env),
            security_settings=security_settings,
            timeout_seconds=timeout_seconds,
        )
    else:
        client = FileControlPlaneClient(policy_file or default_control_plane_policy_path())

    settings = ControlPlaneRuntimeSettings(
        enabled=enabled,
        poll_interval_seconds=poll_seconds,
    ).normalized()

    audit_path = str(getattr(args, "control_plane_audit_path", "") or "").strip()
    if not audit_path:
        audit_path = default_control_plane_audit_path()
    if bool(getattr(args, "control_plane_disable_audit", False)):
        audit_sink = None
    else:
        audit_policy = ControlPlaneAuditPolicy(
            retention_days=float(getattr(args, "control_plane_audit_retention_days", 14.0)),
            max_event_bytes=int(getattr(args, "control_plane_audit_max_event_bytes", 8192)),
            prune_interval_seconds=float(getattr(args, "control_plane_audit_prune_seconds", 900.0)),
        ).normalized()
        audit_sink = ControlPlaneAuditSink(audit_path, policy=audit_policy)

    return client, settings, audit_sink


def build_remote_planner_components(args: argparse.Namespace):
    base_url = str(getattr(args, "remote_planner_url", "") or "").strip()
    settings = RemotePlannerSettings(
        enabled=bool(base_url),
        timeout_seconds=float(getattr(args, "remote_planner_timeout_seconds", 0.5)),
        fallback_to_local=False,
        require_startup_precheck=True,
        max_commands_per_decision=int(getattr(args, "remote_planner_max_commands", 3)),
        contract_version=str(getattr(args, "remote_planner_contract_version", "1.0") or "1.0"),
        client_build=str(getattr(args, "remote_planner_client_build", "xptool-local") or "xptool-local"),
        enforce_https=True,
        allow_insecure_http_localhost=True,
        require_decision_id=True,
    ).normalized()
    if not settings.enabled:
        return NoopRemotePlannerClient(), settings
    token_env = str(getattr(args, "remote_planner_token_env", "XPTOOL_REMOTE_PLANNER_TOKEN") or "")
    signing_env = str(
        getattr(args, "remote_planner_signing_key_env", "XPTOOL_REMOTE_PLANNER_SIGNING_KEY") or ""
    )
    security_settings = RemotePlannerSecuritySettings(
        signing_key="",
        signing_key_id=str(getattr(args, "remote_planner_signing_key_id", "") or ""),
        replay_window_seconds=float(getattr(args, "remote_planner_replay_window_seconds", 30.0)),
        require_response_replay_fields=bool(
            getattr(args, "remote_planner_require_response_replay_fields", False)
        ),
        require_response_signature=bool(
            getattr(args, "remote_planner_require_response_signature", False)
        ),
        verify_command_envelopes=bool(
            getattr(args, "remote_planner_verify_command_envelopes", False)
        ),
        require_command_envelopes=bool(
            getattr(args, "remote_planner_require_command_envelopes", False)
        ),
        emit_local_command_envelopes=bool(
            getattr(args, "remote_planner_emit_local_command_envelopes", False)
        ),
    ).normalized()
    client = HttpRemotePlannerClient(
        base_url=base_url,
        token_provider=EnvTokenProvider(token_env),
        secret_provider=EnvSecretProvider(signing_env),
        settings=settings,
        security_settings=security_settings,
    )
    return client, settings


def strict_remote_policy_violations(
    *,
    remote_planner_settings: RemotePlannerSettings,
) -> list[str]:
    violations: list[str] = []
    if not remote_planner_settings.enabled:
        violations.append("remote_planner_url_required")
    if remote_planner_settings.fallback_to_local:
        violations.append("remote_planner_local_fallback_must_be_disabled")
    if not remote_planner_settings.require_startup_precheck:
        violations.append("remote_planner_startup_precheck_must_be_enabled")
    if not remote_planner_settings.require_decision_id:
        violations.append("remote_planner_decision_id_requirement_must_be_enabled")
    if not remote_planner_settings.enforce_https:
        violations.append("remote_planner_https_enforcement_must_be_enabled")
    return violations


def main(argv: Optional[list[str]] = None) -> int:
    args = parse_args(argv)

    activity = str(getattr(args, "activity", DEFAULT_ACTIVITY)).strip().lower()
    strategy = _RemoteOnlyStrategy()
    strategy_name = _activity_to_strategy_name(activity)
    log_path = args.log or default_log_path()
    command_out = args.command_out or default_command_out_path()

    bot_minutes = max(0.25, float(args.break_bot_time_minutes))
    break_minutes = max(0.25, float(args.break_time_minutes))
    randomized_pct = max(0.0, min(95.0, float(args.break_randomized_pct)))

    legacy_work_min = args.break_work_min
    legacy_work_max = args.break_work_max
    if legacy_work_min is not None or legacy_work_max is not None:
        work_lo = float(legacy_work_min if legacy_work_min is not None else bot_minutes)
        work_hi = float(legacy_work_max if legacy_work_max is not None else bot_minutes)
        if work_lo > work_hi:
            work_lo, work_hi = work_hi, work_lo
        bot_minutes = max(0.25, (work_lo + work_hi) / 2.0)
        if bot_minutes > 0.0:
            randomized_pct = max(randomized_pct, ((work_hi - work_lo) * 50.0) / bot_minutes)

    legacy_break_min = args.break_min
    legacy_break_max = args.break_max
    if legacy_break_min is not None or legacy_break_max is not None:
        break_lo = float(legacy_break_min if legacy_break_min is not None else break_minutes)
        break_hi = float(legacy_break_max if legacy_break_max is not None else break_minutes)
        if break_lo > break_hi:
            break_lo, break_hi = break_hi, break_lo
        break_minutes = max(0.25, (break_lo + break_hi) / 2.0)
        if break_minutes > 0.0:
            randomized_pct = max(randomized_pct, ((break_hi - break_lo) * 50.0) / break_minutes)

    randomized_pct = max(0.0, min(95.0, randomized_pct))
    spread = randomized_pct / 100.0
    break_settings = BreakSettings(
        enabled=bool(args.enable_breaks),
        work_minutes_min=max(0.25, bot_minutes * (1.0 - spread)),
        work_minutes_max=max(0.25, bot_minutes * (1.0 + spread)),
        break_minutes_min=max(0.25, break_minutes * (1.0 - spread)),
        break_minutes_max=max(0.25, break_minutes * (1.0 + spread)),
        command_retry_seconds=float(args.break_command_retry_seconds),
    )

    if args.dry_run:
        writer: Optional[CommandBusWriter] = None
    else:
        writer = CommandBusWriter(command_out)
    control_plane_client, control_plane_settings, control_plane_audit_sink = build_control_plane_components(args)
    remote_planner_client, remote_planner_settings = build_remote_planner_components(args)

    try:
        violations = strict_remote_policy_violations(
            remote_planner_settings=remote_planner_settings,
        )
        if violations:
            print(
                "[ERROR] strict_remote_policy_violation "
                + ",".join(sorted(violations))
            )
            return 2
        runner = RuntimeRunner(
            strategy=strategy,
            writer=writer,
            dry_run=args.dry_run,
            runtime_callback=None,
            stop_event=None,
            break_settings=break_settings,
            control_plane_client=control_plane_client,
            control_plane_settings=control_plane_settings,
            control_plane_audit_sink=control_plane_audit_sink,
            remote_planner_client=remote_planner_client,
            remote_planner_settings=remote_planner_settings,
            strategy_name_override=strategy_name,
            strategy_activity_override=activity,
        )
        return runner.run(log_path=log_path, follow=bool(args.follow))
    finally:
        if writer is not None:
            writer.close()


if __name__ == "__main__":  # pragma: no cover
    sys.exit(main())

