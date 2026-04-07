package com.xptool.motion;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Locale;

public enum MotionProfile {
    WOODCUT(
        MotorGestureMode.GENERAL,
        false,
        "humanized",
        new ClickMotionSettings(0.0, 4L, 6L),
        new ClickMotionSettings(2.7, 40L, 27L),
        new ClickMotionSettings(1.2, 18L, 13L),
        new ClickMotionSettings(0.8, 9L, 8L)
    ),
    MINING(
        MotorGestureMode.GENERAL,
        false,
        "humanized",
        new ClickMotionSettings(0.0, 4L, 6L),
        new ClickMotionSettings(1.8, 32L, 20L),
        new ClickMotionSettings(0.8, 14L, 10L),
        new ClickMotionSettings(0.5, 6L, 6L)
    ),
    FISHING(
        MotorGestureMode.GENERAL,
        false,
        "humanized",
        new ClickMotionSettings(0.0, 4L, 6L),
        new ClickMotionSettings(2.1, 36L, 24L),
        new ClickMotionSettings(1.0, 16L, 12L),
        new ClickMotionSettings(0.6, 8L, 7L)
    ),
    COMBAT(
        MotorGestureMode.GENERAL,
        false,
        "humanized",
        new ClickMotionSettings(0.0, 4L, 6L),
        new ClickMotionSettings(2.2, 28L, 18L),
        new ClickMotionSettings(1.0, 12L, 10L),
        new ClickMotionSettings(0.6, 6L, 6L)
    ),
    DROP(
        MotorGestureMode.DROP_SWEEP,
        false,
        "humanized",
        new ClickMotionSettings(0.3, 8L, 10L),
        new ClickMotionSettings(2.6, 44L, 30L),
        new ClickMotionSettings(1.2, 22L, 14L),
        new ClickMotionSettings(0.8, 8L, 8L)
    ),
    BANK(
        MotorGestureMode.GENERAL,
        false
    ),
    MENU_INTERACTION(
        MotorGestureMode.GENERAL,
        false
    ),
    GENERIC_INTERACT(
        MotorGestureMode.GENERAL,
        false
    );

    private static final String DEFAULT_PAYLOAD_MOTION_PROFILE = "humanized";
    private static final double MOTION_DRIFT_MIN_PX = 0.0;
    private static final double MOTION_DRIFT_MAX_PX = 12.0;
    private static final long MOTION_PRECLICK_MIN_MS = 2L;
    private static final long MOTION_PRECLICK_MAX_MS = 180L;
    private static final long MOTION_POSTCLICK_MIN_MS = 2L;
    private static final long MOTION_POSTCLICK_MAX_MS = 120L;
    private static final int MOTION_MOVE_ACCEL_MIN_PERCENT = 0;
    private static final int MOTION_MOVE_ACCEL_MAX_PERCENT = 100;
    private static final int MOTION_MOVE_DECEL_MIN_PERCENT = 0;
    private static final int MOTION_MOVE_DECEL_MAX_PERCENT = 100;
    private static final int MOTION_TERMINAL_SLOWDOWN_RADIUS_MIN_PX = 0;
    private static final int MOTION_TERMINAL_SLOWDOWN_RADIUS_MAX_PX = 260;

    public final MotorGestureMode motorGestureMode;
    public final boolean allowActivationClickForMotorMove;
    public final ClickMotionSettings directClickSettings;

    private final String defaultPayloadProfileName;
    private final ClickMotionSettings humanizedDefaults;
    private final ClickMotionSettings preciseDefaults;
    private final ClickMotionSettings aggressiveDefaults;

    MotionProfile(
        MotorGestureMode motorGestureMode,
        boolean allowActivationClickForMotorMove
    ) {
        this(
            motorGestureMode,
            allowActivationClickForMotorMove,
            DEFAULT_PAYLOAD_MOTION_PROFILE,
            new ClickMotionSettings(0.0, 4L, 6L),
            new ClickMotionSettings(4.0, 32L, 20L),
            new ClickMotionSettings(1.5, 14L, 10L),
            new ClickMotionSettings(0.8, 6L, 6L)
        );
    }

    MotionProfile(
        MotorGestureMode motorGestureMode,
        boolean allowActivationClickForMotorMove,
        String defaultPayloadProfileName,
        ClickMotionSettings directClickSettings,
        ClickMotionSettings humanizedDefaults,
        ClickMotionSettings preciseDefaults,
        ClickMotionSettings aggressiveDefaults
    ) {
        this.motorGestureMode = motorGestureMode;
        this.allowActivationClickForMotorMove = allowActivationClickForMotorMove;
        this.defaultPayloadProfileName = safeString(defaultPayloadProfileName).trim().toLowerCase(Locale.ROOT);
        this.directClickSettings = directClickSettings;
        this.humanizedDefaults = humanizedDefaults;
        this.preciseDefaults = preciseDefaults;
        this.aggressiveDefaults = aggressiveDefaults;
    }

    public static MotionProfile resolveForCommandType(String commandType) {
        switch (safeString(commandType).trim().toUpperCase(Locale.ROOT)) {
            case "WOODCUT_CHOP_NEAREST_TREE_SAFE":
                return MotionProfile.WOODCUT;
            case "MINE_NEAREST_ROCK_SAFE":
                return MotionProfile.MINING;
            case "FISH_NEAREST_SPOT_SAFE":
                return MotionProfile.FISHING;
            case "AGILITY_OBSTACLE_ACTION_SAFE":
                return MotionProfile.FISHING;
            case "SCENE_OBJECT_ACTION_SAFE":
                return MotionProfile.FISHING;
            case "GROUND_ITEM_ACTION_SAFE":
                return MotionProfile.FISHING;
            case "WALK_TO_WORLDPOINT_SAFE":
                return MotionProfile.GENERIC_INTERACT;
            case "COMBAT_ATTACK_NEAREST_NPC_SAFE":
                return MotionProfile.COMBAT;
            case "DROP_START_SESSION":
            case "DROP_STOP_SESSION":
            case "DROP_ITEM_SAFE":
            case "WOODCUT_START_DROP_SESSION":
            case "WOODCUT_STOP_DROP_SESSION":
            case "WOODCUT_DROP_ITEM_SAFE":
                return MotionProfile.DROP;
            case "OPEN_BANK":
            case "BANK_OPEN_SAFE":
            case "DEPOSIT_ITEM":
            case "DEPOSIT_ALL_EXCEPT":
            case "BANK_DEPOSIT_ALL_EXCEPT_TOOL_SAFE":
            case "WITHDRAW_ITEM":
            case "BANK_WITHDRAW_LOGS_SAFE":
            case "SEARCH_BANK_ITEM":
            case "CLOSE_BANK":
            case "ENTER_BANK_PIN":
                return MotionProfile.BANK;
            case "EAT_FOOD_SAFE":
                return MotionProfile.GENERIC_INTERACT;
            default:
                return MotionProfile.GENERIC_INTERACT;
        }
    }

    public ClickMotionSettings resolveClickSettings(JsonObject payload) {
        String requestedProfile = payload == null
            ? ""
            : safeString(asString(payload.get("motionProfile"))).trim().toLowerCase(Locale.ROOT);
        ClickMotionSettings defaults = defaultsForProfileName(requestedProfile);
        double drift = clampDouble(
            asDouble(payload == null ? null : payload.get("mouseDriftRadius"), defaults.driftRadiusPx),
            MOTION_DRIFT_MIN_PX,
            MOTION_DRIFT_MAX_PX
        );
        long pre = clampLong(
            asLong(payload == null ? null : payload.get("preClickDelayMs"), defaults.preClickDelayMs),
            MOTION_PRECLICK_MIN_MS,
            MOTION_PRECLICK_MAX_MS
        );
        long post = clampLong(
            asLong(payload == null ? null : payload.get("postClickDelayMs"), defaults.postClickDelayMs),
            MOTION_POSTCLICK_MIN_MS,
            MOTION_POSTCLICK_MAX_MS
        );
        int moveAccelPercent = clampInt(
            asInt(payload == null ? null : payload.get("moveAccelPercent"), defaults.moveAccelPercent),
            MOTION_MOVE_ACCEL_MIN_PERCENT,
            MOTION_MOVE_ACCEL_MAX_PERCENT
        );
        int moveDecelPercent = clampInt(
            asInt(payload == null ? null : payload.get("moveDecelPercent"), defaults.moveDecelPercent),
            MOTION_MOVE_DECEL_MIN_PERCENT,
            MOTION_MOVE_DECEL_MAX_PERCENT
        );
        int terminalSlowdownRadiusPx = clampInt(
            asInt(
                payload == null ? null : payload.get("terminalSlowdownRadiusPx"),
                defaults.terminalSlowdownRadiusPx
            ),
            MOTION_TERMINAL_SLOWDOWN_RADIUS_MIN_PX,
            MOTION_TERMINAL_SLOWDOWN_RADIUS_MAX_PX
        );
        return new ClickMotionSettings(
            drift,
            pre,
            post,
            moveAccelPercent,
            moveDecelPercent,
            terminalSlowdownRadiusPx
        );
    }

    private ClickMotionSettings defaultsForProfileName(String requestedProfile) {
        String name = safeString(requestedProfile).trim().toLowerCase(Locale.ROOT);
        if (name.isEmpty()) {
            name = defaultPayloadProfileName;
        }
        if ("precise".equals(name)) {
            return preciseDefaults;
        }
        if ("aggressive".equals(name)) {
            return aggressiveDefaults;
        }
        if ("humanized".equals(name)) {
            return humanizedDefaults;
        }
        if ("precise".equals(defaultPayloadProfileName)) {
            return preciseDefaults;
        }
        if ("aggressive".equals(defaultPayloadProfileName)) {
            return aggressiveDefaults;
        }
        return humanizedDefaults;
    }

    private static String asString(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "";
        }
        try {
            return element.getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static long asLong(JsonElement element, long fallback) {
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            return element.getAsLong();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int asInt(JsonElement element, int fallback) {
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            return element.getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double asDouble(JsonElement element, double fallback) {
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            return element.getAsDouble();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static long clampLong(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum MotorGestureMode {
        GENERAL,
        DROP_SWEEP
    }

    public static final class ClickMotionSettings {
        public final double driftRadiusPx;
        public final long preClickDelayMs;
        public final long postClickDelayMs;
        public final int moveAccelPercent;
        public final int moveDecelPercent;
        public final int terminalSlowdownRadiusPx;

        public ClickMotionSettings(double driftRadiusPx, long preClickDelayMs, long postClickDelayMs) {
            this(driftRadiusPx, preClickDelayMs, postClickDelayMs, 0, 0, 0);
        }

        public ClickMotionSettings(
            double driftRadiusPx,
            long preClickDelayMs,
            long postClickDelayMs,
            int moveAccelPercent,
            int moveDecelPercent,
            int terminalSlowdownRadiusPx
        ) {
            this.driftRadiusPx = driftRadiusPx;
            this.preClickDelayMs = preClickDelayMs;
            this.postClickDelayMs = postClickDelayMs;
            this.moveAccelPercent = clampInt(
                moveAccelPercent,
                MOTION_MOVE_ACCEL_MIN_PERCENT,
                MOTION_MOVE_ACCEL_MAX_PERCENT
            );
            this.moveDecelPercent = clampInt(
                moveDecelPercent,
                MOTION_MOVE_DECEL_MIN_PERCENT,
                MOTION_MOVE_DECEL_MAX_PERCENT
            );
            this.terminalSlowdownRadiusPx = clampInt(
                terminalSlowdownRadiusPx,
                MOTION_TERMINAL_SLOWDOWN_RADIUS_MIN_PX,
                MOTION_TERMINAL_SLOWDOWN_RADIUS_MAX_PX
            );
        }
    }
}
