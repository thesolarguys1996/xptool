from __future__ import annotations

from dataclasses import dataclass

DB_PARITY_PROFILE = "DB_PARITY"


@dataclass(frozen=True)
class ActivityTaskSpec:
    name: str
    default_profile: str | None
    gui_enabled: bool
    headless_enabled: bool


_ACTIVITY_TASKS: tuple[ActivityTaskSpec, ...] = (
    ActivityTaskSpec("woodcutting", DB_PARITY_PROFILE, gui_enabled=True, headless_enabled=True),
    ActivityTaskSpec("mining", DB_PARITY_PROFILE, gui_enabled=True, headless_enabled=True),
    ActivityTaskSpec("fishing", DB_PARITY_PROFILE, gui_enabled=False, headless_enabled=True),
    ActivityTaskSpec("agility", DB_PARITY_PROFILE, gui_enabled=False, headless_enabled=True),
    ActivityTaskSpec("combat", DB_PARITY_PROFILE, gui_enabled=True, headless_enabled=True),
    ActivityTaskSpec("store_bank", DB_PARITY_PROFILE, gui_enabled=False, headless_enabled=True),
    ActivityTaskSpec("bank_probe", None, gui_enabled=True, headless_enabled=True),
    ActivityTaskSpec("drop_probe", None, gui_enabled=False, headless_enabled=True),
)

_TASKS_BY_NAME: dict[str, ActivityTaskSpec] = {task.name: task for task in _ACTIVITY_TASKS}


def supported_gui_activities() -> tuple[str, ...]:
    return tuple(task.name for task in _ACTIVITY_TASKS if task.gui_enabled)


def supported_headless_activities() -> tuple[str, ...]:
    return tuple(task.name for task in _ACTIVITY_TASKS if task.headless_enabled)


def default_headless_activity() -> str:
    return "woodcutting"


def default_profile_for_activity(activity: str) -> str | None:
    normalized = str(activity or "").strip().lower()
    spec = _TASKS_BY_NAME.get(normalized)
    return None if spec is None else spec.default_profile
