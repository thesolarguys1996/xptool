from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum
from typing import Any, Mapping


PROTOCOL_VERSION_V1 = "xptool-bridge/1"


class AuthMethod(str, Enum):
    MTLS = "MTLS"
    LOCAL_KEY = "LOCAL_KEY"


class AckStatus(str, Enum):
    ACCEPTED = "accepted"
    REJECTED = "rejected"
    FAILED = "failed"
    EXECUTED = "executed"


class HealthStatus(str, Enum):
    STARTING = "starting"
    HEALTHY = "healthy"
    DEGRADED = "degraded"
    STOPPING = "stopping"
    STOPPED = "stopped"


class InteractionType(str, Enum):
    MOUSE_MOVE = "MOUSE_MOVE"
    MOUSE_CLICK = "MOUSE_CLICK"
    KEY_CHORD = "KEY_CHORD"
    MENU_CLICK = "MENU_CLICK"
    WIDGET_CLICK = "WIDGET_CLICK"


@dataclass(frozen=True)
class AuthHello:
    bridge_instance_id: str
    controller_id: str
    method: AuthMethod
    issued_at_unix_ms: int
    nonce: str
    protocol_version: str = PROTOCOL_VERSION_V1


@dataclass(frozen=True)
class AuthProof:
    controller_id: str
    session_challenge: str
    issued_at_unix_ms: int
    nonce: str
    signature_base64: str


@dataclass(frozen=True)
class AuthSession:
    session_id: str
    peer_id: str
    issued_at_unix_ms: int
    expires_at_unix_ms: int
    key_id: str


@dataclass(frozen=True)
class SignedCommandEnvelope:
    command_id: str
    session_id: str
    issued_at_unix_ms: int
    nonce: str
    command_type: str
    payload: Mapping[str, Any] = field(default_factory=dict)
    signature_base64: str = ""


@dataclass(frozen=True)
class CommandAck:
    command_id: str
    status: AckStatus
    reason: str
    details: Mapping[str, Any] = field(default_factory=dict)
    ack_unix_ms: int = 0


@dataclass(frozen=True)
class BridgeHealth:
    bridge_instance_id: str
    status: HealthStatus
    now_unix_ms: int
    last_game_tick: int
    last_client_tick: int
    mapping_version: str
    pending_command_queue_depth: int
    runtime_gateway_attached: bool


@dataclass(frozen=True)
class SnapshotDto:
    captured_at_unix_ms: int
    tick: int
    game_state: str
    mouse_canvas_x: int
    mouse_canvas_y: int
    canvas_width: int
    canvas_height: int
    inventory: Mapping[str, Any] = field(default_factory=dict)
    equipment: Mapping[str, Any] = field(default_factory=dict)
    world_state: Mapping[str, Any] = field(default_factory=dict)


@dataclass(frozen=True)
class InteractionCommand:
    interaction_id: str
    type: InteractionType
    issued_at_unix_ms: int
    args: Mapping[str, Any] = field(default_factory=dict)


@dataclass(frozen=True)
class InteractionResult:
    interaction_id: str
    status: str
    reason: str
    details: Mapping[str, Any] = field(default_factory=dict)


@dataclass(frozen=True)
class RuntimeStateDto:
    tick: int
    login_runtime_active: bool
    logout_runtime_active: bool
    motor_program_active: bool
    pending_mouse_move: bool
    runtime_flags: Mapping[str, Any] = field(default_factory=dict)
