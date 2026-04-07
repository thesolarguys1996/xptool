from __future__ import annotations

import argparse
import threading
import tkinter as tk
from tkinter import ttk
from typing import Optional

from .bridge import CommandBusWriter
from .fishing import (
    FISHING_PROFILE_DB_PARITY,
    FishingConfig,
    FishingStrategy,
    parse_drop_cadence_tuning_overrides,
    parse_fishing_motor_tuning_overrides,
    parse_item_ids,
    parse_npc_ids,
    parse_route_anchors,
)
from .gui import RuntimeGUI
from .models import RuntimeCommand
from .runner import RuntimeRunner

FISHING_IDLE_MODES: tuple[str, ...] = ("OFFSCREEN_BIASED",)
FISHING_PROFILE_DESCRIPTIONS: dict[str, str] = {
    FISHING_PROFILE_DB_PARITY: "DB metrics-aligned profile tuned toward captured mouse analytics behavior.",
}

PRESET_ACTIVITIES: dict[str, dict[str, object] | None] = {
    "Custom": None,
    "Beginner Shrimp/Anchovies Auto-Drop": {
        "fish_item_ids": "317,321",
        "target_npc_ids": "",
        "auto_drop_when_full": True,
        "stop_when_inventory_full": False,
        "enable_banking_loop": False,
        "tuning_profile": FISHING_PROFILE_DB_PARITY,
    },
    "Barb Fishing Auto-Drop": {
        "fish_item_ids": "335,331",
        "target_npc_ids": "",
        "auto_drop_when_full": True,
        "stop_when_inventory_full": False,
        "enable_banking_loop": False,
        "fishing_world_x": 3103,
        "fishing_world_y": 3434,
        "fishing_plane": 0,
        "fishing_area_radius_tiles": 4,
        "tuning_profile": FISHING_PROFILE_DB_PARITY,
    },
    "Barbarian Outpost West Pond": {
        "fish_item_ids": "335,331",
        "target_npc_ids": "",
        "auto_drop_when_full": True,
        "stop_when_inventory_full": False,
        "enable_banking_loop": False,
        "fishing_world_x": 2505,
        "fishing_world_y": 3490,
        "fishing_plane": 0,
        "fishing_area_radius_tiles": 4,
        "tuning_profile": FISHING_PROFILE_DB_PARITY,
    },
    "Barb Fishing -> Edgeville Bank": {
        "fish_item_ids": "335,331",
        "target_npc_ids": "",
        "auto_drop_when_full": False,
        "stop_when_inventory_full": False,
        "enable_banking_loop": True,
        "fishing_world_x": 3103,
        "fishing_world_y": 3434,
        "fishing_plane": 0,
        "fishing_area_radius_tiles": 4,
        "bank_world_x": 3093,
        "bank_world_y": 3493,
        "bank_plane": 0,
        "bank_area_radius_tiles": 3,
        "arrive_distance_tiles": 1,
        "waypoint_step_tiles": 6,
        "route_anchor_radius_tiles": 0,
        "route_anchors": "3103,3434;3100,3439;3099,3460;3102,3482;3093,3493",
        "tuning_profile": FISHING_PROFILE_DB_PARITY,
    },
}


class FishingRuntimeGUI(RuntimeGUI):
    def __init__(self, root: tk.Tk, *, auto_start: bool = False) -> None:
        self.activity_preset_var = tk.StringVar(master=root, value="Beginner Shrimp/Anchovies Auto-Drop")
        self.fishing_fish_item_id_var = tk.StringVar(master=root, value="317,321")
        self.fishing_target_npc_id_var = tk.StringVar(master=root, value="")
        self.fishing_tuning_profile_var = tk.StringVar(master=root, value=FISHING_PROFILE_DB_PARITY)
        self.fishing_tuning_profile_help_var = tk.StringVar(master=root, value="")
        self.fishing_auto_drop_when_full_var = tk.BooleanVar(master=root, value=True)
        self.fishing_stop_when_inventory_full_var = tk.BooleanVar(master=root, value=False)
        self.fishing_enable_banking_loop_var = tk.BooleanVar(master=root, value=False)
        self.fishing_idle_mode_var = tk.StringVar(master=root, value="OFFSCREEN_BIASED")
        self.fishing_drop_tuning_var = tk.StringVar(master=root, value="")
        self.fishing_motor_tuning_var = tk.StringVar(master=root, value="")
        self.fishing_world_x_var = tk.StringVar(master=root, value="")
        self.fishing_world_y_var = tk.StringVar(master=root, value="")
        self.fishing_plane_var = tk.StringVar(master=root, value="0")
        self.fishing_area_radius_tiles_var = tk.StringVar(master=root, value="3")
        self.fishing_bank_world_x_var = tk.StringVar(master=root, value="")
        self.fishing_bank_world_y_var = tk.StringVar(master=root, value="")
        self.fishing_bank_plane_var = tk.StringVar(master=root, value="0")
        self.fishing_bank_area_radius_tiles_var = tk.StringVar(master=root, value="3")
        self.fishing_arrive_distance_tiles_var = tk.StringVar(master=root, value="1")
        self.fishing_waypoint_step_tiles_var = tk.StringVar(master=root, value="6")
        self.walk_test_world_x_var = tk.StringVar(master=root, value="")
        self.walk_test_world_y_var = tk.StringVar(master=root, value="")
        self.walk_test_plane_var = tk.StringVar(master=root, value="0")
        self.walk_test_arrive_distance_tiles_var = tk.StringVar(master=root, value="1")
        self.walk_test_click_mode_var = tk.StringVar(master=root, value="MIXED")
        self.walk_test_minimap_chance_pct_var = tk.StringVar(master=root, value="93")
        self.custom_config_frame: ttk.LabelFrame | None = None
        self.banking_opts_frame: ttk.LabelFrame | None = None
        self.walk_test_frame: ttk.LabelFrame | None = None
        super().__init__(root, auto_start=auto_start)
        self.root.title("RuneLite Fishing Planner")
        self._apply_selected_preset(force=True)

    def _build_controls(self) -> None:
        frame = ttk.Frame(self.root, padding=8)
        frame.pack(side=tk.TOP, fill=tk.X)

        ttk.Label(frame, text="Activity").grid(row=0, column=0, sticky="w")
        activity_combo = ttk.Combobox(
            frame,
            textvariable=self.activity_preset_var,
            state="readonly",
            values=tuple(PRESET_ACTIVITIES.keys()),
            width=32,
        )
        activity_combo.grid(row=0, column=1, sticky="w", padx=4)
        activity_combo.bind("<<ComboboxSelected>>", self._on_activity_preset_changed)
        ttk.Label(frame, text="Profile").grid(row=0, column=2, sticky="e", padx=(8, 4))
        ttk.Combobox(
            frame,
            textvariable=self.fishing_tuning_profile_var,
            state="readonly",
            values=(FISHING_PROFILE_DB_PARITY,),
            width=10,
        ).grid(row=0, column=3, sticky="w")
        ttk.Label(
            frame,
            textvariable=self.fishing_tuning_profile_help_var,
            wraplength=360,
            justify="left",
        ).grid(row=0, column=4, sticky="w", padx=(8, 0))
        self.fishing_tuning_profile_var.trace_add("write", self._on_profile_changed)
        self._update_profile_description()

        self.custom_config_frame = ttk.LabelFrame(frame, text="Custom Fishing Config")
        self.custom_config_frame.grid(row=1, column=0, columnspan=3, sticky="we", pady=(4, 0))
        ttk.Label(self.custom_config_frame, text="Fish item IDs").grid(row=0, column=0, sticky="w", padx=(8, 4), pady=(6, 2))
        ttk.Entry(self.custom_config_frame, textvariable=self.fishing_fish_item_id_var, width=18).grid(
            row=0, column=1, sticky="w", padx=(0, 8), pady=(6, 2)
        )
        ttk.Label(self.custom_config_frame, text="Target NPC IDs").grid(row=0, column=2, sticky="w", pady=(6, 2))
        ttk.Entry(self.custom_config_frame, textvariable=self.fishing_target_npc_id_var, width=24).grid(
            row=0, column=3, sticky="w", padx=(4, 8), pady=(6, 2)
        )
        ttk.Checkbutton(
            self.custom_config_frame,
            text="Auto-drop when full",
            variable=self.fishing_auto_drop_when_full_var,
        ).grid(row=1, column=0, sticky="w", padx=(8, 4), pady=(2, 6))
        ttk.Checkbutton(
            self.custom_config_frame,
            text="Stop when inventory full",
            variable=self.fishing_stop_when_inventory_full_var,
        ).grid(row=1, column=1, sticky="w", pady=(2, 6))
        ttk.Checkbutton(
            self.custom_config_frame,
            text="Enable banking loop",
            variable=self.fishing_enable_banking_loop_var,
        ).grid(row=1, column=2, sticky="w", pady=(2, 6))
        ttk.Label(self.custom_config_frame, text="Fishing drop tuning overrides").grid(
            row=2,
            column=0,
            sticky="w",
            padx=(8, 4),
            pady=(0, 6),
        )
        ttk.Entry(self.custom_config_frame, textvariable=self.fishing_drop_tuning_var, width=64).grid(
            row=2,
            column=1,
            columnspan=3,
            sticky="we",
            padx=(0, 8),
            pady=(0, 6),
        )
        ttk.Label(self.custom_config_frame, text="Fishing motor tuning (accel/decel)").grid(
            row=3,
            column=0,
            sticky="w",
            padx=(8, 4),
            pady=(0, 6),
        )
        ttk.Entry(self.custom_config_frame, textvariable=self.fishing_motor_tuning_var, width=64).grid(
            row=3,
            column=1,
            columnspan=3,
            sticky="we",
            padx=(0, 8),
            pady=(0, 6),
        )

        ttk.Checkbutton(frame, text="Follow", variable=self.follow_var).grid(row=2, column=1, sticky="w")
        ttk.Checkbutton(frame, text="Dry run", variable=self.dry_run_var).grid(row=2, column=2, sticky="w")
        ttk.Checkbutton(frame, text="Lock mouse to RuneLite", variable=self.lock_mouse_var).grid(row=3, column=0, sticky="w")
        ttk.Label(frame, text="Fishing Idle Mode").grid(row=3, column=1, sticky="e", padx=(0, 4))
        ttk.Combobox(
            frame,
            textvariable=self.fishing_idle_mode_var,
            state="readonly",
            values=FISHING_IDLE_MODES,
            width=18,
        ).grid(row=3, column=2, sticky="w")

        self.banking_opts_frame = ttk.LabelFrame(frame, text="Bank Loop Route")
        self.banking_opts_frame.grid(row=4, column=0, columnspan=3, sticky="we", pady=(4, 0))
        ttk.Label(self.banking_opts_frame, text="Fish X").grid(row=0, column=0, sticky="w", padx=(8, 4), pady=(6, 2))
        ttk.Entry(self.banking_opts_frame, textvariable=self.fishing_world_x_var, width=8).grid(
            row=0, column=1, padx=(0, 10), pady=(6, 2)
        )
        ttk.Label(self.banking_opts_frame, text="Fish Y").grid(row=0, column=2, sticky="w", pady=(6, 2))
        ttk.Entry(self.banking_opts_frame, textvariable=self.fishing_world_y_var, width=8).grid(
            row=0, column=3, padx=(4, 10), pady=(6, 2)
        )
        ttk.Label(self.banking_opts_frame, text="Fish Plane").grid(row=0, column=4, sticky="w", pady=(6, 2))
        ttk.Entry(self.banking_opts_frame, textvariable=self.fishing_plane_var, width=6).grid(
            row=0, column=5, padx=(4, 8), pady=(6, 2)
        )

        ttk.Label(self.banking_opts_frame, text="Bank X").grid(row=1, column=0, sticky="w", padx=(8, 4), pady=2)
        ttk.Entry(self.banking_opts_frame, textvariable=self.fishing_bank_world_x_var, width=8).grid(
            row=1, column=1, padx=(0, 10), pady=2
        )
        ttk.Label(self.banking_opts_frame, text="Bank Y").grid(row=1, column=2, sticky="w", pady=2)
        ttk.Entry(self.banking_opts_frame, textvariable=self.fishing_bank_world_y_var, width=8).grid(
            row=1, column=3, padx=(4, 10), pady=2
        )
        ttk.Label(self.banking_opts_frame, text="Bank Plane").grid(row=1, column=4, sticky="w", pady=2)
        ttk.Entry(self.banking_opts_frame, textvariable=self.fishing_bank_plane_var, width=6).grid(
            row=1, column=5, padx=(4, 8), pady=2
        )

        ttk.Label(self.banking_opts_frame, text="Fish Radius").grid(row=2, column=0, sticky="w", padx=(8, 4), pady=(2, 6))
        ttk.Entry(self.banking_opts_frame, textvariable=self.fishing_area_radius_tiles_var, width=8).grid(
            row=2, column=1, padx=(0, 10), pady=(2, 6)
        )
        ttk.Label(self.banking_opts_frame, text="Bank Radius").grid(row=2, column=2, sticky="w", pady=(2, 6))
        ttk.Entry(self.banking_opts_frame, textvariable=self.fishing_bank_area_radius_tiles_var, width=8).grid(
            row=2, column=3, padx=(4, 10), pady=(2, 6)
        )
        ttk.Label(self.banking_opts_frame, text="Arrive Distance").grid(row=2, column=4, sticky="w", pady=(2, 6))
        ttk.Entry(self.banking_opts_frame, textvariable=self.fishing_arrive_distance_tiles_var, width=6).grid(
            row=2, column=5, padx=(4, 8), pady=(2, 6)
        )
        ttk.Label(self.banking_opts_frame, text="Step Tiles").grid(row=3, column=0, sticky="w", padx=(8, 4), pady=(0, 6))
        ttk.Entry(self.banking_opts_frame, textvariable=self.fishing_waypoint_step_tiles_var, width=8).grid(
            row=3, column=1, padx=(0, 10), pady=(0, 6), sticky="w"
        )

        self.walk_test_frame = ttk.LabelFrame(frame, text="Walk Test")
        self.walk_test_frame.grid(row=5, column=0, columnspan=3, sticky="we", pady=(4, 0))
        ttk.Label(self.walk_test_frame, text="Target X").grid(row=0, column=0, sticky="w", padx=(8, 4), pady=(6, 2))
        ttk.Entry(self.walk_test_frame, textvariable=self.walk_test_world_x_var, width=8).grid(
            row=0, column=1, padx=(0, 10), pady=(6, 2)
        )
        ttk.Label(self.walk_test_frame, text="Target Y").grid(row=0, column=2, sticky="w", pady=(6, 2))
        ttk.Entry(self.walk_test_frame, textvariable=self.walk_test_world_y_var, width=8).grid(
            row=0, column=3, padx=(4, 10), pady=(6, 2)
        )
        ttk.Label(self.walk_test_frame, text="Plane").grid(row=0, column=4, sticky="w", pady=(6, 2))
        ttk.Entry(self.walk_test_frame, textvariable=self.walk_test_plane_var, width=6).grid(
            row=0, column=5, padx=(4, 10), pady=(6, 2)
        )
        ttk.Label(self.walk_test_frame, text="Arrive").grid(row=0, column=6, sticky="w", pady=(6, 2))
        ttk.Entry(self.walk_test_frame, textvariable=self.walk_test_arrive_distance_tiles_var, width=6).grid(
            row=0, column=7, padx=(4, 10), pady=(6, 2)
        )
        ttk.Label(self.walk_test_frame, text="Mode").grid(row=0, column=8, sticky="w", pady=(6, 2))
        ttk.Combobox(
            self.walk_test_frame,
            textvariable=self.walk_test_click_mode_var,
            state="readonly",
            values=("MIXED", "SCENE", "MINIMAP"),
            width=10,
        ).grid(row=0, column=9, padx=(4, 10), pady=(6, 2), sticky="w")
        ttk.Label(self.walk_test_frame, text="Minimap %").grid(row=0, column=10, sticky="w", pady=(6, 2))
        ttk.Entry(self.walk_test_frame, textvariable=self.walk_test_minimap_chance_pct_var, width=6).grid(
            row=0, column=11, padx=(4, 8), pady=(6, 2), sticky="w"
        )
        ttk.Button(self.walk_test_frame, text="Use Fish Spot", command=self._set_walk_test_target_from_fish).grid(
            row=1, column=0, columnspan=2, padx=(8, 4), pady=(2, 6), sticky="w"
        )
        ttk.Button(self.walk_test_frame, text="Use Bank Spot", command=self._set_walk_test_target_from_bank).grid(
            row=1, column=2, columnspan=2, padx=(0, 4), pady=(2, 6), sticky="w"
        )
        ttk.Button(self.walk_test_frame, text="Send Walk Test", command=self._send_walk_test).grid(
            row=1, column=4, columnspan=3, padx=(0, 4), pady=(2, 6), sticky="w"
        )
        ttk.Label(self.walk_test_frame, text="Camera").grid(row=2, column=0, sticky="w", padx=(8, 4), pady=(2, 6))
        ttk.Button(
            self.walk_test_frame,
            text="Yaw Left",
            command=lambda: self._send_camera_nudge_test("YAW_LEFT"),
        ).grid(row=2, column=1, padx=(0, 4), pady=(2, 6), sticky="w")
        ttk.Button(
            self.walk_test_frame,
            text="Yaw Right",
            command=lambda: self._send_camera_nudge_test("YAW_RIGHT"),
        ).grid(row=2, column=2, padx=(0, 4), pady=(2, 6), sticky="w")
        ttk.Button(
            self.walk_test_frame,
            text="Pitch Up",
            command=lambda: self._send_camera_nudge_test("PITCH_UP"),
        ).grid(row=2, column=3, padx=(0, 4), pady=(2, 6), sticky="w")
        ttk.Button(
            self.walk_test_frame,
            text="Pitch Down",
            command=lambda: self._send_camera_nudge_test("PITCH_DOWN"),
        ).grid(row=2, column=4, padx=(0, 4), pady=(2, 6), sticky="w")

        self._build_break_controls(frame, row=6, columnspan=3)

        start_btn = ttk.Button(frame, text="Start", command=self.start, style="Start.TButton")
        start_btn.grid(row=10, column=0, padx=(0, 4), pady=(6, 0), sticky="we")

        stop_btn = ttk.Button(frame, text="Stop", command=self.stop, style="Danger.TButton")
        stop_btn.grid(row=10, column=1, padx=4, pady=(6, 0), sticky="we")

        clear_btn = ttk.Button(frame, text="Clear output", command=self.clear_output)
        clear_btn.grid(row=10, column=2, padx=(4, 0), pady=(6, 0), sticky="we")

        status_label = ttk.Label(frame, textvariable=self.status_var)
        status_label.grid(row=11, column=0, columnspan=3, sticky="w", pady=(8, 0))

        frame.columnconfigure(1, weight=1)

    def _on_activity_preset_changed(self, _event=None) -> None:
        self._apply_selected_preset(force=False)

    def _on_profile_changed(self, *_args) -> None:
        self._update_profile_description()

    def _update_profile_description(self) -> None:
        selected = (self.fishing_tuning_profile_var.get().strip() or FISHING_PROFILE_DB_PARITY).upper()
        self.fishing_tuning_profile_help_var.set(self._profile_description(selected))

    @staticmethod
    def _profile_description(profile_key: str) -> str:
        normalized = (profile_key or FISHING_PROFILE_DB_PARITY).strip().upper()
        return FISHING_PROFILE_DESCRIPTIONS.get(
            normalized,
            FISHING_PROFILE_DESCRIPTIONS[FISHING_PROFILE_DB_PARITY],
        )

    def _apply_selected_preset(self, *, force: bool) -> None:
        preset_name = self.activity_preset_var.get().strip() or "Custom"
        preset = PRESET_ACTIVITIES.get(preset_name)
        if preset is None:
            if force and preset_name not in PRESET_ACTIVITIES:
                self.activity_preset_var.set("Custom")
            return

        self.fishing_fish_item_id_var.set(str(preset.get("fish_item_ids", "335")).strip())
        self.fishing_target_npc_id_var.set(str(preset.get("target_npc_ids", "")).strip())
        self.fishing_tuning_profile_var.set(
            str(preset.get("tuning_profile", FISHING_PROFILE_DB_PARITY)).strip() or FISHING_PROFILE_DB_PARITY
        )
        self.fishing_auto_drop_when_full_var.set(bool(preset.get("auto_drop_when_full", True)))
        self.fishing_stop_when_inventory_full_var.set(bool(preset.get("stop_when_inventory_full", False)))
        self.fishing_enable_banking_loop_var.set(bool(preset.get("enable_banking_loop", False)))
        self.fishing_drop_tuning_var.set(str(preset.get("fishing_drop_tuning", "")).strip())
        self.fishing_motor_tuning_var.set(str(preset.get("fishing_motor_tuning", "")).strip())
        self.fishing_world_x_var.set(str(int(preset.get("fishing_world_x", -1))))
        self.fishing_world_y_var.set(str(int(preset.get("fishing_world_y", -1))))
        self.fishing_plane_var.set(str(max(0, int(preset.get("fishing_plane", 0)))))
        self.fishing_area_radius_tiles_var.set(str(max(0, int(preset.get("fishing_area_radius_tiles", 3)))))
        self.fishing_bank_world_x_var.set(str(int(preset.get("bank_world_x", -1))))
        self.fishing_bank_world_y_var.set(str(int(preset.get("bank_world_y", -1))))
        self.fishing_bank_plane_var.set(str(max(0, int(preset.get("bank_plane", 0)))))
        self.fishing_bank_area_radius_tiles_var.set(str(max(0, int(preset.get("bank_area_radius_tiles", 3)))))
        self.fishing_arrive_distance_tiles_var.set(str(max(0, int(preset.get("arrive_distance_tiles", 1)))))
        self.fishing_waypoint_step_tiles_var.set(str(max(1, int(preset.get("waypoint_step_tiles", 6)))))
        preset_fish_x = int(preset.get("fishing_world_x", -1))
        preset_fish_y = int(preset.get("fishing_world_y", -1))
        if preset_fish_x > 0 and preset_fish_y > 0:
            self.walk_test_world_x_var.set(str(preset_fish_x))
            self.walk_test_world_y_var.set(str(preset_fish_y))
            self.walk_test_plane_var.set(str(max(0, int(preset.get("fishing_plane", 0)))))
        self._apply_activity_visibility()
        self._update_profile_description()

    def _apply_activity_visibility(self) -> None:
        is_custom = (self.activity_preset_var.get().strip() or "Custom") == "Custom"
        if self.custom_config_frame is not None:
            if is_custom:
                self.custom_config_frame.grid()
            else:
                self.custom_config_frame.grid_remove()
        if self.banking_opts_frame is not None:
            if is_custom:
                self.banking_opts_frame.grid()
            else:
                self.banking_opts_frame.grid_remove()

    def _set_walk_test_target_from_fish(self) -> None:
        self.walk_test_world_x_var.set(self.fishing_world_x_var.get().strip())
        self.walk_test_world_y_var.set(self.fishing_world_y_var.get().strip())
        self.walk_test_plane_var.set(self.fishing_plane_var.get().strip() or "0")
        self._append_output("[INFO] walk test target set from fish spot\n")

    def _set_walk_test_target_from_bank(self) -> None:
        self.walk_test_world_x_var.set(self.fishing_bank_world_x_var.get().strip())
        self.walk_test_world_y_var.set(self.fishing_bank_world_y_var.get().strip())
        self.walk_test_plane_var.set(self.fishing_bank_plane_var.get().strip() or "0")
        self._append_output("[INFO] walk test target set from bank spot\n")

    def _send_walk_test(self) -> None:
        if self._runner_thread and self._runner_thread.is_alive():
            self._append_output("[WARN] stop runner before sending isolated walk test command\n")
            return

        self._apply_backend_runtime_defaults()
        command_out = self.command_out_var.get().strip()
        if not command_out:
            self._append_output("[ERROR] backend command out path is empty\n")
            return

        try:
            target_world_x = int(self.walk_test_world_x_var.get().strip() or "-1")
            target_world_y = int(self.walk_test_world_y_var.get().strip() or "-1")
            target_plane = max(0, int(self.walk_test_plane_var.get().strip() or "0"))
            arrive_distance_tiles = max(0, int(self.walk_test_arrive_distance_tiles_var.get().strip() or "1"))
            minimap_click_chance_pct = max(0, min(100, int(self.walk_test_minimap_chance_pct_var.get().strip() or "93")))
        except ValueError:
            self._append_output("[ERROR] Walk test X/Y/plane/arrive/minimap% must be integers\n")
            return

        if target_world_x <= 0 or target_world_y <= 0:
            self._append_output("[ERROR] Walk test target X/Y must be > 0\n")
            return

        walk_click_mode = (self.walk_test_click_mode_var.get().strip() or "MIXED").upper()
        if walk_click_mode not in {"MIXED", "SCENE", "MINIMAP"}:
            self._append_output("[ERROR] Walk test mode must be MIXED, SCENE, or MINIMAP\n")
            return

        payload = {
            "targetWorldX": target_world_x,
            "targetWorldY": target_world_y,
            "targetPlane": target_plane,
            "arriveDistanceTiles": arrive_distance_tiles,
            "walkClickMode": walk_click_mode,
            "minimapClickChancePct": minimap_click_chance_pct,
        }

        writer = CommandBusWriter(command_out)
        try:
            command_id = writer.write_command(
                RuntimeCommand(
                    command_type="WALK_TO_WORLDPOINT_SAFE",
                    payload=payload,
                    reason="fishing_walk_test_button",
                    source="fishing_gui",
                )
            )
        finally:
            writer.close()

        self._append_output(
            "[INFO] walk test command sent "
            f"id={command_id} target=({target_world_x},{target_world_y},{target_plane}) "
            f"arrive={arrive_distance_tiles} mode={walk_click_mode} minimap%={minimap_click_chance_pct}\n"
        )

    def _send_camera_nudge_test(self, direction: str) -> None:
        if self._runner_thread and self._runner_thread.is_alive():
            self._append_output("[WARN] stop runner before sending isolated camera test command\n")
            return

        self._apply_backend_runtime_defaults()
        command_out = self.command_out_var.get().strip()
        if not command_out:
            self._append_output("[ERROR] backend command out path is empty\n")
            return

        normalized_direction = (direction or "").strip().upper()
        if normalized_direction not in {"YAW_LEFT", "YAW_RIGHT", "PITCH_UP", "PITCH_DOWN"}:
            self._append_output("[ERROR] Camera direction must be YAW_LEFT, YAW_RIGHT, PITCH_UP, or PITCH_DOWN\n")
            return

        writer = CommandBusWriter(command_out)
        try:
            command_id = writer.write_command(
                RuntimeCommand(
                    command_type="CAMERA_NUDGE_SAFE",
                    payload={"direction": normalized_direction},
                    reason="fishing_camera_test_button",
                    source="fishing_gui",
                )
            )
        finally:
            writer.close()

        self._append_output(
            "[INFO] camera test command sent "
            f"id={command_id} direction={normalized_direction}\n"
        )

    def start(self) -> None:
        if self._runner_thread and self._runner_thread.is_alive():
            self._append_output("[WARN] Runner is already active\n")
            return

        self._apply_backend_runtime_defaults()
        log_path = self.log_path_var.get().strip()
        command_out = self.command_out_var.get().strip()
        fish_item_id_raw = self.fishing_fish_item_id_var.get().strip() or "335"
        target_npc_ids_raw = self.fishing_target_npc_id_var.get().strip()
        fishing_drop_tuning_raw = self.fishing_drop_tuning_var.get().strip()
        fishing_motor_tuning_raw = self.fishing_motor_tuning_var.get().strip()
        enable_banking_loop = bool(self.fishing_enable_banking_loop_var.get())
        preset_name = self.activity_preset_var.get().strip() or "Custom"
        preset = PRESET_ACTIVITIES.get(preset_name) or {}
        fishing_idle_mode = (self.fishing_idle_mode_var.get().strip() or "OFFSCREEN_BIASED").upper()
        tuning_profile = (self.fishing_tuning_profile_var.get().strip() or FISHING_PROFILE_DB_PARITY).upper()

        if not log_path:
            self._append_output("[ERROR] backend log path is empty\n")
            return

        try:
            fish_item_ids = parse_item_ids(fish_item_id_raw)
            if not fish_item_ids:
                fish_item_ids = [335]
            target_npc_ids = tuple(parse_npc_ids(target_npc_ids_raw))
            fishing_world_x = int(self.fishing_world_x_var.get().strip() or "-1")
            fishing_world_y = int(self.fishing_world_y_var.get().strip() or "-1")
            fishing_plane = max(0, int(self.fishing_plane_var.get().strip() or "0"))
            fishing_area_radius_tiles = max(0, int(self.fishing_area_radius_tiles_var.get().strip() or "3"))
            bank_world_x = int(self.fishing_bank_world_x_var.get().strip() or "-1")
            bank_world_y = int(self.fishing_bank_world_y_var.get().strip() or "-1")
            bank_plane = max(0, int(self.fishing_bank_plane_var.get().strip() or "0"))
            bank_area_radius_tiles = max(0, int(self.fishing_bank_area_radius_tiles_var.get().strip() or "3"))
            arrive_distance_tiles = max(0, int(self.fishing_arrive_distance_tiles_var.get().strip() or "1"))
            waypoint_step_tiles = max(1, int(self.fishing_waypoint_step_tiles_var.get().strip() or "6"))
            route_anchor_radius_tiles = max(0, int(preset.get("route_anchor_radius_tiles", 1)))
        except ValueError:
            self._append_output("[ERROR] Fish item IDs must be comma/space-separated integers\n")
            return
        if fishing_idle_mode not in FISHING_IDLE_MODES:
            self._append_output("[ERROR] Fishing idle mode must be OFFSCREEN_BIASED\n")
            return
        if tuning_profile != FISHING_PROFILE_DB_PARITY:
            self._append_output("[ERROR] Fishing tuning profile must be DB_PARITY\n")
            return

        route_anchors = parse_route_anchors(
            preset.get("route_anchors", ""),
            default_plane=fishing_plane,
        )
        try:
            fishing_drop_tuning = parse_drop_cadence_tuning_overrides(fishing_drop_tuning_raw)
        except ValueError as exc:
            self._append_output(f"[ERROR] {exc}\n")
            return
        try:
            fishing_motor_tuning = parse_fishing_motor_tuning_overrides(fishing_motor_tuning_raw)
        except ValueError as exc:
            self._append_output(f"[ERROR] {exc}\n")
            return

        if enable_banking_loop and (
            fishing_world_x <= 0
            or fishing_world_y <= 0
            or bank_world_x <= 0
            or bank_world_y <= 0
        ):
            self._append_output("[ERROR] Banking loop enabled: Fish and Bank X/Y must be > 0\n")
            return

        self._append_output("[INFO] selected activity=fishing\n")
        if enable_banking_loop:
            self._append_output("[INFO] fishing banking loop enabled\n")
            if route_anchors:
                self._append_output(f"[INFO] fishing route anchors enabled ({len(route_anchors)} points)\n")
        self._append_output(f"[INFO] fishing idle mode={fishing_idle_mode}\n")
        self._append_output(f"[INFO] fishing tuning profile={tuning_profile}\n")
        if fishing_drop_tuning:
            self._append_output(f"[INFO] fishing drop tuning overrides={fishing_drop_tuning}\n")
        if fishing_motor_tuning:
            self._append_output(f"[INFO] fishing motor tuning overrides={fishing_motor_tuning}\n")
        self._append_output(f"[INFO] profile detail={self._profile_description(tuning_profile)}\n")
        lock_mouse_requested = bool(self.lock_mouse_var.get())
        if fishing_idle_mode == "OFFSCREEN_BIASED" and lock_mouse_requested:
            self._append_output(
                "[WARN] Lock mouse to RuneLite is incompatible with OFFSCREEN_BIASED idle; disabling lock for this run\n"
            )
            lock_mouse_requested = False
            self.lock_mouse_var.set(False)

        strategy = FishingStrategy(
            cfg=FishingConfig(
                auto_drop_when_full=bool(self.fishing_auto_drop_when_full_var.get()),
                fish_item_ids=tuple(fish_item_ids),
                stop_when_inventory_full=bool(self.fishing_stop_when_inventory_full_var.get()),
                target_npc_ids=target_npc_ids,
                enable_banking_loop=enable_banking_loop,
                fishing_world_x=fishing_world_x,
                fishing_world_y=fishing_world_y,
                fishing_plane=fishing_plane,
                fishing_area_radius_tiles=fishing_area_radius_tiles,
                bank_world_x=bank_world_x,
                bank_world_y=bank_world_y,
                bank_plane=bank_plane,
                bank_area_radius_tiles=bank_area_radius_tiles,
                arrive_distance_tiles=arrive_distance_tiles,
                waypoint_step_tiles=waypoint_step_tiles,
                route_anchors=tuple(route_anchors),
                route_anchor_radius_tiles=route_anchor_radius_tiles,
                tuning_profile=tuning_profile,
                fishing_drop_tuning=fishing_drop_tuning,
                fishing_motor_tuning=fishing_motor_tuning,
            )
        )

        dry_run = bool(self.dry_run_var.get())
        follow = bool(self.follow_var.get())
        break_settings = self._read_break_settings_from_ui()
        if break_settings is None:
            return

        if dry_run or not command_out:
            writer: Optional[CommandBusWriter] = None
        else:
            if not self._push_fishing_idle_mode(command_out, fishing_idle_mode):
                return
            writer = CommandBusWriter(command_out)

        self._stop_event = threading.Event()
        self._active_strategy = strategy
        self.status_var.set("Status: running activity=fishing")

        runner = RuntimeRunner(
            strategy=strategy,
            writer=writer,
            dry_run=dry_run,
            runtime_callback=self._runtime_callback,
            stop_event=self._stop_event,
            break_settings=break_settings,
        )
        self._active_runner = runner

        def run_wrapper() -> None:
            self._runtime_callback("[INFO] runner starting")
            try:
                code = runner.run(log_path=log_path, follow=follow)
                self._runtime_callback(f"[INFO] runner finished with code {code}")
            finally:
                self._mouse_lock.stop()
                if writer is not None:
                    writer.close()
                if self._stop_event is not None:
                    self._stop_event.set()
                self._active_runner = None

        self._runner_thread = threading.Thread(target=run_wrapper, daemon=True)
        self._runner_thread.start()
        if lock_mouse_requested:
            self._mouse_lock.start()
        else:
            self._mouse_lock.stop()

    def _push_fishing_idle_mode(self, command_out: str, fishing_idle_mode: str) -> bool:
        writer = CommandBusWriter(command_out)
        try:
            command_id = writer.write_command(
                RuntimeCommand(
                    command_type="SET_FISHING_IDLE_MODE_SAFE",
                    payload={"mode": fishing_idle_mode, "plannerTag": "fishing"},
                    reason="fishing_gui_idle_mode_apply",
                    source="fishing_gui",
                )
            )
        except Exception as exc:
            self._append_output(f"[ERROR] failed to apply fishing idle mode {fishing_idle_mode}: {exc!r}\n")
            return False
        finally:
            writer.close()
        self._append_output(f"[INFO] applied fishing idle mode={fishing_idle_mode} command_id={command_id}\n")
        return True

    def _refresh_status(self) -> None:
        running = self._runner_thread is not None and self._runner_thread.is_alive()
        if running:
            self.status_var.set("Status: running activity=fishing")
            return
        self.status_var.set("Status: idle")

    def _on_hotkey_stop_local(self, _event: tk.Event) -> None:
        super()._on_hotkey_stop_local(_event)

    def _handle_hotkey_stop_request(self) -> None:
        super()._handle_hotkey_stop_request()

    def _poll_f8_fallback(self) -> None:
        super()._poll_f8_fallback()


def main(argv: Optional[list[str]] = None) -> None:
    parser = argparse.ArgumentParser(description="RuneLite fishing planner GUI")
    parser.add_argument("--auto-start", action="store_true", help="Start planner automatically on GUI launch")
    args = parser.parse_args(argv)

    root = tk.Tk()
    root.title("RuneLite Fishing Planner")
    root.geometry("1000x680")
    FishingRuntimeGUI(root, auto_start=bool(args.auto_start))
    root.mainloop()


if __name__ == "__main__":  # pragma: no cover
    main()

