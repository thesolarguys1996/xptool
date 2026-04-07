from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Mapping, Optional, Sequence

from .runtime_core.models import Intent, IntentKind
from .models import Snapshot


@dataclass
class DropSessionController:
    _session_active: bool = False
    _session_item_id: Optional[int] = None

    @property
    def session_active(self) -> bool:
        return self._session_active

    @property
    def session_item_id(self) -> Optional[int]:
        return self._session_item_id

    def stop_session(
        self,
        *,
        activity: str,
        policy_key: str,
        reason: str,
    ) -> Sequence[Intent]:
        if not self._session_active:
            return []
        item_id = int(self._session_item_id or -1)
        self._session_active = False
        self._session_item_id = None
        payload = {"itemId": item_id} if item_id > 0 else {}
        return [
            Intent(
                intent_key=f"{activity}:STOP_DROP_SESSION",
                activity=activity,
                kind=IntentKind.STOP_DROP_SESSION,
                target=payload,
                params=payload,
                policy_key=policy_key,
                reason=reason,
            )
        ]

    def step(
        self,
        snapshot: Snapshot,
        *,
        activity: str,
        policy_key: str,
        start_condition: bool,
        candidate_item_id: Optional[int],
        start_reason: str,
        stop_reason_prefix: str,
        switch_item_on_change: bool = False,
        start_extra_params: Optional[Mapping[str, Any]] = None,
    ) -> Sequence[Intent]:
        if not snapshot.logged_in:
            return self.stop_session(
                activity=activity,
                policy_key=policy_key,
                reason=f"{stop_reason_prefix}_logged_out",
            )
        if snapshot.bank_open:
            return self.stop_session(
                activity=activity,
                policy_key=policy_key,
                reason=f"{stop_reason_prefix}_bank_open",
            )

        resolved_item_id = int(candidate_item_id or -1)
        if resolved_item_id <= 0:
            resolved_item_id = -1

        if not self._session_active:
            if not start_condition or resolved_item_id <= 0:
                return []
            self._session_active = True
            self._session_item_id = resolved_item_id
            payload = {"itemId": resolved_item_id}
            if isinstance(start_extra_params, Mapping):
                for key, value in start_extra_params.items():
                    payload[str(key)] = value
            return [
                Intent(
                    intent_key=f"{activity}:START_DROP_SESSION:{resolved_item_id}",
                    activity=activity,
                    kind=IntentKind.START_DROP_SESSION,
                    target=payload,
                    params=payload,
                    policy_key=policy_key,
                    reason=start_reason,
                )
            ]

        if resolved_item_id <= 0:
            return self.stop_session(
                activity=activity,
                policy_key=policy_key,
                reason=f"{stop_reason_prefix}_inventory_drained",
            )
        if self._session_item_id is None:
            self._session_item_id = resolved_item_id
        if int(self._session_item_id) != resolved_item_id:
            if switch_item_on_change:
                self._session_item_id = resolved_item_id
                # Keep the active session running; executor-side drop runtime can
                # continue in slot order across configured target item ids.
                return []
            return self.stop_session(
                activity=activity,
                policy_key=policy_key,
                reason=f"{stop_reason_prefix}_item_changed",
            )
        return []

