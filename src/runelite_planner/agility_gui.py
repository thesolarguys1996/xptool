from __future__ import annotations

import argparse
import json
import re
import statistics
import threading
import time
import tkinter as tk
from tkinter import ttk
from typing import Optional

from .agility import (
    AGILITY_TUNING_PROFILE_DB_PARITY,
    AgilityConfig,
    AgilityStrategy,
    parse_keywords,
    parse_obstacle_route,
)
from .bridge import CommandBusWriter, parse_snapshot_line
from .gui import RuntimeGUI
from .runner import RuntimeRunner

PRESET_ACTIVITIES: dict[str, dict[str, object] | None] = {
    "Custom": None,
    "Gnome Stronghold Course": {
        "start_world_x": 2474,
        "start_world_y": 3438,
        "start_plane": 0,
        "start_radius_tiles": 6,
        "course_world_x": 2474,
        "course_world_y": 3438,
        "course_plane": 0,
        "course_radius_tiles": 18,
        "option_keywords": "climb,net,branch,rope,pipe",
        "target_object_name_contains": "",
        "target_object_id": -1,
        "tuning_profile": AGILITY_TUNING_PROFILE_DB_PARITY,
    },
    "Draynor Rooftop Course": {
        "start_world_x": 3103,
        "start_world_y": 3279,
        "start_plane": 0,
        "start_radius_tiles": 6,
        "course_world_x": 3103,
        "course_world_y": 3279,
        "course_plane": 0,
        "course_radius_tiles": 22,
        "option_keywords": "climb,cross,balance,jump,climb-down",
        "target_object_name_contains": "",
        "target_object_id": -1,
        "tuning_profile": AGILITY_TUNING_PROFILE_DB_PARITY,
        "obstacle_route": (
            {
                "world_x": 3103,
                "world_y": 3279,
                "plane": 0,
                "action": "climb rough wall",
                "object_id": 11404,
                "object_name_contains": "rough wall",
            },
            {
                "world_x": 3098,
                "world_y": 3277,
                "plane": 3,
                "action": "cross tightrope",
                "object_id": 11405,
                "object_name_contains": "tightrope",
            },
            {
                "world_x": 3092,
                "world_y": 3276,
                "plane": 3,
                "action": "cross tightrope",
                "object_id": 11406,
                "object_name_contains": "tightrope",
            },
            {
                "world_x": 3089,
                "world_y": 3264,
                "plane": 3,
                "action": "balance narrow wall",
                "object_id": 11430,
                "object_name_contains": "narrow wall",
            },
            {
                "world_x": 3088,
                "world_y": 3256,
                "plane": 3,
                "action": "jump-up wall",
                "object_id": 11630,
                "object_name_contains": "wall",
            },
            {
                "world_x": 3095,
                "world_y": 3255,
                "plane": 3,
                "action": "jump gap",
                "object_id": 11631,
                "object_name_contains": "gap",
            },
            {
                "world_x": 3102,
                "world_y": 3261,
                "plane": 3,
                "action": "climb-down crate",
                "object_id": 11632,
                "object_name_contains": "crate",
            },
        ),
    },
    "Canifis Rooftop Course": {
        "start_world_x": 3500,
        "start_world_y": 3485,
        "start_plane": 0,
        "start_radius_tiles": 6,
        "course_world_x": 3500,
        "course_world_y": 3485,
        "course_plane": 0,
        "course_radius_tiles": 22,
        "option_keywords": "climb,jump,leap,cross,vault",
        "target_object_name_contains": "",
        "target_object_id": -1,
        "tuning_profile": AGILITY_TUNING_PROFILE_DB_PARITY,
    },
}

AGILITY_CAPTURE_LINE_PATTERN = re.compile(
    r"xptool\.agility_capture\s+"
    r"tick=(?P<tick>-?\d+)\s+"
    r"option=(?P<option>\S+)\s+"
    r"target=(?P<target>.*?)\s+"
    r"objectId=(?P<object_id>-?\d+)\s+"
    r"sceneX=(?P<scene_x>-?\d+)\s+"
    r"sceneY=(?P<scene_y>-?\d+)\s+"
    r"worldX=(?P<world_x>-?\d+)\s+"
    r"worldY=(?P<world_y>-?\d+)\s+"
    r"plane=(?P<plane>-?\d+)\s+"
    r"worldViewId=(?P<world_view_id>-?\d+)"
)
AGILITY_TICK_DURATION_MS = 600
AGILITY_STEP_EXIT_DISTANCE_TILES = 3


class AgilityRuntimeGUI(RuntimeGUI):
    def __init__(self, root: tk.Tk, *, auto_start: bool = False) -> None:
        self.activity_preset_var = tk.StringVar(master=root, value="Gnome Stronghold Course")
        self.agility_start_world_x_var = tk.StringVar(master=root, value="")
        self.agility_start_world_y_var = tk.StringVar(master=root, value="")
        self.agility_course_world_x_var = tk.StringVar(master=root, value="")
        self.agility_course_world_y_var = tk.StringVar(master=root, value="")
        self.agility_walk_arrive_distance_tiles_var = tk.StringVar(master=root, value="2")
        self.agility_tuning_profile_var = tk.StringVar(master=root, value=AGILITY_TUNING_PROFILE_DB_PARITY)
        self.agility_walk_to_course_when_outside_var = tk.BooleanVar(master=root, value=True)
        self.agility_option_keywords_var = tk.StringVar(
            master=root,
            value="climb,cross,jump,balance,squeeze,swing,vault",
        )
        self.use_captured_route_var = tk.BooleanVar(master=root, value=True)
        self.capture_status_var = tk.StringVar(master=root, value="Capture: idle")
        self.agility_target_object_name_contains_var = tk.StringVar(master=root, value="")
        self.agility_target_object_id_var = tk.StringVar(master=root, value="")
        self._capture_thread: Optional[threading.Thread] = None
        self._capture_stop_event: Optional[threading.Event] = None
        self._capture_events: list[dict[str, object]] = []
        self._capture_seen_signatures: set[str] = set()
        self._manual_captured_route: list[dict[str, object]] = []
        self._capture_timing_clicks: list[dict[str, object]] = []
        self._capture_last_snapshot: Optional[dict[str, int]] = None
        self.custom_coords_frame: ttk.LabelFrame | None = None
        self.option_keywords_label: ttk.Label | None = None
        self.option_keywords_entry: ttk.Entry | None = None
        self.object_name_label: ttk.Label | None = None
        self.object_name_entry: ttk.Entry | None = None
        self.object_id_label: ttk.Label | None = None
        self.object_id_entry: ttk.Entry | None = None
        super().__init__(root, auto_start=auto_start)
        self.root.title("RuneLite Agility Planner")
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

        course_opts = ttk.LabelFrame(frame, text="Agility Config")
        course_opts.grid(row=1, column=0, columnspan=3, sticky="we", pady=(4, 0))

        self.custom_coords_frame = ttk.LabelFrame(course_opts, text="Custom Course Coordinates")
        self.custom_coords_frame.grid(row=0, column=0, columnspan=8, sticky="we", padx=(8, 8), pady=(6, 2))
        ttk.Label(self.custom_coords_frame, text="Start X").grid(row=0, column=0, sticky="w", padx=(8, 4), pady=(6, 2))
        ttk.Entry(self.custom_coords_frame, textvariable=self.agility_start_world_x_var, width=8).grid(
            row=0, column=1, padx=(0, 10), pady=(6, 2), sticky="w"
        )
        ttk.Label(self.custom_coords_frame, text="Start Y").grid(row=0, column=2, sticky="w", pady=(6, 2))
        ttk.Entry(self.custom_coords_frame, textvariable=self.agility_start_world_y_var, width=8).grid(
            row=0, column=3, padx=(4, 10), pady=(6, 2), sticky="w"
        )
        ttk.Label(self.custom_coords_frame, text="Course X (optional)").grid(
            row=1, column=0, sticky="w", padx=(8, 4), pady=(2, 6)
        )
        ttk.Entry(self.custom_coords_frame, textvariable=self.agility_course_world_x_var, width=8).grid(
            row=1, column=1, padx=(0, 10), pady=(2, 6), sticky="w"
        )
        ttk.Label(self.custom_coords_frame, text="Course Y").grid(row=1, column=2, sticky="w", pady=(2, 6))
        ttk.Entry(self.custom_coords_frame, textvariable=self.agility_course_world_y_var, width=8).grid(
            row=1, column=3, padx=(4, 10), pady=(2, 6), sticky="w"
        )

        ttk.Label(course_opts, text="Walk arrive").grid(row=1, column=0, sticky="w", padx=(8, 4), pady=2)
        ttk.Entry(course_opts, textvariable=self.agility_walk_arrive_distance_tiles_var, width=6).grid(
            row=1, column=1, padx=(0, 10), pady=2, sticky="w"
        )
        ttk.Label(course_opts, text="Tuning profile").grid(row=1, column=2, sticky="w", pady=2)
        ttk.Combobox(
            course_opts,
            textvariable=self.agility_tuning_profile_var,
            state="readonly",
            values=(AGILITY_TUNING_PROFILE_DB_PARITY,),
            width=10,
        ).grid(row=1, column=3, padx=(4, 10), pady=2, sticky="w")

        self.option_keywords_label = ttk.Label(course_opts, text="Option keywords")
        self.option_keywords_label.grid(row=2, column=0, sticky="w", padx=(8, 4), pady=(2, 6))
        self.option_keywords_entry = ttk.Entry(course_opts, textvariable=self.agility_option_keywords_var, width=44)
        self.option_keywords_entry.grid(row=2, column=1, columnspan=4, padx=(0, 10), pady=(2, 6), sticky="we")
        self.object_name_label = ttk.Label(course_opts, text="Object name contains")
        self.object_name_label.grid(row=2, column=5, sticky="w", pady=(2, 6))
        self.object_name_entry = ttk.Entry(course_opts, textvariable=self.agility_target_object_name_contains_var, width=18)
        self.object_name_entry.grid(row=2, column=6, padx=(4, 10), pady=(2, 6), sticky="w")
        self.object_id_label = ttk.Label(course_opts, text="Object ID")
        self.object_id_label.grid(row=2, column=7, sticky="w", pady=(2, 6))
        self.object_id_entry = ttk.Entry(course_opts, textvariable=self.agility_target_object_id_var, width=8)
        self.object_id_entry.grid(row=2, column=8, padx=(4, 8), pady=(2, 6), sticky="w")

        ttk.Checkbutton(
            course_opts,
            text="Walk to course start/anchor when outside",
            variable=self.agility_walk_to_course_when_outside_var,
        ).grid(row=3, column=0, columnspan=8, sticky="w", padx=(8, 4), pady=(0, 6))

        ttk.Checkbutton(frame, text="Follow", variable=self.follow_var).grid(row=2, column=0, sticky="w")
        ttk.Checkbutton(frame, text="Dry run", variable=self.dry_run_var).grid(row=2, column=1, sticky="w")
        ttk.Checkbutton(frame, text="Lock mouse to RuneLite", variable=self.lock_mouse_var).grid(row=2, column=2, sticky="w")

        self._build_break_controls(frame, row=3, columnspan=3)

        capture_frame = ttk.LabelFrame(frame, text="Manual Obstacle Capture")
        capture_frame.grid(row=4, column=0, columnspan=3, sticky="we", pady=(4, 0))
        ttk.Button(capture_frame, text="Start Capture", command=self._start_manual_capture).grid(
            row=0, column=0, padx=(8, 6), pady=(6, 4), sticky="w"
        )
        ttk.Button(capture_frame, text="Stop Capture", command=self._stop_manual_capture).grid(
            row=0, column=1, padx=(0, 6), pady=(6, 4), sticky="w"
        )
        ttk.Button(capture_frame, text="Clear Captured", command=self._clear_manual_capture).grid(
            row=0, column=2, padx=(0, 6), pady=(6, 4), sticky="w"
        )
        ttk.Button(capture_frame, text="Show Captured Route", command=self._emit_manual_capture_route).grid(
            row=0, column=3, padx=(0, 6), pady=(6, 4), sticky="w"
        )
        ttk.Button(capture_frame, text="Use Captured Route", command=self._apply_manual_capture_to_custom).grid(
            row=0, column=4, padx=(0, 6), pady=(6, 4), sticky="w"
        )
        ttk.Button(capture_frame, text="Show Timing Stats", command=self._emit_manual_capture_timing_stats).grid(
            row=0, column=5, padx=(0, 6), pady=(6, 4), sticky="w"
        )
        ttk.Checkbutton(
            capture_frame,
            text="Use captured route for Custom run",
            variable=self.use_captured_route_var,
        ).grid(row=1, column=0, columnspan=3, padx=(8, 6), pady=(0, 6), sticky="w")
        ttk.Label(capture_frame, textvariable=self.capture_status_var).grid(
            row=1, column=3, columnspan=2, padx=(0, 8), pady=(0, 6), sticky="w"
        )
        capture_frame.columnconfigure(5, weight=1)

        ttk.Button(frame, text="Start", command=self.start, style="Start.TButton").grid(row=6, column=0, padx=(0, 4), pady=(6, 0), sticky="we")
        ttk.Button(frame, text="Stop", command=self.stop, style="Danger.TButton").grid(row=6, column=1, padx=4, pady=(6, 0), sticky="we")
        ttk.Button(frame, text="Clear output", command=self.clear_output).grid(row=6, column=2, padx=(4, 0), pady=(6, 0), sticky="we")

        ttk.Label(frame, textvariable=self.status_var).grid(row=7, column=0, columnspan=3, sticky="w", pady=(8, 0))
        frame.columnconfigure(1, weight=1)
        course_opts.columnconfigure(1, weight=1)
        if self.custom_coords_frame is not None:
            self.custom_coords_frame.columnconfigure(1, weight=1)

    def _on_activity_preset_changed(self, _event=None) -> None:
        self._apply_selected_preset(force=False)

    def _apply_selected_preset(self, *, force: bool) -> None:
        preset_name = self.activity_preset_var.get().strip() or "Custom"
        preset = PRESET_ACTIVITIES.get(preset_name)
        if preset is None:
            if force and preset_name not in PRESET_ACTIVITIES:
                self.activity_preset_var.set("Custom")
            self._apply_activity_visibility()
            return
        self.agility_start_world_x_var.set(str(int(preset.get("start_world_x", preset.get("course_world_x", -1)))))
        self.agility_start_world_y_var.set(str(int(preset.get("start_world_y", preset.get("course_world_y", -1)))))
        self.agility_course_world_x_var.set(str(int(preset.get("course_world_x", -1))))
        self.agility_course_world_y_var.set(str(int(preset.get("course_world_y", -1))))
        self.agility_option_keywords_var.set(str(preset.get("option_keywords", "climb,cross,jump,balance,squeeze,swing,vault")))
        self.agility_target_object_name_contains_var.set(str(preset.get("target_object_name_contains", "") or "").strip())
        preset_object_id = int(preset.get("target_object_id", -1))
        self.agility_target_object_id_var.set("" if preset_object_id <= 0 else str(preset_object_id))
        self.agility_tuning_profile_var.set(
            str(preset.get("tuning_profile", AGILITY_TUNING_PROFILE_DB_PARITY)).strip().upper()
            or AGILITY_TUNING_PROFILE_DB_PARITY
        )
        self._apply_activity_visibility()

    def _apply_activity_visibility(self) -> None:
        if self.custom_coords_frame is None:
            return
        is_custom = (self.activity_preset_var.get().strip() or "Custom") == "Custom"
        if is_custom:
            self.custom_coords_frame.grid()
            if self.option_keywords_label is not None:
                self.option_keywords_label.grid()
            if self.option_keywords_entry is not None:
                self.option_keywords_entry.grid()
            if self.object_name_label is not None:
                self.object_name_label.grid()
            if self.object_name_entry is not None:
                self.object_name_entry.grid()
            if self.object_id_label is not None:
                self.object_id_label.grid()
            if self.object_id_entry is not None:
                self.object_id_entry.grid()
        else:
            self.custom_coords_frame.grid_remove()
            if self.option_keywords_label is not None:
                self.option_keywords_label.grid_remove()
            if self.option_keywords_entry is not None:
                self.option_keywords_entry.grid_remove()
            if self.object_name_label is not None:
                self.object_name_label.grid_remove()
            if self.object_name_entry is not None:
                self.object_name_entry.grid_remove()
            if self.object_id_label is not None:
                self.object_id_label.grid_remove()
            if self.object_id_entry is not None:
                self.object_id_entry.grid_remove()

    def _parse_agility_capture_line(self, line: str) -> Optional[dict[str, object]]:
        match = AGILITY_CAPTURE_LINE_PATTERN.search(str(line or ""))
        if not match:
            return None
        try:
            return {
                "tick": int(match.group("tick")),
                "option": str(match.group("option") or "").strip().lower(),
                "target": str(match.group("target") or "").strip().lower(),
                "object_id": int(match.group("object_id")),
                "scene_x": int(match.group("scene_x")),
                "scene_y": int(match.group("scene_y")),
                "world_x": int(match.group("world_x")),
                "world_y": int(match.group("world_y")),
                "plane": int(match.group("plane")),
                "world_view_id": int(match.group("world_view_id")),
            }
        except ValueError:
            return None

    def _capture_route_rows(self) -> list[dict[str, object]]:
        rows: list[dict[str, object]] = []
        for event in self._capture_events:
            option = str(event.get("option", "") or "").strip().lower()
            target = str(event.get("target", "") or "").strip().lower()
            object_id = int(event.get("object_id", -1))
            action = option if not target else f"{option} {target}"
            rows.append(
                {
                    "world_x": int(event.get("world_x", -1)),
                    "world_y": int(event.get("world_y", -1)),
                    "plane": int(event.get("plane", 0)),
                    "action": action,
                    "object_id": object_id if object_id > 0 else -1,
                    "object_name_contains": target,
                }
            )
        return rows

    @staticmethod
    def _percentile(values: list[float], pct: float) -> float:
        if not values:
            return 0.0
        ordered = sorted(float(v) for v in values)
        if len(ordered) == 1:
            return ordered[0]
        p = max(0.0, min(100.0, float(pct)))
        rank = (len(ordered) - 1) * (p / 100.0)
        lo = int(rank)
        hi = min(lo + 1, len(ordered) - 1)
        frac = rank - lo
        return ordered[lo] + (ordered[hi] - ordered[lo]) * frac

    @staticmethod
    def _format_stats(values: list[int]) -> dict[str, float]:
        if not values:
            return {
                "count": 0,
                "min": 0.0,
                "p50": 0.0,
                "p95": 0.0,
                "avg": 0.0,
                "max": 0.0,
            }
        return {
            "count": float(len(values)),
            "min": float(min(values)),
            "p50": AgilityRuntimeGUI._percentile([float(v) for v in values], 50.0),
            "p95": AgilityRuntimeGUI._percentile([float(v) for v in values], 95.0),
            "avg": float(statistics.fmean(values)),
            "max": float(max(values)),
        }

    def _parse_capture_snapshot_state(self, line: str) -> Optional[dict[str, int]]:
        snapshot = parse_snapshot_line(line)
        if snapshot is None:
            return None
        raw = snapshot.raw if isinstance(snapshot.raw, dict) else {}
        player = raw.get("player")
        if not isinstance(player, dict):
            return None
        try:
            world_x = int(player.get("worldX", -1))
            world_y = int(player.get("worldY", -1))
            plane = int(player.get("plane", -1))
            tick = int(snapshot.tick)
            animation = int(snapshot.player_animation if snapshot.player_animation is not None else -1)
        except (TypeError, ValueError):
            return None
        if world_x <= 0 or world_y <= 0 or plane < 0:
            return None
        captured_at_raw = raw.get("capturedAtUnixMillis", raw.get("capturedAt", -1))
        try:
            captured_at_ms = int(captured_at_raw)
        except (TypeError, ValueError):
            captured_at_ms = -1
        return {
            "tick": tick,
            "captured_at_ms": captured_at_ms,
            "player_world_x": world_x,
            "player_world_y": world_y,
            "player_plane": plane,
            "player_animation": animation,
        }

    def _on_manual_capture_snapshot(self, snapshot_state: dict[str, int]) -> None:
        self._capture_last_snapshot = snapshot_state
        tick = int(snapshot_state.get("tick", -1))
        px = int(snapshot_state.get("player_world_x", -1))
        py = int(snapshot_state.get("player_world_y", -1))
        pplane = int(snapshot_state.get("player_plane", -1))
        panim = int(snapshot_state.get("player_animation", -1))
        if tick < 0:
            return
        for row in self._capture_timing_clicks:
            click_tick = int(row.get("click_tick", -1))
            if click_tick < 0 or tick < click_tick:
                continue
            if int(row.get("animation_start_tick", -1)) < 0 and panim not in (-1, 0):
                row["animation_start_tick"] = tick
            origin_x = int(row.get("player_origin_x", -1))
            origin_y = int(row.get("player_origin_y", -1))
            origin_plane = int(row.get("player_origin_plane", -1))
            if int(row.get("movement_start_tick", -1)) < 0 and origin_x > 0 and origin_y > 0 and origin_plane >= 0:
                moved = (
                    pplane != origin_plane
                    or max(abs(px - origin_x), abs(py - origin_y)) >= 1
                )
                if moved:
                    row["movement_start_tick"] = tick
            step_x = int(row.get("world_x", -1))
            step_y = int(row.get("world_y", -1))
            step_plane = int(row.get("plane", -1))
            if int(row.get("step_exit_tick", -1)) < 0 and step_x > 0 and step_y > 0 and step_plane >= 0:
                moved_off_step = (
                    pplane != step_plane
                    or max(abs(px - step_x), abs(py - step_y)) >= AGILITY_STEP_EXIT_DISTANCE_TILES
                )
                if moved_off_step:
                    row["step_exit_tick"] = tick

    def _on_manual_capture_timing_click(self, event: dict[str, object]) -> None:
        click_tick = int(event.get("tick", -1))
        if click_tick < 0:
            return
        option = str(event.get("option", "") or "").strip().lower()
        target = str(event.get("target", "") or "").strip().lower()
        action = option if not target else f"{option} {target}"
        origin_x = -1
        origin_y = -1
        origin_plane = -1
        if self._capture_last_snapshot is not None:
            snap_tick = int(self._capture_last_snapshot.get("tick", -999999))
            if abs(snap_tick - click_tick) <= 1:
                origin_x = int(self._capture_last_snapshot.get("player_world_x", -1))
                origin_y = int(self._capture_last_snapshot.get("player_world_y", -1))
                origin_plane = int(self._capture_last_snapshot.get("player_plane", -1))
        if self._capture_timing_clicks:
            prev = self._capture_timing_clicks[-1]
            prev_tick = int(prev.get("click_tick", -1))
            if click_tick >= prev_tick and int(prev.get("next_click_tick", -1)) < 0:
                prev["next_click_tick"] = click_tick
        self._capture_timing_clicks.append(
            {
                "click_tick": click_tick,
                "option": option,
                "target": target,
                "action": action,
                "object_id": int(event.get("object_id", -1)),
                "world_x": int(event.get("world_x", -1)),
                "world_y": int(event.get("world_y", -1)),
                "plane": int(event.get("plane", -1)),
                "player_origin_x": origin_x,
                "player_origin_y": origin_y,
                "player_origin_plane": origin_plane,
                "movement_start_tick": -1,
                "animation_start_tick": -1,
                "step_exit_tick": -1,
                "next_click_tick": -1,
            }
        )

    def _on_manual_capture_event(self, event: dict[str, object]) -> None:
        self._on_manual_capture_timing_click(event)
        world_x = int(event.get("world_x", -1))
        world_y = int(event.get("world_y", -1))
        plane = int(event.get("plane", -1))
        object_id = int(event.get("object_id", -1))
        option = str(event.get("option", "") or "").strip().lower()
        target = str(event.get("target", "") or "").strip().lower()
        if world_x <= 0 or world_y <= 0:
            return
        signature = f"{world_x}:{world_y}:{plane}:{object_id}:{option}:{target}"
        if signature in self._capture_seen_signatures:
            return
        self._capture_seen_signatures.add(signature)
        self._capture_events.append(event)
        self.capture_status_var.set(f"Capture: running ({len(self._capture_events)} steps)")
        self._append_output(
            "[AGILITY_CAPTURE_GUI] "
            f"step={len(self._capture_events) - 1} "
            f"action={option!r} target={target!r} "
            f"objectId={object_id} world=({world_x},{world_y},{plane})\n"
        )

    def _capture_reader_loop(self, log_path: str, stop_event: threading.Event) -> None:
        try:
            with open(log_path, "r", encoding="utf-8", errors="replace") as fh:
                fh.seek(0, 2)
                while not stop_event.is_set():
                    line = fh.readline()
                    if not line:
                        time.sleep(0.05)
                        continue
                    parsed_snapshot = self._parse_capture_snapshot_state(line)
                    if parsed_snapshot is not None:
                        self.root.after(0, lambda payload=parsed_snapshot: self._on_manual_capture_snapshot(payload))
                    parsed_capture = self._parse_agility_capture_line(line)
                    if parsed_capture is None:
                        continue
                    self.root.after(0, lambda payload=parsed_capture: self._on_manual_capture_event(payload))
        except Exception as exc:
            error_text = f"{exc!r}"
            self.root.after(
                0,
                lambda: self._append_output(f"[WARN] manual capture stopped due to read error: {error_text}\n"),
            )
        finally:
            self.root.after(0, lambda: self.capture_status_var.set(f"Capture: idle ({len(self._capture_events)} steps)"))

    def _start_manual_capture(self) -> None:
        if self._capture_thread is not None and self._capture_thread.is_alive():
            self._append_output("[INFO] manual capture is already running\n")
            return
        self._apply_backend_runtime_defaults()
        log_path = self.log_path_var.get().strip()
        if not log_path:
            self._append_output("[ERROR] backend log path is empty; cannot start capture\n")
            return
        stop_event = threading.Event()
        self._capture_stop_event = stop_event
        self._capture_thread = threading.Thread(
            target=self._capture_reader_loop,
            args=(log_path, stop_event),
            daemon=True,
        )
        self._capture_thread.start()
        self.capture_status_var.set(f"Capture: running ({len(self._capture_events)} steps)")
        self._append_output("[INFO] manual agility capture started; perform obstacle clicks now\n")

    def _stop_manual_capture(self, *, append_message: bool = True) -> None:
        if self._capture_stop_event is not None:
            self._capture_stop_event.set()
        self._capture_stop_event = None
        self.capture_status_var.set(f"Capture: idle ({len(self._capture_events)} steps)")
        if append_message:
            self._append_output("[INFO] manual agility capture stopped\n")

    def _clear_manual_capture(self) -> None:
        self._capture_events.clear()
        self._capture_seen_signatures.clear()
        self._manual_captured_route = []
        self._capture_timing_clicks.clear()
        self._capture_last_snapshot = None
        self.capture_status_var.set("Capture: idle (0 steps)")
        self._append_output("[INFO] cleared captured agility obstacle steps\n")

    def _emit_manual_capture_route(self) -> None:
        route_rows = self._capture_route_rows()
        if not route_rows:
            self._append_output("[WARN] no captured agility steps to show yet\n")
            return
        self._append_output("[AGILITY_CAPTURE_ROUTE_JSON] begin\n")
        self._append_output(json.dumps(route_rows, indent=2, ensure_ascii=True) + "\n")
        self._append_output("[AGILITY_CAPTURE_ROUTE_JSON] end\n")

    def _apply_manual_capture_to_custom(self) -> None:
        route_rows = self._capture_route_rows()
        if not route_rows:
            self._append_output("[WARN] capture at least one obstacle first\n")
            return
        self._manual_captured_route = route_rows
        first = route_rows[0]
        self.activity_preset_var.set("Custom")
        self._apply_activity_visibility()
        self.agility_start_world_x_var.set(str(int(first.get("world_x", -1))))
        self.agility_start_world_y_var.set(str(int(first.get("world_y", -1))))
        self.agility_course_world_x_var.set(str(int(first.get("world_x", -1))))
        self.agility_course_world_y_var.set(str(int(first.get("world_y", -1))))
        unique_keywords: list[str] = []
        for row in route_rows:
            action = str(row.get("action", "") or "").strip().lower()
            keyword = action.split(" ", 1)[0] if action else ""
            if keyword and keyword not in unique_keywords:
                unique_keywords.append(keyword)
        if unique_keywords:
            self.agility_option_keywords_var.set(",".join(unique_keywords))
        self._append_output(
            "[INFO] applied captured route to Custom mode "
            f"steps={len(route_rows)} start=({self.agility_start_world_x_var.get()},{self.agility_start_world_y_var.get()})\n"
        )

    def _emit_manual_capture_timing_stats(self) -> None:
        if not self._capture_timing_clicks:
            self._append_output("[WARN] no timing clicks captured yet; start capture and run one course loop\n")
            return
        click_intervals: list[int] = []
        movement_delays: list[int] = []
        animation_delays: list[int] = []
        step_exit_delays: list[int] = []
        per_action_intervals: dict[str, list[int]] = {}
        for row in self._capture_timing_clicks:
            click_tick = int(row.get("click_tick", -1))
            next_click_tick = int(row.get("next_click_tick", -1))
            movement_start_tick = int(row.get("movement_start_tick", -1))
            animation_start_tick = int(row.get("animation_start_tick", -1))
            step_exit_tick = int(row.get("step_exit_tick", -1))
            action = str(row.get("action", "") or "").strip().lower()
            if click_tick >= 0 and next_click_tick >= click_tick:
                interval = next_click_tick - click_tick
                click_intervals.append(interval)
                if action:
                    per_action_intervals.setdefault(action, []).append(interval)
            if click_tick >= 0 and movement_start_tick >= click_tick:
                movement_delays.append(movement_start_tick - click_tick)
            if click_tick >= 0 and animation_start_tick >= click_tick:
                animation_delays.append(animation_start_tick - click_tick)
            if click_tick >= 0 and step_exit_tick >= click_tick:
                step_exit_delays.append(step_exit_tick - click_tick)

        click_stats = self._format_stats(click_intervals)
        move_stats = self._format_stats(movement_delays)
        anim_stats = self._format_stats(animation_delays)
        exit_stats = self._format_stats(step_exit_delays)

        if step_exit_delays:
            suggested_retry_min = max(8, int(round(self._percentile([float(v) for v in step_exit_delays], 25.0))))
            suggested_retry_max = max(
                suggested_retry_min + 2,
                int(round(self._percentile([float(v) for v in step_exit_delays], 75.0))) + 1,
            )
        elif click_intervals:
            suggested_retry_min = max(8, int(round(self._percentile([float(v) for v in click_intervals], 25.0))))
            suggested_retry_max = max(
                suggested_retry_min + 2,
                int(round(self._percentile([float(v) for v in click_intervals], 75.0))),
            )
        else:
            suggested_retry_min = 12
            suggested_retry_max = 18

        if movement_delays:
            suggested_no_progress_retry = max(
                3,
                min(
                    12,
                    int(round(self._percentile([float(v) for v in movement_delays], 95.0))) + 1,
                ),
            )
        else:
            suggested_no_progress_retry = 5

        per_action_summary: list[dict[str, object]] = []
        for action, values in sorted(per_action_intervals.items(), key=lambda kv: kv[0]):
            stats = self._format_stats(values)
            per_action_summary.append(
                {
                    "action": action,
                    "count": int(stats["count"]),
                    "interval_ticks_avg": round(stats["avg"], 2),
                    "interval_ticks_p50": round(stats["p50"], 2),
                    "interval_ticks_p95": round(stats["p95"], 2),
                }
            )

        summary = {
            "click_count": len(self._capture_timing_clicks),
            "click_interval_ticks": {
                "count": int(click_stats["count"]),
                "min": round(click_stats["min"], 2),
                "p50": round(click_stats["p50"], 2),
                "p95": round(click_stats["p95"], 2),
                "avg": round(click_stats["avg"], 2),
                "max": round(click_stats["max"], 2),
            },
            "movement_start_delay_ticks": {
                "count": int(move_stats["count"]),
                "min": round(move_stats["min"], 2),
                "p50": round(move_stats["p50"], 2),
                "p95": round(move_stats["p95"], 2),
                "avg": round(move_stats["avg"], 2),
                "max": round(move_stats["max"], 2),
            },
            "animation_start_delay_ticks": {
                "count": int(anim_stats["count"]),
                "min": round(anim_stats["min"], 2),
                "p50": round(anim_stats["p50"], 2),
                "p95": round(anim_stats["p95"], 2),
                "avg": round(anim_stats["avg"], 2),
                "max": round(anim_stats["max"], 2),
            },
            "step_exit_delay_ticks": {
                "count": int(exit_stats["count"]),
                "min": round(exit_stats["min"], 2),
                "p50": round(exit_stats["p50"], 2),
                "p95": round(exit_stats["p95"], 2),
                "avg": round(exit_stats["avg"], 2),
                "max": round(exit_stats["max"], 2),
            },
            "step_exit_delay_ms_avg_estimate": round(exit_stats["avg"] * AGILITY_TICK_DURATION_MS, 2),
            "suggested_tuning": {
                "step_retry_block_min_ticks": suggested_retry_min,
                "step_retry_block_max_ticks": suggested_retry_max,
                "step_no_progress_retry_ticks": suggested_no_progress_retry,
            },
            "per_action_interval_ticks": per_action_summary,
        }

        self._append_output(
            "[AGILITY_CAPTURE_TIMING] "
            f"clicks={len(self._capture_timing_clicks)} "
            f"interval_p50={summary['click_interval_ticks']['p50']} "
            f"move_delay_p95={summary['movement_start_delay_ticks']['p95']} "
            f"exit_delay_p50={summary['step_exit_delay_ticks']['p50']} "
            f"suggested_retry={suggested_retry_min}-{suggested_retry_max} "
            f"no_progress_retry={suggested_no_progress_retry}\n"
        )
        self._append_output("[AGILITY_CAPTURE_TIMING_JSON] begin\n")
        self._append_output(json.dumps(summary, indent=2, ensure_ascii=True) + "\n")
        self._append_output("[AGILITY_CAPTURE_TIMING_JSON] end\n")

    def _on_close(self) -> None:
        self._stop_manual_capture(append_message=False)
        super()._on_close()

    def start(self) -> None:
        if self._runner_thread and self._runner_thread.is_alive():
            self._append_output("[WARN] Runner is already active\n")
            return

        self._apply_backend_runtime_defaults()
        log_path = self.log_path_var.get().strip()
        command_out = self.command_out_var.get().strip()
        if not log_path:
            self._append_output("[ERROR] backend log path is empty\n")
            return

        preset_name = self.activity_preset_var.get().strip() or "Custom"
        preset = PRESET_ACTIVITIES.get(preset_name)
        default_cfg = AgilityConfig()

        try:
            walk_arrive_distance_tiles = max(0, int(self.agility_walk_arrive_distance_tiles_var.get().strip() or "2"))
            target_object_id = int(self.agility_target_object_id_var.get().strip() or "-1")
            if preset is None:
                start_world_x = int(self.agility_start_world_x_var.get().strip() or "-1")
                start_world_y = int(self.agility_start_world_y_var.get().strip() or "-1")
                course_world_x = int(self.agility_course_world_x_var.get().strip() or str(start_world_x))
                course_world_y = int(self.agility_course_world_y_var.get().strip() or str(start_world_y))
            else:
                start_world_x = int(preset.get("start_world_x", preset.get("course_world_x", -1)))
                start_world_y = int(preset.get("start_world_y", preset.get("course_world_y", -1)))
                course_world_x = int(preset.get("course_world_x", start_world_x))
                course_world_y = int(preset.get("course_world_y", start_world_y))
        except ValueError:
            self._append_output("[ERROR] Agility numeric fields must be valid integers\n")
            return

        walk_click_mode = "SCENE"
        if start_world_x <= 0 or start_world_y <= 0:
            self._append_output("[ERROR] Invalid preset/custom start X/Y\n")
            return

        preset_data = preset or {}
        if preset is None:
            option_keywords = parse_keywords(self.agility_option_keywords_var.get().strip())
            target_object_name_contains = self.agility_target_object_name_contains_var.get().strip()
            target_object_id = target_object_id
            tuning_profile = str(self.agility_tuning_profile_var.get().strip() or AGILITY_TUNING_PROFILE_DB_PARITY).upper()
        else:
            option_keywords = parse_keywords(preset_data.get("option_keywords", ""))
            target_object_name_contains = str(preset_data.get("target_object_name_contains", "") or "").strip()
            target_object_id = int(preset_data.get("target_object_id", -1))
            tuning_profile = str(
                preset_data.get("tuning_profile", AGILITY_TUNING_PROFILE_DB_PARITY)
                or AGILITY_TUNING_PROFILE_DB_PARITY
            ).upper()
        if tuning_profile != AGILITY_TUNING_PROFILE_DB_PARITY:
            self._append_output("[ERROR] Agility tuning profile must be DB_PARITY\n")
            return
        if not option_keywords:
            option_keywords = ["climb", "cross", "jump", "balance", "squeeze", "swing", "vault"]
        start_plane = max(0, int(preset_data.get("start_plane", preset_data.get("course_plane", default_cfg.start_plane))))
        start_radius_tiles = max(0, int(preset_data.get("start_radius_tiles", default_cfg.start_radius_tiles)))
        course_plane = max(0, int(preset_data.get("course_plane", start_plane)))
        course_radius_tiles = max(0, int(preset_data.get("course_radius_tiles", default_cfg.course_radius_tiles)))
        obstacle_target_radius_tiles = max(
            1,
            int(preset_data.get("obstacle_target_radius_tiles", default_cfg.obstacle_target_radius_tiles)),
        )
        minimap_click_chance_pct = max(
            0,
            min(100, int(preset_data.get("minimap_click_chance_pct", default_cfg.minimap_click_chance_pct))),
        )
        max_obstacle_distance_tiles = max(
            1,
            int(preset_data.get("max_obstacle_distance_tiles", default_cfg.max_obstacle_distance_tiles)),
        )
        obstacle_route = parse_obstacle_route(preset_data.get("obstacle_route"))
        if preset is None and bool(self.use_captured_route_var.get()) and self._manual_captured_route:
            obstacle_route = parse_obstacle_route(self._manual_captured_route)

        self._append_output("[INFO] selected activity=agility\n")
        if preset is None and obstacle_route:
            self._append_output(f"[INFO] using captured/custom obstacle route steps={len(obstacle_route)}\n")

        strategy = AgilityStrategy(
            cfg=AgilityConfig(
                start_world_x=start_world_x,
                start_world_y=start_world_y,
                start_plane=start_plane,
                start_radius_tiles=start_radius_tiles,
                course_world_x=course_world_x,
                course_world_y=course_world_y,
                course_plane=course_plane,
                course_radius_tiles=course_radius_tiles,
                walk_arrive_distance_tiles=walk_arrive_distance_tiles,
                walk_click_mode=walk_click_mode,
                minimap_click_chance_pct=minimap_click_chance_pct,
                walk_to_course_when_outside=bool(self.agility_walk_to_course_when_outside_var.get()),
                max_obstacle_distance_tiles=max_obstacle_distance_tiles,
                obstacle_target_radius_tiles=obstacle_target_radius_tiles,
                obstacle_option_keywords=tuple(option_keywords),
                target_object_name_contains=target_object_name_contains,
                target_object_id=target_object_id,
                obstacle_route=tuple(obstacle_route),
                tuning_profile=tuning_profile,
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
            writer = CommandBusWriter(command_out)

        self._stop_event = threading.Event()
        self._active_strategy = strategy
        self.status_var.set("Status: running activity=agility")

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
        if bool(self.lock_mouse_var.get()):
            self._mouse_lock.start()
        else:
            self._mouse_lock.stop()

    def _refresh_status(self) -> None:
        running = self._runner_thread is not None and self._runner_thread.is_alive()
        if running:
            self.status_var.set("Status: running activity=agility")
            return
        self.status_var.set("Status: idle")


def main(argv: Optional[list[str]] = None) -> None:
    parser = argparse.ArgumentParser(description="RuneLite agility planner GUI")
    parser.add_argument("--auto-start", action="store_true", help="Start planner automatically on GUI launch")
    args = parser.parse_args(argv)

    root = tk.Tk()
    root.title("RuneLite Agility Planner")
    root.geometry("1040x740")
    AgilityRuntimeGUI(root, auto_start=bool(args.auto_start))
    root.mainloop()


if __name__ == "__main__":  # pragma: no cover
    main()
