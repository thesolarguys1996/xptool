from __future__ import annotations

import argparse
import ctypes
import ctypes.wintypes
import queue
import subprocess
import sys
import threading
import tkinter as tk
from tkinter import filedialog, ttk
from typing import Optional

from .activity_profiles import (
    COMBAT_PROFILE_DB_PARITY,
    MINING_PROFILE_DB_PARITY,
    WOODCUTTING_PROFILE_DB_PARITY,
)
from .activities import (
    ActivityBuildError,
    GuiActivityInputs,
    build_activity_strategy,
    default_profile_for_activity,
    supported_gui_activities,
)
from .activities.combat_presets import (
    COMBAT_ENCOUNTER_DEFAULT_LABEL,
    COMBAT_ENCOUNTER_LABEL_TO_PROFILE,
    COMBAT_ENCOUNTER_PROFILE_TO_LABEL,
    COMBAT_PRESET_DEFAULT,
    COMBAT_PRESETS,
)
from .bridge import CommandBusWriter
from .paths import default_command_out_path, default_log_path
from .runner import RuntimeRunner, BreakSettings


WM_HOTKEY = 0x0312
WM_QUIT = 0x0012
MOD_NOREPEAT = 0x4000
PM_REMOVE = 0x0001
VK_F8 = 0x77

class RuneLiteMouseLock:
    """
    Windows-only cursor clipper that confines the mouse to the RuneLite client area.
    """

    def __init__(self, logger=None) -> None:
        self.logger = logger
        self._thread: Optional[threading.Thread] = None
        self._running = threading.Event()

    def start(self) -> None:
        if sys.platform != "win32":
            if self.logger is not None:
                self.logger("[WARN] mouse lock is only supported on Windows")
            return
        if self._thread is not None and self._thread.is_alive():
            return
        self._running.set()
        self._thread = threading.Thread(target=self._run, daemon=True)
        self._thread.start()
        if self.logger is not None:
            self.logger("[INFO] mouse lock enabled")

    def stop(self) -> None:
        if sys.platform != "win32":
            return
        self._running.clear()
        self._release_clip()
        if self.logger is not None:
            self.logger("[INFO] mouse lock disabled")

    def _run(self) -> None:
        while self._running.is_set():
            rect = self._find_runelite_client_rect()
            if rect is not None:
                self._apply_clip(rect)
            else:
                # If RuneLite is not currently detectable, do not keep a stale
                # cursor clip region active.
                self._release_clip()
            ctypes.windll.kernel32.Sleep(120)
        self._release_clip()

    def _find_runelite_client_rect(self):
        user32 = ctypes.windll.user32
        hwnd = user32.GetForegroundWindow()
        if not hwnd:
            hwnd = self._find_any_runelite_window()
        if not hwnd:
            return None
        if not self._is_runelite_window(hwnd):
            alt = self._find_any_runelite_window()
            if alt:
                hwnd = alt
            else:
                return None
        client = ctypes.wintypes.RECT()
        if not user32.GetClientRect(hwnd, ctypes.byref(client)):
            return None
        tl = ctypes.wintypes.POINT(client.left, client.top)
        br = ctypes.wintypes.POINT(client.right, client.bottom)
        if not user32.ClientToScreen(hwnd, ctypes.byref(tl)):
            return None
        if not user32.ClientToScreen(hwnd, ctypes.byref(br)):
            return None
        if br.x - tl.x < 100 or br.y - tl.y < 100:
            return None
        out = ctypes.wintypes.RECT()
        out.left = tl.x + 1
        out.top = tl.y + 1
        out.right = br.x - 1
        out.bottom = br.y - 1
        return out

    def _find_any_runelite_window(self):
        user32 = ctypes.windll.user32
        found = {"hwnd": None}

        @ctypes.WINFUNCTYPE(ctypes.c_bool, ctypes.wintypes.HWND, ctypes.wintypes.LPARAM)
        def enum_proc(hwnd, _lparam):
            if not user32.IsWindowVisible(hwnd):
                return True
            if self._is_runelite_window(hwnd):
                found["hwnd"] = hwnd
                return False
            return True

        user32.EnumWindows(enum_proc, 0)
        return found["hwnd"]

    def _is_runelite_window(self, hwnd) -> bool:
        user32 = ctypes.windll.user32
        length = user32.GetWindowTextLengthW(hwnd)
        if length <= 0:
            return False
        buf = ctypes.create_unicode_buffer(length + 1)
        user32.GetWindowTextW(hwnd, buf, len(buf))
        title = (buf.value or "").strip().lower()
        # Exclude this planner window and only accept likely RuneLite client titles.
        if "runelite planner" in title:
            return False
        return (
            title == "runelite"
            or title.startswith("runelite -")
            or title.startswith("runelite (")
        )

    def _apply_clip(self, rect) -> None:
        ctypes.windll.user32.ClipCursor(ctypes.byref(rect))

    def _release_clip(self) -> None:
        ctypes.windll.user32.ClipCursor(None)


class GlobalHotkeyListener:
    def __init__(self, hotkey_id: int, callback, logger=None) -> None:
        self.hotkey_id = hotkey_id
        self.callback = callback
        self.logger = logger
        self._thread: Optional[threading.Thread] = None
        self._running = threading.Event()
        self._registered = False
        self._thread_id = 0

    def start(self) -> None:
        if sys.platform != "win32":
            return
        if self._thread is not None and self._thread.is_alive():
            return
        self._running.set()
        self._thread = threading.Thread(target=self._run, daemon=True)
        self._thread.start()

    def stop(self) -> None:
        if sys.platform != "win32":
            return
        self._running.clear()
        try:
            user32 = ctypes.windll.user32
            if self._thread_id:
                user32.PostThreadMessageW(self._thread_id, WM_QUIT, 0, 0)
            user32.UnregisterHotKey(None, self.hotkey_id)
        except Exception:
            pass
        self._registered = False
        self._thread_id = 0

    def _run(self) -> None:
        user32 = ctypes.windll.user32
        self._thread_id = ctypes.windll.kernel32.GetCurrentThreadId()
        try:
            # Try MOD_NOREPEAT first; fall back to plain F8 for broader compatibility.
            ok = user32.RegisterHotKey(None, self.hotkey_id, MOD_NOREPEAT, VK_F8)
            if not ok:
                ok = user32.RegisterHotKey(None, self.hotkey_id, 0, VK_F8)
            if not ok:
                if self.logger is not None:
                    self.logger("[WARN] global F8 hotkey registration failed")
                return
            self._registered = True
            if self.logger is not None:
                self.logger("[INFO] global F8 hotkey registered")

            msg = ctypes.wintypes.MSG()
            while self._running.is_set():
                has_msg = user32.PeekMessageW(ctypes.byref(msg), None, 0, 0, PM_REMOVE)
                if not has_msg:
                    ctypes.windll.kernel32.Sleep(50)
                    continue
                if msg.message == WM_QUIT:
                    break
                if msg.message == WM_HOTKEY and msg.wParam == self.hotkey_id:
                    try:
                        self.callback()
                    except Exception:
                        pass
                user32.TranslateMessage(ctypes.byref(msg))
                user32.DispatchMessageW(ctypes.byref(msg))
        except Exception:
            if self.logger is not None:
                self.logger("[WARN] global F8 hotkey listener crashed")
        finally:
            if self._registered:
                try:
                    user32.UnregisterHotKey(None, self.hotkey_id)
                except Exception:
                    pass
                self._registered = False
                if self.logger is not None:
                    self.logger("[INFO] global F8 hotkey unregistered")
            self._thread_id = 0


class RuntimeGUI:
    """
    Tkinter GUI front-end for RuntimeRunner.

    Features:
      - Runtime paths/cooldown are backend-managed defaults
      - Toggle follow / dry-run
      - Start/Stop buttons
      - Live log output panel
    """
    RUNTIME_POLL_INTERVAL_MS = 20

    def __init__(self, root: tk.Tk, *, auto_start: bool = False) -> None:
        self.root = root
        self.root.title("XPTool")
        self._apply_dark_theme()

        # --- state ---

        self.log_path_var = tk.StringVar(value=default_log_path())
        self.command_out_var = tk.StringVar(value=default_command_out_path())
        gui_activities = supported_gui_activities()
        gui_default_activity = gui_activities[0] if gui_activities else "woodcutting"
        self.activity_var = tk.StringVar(value=gui_default_activity)
        self.cooldown_var = tk.StringVar(value="0")
        self.follow_var = tk.BooleanVar(value=True)
        self.dry_run_var = tk.BooleanVar(value=False)
        self.lock_mouse_var = tk.BooleanVar(value=False)
        self.status_var = tk.StringVar(value="Status: idle")
        self.bank_probe_target_x_var = tk.StringVar(value="")
        self.bank_probe_target_y_var = tk.StringVar(value="")
        self.bank_probe_item_id_var = tk.StringVar(value="1511")
        self.bank_probe_withdraw_qty_var = tk.StringVar(value="1")
        self.bank_probe_deposit_qty_var = tk.StringVar(value="ALL")
        self.woodcut_target_category_var = tk.StringVar(value="SELECTED")
        self.woodcut_target_x_var = tk.StringVar(value="")
        self.woodcut_target_y_var = tk.StringVar(value="")
        self.woodcut_target_max_distance_var = tk.StringVar(value="12")
        self.woodcut_tuning_profile_var = tk.StringVar(value=WOODCUTTING_PROFILE_DB_PARITY)
        self.mining_tuning_profile_var = tk.StringVar(value=MINING_PROFILE_DB_PARITY)
        self.combat_preset_var = tk.StringVar(value=COMBAT_PRESET_DEFAULT)
        self.combat_npc_id_var = tk.StringVar(value="")
        self.combat_target_x_var = tk.StringVar(value="")
        self.combat_target_y_var = tk.StringVar(value="")
        self.combat_target_max_distance_var = tk.StringVar(value="8")
        self.combat_max_chase_distance_var = tk.StringVar(value="8")
        self.combat_eat_hp_var = tk.StringVar(value="")
        self.combat_eat_randomized_pct_var = tk.StringVar(value="0")
        self.combat_food_item_id_var = tk.StringVar(value="")
        self.combat_encounter_var = tk.StringVar(value=COMBAT_ENCOUNTER_DEFAULT_LABEL)
        self.combat_tuning_profile_var = tk.StringVar(value=COMBAT_PROFILE_DB_PARITY)
        self.breaks_enabled_var = tk.BooleanVar(value=False)
        self.break_bot_time_minutes_var = tk.StringVar(value="50")
        self.break_time_minutes_var = tk.StringVar(value="30")
        self.break_randomized_pct_var = tk.StringVar(value="15")

        self._runner_thread: Optional[threading.Thread] = None
        self._stop_event: Optional[threading.Event] = None
        self._active_runner: Optional[RuntimeRunner] = None
        self._runtime_queue: "queue.Queue[str]" = queue.Queue()
        self._hotkey_stop_requested = threading.Event()
        self._hotkey_listener = GlobalHotkeyListener(0xB008, self._request_hotkey_stop, self._runtime_callback)
        self._mouse_lock = RuneLiteMouseLock(self._runtime_callback)
        self._f8_was_down = False
        self._active_strategy = None
        self._apply_backend_runtime_defaults()

        # --- layout ---

        self._build_controls()
        self._build_output()
        self.root.bind_all("<F8>", self._on_hotkey_stop_local)
        self.root.protocol("WM_DELETE_WINDOW", self._on_close)
        self._hotkey_listener.start()

        # Poll queue for runtime logs.
        self._schedule_poll()
        if auto_start:
            self.root.after(250, self.start)

    def _apply_dark_theme(self) -> None:
        bg = "#111418"
        panel_bg = "#181d23"
        input_bg = "#20262e"
        border = "#2a333d"
        fg = "#e6edf3"
        muted_fg = "#b7c2cf"
        accent = "#2e8bff"
        button_bg = "#1c2027"
        button_hover = "#272c35"
        button_pressed = "#222831"
        button_border = "#353c47"
        button_text = "#b8d6ff"
        button_text_disabled = "#7b8593"
        text_bg = "#0f141b"

        self.root.configure(bg=bg)
        style = ttk.Style(self.root)
        try:
            style.theme_use("clam")
        except tk.TclError:
            pass

        style.configure(".", background=panel_bg, foreground=fg)
        style.configure("TFrame", background=panel_bg)
        style.configure("TLabel", background=panel_bg, foreground=fg)
        style.configure(
            "TEntry",
            fieldbackground=input_bg,
            background=input_bg,
            foreground=fg,
            insertcolor=fg,
            bordercolor=border,
            lightcolor=border,
            darkcolor=border,
        )
        style.configure(
            "TCombobox",
            fieldbackground=input_bg,
            background=input_bg,
            foreground=fg,
            bordercolor=border,
            arrowcolor=fg,
        )
        style.map(
            "TCombobox",
            fieldbackground=[("readonly", input_bg)],
            selectbackground=[("readonly", input_bg)],
            selectforeground=[("readonly", fg)],
            foreground=[("readonly", fg)],
        )
        style.configure("TCheckbutton", background=panel_bg, foreground=muted_fg)
        style.map("TCheckbutton", foreground=[("active", fg), ("selected", fg)])

        style.configure(
            "TButton",
            background=button_bg,
            foreground=button_text,
            bordercolor=button_border,
            lightcolor=button_border,
            darkcolor=button_border,
            focusthickness=0,
            relief="flat",
            padding=(16, 3),
        )
        style.map(
            "TButton",
            background=[("active", button_hover), ("pressed", button_pressed)],
            foreground=[("active", "#d2e6ff"), ("pressed", "#d2e6ff"), ("disabled", button_text_disabled)],
        )

        style.configure(
            "Start.TButton",
            background="#1f8f65",
            foreground="#ecfff5",
            bordercolor="#0f5a42",
            lightcolor="#0f5a42",
            darkcolor="#0f5a42",
            focusthickness=0,
            relief="flat",
            padding=(16, 3),
        )
        style.map(
            "Start.TButton",
            background=[("active", "#2ba87a"), ("pressed", "#177550"), ("disabled", button_bg)],
            foreground=[("active", "#f3fff9"), ("pressed", "#f3fff9"), ("disabled", button_text_disabled)],
        )

        style.configure(
            "Danger.TButton",
            background=button_bg,
            foreground=button_text,
            bordercolor=button_border,
            lightcolor=button_border,
            darkcolor=button_border,
            focusthickness=0,
            relief="flat",
            padding=(16, 3),
        )
        style.map(
            "Danger.TButton",
            background=[("active", button_hover), ("pressed", button_pressed)],
            foreground=[("active", "#d2e6ff"), ("pressed", "#d2e6ff"), ("disabled", button_text_disabled)],
        )

        self.root.option_add("*TCombobox*Listbox*Background", input_bg)
        self.root.option_add("*TCombobox*Listbox*Foreground", fg)
        self.root.option_add("*TCombobox*Listbox*selectBackground", accent)
        self.root.option_add("*TCombobox*Listbox*selectForeground", fg)

    # ------------------------------------------------------------------ #
    # UI layout
    # ------------------------------------------------------------------ #

    def _build_controls(self) -> None:
        frame = ttk.Frame(self.root, padding=8)
        frame.pack(side=tk.TOP, fill=tk.X)

        # Activity
        ttk.Label(frame, text="Activity:").grid(row=0, column=0, sticky="w")
        activity_combo = ttk.Combobox(
            frame,
            textvariable=self.activity_var,
            state="readonly",
            values=supported_gui_activities(),
            width=20,
        )
        activity_combo.grid(row=0, column=1, sticky="w", padx=4)

        # Toggles
        follow_check = ttk.Checkbutton(frame, text="Follow", variable=self.follow_var)
        follow_check.grid(row=1, column=0, sticky="w")

        dry_check = ttk.Checkbutton(frame, text="Dry run", variable=self.dry_run_var)
        dry_check.grid(row=1, column=1, sticky="w")

        lock_mouse_check = ttk.Checkbutton(frame, text="Lock mouse to RuneLite", variable=self.lock_mouse_var)
        lock_mouse_check.grid(row=1, column=2, sticky="w")

        self._build_break_controls(frame, row=2, columnspan=3)

        woodcut_opts = ttk.Frame(frame)
        woodcut_opts.grid(row=3, column=0, columnspan=3, sticky="we", pady=(4, 0))
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

        mining_opts = ttk.Frame(frame)
        mining_opts.grid(row=4, column=0, columnspan=3, sticky="we", pady=(4, 0))
        ttk.Label(mining_opts, text="Mining Profile").grid(row=0, column=0, sticky="w")
        ttk.Combobox(
            mining_opts,
            textvariable=self.mining_tuning_profile_var,
            state="readonly",
            values=(MINING_PROFILE_DB_PARITY,),
            width=10,
        ).grid(row=0, column=1, padx=(4, 0), sticky="w")

        bank_probe_opts = ttk.Frame(frame)
        bank_probe_opts.grid(row=5, column=0, columnspan=3, sticky="we", pady=(4, 0))
        ttk.Label(bank_probe_opts, text="Bank Probe X").grid(row=0, column=0, sticky="w")
        ttk.Entry(bank_probe_opts, textvariable=self.bank_probe_target_x_var, width=8).grid(row=0, column=1, padx=(4, 10))
        ttk.Label(bank_probe_opts, text="Y").grid(row=0, column=2, sticky="w")
        ttk.Entry(bank_probe_opts, textvariable=self.bank_probe_target_y_var, width=8).grid(row=0, column=3, padx=(4, 10))
        ttk.Label(bank_probe_opts, text="Item ID").grid(row=0, column=4, sticky="w")
        ttk.Entry(bank_probe_opts, textvariable=self.bank_probe_item_id_var, width=8).grid(row=0, column=5, padx=(4, 10))
        ttk.Label(bank_probe_opts, text="WQty").grid(row=0, column=6, sticky="w")
        ttk.Entry(bank_probe_opts, textvariable=self.bank_probe_withdraw_qty_var, width=6).grid(row=0, column=7, padx=(4, 10))
        ttk.Label(bank_probe_opts, text="DQty").grid(row=0, column=8, sticky="w")
        ttk.Entry(bank_probe_opts, textvariable=self.bank_probe_deposit_qty_var, width=6).grid(row=0, column=9, padx=(4, 0))

        combat_opts = ttk.Frame(frame)
        combat_opts.grid(row=6, column=0, columnspan=3, sticky="we", pady=(4, 0))
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

        # Buttons
        start_btn = ttk.Button(frame, text="Start", command=self.start, style="Start.TButton")
        start_btn.grid(row=9, column=0, padx=(0, 4), pady=(6, 0), sticky="we")

        stop_btn = ttk.Button(frame, text="Stop", command=self.stop, style="Danger.TButton")
        stop_btn.grid(row=9, column=1, padx=4, pady=(6, 0), sticky="we")

        clear_btn = ttk.Button(frame, text="Clear output", command=self.clear_output)
        clear_btn.grid(row=9, column=2, padx=(4, 0), pady=(6, 0), sticky="we")

        gui_launch_row = ttk.Frame(frame)
        gui_launch_row.grid(row=10, column=0, columnspan=3, pady=(6, 0), sticky="we")
        ttk.Button(gui_launch_row, text="Open Woodcutting GUI", command=self._launch_woodcutting_gui).grid(
            row=0, column=0, padx=(0, 6), sticky="we"
        )
        ttk.Button(gui_launch_row, text="Open Mining GUI", command=self._launch_mining_gui).grid(
            row=0, column=1, padx=(0, 6), sticky="we"
        )
        ttk.Button(gui_launch_row, text="Open Combat GUI", command=self._launch_combat_gui).grid(
            row=0, column=2, padx=(0, 6), sticky="we"
        )
        ttk.Button(gui_launch_row, text="Open Fishing GUI", command=self._launch_fishing_gui).grid(
            row=0, column=3, padx=(0, 6), sticky="we"
        )
        ttk.Button(gui_launch_row, text="Open Agility GUI", command=self._launch_agility_gui).grid(
            row=0, column=4, padx=(0, 6), sticky="we"
        )
        ttk.Button(gui_launch_row, text="Open Store-Bank GUI", command=self._launch_store_bank_gui).grid(
            row=0, column=5, padx=(0, 6), sticky="we"
        )
        for col in range(6):
            gui_launch_row.columnconfigure(col, weight=1)

        status_label = ttk.Label(frame, textvariable=self.status_var)
        status_label.grid(row=11, column=0, columnspan=3, sticky="w", pady=(8, 0))

        frame.columnconfigure(1, weight=1)
        self._apply_combat_preset(COMBAT_PRESET_DEFAULT)

    def _build_break_controls(self, parent: ttk.Frame, *, row: int, columnspan: int = 3) -> None:
        break_group = ttk.LabelFrame(parent, text="Breakmanager")
        break_group.grid(row=row, column=0, columnspan=columnspan, sticky="we", pady=(4, 0))

        ttk.Checkbutton(break_group, text="Breaking enabled", variable=self.breaks_enabled_var).grid(
            row=0, column=0, sticky="w", padx=8, pady=(6, 4)
        )

        timing_group = ttk.LabelFrame(break_group, text="Timing")
        timing_group.grid(row=1, column=0, sticky="we", padx=8, pady=(0, 6))

        ttk.Label(timing_group, text="Work time:").grid(row=0, column=0, sticky="w", padx=(8, 4), pady=(6, 2))
        ttk.Entry(timing_group, textvariable=self.break_bot_time_minutes_var, width=6).grid(
            row=0, column=1, sticky="w", padx=(0, 6), pady=(6, 2)
        )
        ttk.Label(timing_group, text="Minutes").grid(row=0, column=2, sticky="w", pady=(6, 2))

        ttk.Label(timing_group, text="Break time:").grid(row=1, column=0, sticky="w", padx=(8, 4), pady=2)
        ttk.Entry(timing_group, textvariable=self.break_time_minutes_var, width=6).grid(
            row=1, column=1, sticky="w", padx=(0, 6), pady=2
        )
        ttk.Label(timing_group, text="Minutes").grid(row=1, column=2, sticky="w", pady=2)

        ttk.Label(timing_group, text="Randomized by:").grid(row=2, column=0, sticky="w", padx=(8, 4), pady=(2, 6))
        ttk.Entry(timing_group, textvariable=self.break_randomized_pct_var, width=6).grid(
            row=2, column=1, sticky="w", padx=(0, 6), pady=(2, 6)
        )
        ttk.Label(timing_group, text="%").grid(row=2, column=2, sticky="w", pady=(2, 6))

    def _read_break_settings_from_ui(self) -> Optional[BreakSettings]:
        enabled = bool(self.breaks_enabled_var.get())
        if not enabled:
            return BreakSettings(enabled=False)
        try:
            bot_time_minutes = float(self.break_bot_time_minutes_var.get().strip() or "50")
            break_time_minutes = float(self.break_time_minutes_var.get().strip() or "30")
            randomized_pct = float(self.break_randomized_pct_var.get().strip() or "15")
        except ValueError:
            self._append_output("[ERROR] Break bot time, break time, and randomized % must be numeric\n")
            return None

        if bot_time_minutes <= 0.0 or break_time_minutes <= 0.0:
            self._append_output("[ERROR] Break bot time and break time must be > 0\n")
            return None

        randomized_pct = max(0.0, min(95.0, randomized_pct))
        spread = randomized_pct / 100.0
        work_min = max(0.25, bot_time_minutes * (1.0 - spread))
        work_max = max(work_min, bot_time_minutes * (1.0 + spread))
        break_min = max(0.25, break_time_minutes * (1.0 - spread))
        break_max = max(break_min, break_time_minutes * (1.0 + spread))
        return BreakSettings(
            enabled=True,
            work_minutes_min=work_min,
            work_minutes_max=work_max,
            break_minutes_min=break_min,
            break_minutes_max=break_max,
        )

    def _build_output(self) -> None:
        frame = ttk.Frame(self.root, padding=(8, 0, 8, 8))
        frame.pack(side=tk.TOP, fill=tk.BOTH, expand=True)

        self.output = tk.Text(
            frame,
            height=20,
            wrap="none",
            bg="#0f141b",
            fg="#e6edf3",
            insertbackground="#e6edf3",
            relief="flat",
            highlightthickness=1,
            highlightbackground="#2a333d",
            highlightcolor="#2a333d",
            selectbackground="#2e8bff",
            selectforeground="#e6edf3",
        )
        self.output.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        scroll = ttk.Scrollbar(frame, orient="vertical", command=self.output.yview)
        scroll.pack(side=tk.RIGHT, fill=tk.Y)
        self.output.configure(yscrollcommand=scroll.set)

    # ------------------------------------------------------------------ #
    # Event handlers
    # ------------------------------------------------------------------ #

    def _browse_log(self) -> None:
        path = filedialog.askopenfilename(title="Select RuneLite client.log")
        if path:
            self.log_path_var.set(path)

    def _browse_command_out(self) -> None:
        path = filedialog.asksaveasfilename(title="Select command output NDJSON file")
        if path:
            self.command_out_var.set(path)

    def _apply_backend_runtime_defaults(self) -> None:
        self.log_path_var.set(default_log_path())
        self.command_out_var.set(default_command_out_path())
        self.cooldown_var.set("0")

    def _launch_module_gui(self, module_name: str, label: str) -> None:
        command = [sys.executable, "-m", module_name]
        try:
            subprocess.Popen(command)
            self._append_output(f"[INFO] launched {label}\n")
        except Exception as exc:
            self._append_output(f"[ERROR] failed to launch {label}: {exc}\n")

    def _launch_woodcutting_gui(self) -> None:
        self._launch_module_gui("runelite_planner.woodcutting_gui", "woodcutting GUI")

    def _launch_mining_gui(self) -> None:
        self._launch_module_gui("runelite_planner.mining_gui", "mining GUI")

    def _launch_combat_gui(self) -> None:
        self._launch_module_gui("runelite_planner.combat_gui", "combat GUI")

    def _launch_fishing_gui(self) -> None:
        self._launch_module_gui("runelite_planner.fishing_gui", "fishing GUI")

    def _launch_agility_gui(self) -> None:
        self._launch_module_gui("runelite_planner.agility_gui", "agility GUI")

    def _launch_store_bank_gui(self) -> None:
        self._launch_module_gui("runelite_planner.store_bank_gui", "store-bank GUI")

    def _on_combat_preset_changed(self, _event=None) -> None:
        self._apply_combat_preset(self.combat_preset_var.get().strip())

    def _apply_combat_preset(self, preset_name: str) -> None:
        preset = COMBAT_PRESETS.get(preset_name) or COMBAT_PRESETS.get(COMBAT_PRESET_DEFAULT, {})
        npc_id = int(preset.get("npc_id", -1))
        world_x = int(preset.get("world_x", -1))
        world_y = int(preset.get("world_y", -1))
        max_distance = max(1, int(preset.get("max_distance", 8)))
        max_chase_distance = max(1, int(preset.get("max_chase_distance", 8)))
        encounter_profile = str(preset.get("encounter_profile", "none") or "none").strip().lower()

        self.combat_npc_id_var.set("" if npc_id <= 0 else str(npc_id))
        self.combat_target_x_var.set("" if world_x <= 0 else str(world_x))
        self.combat_target_y_var.set("" if world_y <= 0 else str(world_y))
        self.combat_target_max_distance_var.set(str(max_distance))
        self.combat_max_chase_distance_var.set(str(max_chase_distance))
        self.combat_encounter_var.set(
            COMBAT_ENCOUNTER_PROFILE_TO_LABEL.get(encounter_profile, COMBAT_ENCOUNTER_DEFAULT_LABEL)
        )

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

        dry_run = bool(self.dry_run_var.get())
        follow = bool(self.follow_var.get())
        activity = self.activity_var.get().strip().lower()
        self._append_output(f"[INFO] selected activity={activity}\n")
        build_inputs = GuiActivityInputs(
            woodcut_target_category=self.woodcut_target_category_var.get().strip(),
            woodcut_target_x=self.woodcut_target_x_var.get().strip(),
            woodcut_target_y=self.woodcut_target_y_var.get().strip(),
            woodcut_target_max_distance=self.woodcut_target_max_distance_var.get().strip(),
            woodcut_tuning_profile=self.woodcut_tuning_profile_var.get().strip(),
            mining_tuning_profile=self.mining_tuning_profile_var.get().strip(),
            combat_npc_id=self.combat_npc_id_var.get().strip(),
            combat_target_x=self.combat_target_x_var.get().strip(),
            combat_target_y=self.combat_target_y_var.get().strip(),
            combat_target_max_distance=self.combat_target_max_distance_var.get().strip(),
            combat_max_chase_distance=self.combat_max_chase_distance_var.get().strip(),
            combat_eat_hp=self.combat_eat_hp_var.get().strip(),
            combat_eat_randomized_pct=self.combat_eat_randomized_pct_var.get().strip(),
            combat_food_item_id=self.combat_food_item_id_var.get().strip(),
            combat_encounter_label=self.combat_encounter_var.get().strip() or COMBAT_ENCOUNTER_DEFAULT_LABEL,
            combat_tuning_profile=self.combat_tuning_profile_var.get().strip(),
            bank_probe_target_x=self.bank_probe_target_x_var.get().strip(),
            bank_probe_target_y=self.bank_probe_target_y_var.get().strip(),
            bank_probe_item_id=self.bank_probe_item_id_var.get().strip(),
            bank_probe_withdraw_qty=self.bank_probe_withdraw_qty_var.get().strip(),
            bank_probe_deposit_qty=self.bank_probe_deposit_qty_var.get().strip(),
        )
        try:
            outcome = build_activity_strategy(activity, inputs=build_inputs)
        except ActivityBuildError as exc:
            self._append_output(f"[ERROR] {exc}\n")
            return
        strategy = outcome.strategy
        for line in outcome.info_messages:
            self._append_output(f"{line}\n")

        break_settings = self._read_break_settings_from_ui()
        if break_settings is None:
            return

        if dry_run or not command_out:
            writer: Optional[CommandBusWriter] = None
        else:
            writer = CommandBusWriter(command_out)

        self._stop_event = threading.Event()
        self._active_strategy = strategy
        profile = self._active_profile_for_activity(activity)
        if profile:
            self.status_var.set(f"Status: running activity={activity} profile={profile}")
        else:
            self.status_var.set(f"Status: running activity={activity}")

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
        if self.lock_mouse_var.get():
            self._mouse_lock.start()
        else:
            self._mouse_lock.stop()

    def stop(self) -> None:
        self._mouse_lock.stop()
        if self._active_runner is not None:
            self._active_runner.request_stop(source="gui")
        elif self._stop_event is not None:
            self._stop_event.set()
            self._append_output("[INFO] stop requested (button)\n")
            self.status_var.set("Status: stopping")
        else:
            self._append_output("[INFO] runner is not active\n")
            self.status_var.set("Status: idle")
            return
        self._append_output("[INFO] stop requested (button)\n")
        self.status_var.set("Status: stopping")

    def _on_hotkey_stop_local(self, _event: tk.Event) -> None:
        self._append_output("[INFO] stop requested (F8 local)\n")
        self.stop()

    def _request_hotkey_stop(self) -> None:
        self._hotkey_stop_requested.set()

    def _on_close(self) -> None:
        self._hotkey_listener.stop()
        self.stop()
        self.root.destroy()

    def _handle_hotkey_stop_request(self) -> None:
        if not self._hotkey_stop_requested.is_set():
            return
        self._hotkey_stop_requested.clear()
        self._append_output("[INFO] stop requested (F8 hotkey)\n")
        self.stop()

    def _poll_f8_fallback(self) -> None:
        if sys.platform != "win32":
            return
        try:
            f8_down = (ctypes.windll.user32.GetAsyncKeyState(VK_F8) & 0x8000) != 0
        except Exception:
            return
        if f8_down and not self._f8_was_down:
            self._append_output("[INFO] stop requested (F8 fallback)\n")
            self.stop()
        self._f8_was_down = f8_down

    def clear_output(self) -> None:
        self.output.delete("1.0", tk.END)

    def _clear_command_out_file(self) -> None:
        self._apply_backend_runtime_defaults()
        path = self.command_out_var.get().strip()
        if not path:
            return
        try:
            with open(path, "w", encoding="utf-8"):
                pass
            self._append_output("[INFO] command-out cleared\n")
        except Exception as exc:
            self._append_output(f"[WARN] failed to clear command-out: {exc}\n")

    # ------------------------------------------------------------------ #
    # Runtime callback & queue polling
    # ------------------------------------------------------------------ #

    def _runtime_callback(self, message: str) -> None:
        """
        Called from the runner's background thread.
        We must not touch Tkinter widgets from here; instead we enqueue.
        """
        self._runtime_queue.put(message)

    def _schedule_poll(self) -> None:
        self.root.after(self.RUNTIME_POLL_INTERVAL_MS, self._poll_runtime_queue)

    def _poll_runtime_queue(self) -> None:
        buffer: list[str] = []
        try:
            self._handle_hotkey_stop_request()
            self._poll_f8_fallback()
            while True:
                msg = self._runtime_queue.get_nowait()
                buffer.append(msg + "\n")
        except queue.Empty:
            pass
        finally:
            if buffer:
                self._append_output("".join(buffer))
            self._refresh_status()
            self._schedule_poll()

    def _append_output(self, text: str) -> None:
        self.output.insert(tk.END, text)
        self.output.see(tk.END)

    def _active_profile_for_activity(self, activity: str) -> str | None:
        normalized = str(activity or "").strip().lower()
        default_profile = default_profile_for_activity(normalized)
        if default_profile is None:
            return None
        if normalized == "mining":
            profile = (self.mining_tuning_profile_var.get().strip() or default_profile).upper()
            if profile == default_profile:
                return profile
            return default_profile
        if normalized == "woodcutting":
            profile = (self.woodcut_tuning_profile_var.get().strip() or default_profile).upper()
            if profile == default_profile:
                return profile
            return default_profile
        if normalized == "combat":
            profile = (self.combat_tuning_profile_var.get().strip() or default_profile).upper()
            if profile == default_profile:
                return profile
            return default_profile
        return default_profile

    def _refresh_status(self) -> None:
        running = self._runner_thread is not None and self._runner_thread.is_alive()
        if not running:
            self.status_var.set("Status: idle")
            return

        activity = self.activity_var.get().strip().lower()
        if activity != "bank_probe":
            profile = self._active_profile_for_activity(activity)
            if profile:
                self.status_var.set(f"Status: running activity={activity} profile={profile}")
            else:
                self.status_var.set(f"Status: running activity={activity}")
            return

        phase = getattr(self._active_strategy, "_phase", None)
        phase_value = getattr(phase, "value", None)
        if phase_value:
            self.status_var.set(f"Status: bank_probe phase={phase_value}")
            return
        self.status_var.set("Status: bank_probe phase=UNKNOWN")


def main(argv: Optional[list[str]] = None) -> None:
    parser = argparse.ArgumentParser(description="RuneLite planner GUI")
    parser.add_argument("--auto-start", action="store_true", help="Start planner automatically on GUI launch")
    args = parser.parse_args(argv)

    root = tk.Tk()
    RuntimeGUI(root, auto_start=bool(args.auto_start))
    root.mainloop()


if __name__ == "__main__":  # pragma: no cover
    main()


