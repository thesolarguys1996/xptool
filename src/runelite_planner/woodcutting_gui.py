from __future__ import annotations

import argparse
import tkinter as tk
from tkinter import ttk
from typing import Optional

from .activity_profiles import WOODCUTTING_PROFILE_DB_PARITY
from .bridge import CommandBusWriter
from .gui import RuntimeGUI
from .idle_metrics_tuning import resolve_idle_cadence_tuning_payload
from .models import RuntimeCommand


class WoodcuttingRuntimeGUI(RuntimeGUI):
    def __init__(self, root: tk.Tk, *, auto_start: bool = False) -> None:
        super().__init__(root, auto_start=auto_start)
        self.root.title("RuneLite Woodcutting Planner")
        self.activity_var.set("woodcutting")

    def _build_controls(self) -> None:
        frame = ttk.Frame(self.root, padding=8)
        frame.pack(side=tk.TOP, fill=tk.X)

        ttk.Checkbutton(frame, text="Follow", variable=self.follow_var).grid(row=0, column=0, sticky="w")
        ttk.Checkbutton(frame, text="Dry run", variable=self.dry_run_var).grid(row=0, column=1, sticky="w")
        ttk.Checkbutton(frame, text="Lock mouse to RuneLite", variable=self.lock_mouse_var).grid(row=0, column=2, sticky="w")

        self._build_break_controls(frame, row=1, columnspan=3)

        woodcut_opts = ttk.Frame(frame)
        woodcut_opts.grid(row=2, column=0, columnspan=3, sticky="we", pady=(4, 0))
        ttk.Label(woodcut_opts, text="Woodcut Mode").grid(row=0, column=0, sticky="w")
        ttk.Combobox(
            woodcut_opts,
            textvariable=self.woodcut_target_category_var,
            state="readonly",
            values=("SELECTED", "NORMAL", "OAK", "WILLOW"),
            width=12,
        ).grid(row=0, column=1, padx=(4, 10), sticky="w")
        ttk.Label(woodcut_opts, text="Area X").grid(row=0, column=2, sticky="w")
        ttk.Entry(woodcut_opts, textvariable=self.woodcut_target_x_var, width=8).grid(row=0, column=3, padx=(4, 10))
        ttk.Label(woodcut_opts, text="Y").grid(row=0, column=4, sticky="w")
        ttk.Entry(woodcut_opts, textvariable=self.woodcut_target_y_var, width=8).grid(row=0, column=5, padx=(4, 10))
        ttk.Label(woodcut_opts, text="Range").grid(row=0, column=6, sticky="w")
        ttk.Entry(woodcut_opts, textvariable=self.woodcut_target_max_distance_var, width=6).grid(row=0, column=7, padx=(4, 0))
        ttk.Label(woodcut_opts, text="Profile").grid(row=0, column=8, sticky="w", padx=(10, 0))
        ttk.Combobox(
            woodcut_opts,
            textvariable=self.woodcut_tuning_profile_var,
            state="readonly",
            values=(WOODCUTTING_PROFILE_DB_PARITY,),
            width=10,
        ).grid(row=0, column=9, padx=(4, 0), sticky="w")

        ttk.Button(frame, text="Start", command=self.start, style="Start.TButton").grid(row=4, column=0, padx=(0, 4), pady=(6, 0), sticky="we")
        ttk.Button(frame, text="Stop", command=self.stop, style="Danger.TButton").grid(row=4, column=1, padx=4, pady=(6, 0), sticky="we")
        ttk.Button(frame, text="Clear output", command=self.clear_output).grid(row=4, column=2, padx=(4, 0), pady=(6, 0), sticky="we")
        ttk.Button(frame, text="Send LOGIN_START_TEST", command=self._send_manual_login_start_test).grid(
            row=5,
            column=0,
            columnspan=3,
            padx=0,
            pady=(6, 0),
            sticky="we",
        )

        ttk.Label(frame, textvariable=self.status_var).grid(row=6, column=0, columnspan=3, sticky="w", pady=(8, 0))

        frame.columnconfigure(1, weight=1)

    def start(self) -> None:
        self.activity_var.set("woodcutting")
        super().start()

    def _send_manual_login_start_test(self) -> None:
        self._apply_backend_runtime_defaults()
        command_out = self.command_out_var.get().strip()
        if not command_out:
            self._append_output("[ERROR] backend command out path is empty\n")
            return
        idle_cadence_tuning = self._resolve_manual_login_idle_cadence_tuning()
        if not idle_cadence_tuning:
            self._append_output(
                "[ERROR] manual LOGIN_START_TEST blocked: idle cadence tuning is missing "
                "(manual metrics signal required)\n"
            )
            return

        writer = CommandBusWriter(command_out)
        try:
            command_id = writer.write_command(
                RuntimeCommand(
                    command_type="LOGIN_START_TEST",
                    payload={
                        "prefilled": True,
                        "plannerTag": "break_scheduler",
                        "idleCadenceTuning": idle_cadence_tuning,
                    },
                    reason="woodcutting_gui_manual_login_start_test",
                    source="woodcutting_gui",
                )
            )
        finally:
            writer.close()

        self._append_output(
            f"[INFO] manual LOGIN_START_TEST sent command_id={command_id} "
            "(idleCadenceTuning attached)\n"
        )

    def _resolve_manual_login_idle_cadence_tuning(self) -> dict[str, int] | None:
        active_strategy = getattr(self, "_active_strategy", None)
        if active_strategy is not None:
            runtime_payload = getattr(active_strategy, "_idle_cadence_tuning_payload", None)
            if isinstance(runtime_payload, dict) and runtime_payload:
                return {
                    str(key): int(value)
                    for key, value in runtime_payload.items()
                    if self._is_int_like(value)
                }
        derived_payload = resolve_idle_cadence_tuning_payload(
            activity_key="woodcutting",
            user_key="default_user",
        )
        if isinstance(derived_payload, dict) and derived_payload:
            return {
                str(key): int(value)
                for key, value in derived_payload.items()
                if self._is_int_like(value)
            }
        return None

    @staticmethod
    def _is_int_like(value: object) -> bool:
        try:
            int(value)
            return True
        except (TypeError, ValueError):
            return False


def main(argv: Optional[list[str]] = None) -> None:
    parser = argparse.ArgumentParser(description="RuneLite woodcutting planner GUI")
    parser.add_argument("--auto-start", action="store_true", help="Start planner automatically on GUI launch")
    args = parser.parse_args(argv)

    root = tk.Tk()
    root.title("RuneLite Woodcutting Planner")
    root.geometry("1000x680")
    WoodcuttingRuntimeGUI(root, auto_start=bool(args.auto_start))
    root.mainloop()


if __name__ == "__main__":  # pragma: no cover
    main()

