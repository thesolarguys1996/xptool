from __future__ import annotations

import random
from dataclasses import dataclass, field
from typing import Any, Dict


@dataclass
class MotionEngine:
    """
    Session-level motion model with stable per-session baseline.
    """

    seed: int | None = None
    profile_name: str = "humanized"
    _rng: random.Random = field(init=False, repr=False)
    _base_drift: float = field(init=False)
    _base_pre_ms: int = field(init=False)
    _base_post_ms: int = field(init=False)

    def __post_init__(self) -> None:
        resolved_seed = int(self.seed) if self.seed is not None else 0
        if resolved_seed <= 0:
            resolved_seed = random.SystemRandom().randint(1, 2_147_483_647)
        self.seed = resolved_seed
        self._rng = random.Random(resolved_seed)
        self._base_drift = self._rng.uniform(2.0, 6.0)
        self._base_pre_ms = self._rng.randint(28, 82)
        self._base_post_ms = self._rng.randint(12, 46)

    def decorate_payload(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        interaction_kind = str(payload.get("interactionKind", "")).strip().upper()
        if interaction_kind == "CHOP_NEAREST_TREE":
            # Woodcutting receives slower, less regular click cadence.
            drift = max(0.0, self._base_drift + self._rng.uniform(-1.6, 1.8))
            pre = max(4, self._base_pre_ms + self._rng.randint(12, 58) + self._rng.randint(-20, 32))
            post = max(4, self._base_post_ms + self._rng.randint(6, 42) + self._rng.randint(-14, 18))
            if self._rng.random() < 0.27:
                pre += self._rng.randint(22, 88)
            if self._rng.random() < 0.16:
                post += self._rng.randint(10, 48)
            pre = min(170, pre)
            post = min(110, post)
        else:
            drift = max(0.0, self._base_drift + self._rng.uniform(-0.9, 0.9))
            pre = max(2, self._base_pre_ms + self._rng.randint(-12, 12))
            post = max(2, self._base_post_ms + self._rng.randint(-8, 8))
        payload["motionProfile"] = self.profile_name
        payload["mouseDriftRadius"] = round(drift, 3)
        payload["preClickDelayMs"] = int(pre)
        payload["postClickDelayMs"] = int(post)
        return payload
