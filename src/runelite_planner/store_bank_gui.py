from __future__ import annotations

import argparse
import threading
import tkinter as tk
from tkinter import ttk
from typing import Optional

from .bridge import CommandBusWriter
from .gui import RuntimeGUI
from .models import RuntimeCommand
from .runner import BreakSettings, RuntimeRunner
from .store_bank import (
    MODE_THESSALIA_SKIRT_BUYER,
    MODE_WALK_ONLY,
    STORE_BANK_TUNING_PROFILE_DB_PARITY,
    StoreBankConfig,
    StoreBankStrategy,
)

PRESET_ACTIVITIES: dict[str, dict[str, int | str] | None] = {
    "Custom": None,
    "Thessalia's skirt buyer": {
        "store_world_x": 3206,
        "store_world_y": 3416,
        "store_plane": 0,
        "store_area_radius_tiles": 3,
        "bank_world_x": 3182,
        "bank_world_y": 3440,
        "bank_plane": 0,
        "bank_area_radius_tiles": 3,
        "arrive_distance_tiles": 1,
        "waypoint_step_tiles": 6,
        "endpoint_hold_ticks": 2,
        "start_leg": "TO_STORE",
        "mode": MODE_THESSALIA_SKIRT_BUYER,
        "shop_npc_name_contains": "thessalia",
        "pink_skirt_item_id": 1013,
        "blue_skirt_item_id": 1011,
        "shop_buy_quantity": 5,
        "inventory_full_slots": 28,
        "hop_worlds": "301,308,316,326,335,383,394,417,425",
        "tuning_profile": STORE_BANK_TUNING_PROFILE_DB_PARITY,
    },
}


class StoreBankRuntimeGUI(RuntimeGUI):
    def __init__(self, root: tk.Tk, *, auto_start: bool = False) -> None:
        self.activity_preset_var = tk.StringVar(master=root, value="Thessalia's skirt buyer")
        self.store_world_x_var = tk.StringVar(master=root, value="")
        self.store_world_y_var = tk.StringVar(master=root, value="")
        self.store_plane_var = tk.StringVar(master=root, value="0")
        self.store_area_radius_tiles_var = tk.StringVar(master=root, value="3")
        self.bank_world_x_var = tk.StringVar(master=root, value="")
        self.bank_world_y_var = tk.StringVar(master=root, value="")
        self.bank_plane_var = tk.StringVar(master=root, value="0")
        self.bank_area_radius_tiles_var = tk.StringVar(master=root, value="3")
        self.arrive_distance_tiles_var = tk.StringVar(master=root, value="1")
        self.waypoint_step_tiles_var = tk.StringVar(master=root, value="6")
        self.endpoint_hold_ticks_var = tk.StringVar(master=root, value="2")
        self.start_leg_var = tk.StringVar(master=root, value="TO_BANK")
        self.mode_var = tk.StringVar(master=root, value=MODE_WALK_ONLY)
        self.shop_npc_name_contains_var = tk.StringVar(master=root, value="thessalia")
        self.pink_skirt_item_id_var = tk.StringVar(master=root, value="1013")
        self.blue_skirt_item_id_var = tk.StringVar(master=root, value="1011")
        self.shop_buy_quantity_var = tk.StringVar(master=root, value="5")
        self.inventory_full_slots_var = tk.StringVar(master=root, value="28")
        self.hop_worlds_var = tk.StringVar(master=root, value="")
        self.tuning_profile_var = tk.StringVar(master=root, value=STORE_BANK_TUNING_PROFILE_DB_PARITY)
        self.hop_test_world_var = tk.StringVar(master=root, value="")
        self.phase_var = tk.StringVar(master=root, value="Phase: idle")
        self._last_phase_label = "IDLE"
        super().__init__(root, auto_start=auto_start)
        self.root.title("RuneLite Store-Bank Walker")
        self._apply_selected_preset(force=True)

    def _build_controls(self) -> None:
        frame = ttk.Frame(self.root, padding=8)
        frame.pack(side=tk.TOP, fill=tk.X)

        route_opts = ttk.LabelFrame(frame, text="Route")
        route_opts.grid(row=0, column=0, columnspan=3, sticky="we", pady=(4, 0))

        ttk.Label(route_opts, text="Activity").grid(row=0, column=0, sticky="w", padx=(8, 4), pady=(6, 2))
        activity_combo = ttk.Combobox(
            route_opts,
            textvariable=self.activity_preset_var,
            state="readonly",
            values=tuple(PRESET_ACTIVITIES.keys()),
            width=26,
        )
        activity_combo.grid(row=0, column=1, columnspan=3, padx=(0, 10), pady=(6, 2), sticky="w")
        activity_combo.bind("<<ComboboxSelected>>", self._on_activity_preset_changed)

        ttk.Label(route_opts, text="Store X").grid(row=1, column=0, sticky="w", padx=(8, 4), pady=(6, 2))
        ttk.Entry(route_opts, textvariable=self.store_world_x_var, width=8).grid(row=1, column=1, padx=(0, 10), pady=(6, 2))
        ttk.Label(route_opts, text="Store Y").grid(row=1, column=2, sticky="w", pady=(6, 2))
        ttk.Entry(route_opts, textvariable=self.store_world_y_var, width=8).grid(row=1, column=3, padx=(4, 10), pady=(6, 2))
        ttk.Label(route_opts, text="Store Plane").grid(row=1, column=4, sticky="w", pady=(6, 2))
        ttk.Entry(route_opts, textvariable=self.store_plane_var, width=6).grid(row=1, column=5, padx=(4, 8), pady=(6, 2))

        ttk.Label(route_opts, text="Bank X").grid(row=2, column=0, sticky="w", padx=(8, 4), pady=2)
        ttk.Entry(route_opts, textvariable=self.bank_world_x_var, width=8).grid(row=2, column=1, padx=(0, 10), pady=2)
        ttk.Label(route_opts, text="Bank Y").grid(row=2, column=2, sticky="w", pady=2)
        ttk.Entry(route_opts, textvariable=self.bank_world_y_var, width=8).grid(row=2, column=3, padx=(4, 10), pady=2)
        ttk.Label(route_opts, text="Bank Plane").grid(row=2, column=4, sticky="w", pady=2)
        ttk.Entry(route_opts, textvariable=self.bank_plane_var, width=6).grid(row=2, column=5, padx=(4, 8), pady=2)

        ttk.Label(route_opts, text="Store Radius").grid(row=3, column=0, sticky="w", padx=(8, 4), pady=2)
        ttk.Entry(route_opts, textvariable=self.store_area_radius_tiles_var, width=8).grid(
            row=3, column=1, padx=(0, 10), pady=2
        )
        ttk.Label(route_opts, text="Bank Radius").grid(row=3, column=2, sticky="w", pady=2)
        ttk.Entry(route_opts, textvariable=self.bank_area_radius_tiles_var, width=8).grid(
            row=3, column=3, padx=(4, 10), pady=2
        )

        ttk.Label(route_opts, text="Arrive Distance").grid(row=4, column=0, sticky="w", padx=(8, 4), pady=(2, 6))
        ttk.Entry(route_opts, textvariable=self.arrive_distance_tiles_var, width=8).grid(
            row=4,
            column=1,
            padx=(0, 10),
            pady=(2, 6),
        )
        ttk.Label(route_opts, text="Step Tiles").grid(row=4, column=2, sticky="w", pady=(2, 6))
        ttk.Entry(route_opts, textvariable=self.waypoint_step_tiles_var, width=8).grid(
            row=4,
            column=3,
            padx=(4, 10),
            pady=(2, 6),
        )
        ttk.Label(route_opts, text="Hold Ticks").grid(row=4, column=4, sticky="w", pady=(2, 6))
        ttk.Entry(route_opts, textvariable=self.endpoint_hold_ticks_var, width=6).grid(
            row=4,
            column=5,
            padx=(4, 8),
            pady=(2, 6),
        )

        ttk.Label(route_opts, text="Start Leg").grid(row=5, column=0, sticky="w", padx=(8, 4), pady=(0, 6))
        ttk.Combobox(
            route_opts,
            textvariable=self.start_leg_var,
            state="readonly",
            values=("TO_BANK", "TO_STORE"),
            width=12,
        ).grid(row=5, column=1, padx=(0, 10), pady=(0, 6), sticky="w")

        ttk.Label(route_opts, text="Mode").grid(row=6, column=0, sticky="w", padx=(8, 4), pady=(0, 6))
        ttk.Combobox(
            route_opts,
            textvariable=self.mode_var,
            state="readonly",
            values=(MODE_WALK_ONLY, MODE_THESSALIA_SKIRT_BUYER),
            width=24,
        ).grid(row=6, column=1, columnspan=2, padx=(0, 10), pady=(0, 6), sticky="w")

        ttk.Label(route_opts, text="Hop Worlds CSV").grid(row=6, column=3, sticky="w", pady=(0, 6))
        ttk.Entry(route_opts, textvariable=self.hop_worlds_var, width=28).grid(
            row=6, column=4, columnspan=2, padx=(4, 8), pady=(0, 6), sticky="we"
        )
        ttk.Label(route_opts, text="Tuning Profile").grid(row=7, column=0, sticky="w", padx=(8, 4), pady=(0, 6))
        ttk.Combobox(
            route_opts,
            textvariable=self.tuning_profile_var,
            state="readonly",
            values=(STORE_BANK_TUNING_PROFILE_DB_PARITY,),
            width=14,
        ).grid(row=7, column=1, padx=(0, 10), pady=(0, 6), sticky="w")

        ttk.Checkbutton(frame, text="Follow", variable=self.follow_var).grid(row=1, column=0, sticky="w")
        ttk.Checkbutton(frame, text="Dry run", variable=self.dry_run_var).grid(row=1, column=1, sticky="w")
        ttk.Checkbutton(frame, text="Lock mouse to RuneLite", variable=self.lock_mouse_var).grid(row=1, column=2, sticky="w")

        self._build_break_controls(frame, row=2, columnspan=3)

        ttk.Button(frame, text="Start", command=self.start, style="Start.TButton").grid(row=4, column=0, padx=(0, 4), pady=(6, 0), sticky="we")
        ttk.Button(frame, text="Stop", command=self.stop, style="Danger.TButton").grid(row=4, column=1, padx=4, pady=(6, 0), sticky="we")
        ttk.Button(frame, text="Clear output", command=self.clear_output).grid(row=4, column=2, padx=(4, 0), pady=(6, 0), sticky="we")
        test_row = ttk.LabelFrame(frame, text="Manual Tests")
        test_row.grid(row=5, column=0, columnspan=3, sticky="we", pady=(6, 0))
        ttk.Label(test_row, text="Hop target world").grid(row=0, column=0, padx=(8, 4), pady=6, sticky="w")
        ttk.Entry(test_row, textvariable=self.hop_test_world_var, width=10).grid(
            row=0, column=1, padx=(0, 8), pady=6, sticky="w"
        )
        ttk.Button(test_row, text="Send World Hop Test", command=self._send_world_hop_test).grid(
            row=0,
            column=2,
            padx=(0, 8),
            pady=6,
            sticky="we",
        )
        test_row.columnconfigure(2, weight=1)

        ttk.Label(frame, textvariable=self.status_var).grid(row=6, column=0, columnspan=3, sticky="w", pady=(8, 0))
        ttk.Label(frame, textvariable=self.phase_var).grid(row=7, column=0, columnspan=3, sticky="w", pady=(2, 0))
        frame.columnconfigure(1, weight=1)

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

        try:
            store_x = int(self.store_world_x_var.get().strip() or "-1")
            store_y = int(self.store_world_y_var.get().strip() or "-1")
            store_plane = max(0, int(self.store_plane_var.get().strip() or "0"))
            store_area_radius_tiles = max(0, int(self.store_area_radius_tiles_var.get().strip() or "0"))
            bank_x = int(self.bank_world_x_var.get().strip() or "-1")
            bank_y = int(self.bank_world_y_var.get().strip() or "-1")
            bank_plane = max(0, int(self.bank_plane_var.get().strip() or "0"))
            bank_area_radius_tiles = max(0, int(self.bank_area_radius_tiles_var.get().strip() or "0"))
            arrive_distance_tiles = max(0, int(self.arrive_distance_tiles_var.get().strip() or "1"))
            waypoint_step_tiles = max(1, int(self.waypoint_step_tiles_var.get().strip() or "6"))
            endpoint_hold_ticks = max(0, int(self.endpoint_hold_ticks_var.get().strip() or "2"))
            pink_skirt_item_id = max(1, int(self.pink_skirt_item_id_var.get().strip() or "1013"))
            blue_skirt_item_id = max(1, int(self.blue_skirt_item_id_var.get().strip() or "1011"))
            shop_buy_quantity = max(1, int(self.shop_buy_quantity_var.get().strip() or "5"))
            inventory_full_slots = max(1, min(28, int(self.inventory_full_slots_var.get().strip() or "28")))
        except ValueError:
            self._append_output("[ERROR] Store/bank coordinates, radii, and route fields must be integers\n")
            return

        if store_x <= 0 or store_y <= 0 or bank_x <= 0 or bank_y <= 0:
            self._append_output("[ERROR] Store and bank X/Y must be > 0\n")
            return

        tuning_profile = str(self.tuning_profile_var.get().strip() or STORE_BANK_TUNING_PROFILE_DB_PARITY).upper()
        if tuning_profile != STORE_BANK_TUNING_PROFILE_DB_PARITY:
            self._append_output("[ERROR] Store-bank tuning profile must be DB_PARITY\n")
            return

        self._append_output("[INFO] selected activity=store_bank\n")

        strategy = StoreBankStrategy(
            cfg=StoreBankConfig(
                store_world_x=store_x,
                store_world_y=store_y,
                store_plane=store_plane,
                store_area_radius_tiles=store_area_radius_tiles,
                bank_world_x=bank_x,
                bank_world_y=bank_y,
                bank_plane=bank_plane,
                bank_area_radius_tiles=bank_area_radius_tiles,
                arrive_distance_tiles=arrive_distance_tiles,
                waypoint_step_tiles=waypoint_step_tiles,
                endpoint_hold_ticks=endpoint_hold_ticks,
                start_leg=self.start_leg_var.get().strip().upper() or "TO_BANK",
                mode=self.mode_var.get().strip().upper() or MODE_WALK_ONLY,
                shop_npc_name_contains=self.shop_npc_name_contains_var.get().strip() or "thessalia",
                pink_skirt_item_id=pink_skirt_item_id,
                blue_skirt_item_id=blue_skirt_item_id,
                shop_buy_quantity=shop_buy_quantity,
                inventory_full_slots=inventory_full_slots,
                hop_worlds=tuple(
                    int(token.strip())
                    for token in self.hop_worlds_var.get().split(",")
                    if token.strip().isdigit() and int(token.strip()) > 0
                ),
                tuning_profile=tuning_profile,
            )
        )

        dry_run = bool(self.dry_run_var.get())
        follow = bool(self.follow_var.get())
        if bool(self.breaks_enabled_var.get()):
            self._append_output(
                "[INFO] Breakmanager is disabled for store_bank to keep shop/world-hop flow uninterrupted\n"
            )
        break_settings = BreakSettings(enabled=False)

        if dry_run or not command_out:
            writer: Optional[CommandBusWriter] = None
        else:
            writer = CommandBusWriter(command_out)

        self._stop_event = threading.Event()
        self._active_strategy = strategy
        self.status_var.set("Status: running activity=store_bank")
        self._set_phase("STARTING")

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
                if writer is not None:
                    writer.close()
                if self._stop_event is not None:
                    self._stop_event.set()
                self._active_runner = None

        self._runner_thread = threading.Thread(target=run_wrapper, daemon=True)
        self._runner_thread.start()
        if self.lock_mouse_var.get():
            self._mouse_lock.start()

    def _refresh_status(self) -> None:
        running = self._runner_thread is not None and self._runner_thread.is_alive()
        if running:
            self.status_var.set("Status: running activity=store_bank")
            self.phase_var.set(f"Phase: {self._last_phase_label}")
            return
        self.status_var.set("Status: idle")
        self.phase_var.set("Phase: idle")

    def _runtime_callback(self, message: str) -> None:
        self._update_phase_from_runtime_message(message)
        super()._runtime_callback(message)

    def _update_phase_from_runtime_message(self, message: str) -> None:
        text = (message or "").strip().lower()
        if not text:
            return
        if "store_bank_walk_waypoint" in text:
            self._set_phase("WALK")
            return
        if "store_bank_open_shop_door" in text or "scene_object_action" in text:
            self._set_phase("DOOR")
            return
        if "store_bank_trade_shop_npc" in text or "store_bank_shop_buy_item" in text:
            self._set_phase("SHOP")
            return
        if "store_bank_hop_for_shop_restock" in text:
            self._set_phase("HOP")
            return
        if (
            "store_bank_open_bank" in text
            or "store_bank_deposit_" in text
            or "store_bank_close_bank" in text
        ):
            self._set_phase("BANK")
            return
        if "[break]" in text:
            self._set_phase("BREAK")
            return
        if "runner finished" in text:
            self._set_phase("IDLE")

    def _set_phase(self, label: str) -> None:
        text = str(label or "").strip().upper() or "IDLE"
        self._last_phase_label = text

    def _on_activity_preset_changed(self, _event=None) -> None:
        self._apply_selected_preset(force=False)

    def _apply_selected_preset(self, *, force: bool) -> None:
        preset_name = self.activity_preset_var.get().strip() or "Custom"
        preset = PRESET_ACTIVITIES.get(preset_name)
        if preset is None:
            if force and preset_name not in PRESET_ACTIVITIES:
                self.activity_preset_var.set("Custom")
            return

        self.store_world_x_var.set(str(int(preset.get("store_world_x", -1))))
        self.store_world_y_var.set(str(int(preset.get("store_world_y", -1))))
        self.store_plane_var.set(str(max(0, int(preset.get("store_plane", 0)))))
        self.store_area_radius_tiles_var.set(str(max(0, int(preset.get("store_area_radius_tiles", 0)))))
        self.bank_world_x_var.set(str(int(preset.get("bank_world_x", -1))))
        self.bank_world_y_var.set(str(int(preset.get("bank_world_y", -1))))
        self.bank_plane_var.set(str(max(0, int(preset.get("bank_plane", 0)))))
        self.bank_area_radius_tiles_var.set(str(max(0, int(preset.get("bank_area_radius_tiles", 0)))))
        self.arrive_distance_tiles_var.set(str(max(0, int(preset.get("arrive_distance_tiles", 1)))))
        self.waypoint_step_tiles_var.set(str(max(1, int(preset.get("waypoint_step_tiles", 6)))))
        self.endpoint_hold_ticks_var.set(str(max(0, int(preset.get("endpoint_hold_ticks", 2)))))
        self.start_leg_var.set(str(preset.get("start_leg", "TO_BANK")).strip().upper() or "TO_BANK")
        self.mode_var.set(str(preset.get("mode", MODE_WALK_ONLY)).strip().upper() or MODE_WALK_ONLY)
        self.shop_npc_name_contains_var.set(str(preset.get("shop_npc_name_contains", "thessalia")).strip() or "thessalia")
        self.pink_skirt_item_id_var.set(str(max(1, int(preset.get("pink_skirt_item_id", 1013)))))
        self.blue_skirt_item_id_var.set(str(max(1, int(preset.get("blue_skirt_item_id", 1011)))))
        self.shop_buy_quantity_var.set(str(max(1, int(preset.get("shop_buy_quantity", 5)))))
        self.inventory_full_slots_var.set(str(max(1, min(28, int(preset.get("inventory_full_slots", 28))))))
        self.hop_worlds_var.set(str(preset.get("hop_worlds", "")).strip())
        self.tuning_profile_var.set(
            str(preset.get("tuning_profile", STORE_BANK_TUNING_PROFILE_DB_PARITY)).strip().upper()
            or STORE_BANK_TUNING_PROFILE_DB_PARITY
        )

    def _resolve_hop_test_target_world(self) -> int:
        manual = self.hop_test_world_var.get().strip()
        if manual:
            try:
                world = int(manual)
                if world > 0:
                    return world
            except ValueError:
                return -1
            return -1
        for token in self.hop_worlds_var.get().split(","):
            token = token.strip()
            if not token:
                continue
            if token.isdigit():
                world = int(token)
                if world > 0:
                    return world
        return -1

    def _send_world_hop_test(self) -> None:
        self._apply_backend_runtime_defaults()
        command_out = self.command_out_var.get().strip()
        if not command_out:
            self._append_output("[ERROR] backend command out path is empty\n")
            return

        target_world = self._resolve_hop_test_target_world()
        if target_world <= 0:
            self._append_output(
                "[ERROR] Set Hop target world or provide a valid Hop Worlds CSV before sending world hop test\n"
            )
            return

        writer = CommandBusWriter(command_out, source="store_bank.gui")
        try:
            command = RuntimeCommand(
                command_type="WORLD_HOP_SAFE",
                payload={
                    "interactionKind": "WORLD_HOP",
                    "plannerTag": "store_bank",
                    "targetWorld": int(target_world),
                },
                reason="store_bank_gui_world_hop_test",
                source="store_bank",
            )
            command_id = writer.write_command(command)
        finally:
            writer.close()

        self._set_phase("HOP_TEST")
        self._append_output(
            "[INFO] Sent world hop test command "
            f"id={command_id} targetWorld={target_world}\n"
        )


def main(argv: Optional[list[str]] = None) -> None:
    parser = argparse.ArgumentParser(description="RuneLite store-bank walker GUI")
    parser.add_argument("--auto-start", action="store_true", help="Start planner automatically on GUI launch")
    args = parser.parse_args(argv)

    root = tk.Tk()
    root.title("RuneLite Store-Bank Walker")
    root.geometry("1000x680")
    StoreBankRuntimeGUI(root, auto_start=bool(args.auto_start))
    root.mainloop()


if __name__ == "__main__":  # pragma: no cover
    main()
