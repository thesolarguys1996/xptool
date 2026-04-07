from __future__ import annotations

import argparse
import tkinter as tk
from tkinter import ttk
from typing import Optional

from .activity_profiles import COMBAT_PROFILE_DB_PARITY
from .gui import (
    COMBAT_ENCOUNTER_LABEL_TO_PROFILE,
    COMBAT_PRESETS,
    COMBAT_PRESET_DEFAULT,
    RuntimeGUI,
)


class CombatRuntimeGUI(RuntimeGUI):
    def __init__(self, root: tk.Tk, *, auto_start: bool = False) -> None:
        super().__init__(root, auto_start=auto_start)
        self.root.title("RuneLite Combat Planner")
        self.activity_var.set("combat")

    def _build_controls(self) -> None:
        frame = ttk.Frame(self.root, padding=8)
        frame.pack(side=tk.TOP, fill=tk.X)

        ttk.Checkbutton(frame, text="Follow", variable=self.follow_var).grid(row=0, column=0, sticky="w")
        ttk.Checkbutton(frame, text="Dry run", variable=self.dry_run_var).grid(row=0, column=1, sticky="w")
        ttk.Checkbutton(frame, text="Lock mouse to RuneLite", variable=self.lock_mouse_var).grid(row=0, column=2, sticky="w")

        self._build_break_controls(frame, row=1, columnspan=3)

        combat_opts = ttk.Frame(frame)
        combat_opts.grid(row=2, column=0, columnspan=3, sticky="we", pady=(4, 0))
        ttk.Label(combat_opts, text="Combat Preset").grid(row=0, column=0, sticky="w")
        combat_preset_combo = ttk.Combobox(
            combat_opts,
            textvariable=self.combat_preset_var,
            state="readonly",
            values=tuple(COMBAT_PRESETS.keys()),
            width=30,
        )
        combat_preset_combo.grid(row=0, column=1, padx=(4, 10), sticky="w")
        combat_preset_combo.bind("<<ComboboxSelected>>", self._on_combat_preset_changed)
        ttk.Label(combat_opts, text="NPC ID").grid(row=0, column=2, sticky="w")
        ttk.Entry(combat_opts, textvariable=self.combat_npc_id_var, width=8).grid(row=0, column=3, padx=(4, 10))
        ttk.Label(combat_opts, text="X").grid(row=0, column=4, sticky="w")
        ttk.Entry(combat_opts, textvariable=self.combat_target_x_var, width=8).grid(row=0, column=5, padx=(4, 10))
        ttk.Label(combat_opts, text="Y").grid(row=0, column=6, sticky="w")
        ttk.Entry(combat_opts, textvariable=self.combat_target_y_var, width=8).grid(row=0, column=7, padx=(4, 10))
        ttk.Label(combat_opts, text="Range").grid(row=0, column=8, sticky="w")
        ttk.Entry(combat_opts, textvariable=self.combat_target_max_distance_var, width=6).grid(row=0, column=9, padx=(4, 10))
        ttk.Label(combat_opts, text="Chase").grid(row=0, column=10, sticky="w")
        ttk.Entry(combat_opts, textvariable=self.combat_max_chase_distance_var, width=6).grid(row=0, column=11, padx=(4, 0))
        ttk.Label(combat_opts, text="Eat HP").grid(row=1, column=0, sticky="w", pady=(4, 0))
        ttk.Entry(combat_opts, textvariable=self.combat_eat_hp_var, width=8).grid(row=1, column=1, padx=(4, 10), pady=(4, 0), sticky="w")
        ttk.Label(combat_opts, text="Eat Rand %").grid(row=1, column=2, sticky="w", pady=(4, 0))
        ttk.Entry(combat_opts, textvariable=self.combat_eat_randomized_pct_var, width=6).grid(
            row=1, column=3, padx=(4, 10), pady=(4, 0), sticky="w"
        )
        ttk.Label(combat_opts, text="Food ID").grid(row=1, column=4, sticky="w", pady=(4, 0))
        ttk.Entry(combat_opts, textvariable=self.combat_food_item_id_var, width=8).grid(row=1, column=5, padx=(4, 10), pady=(4, 0), sticky="w")
        ttk.Label(combat_opts, text="Encounter").grid(row=1, column=6, sticky="w", pady=(4, 0))
        ttk.Combobox(
            combat_opts,
            textvariable=self.combat_encounter_var,
            state="readonly",
            values=tuple(COMBAT_ENCOUNTER_LABEL_TO_PROFILE.keys()),
            width=12,
        ).grid(row=1, column=7, padx=(4, 0), pady=(4, 0), sticky="w")
        ttk.Label(combat_opts, text="Profile").grid(row=1, column=8, sticky="w", pady=(4, 0), padx=(10, 0))
        ttk.Combobox(
            combat_opts,
            textvariable=self.combat_tuning_profile_var,
            state="readonly",
            values=(COMBAT_PROFILE_DB_PARITY,),
            width=10,
        ).grid(row=1, column=9, padx=(4, 0), pady=(4, 0), sticky="w")

        ttk.Button(frame, text="Start", command=self.start, style="Start.TButton").grid(row=4, column=0, padx=(0, 4), pady=(6, 0), sticky="we")
        ttk.Button(frame, text="Stop", command=self.stop, style="Danger.TButton").grid(row=4, column=1, padx=4, pady=(6, 0), sticky="we")
        ttk.Button(frame, text="Clear output", command=self.clear_output).grid(row=4, column=2, padx=(4, 0), pady=(6, 0), sticky="we")

        ttk.Label(frame, textvariable=self.status_var).grid(row=5, column=0, columnspan=3, sticky="w", pady=(8, 0))
        frame.columnconfigure(1, weight=1)
        self._apply_combat_preset(COMBAT_PRESET_DEFAULT)

    def start(self) -> None:
        self.activity_var.set("combat")
        super().start()


def main(argv: Optional[list[str]] = None) -> None:
    parser = argparse.ArgumentParser(description="RuneLite combat planner GUI")
    parser.add_argument("--auto-start", action="store_true", help="Start planner automatically on GUI launch")
    args = parser.parse_args(argv)

    root = tk.Tk()
    root.title("RuneLite Combat Planner")
    root.geometry("1000x680")
    CombatRuntimeGUI(root, auto_start=bool(args.auto_start))
    root.mainloop()


if __name__ == "__main__":  # pragma: no cover
    main()

