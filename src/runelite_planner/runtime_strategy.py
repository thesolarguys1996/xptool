from __future__ import annotations

from typing import Any, Protocol, Sequence, TYPE_CHECKING

if TYPE_CHECKING:
    from .runtime_core.models import Intent
else:
    Intent = Any
from .models import Snapshot


class RuntimeStrategy(Protocol):
    """
    Stateless intent provider.
    """

    def intents(self, snapshot: Snapshot) -> Sequence[Intent]:
        ...

