from __future__ import annotations

import argparse
import tkinter as tk
from tkinter import ttk
from typing import Optional

from .activity_profiles import MINING_PROFILE_DB_PARITY
from .gui import RuntimeGUI


class MiningRuntimeGUI(RuntimeGUI):
    def __init__(self, root: tk.Tk, *, auto_start: bool = False) -> None:
        super().__init__(root, auto_start=auto_start)
        self.root.title("RuneLite Mining Planner")
        self.activity_var.set("mining")

    def _build_controls(self) -> None:
        frame = ttk.Frame(self.root, padding=8)
        frame.pack(side=tk.TOP, fill=tk.X)

        ttk.Checkbutton(frame, text="Follow", variable=self.follow_var).grid(row=0, column=0, sticky="w")
        ttk.Checkbutton(frame, text="Dry run", variable=self.dry_run_var).grid(row=0, column=1, sticky="w")
        ttk.Checkbutton(frame, text="Lock mouse to RuneLite", variable=self.lock_mouse_var).grid(row=0, column=2, sticky="w")
        ttk.Label(frame, text="Profile").grid(row=0, column=3, sticky="e", padx=(10, 4))
        ttk.Combobox(
            frame,
            textvariable=self.mining_tuning_profile_var,
            state="readonly",
            values=(MINING_PROFILE_DB_PARITY,),
            width=10,
        ).grid(row=0, column=4, sticky="w")

        self._build_break_controls(frame, row=1, columnspan=5)

        ttk.Button(frame, text="Start", command=self.start, style="Start.TButton").grid(row=3, column=0, padx=(0, 4), pady=(6, 0), sticky="we")
        ttk.Button(frame, text="Stop", command=self.stop, style="Danger.TButton").grid(row=3, column=1, padx=4, pady=(6, 0), sticky="we")
        ttk.Button(frame, text="Clear output", command=self.clear_output).grid(row=3, column=2, padx=(4, 0), pady=(6, 0), sticky="we")

        ttk.Label(frame, textvariable=self.status_var).grid(row=4, column=0, columnspan=5, sticky="w", pady=(8, 0))
        frame.columnconfigure(1, weight=1)

    def start(self) -> None:
        self.activity_var.set("mining")
        super().start()


def main(argv: Optional[list[str]] = None) -> None:
    parser = argparse.ArgumentParser(description="RuneLite mining planner GUI")
    parser.add_argument("--auto-start", action="store_true", help="Start planner automatically on GUI launch")
    args = parser.parse_args(argv)

    root = tk.Tk()
    root.title("RuneLite Mining Planner")
    root.geometry("1000x680")
    MiningRuntimeGUI(root, auto_start=bool(args.auto_start))
    root.mainloop()


if __name__ == "__main__":  # pragma: no cover
    main()

