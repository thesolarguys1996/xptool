package com.xptool.executor;

import com.google.gson.JsonObject;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Reusable camera control service.
 *
 * Camera motion is arrow-key based. Idle currently uses these nudges for
 * micro-adjust behavior, and other systems can reuse the same camera API later.
 */
final class CameraMotionService {
    interface Host {
        Robot getOrCreateRobot();

        boolean isClientCanvasFocused();

        boolean focusClientWindowAndCanvas(boolean focusWindow, boolean focusCanvas);

        void sleepNoCooldown(long ms);

        void reserveMotorCooldown(long ms);

        void noteMotorAction();

        int cameraYaw();

        int cameraPitch();
    }

    private static final boolean HUMANIZED_TIMING_ENABLED =
        Boolean.parseBoolean(System.getProperty("xptool.humanizedTimingEnabled", "true"));
    private static final int IDLE_CAMERA_FOCUS_RETRY_MIN_INTERVAL_MS = 1500;
    private static final int IDLE_CAMERA_ACTION_MIN_INTERVAL_MS = 2600;
    private static final int IDLE_CAMERA_ACTION_MAX_INTERVAL_MS = 5200;
    private static final int IDLE_CAMERA_YAW_DELTA_MIN = 7;
    private static final int IDLE_CAMERA_PITCH_DELTA_MIN = 4;
    private static final long CAMERA_KEY_NUDGE_HOLD_MIN_MS = 36L;
    private static final long CAMERA_KEY_NUDGE_HOLD_MAX_MS = 102L;
    private static final long CAMERA_KEY_NUDGE_SETTLE_MIN_MS = 12L;
    private static final long CAMERA_KEY_NUDGE_SETTLE_MAX_MS = 34L;
    private static final long CAMERA_KEY_NUDGE_COOLDOWN_MIN_MS = 180L;
    private static final long CAMERA_KEY_NUDGE_COOLDOWN_MAX_MS = 420L;

    private final Host host;

    private long idleCameraFocusRetryAfterMs = 0L;
    private long idleCameraNextAllowedAtMs = 0L;
    private long cameraKeyNudgeNextAllowedAtMs = 0L;
    private CameraNudgeAction lastCameraNudgeAction = null;
    private int cameraNudgeRepeatStreak = 0;
    private JsonObject lastNudgeDetails = new JsonObject();

    CameraMotionService(Host host) {
        this.host = host;
    }

    boolean performIdleMicroAdjust() {
        long now = System.currentTimeMillis();
        if (now < idleCameraNextAllowedAtMs) {
            return false;
        }
        CameraNudgeAction action = sampleIdleMicroAdjustAction();
        if (!performCameraKeyNudge(action)) {
            return false;
        }
        long cooldownMin = IDLE_CAMERA_ACTION_MIN_INTERVAL_MS;
        long cooldownMax = IDLE_CAMERA_ACTION_MAX_INTERVAL_MS;
        if (cameraNudgeRepeatStreak > 0) {
            cooldownMin += Math.min(640L, 120L * cameraNudgeRepeatStreak);
            cooldownMax += Math.min(1500L, 260L * cameraNudgeRepeatStreak);
        }
        idleCameraNextAllowedAtMs = System.currentTimeMillis() + randomBetween(cooldownMin, cooldownMax);
        return true;
    }

    boolean nudgeYawLeft() {
        return performCameraKeyNudge(CameraNudgeAction.YAW_LEFT);
    }

    boolean nudgeYawRight() {
        return performCameraKeyNudge(CameraNudgeAction.YAW_RIGHT);
    }

    boolean nudgePitchUp() {
        return performCameraKeyNudge(CameraNudgeAction.PITCH_UP);
    }

    boolean nudgePitchDown() {
        return performCameraKeyNudge(CameraNudgeAction.PITCH_DOWN);
    }

    JsonObject lastNudgeDetails() {
        return lastNudgeDetails == null ? new JsonObject() : lastNudgeDetails.deepCopy();
    }

    void releasePendingMotion() {
        Robot robot = host.getOrCreateRobot();
        if (robot == null) {
            return;
        }
        try {
            robot.keyRelease(KeyEvent.VK_LEFT);
            robot.keyRelease(KeyEvent.VK_RIGHT);
            robot.keyRelease(KeyEvent.VK_UP);
            robot.keyRelease(KeyEvent.VK_DOWN);
        } catch (Exception ignored) {
            // Best-effort release during shutdown/focus churn.
        }
    }

    private boolean performCameraKeyNudge(CameraNudgeAction action) {
        if (action == null) {
            lastNudgeDetails = details("status", "invalid_action");
            return false;
        }
        long now = System.currentTimeMillis();
        if (now < cameraKeyNudgeNextAllowedAtMs) {
            lastNudgeDetails = details(
                "status", "cooldown_active",
                "direction", action.name(),
                "waitMsRemaining", Math.max(0L, cameraKeyNudgeNextAllowedAtMs - now)
            );
            return false;
        }
        Robot robot = host.getOrCreateRobot();
        if (robot == null) {
            lastNudgeDetails = details("status", "robot_unavailable", "direction", action.name());
            return false;
        }
        if (!host.isClientCanvasFocused()) {
            if (now < idleCameraFocusRetryAfterMs) {
                lastNudgeDetails = details(
                    "status", "focus_retry_blocked",
                    "direction", action.name(),
                    "waitMsRemaining", Math.max(0L, idleCameraFocusRetryAfterMs - now)
                );
                return false;
            }
            if (!host.focusClientWindowAndCanvas(false, false)) {
                idleCameraFocusRetryAfterMs = now + IDLE_CAMERA_FOCUS_RETRY_MIN_INTERVAL_MS;
                lastNudgeDetails = details(
                    "status", "focus_failed",
                    "direction", action.name(),
                    "retryAfterMs", IDLE_CAMERA_FOCUS_RETRY_MIN_INTERVAL_MS
                );
                return false;
            }
            idleCameraFocusRetryAfterMs = now + IDLE_CAMERA_FOCUS_RETRY_MIN_INTERVAL_MS;
        }
        CameraPose beforePose = captureCameraPose();
        if (beforePose == null) {
            lastNudgeDetails = details("status", "pose_before_unavailable", "direction", action.name());
            return false;
        }
        long holdMs = randomBetween(CAMERA_KEY_NUDGE_HOLD_MIN_MS, CAMERA_KEY_NUDGE_HOLD_MAX_MS);
        long settleMs = randomBetween(CAMERA_KEY_NUDGE_SETTLE_MIN_MS, CAMERA_KEY_NUDGE_SETTLE_MAX_MS);
        try {
            robot.keyPress(action.keyCode);
            host.sleepNoCooldown(holdMs);
            robot.keyRelease(action.keyCode);
            host.sleepNoCooldown(settleMs);
        } catch (Exception ex) {
            try {
                robot.keyRelease(action.keyCode);
            } catch (Exception ignored) {
                // Best effort.
            }
            lastNudgeDetails = details(
                "status", "key_press_failed",
                "direction", action.name(),
                "holdMs", holdMs,
                "settleMs", settleMs
            );
            return false;
        }
        CameraPose afterPose = captureCameraPose();
        if (afterPose == null) {
            lastNudgeDetails = details(
                "status", "pose_after_unavailable",
                "direction", action.name(),
                "holdMs", holdMs,
                "settleMs", settleMs
            );
            return false;
        }
        int yawDelta = circularAngleDeltaAbs(beforePose.yaw, afterPose.yaw, 2048);
        int pitchDelta = Math.abs(afterPose.pitch - beforePose.pitch);
        boolean poseDeltaDetected = yawDelta >= IDLE_CAMERA_YAW_DELTA_MIN || pitchDelta >= IDLE_CAMERA_PITCH_DELTA_MIN;
        if (!poseDeltaDetected) {
            lastNudgeDetails = details(
                "status", "pose_delta_not_detected",
                "direction", action.name(),
                "holdMs", holdMs,
                "settleMs", settleMs,
                "yawDelta", yawDelta,
                "pitchDelta", pitchDelta
            );
            return false;
        }
        updateCameraNudgePattern(action);
        long cooldownMin = CAMERA_KEY_NUDGE_COOLDOWN_MIN_MS;
        long cooldownMax = CAMERA_KEY_NUDGE_COOLDOWN_MAX_MS;
        if (cameraNudgeRepeatStreak > 0) {
            cooldownMin += Math.min(220L, 40L * cameraNudgeRepeatStreak);
            cooldownMax += Math.min(480L, 80L * cameraNudgeRepeatStreak);
        }
        cameraKeyNudgeNextAllowedAtMs = System.currentTimeMillis() + randomBetween(cooldownMin, cooldownMax);
        host.reserveMotorCooldown(holdMs + settleMs + randomBetween(24L, 72L));
        host.noteMotorAction();
        lastNudgeDetails = details(
            "status", "moved",
            "direction", action.name(),
            "holdMs", holdMs,
            "settleMs", settleMs,
            "yawDelta", yawDelta,
            "pitchDelta", pitchDelta,
            "repeatStreak", cameraNudgeRepeatStreak
        );
        return true;
    }

    private CameraNudgeAction sampleIdleMicroAdjustAction() {
        CameraNudgeAction[] values = CameraNudgeAction.values();
        if (values.length == 0) {
            return null;
        }
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        CameraNudgeAction sampled = values[rng.nextInt(values.length)];
        if (lastCameraNudgeAction != null && sampled == lastCameraNudgeAction && values.length > 1) {
            sampled = values[(sampled.ordinal() + 1 + rng.nextInt(values.length - 1)) % values.length];
        }
        return sampled;
    }

    private void updateCameraNudgePattern(CameraNudgeAction action) {
        if (action == null) {
            return;
        }
        if (lastCameraNudgeAction == action) {
            cameraNudgeRepeatStreak = Math.min(6, cameraNudgeRepeatStreak + 1);
        } else {
            cameraNudgeRepeatStreak = 0;
        }
        lastCameraNudgeAction = action;
    }

    private CameraPose captureCameraPose() {
        try {
            return new CameraPose(host.cameraYaw(), host.cameraPitch());
        } catch (Exception ex) {
            return null;
        }
    }

    private static int circularAngleDeltaAbs(int from, int to, int modulus) {
        int mod = Math.max(1, modulus);
        int raw = Math.floorMod(to - from, mod);
        int reverse = mod - raw;
        return Math.min(raw, reverse);
    }

    private static long randomBetween(long minInclusive, long maxInclusive) {
        long lo = Math.min(minInclusive, maxInclusive);
        long hi = Math.max(minInclusive, maxInclusive);
        if (hi <= lo) {
            return lo;
        }
        if (!HUMANIZED_TIMING_ENABLED) {
            return lo;
        }
        if (hi == Long.MAX_VALUE) {
            long sampled = ThreadLocalRandom.current().nextLong(lo, hi);
            return ThreadLocalRandom.current().nextBoolean() ? sampled : hi;
        }
        return ThreadLocalRandom.current().nextLong(lo, hi + 1L);
    }

    private static JsonObject details(Object... kvPairs) {
        JsonObject out = new JsonObject();
        if (kvPairs == null) {
            return out;
        }
        for (int i = 0; i + 1 < kvPairs.length; i += 2) {
            String key = String.valueOf(kvPairs[i]);
            Object value = kvPairs[i + 1];
            if (value == null) {
                continue;
            }
            if (value instanceof Number) {
                out.addProperty(key, (Number) value);
            } else if (value instanceof Boolean) {
                out.addProperty(key, (Boolean) value);
            } else {
                out.addProperty(key, String.valueOf(value));
            }
        }
        return out;
    }

    private static final class CameraPose {
        private final int yaw;
        private final int pitch;

        private CameraPose(int yaw, int pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    private enum CameraNudgeAction {
        YAW_LEFT(KeyEvent.VK_LEFT),
        YAW_RIGHT(KeyEvent.VK_RIGHT),
        PITCH_UP(KeyEvent.VK_UP),
        PITCH_DOWN(KeyEvent.VK_DOWN);

        private final int keyCode;

        CameraNudgeAction(int keyCode) {
            this.keyCode = keyCode;
        }
    }
}
