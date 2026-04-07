from __future__ import annotations

COMBAT_PRESET_DEFAULT = "Custom"
COMBAT_PRESETS: dict[str, dict[str, object]] = {
    "Custom": {
        "npc_id": -1,
        "world_x": -1,
        "world_y": -1,
        "max_distance": 8,
        "max_chase_distance": 8,
        "encounter_profile": "none",
    },
    "Cows (Lumbridge)": {"npc_id": 2791, "world_x": 3257, "world_y": 3267, "max_distance": 18, "max_chase_distance": 9},
    "Hill Giants (Edgeville Dungeon)": {"npc_id": 2098, "world_x": 3117, "world_y": 9852, "max_distance": 20, "max_chase_distance": 10},
    "Moss Giants (Varrock Sewers)": {"npc_id": 2090, "world_x": 3173, "world_y": 9890, "max_distance": 20, "max_chase_distance": 10},
    "Brutus": {
        "npc_id": -1,
        "world_x": -1,
        "world_y": -1,
        "max_distance": 18,
        "max_chase_distance": 10,
        "encounter_profile": "brutus",
    },
}
COMBAT_ENCOUNTER_DEFAULT_LABEL = "None"
COMBAT_ENCOUNTER_LABEL_TO_PROFILE: dict[str, str] = {
    "None": "none",
    "Brutus": "brutus",
}
COMBAT_ENCOUNTER_PROFILE_TO_LABEL: dict[str, str] = {
    profile: label for label, profile in COMBAT_ENCOUNTER_LABEL_TO_PROFILE.items()
}


def encounter_profile_from_label(label: str) -> str:
    token = str(label or "").strip()
    return COMBAT_ENCOUNTER_LABEL_TO_PROFILE.get(token, token.lower())
