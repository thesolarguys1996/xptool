package com.xptool.activities.mining;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xptool.core.motor.MotorDispatchResult;
import com.xptool.core.motor.MotorDispatchStatus;
import com.xptool.core.runtime.RuntimeDecision;
import com.xptool.motion.MotionProfile;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import java.awt.Point;
import java.util.Locale;
import java.util.Optional;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

public final class MiningCommandService {
    private static final double MINING_RECLICK_JITTER_MIN_SCALE = 0.82;
    private static final double MINING_RECLICK_JITTER_MAX_SCALE = 1.22;
    private static final long MINING_RECLICK_COOLDOWN_MIN_MS = 780L;

    public interface Host {
        boolean isDropSweepSessionActive();

        void endDropSweepSession();

        void pruneMiningRockSuppression();

        void extendMiningRetryWindow();

        ClickMotionSettings resolveClickMotion(JsonObject payload, MotionProfile motionProfile);

        int currentPlayerAnimation();

        void clearMiningOutcomeWaitWindow();

        long miningOutcomeWaitUntilMs();

        Optional<TileObject> resolveNearestSelectedRockTargetExcludingLocked();

        Optional<TileObject> resolveNearestSelectedRockTarget();

        boolean hasLockedMiningTarget();

        Optional<TileObject> resolveLockedRockTarget();

        Optional<TileObject> resolveNearestRockTarget();

        Optional<TileObject> resolveNearestRockTargetExcluding(WorldPoint excludedWorldPoint);

        void lockMiningTarget(TileObject targetObject);

        void clearMiningInteractionWindows();

        int selectedMiningTargetCount();

        Point resolveMiningHoverPoint(TileObject targetObject);

        boolean isUsableCanvasPoint(Point point);

        void clearMiningTargetLock();

        void clearMiningHoverPoint();

        void rememberInteractionAnchorForTileObject(TileObject targetObject, Point point);

        MotorDispatchResult dispatchMiningMoveAndClick(
            Point canvasPoint,
            ClickMotionSettings motion,
            TileObject targetObject
        );

        void noteInteractionActivityNow();

        void suppressMiningRockTarget(WorldPoint worldPoint, long durationMs);

        long miningTargetReclickCooldownMs();

        void beginMiningOutcomeWaitWindow();

        void incrementClicksDispatched();

        JsonObject details(Object... kvPairs);

        String safeString(String value);

        RuntimeDecision accept(String reason, JsonObject details);

        RuntimeDecision reject(String reason);
    }

    private final Host host;
    private WorldPoint lastDispatchWorldPoint = null;
    private long lastDispatchAtMs = 0L;

    public MiningCommandService(Host host) {
        this.host = host;
    }

    public RuntimeDecision executeMineNearestRock(JsonObject payload, MotionProfile motionProfile) {
        if (host.isDropSweepSessionActive()) {
            host.endDropSweepSession();
        }
        host.pruneMiningRockSuppression();
        host.extendMiningRetryWindow();
        ClickMotionSettings motion = host.resolveClickMotion(payload, motionProfile);
        int animation = host.currentPlayerAnimation();
        if (isAnimationActive(animation)) {
            host.clearMiningOutcomeWaitWindow();
            return host.accept("mining_busy_animation_active", host.details("animation", animation));
        }
        long now = System.currentTimeMillis();
        if (now <= host.miningOutcomeWaitUntilMs()) {
            return host.accept(
                "mining_waiting_outcome_window",
                host.details("waitMsRemaining", Math.max(0L, host.miningOutcomeWaitUntilMs() - now))
            );
        }

        String targetCategory = safeString(asString(payload == null ? null : payload.get("targetCategory")))
            .trim()
            .toUpperCase(Locale.ROOT);
        Optional<TileObject> targetObject;
        if ("SELECTED".equals(targetCategory)) {
            targetObject = host.resolveNearestSelectedRockTargetExcludingLocked();
            if (targetObject.isEmpty()) {
                if (host.hasLockedMiningTarget()) {
                    targetObject = host.resolveLockedRockTarget();
                } else {
                    targetObject = host.resolveNearestSelectedRockTarget();
                }
            }
            targetObject.ifPresent(host::lockMiningTarget);
        } else {
            targetObject = host.resolveLockedRockTarget();
            if (targetObject.isEmpty()) {
                targetObject = host.resolveNearestRockTarget();
                targetObject.ifPresent(host::lockMiningTarget);
            }
        }
        if (targetObject.isEmpty()) {
            host.clearMiningInteractionWindows();
            if ("SELECTED".equals(targetCategory)) {
                return host.accept(
                    "selected_rock_target_unavailable",
                    host.details("selectedTargetCount", host.selectedMiningTargetCount())
                );
            }
            return host.reject("rock_target_point_unavailable");
        }
        TileObject resolvedTargetObject = targetObject.get();
        Point targetCanvas = host.resolveMiningHoverPoint(resolvedTargetObject);
        if (targetCanvas == null || !host.isUsableCanvasPoint(targetCanvas)) {
            host.clearMiningInteractionWindows();
            host.clearMiningTargetLock();
            host.clearMiningHoverPoint();
            return host.reject("rock_click_point_unavailable");
        }
        host.rememberInteractionAnchorForTileObject(resolvedTargetObject, targetCanvas);

        WorldPoint targetWorldPoint = resolvedTargetObject.getWorldLocation();
        boolean reroutedFromSameTargetCooldown = false;
        long sameTargetCooldownMs = variedMiningTargetReclickCooldownMs(
            host.miningTargetReclickCooldownMs(),
            lastDispatchAtMs,
            targetWorldPoint
        );
        if (isSameTargetCooldownActive(targetWorldPoint, now, sameTargetCooldownMs)) {
            Optional<TileObject> reroutedTarget = resolveAlternateTargetForSameTargetCooldown(targetCategory, targetWorldPoint);
            if (reroutedTarget.isPresent()) {
                TileObject alternateTarget = reroutedTarget.get();
                Point alternateCanvas = host.resolveMiningHoverPoint(alternateTarget);
                if (alternateCanvas != null && host.isUsableCanvasPoint(alternateCanvas)) {
                    resolvedTargetObject = alternateTarget;
                    targetCanvas = alternateCanvas;
                    host.lockMiningTarget(resolvedTargetObject);
                    host.rememberInteractionAnchorForTileObject(resolvedTargetObject, targetCanvas);
                    targetWorldPoint = resolvedTargetObject.getWorldLocation();
                    reroutedFromSameTargetCooldown = true;
                    sameTargetCooldownMs = variedMiningTargetReclickCooldownMs(
                        host.miningTargetReclickCooldownMs(),
                        lastDispatchAtMs,
                        targetWorldPoint
                    );
                }
            }
        }
        if (isSameTargetCooldownActive(targetWorldPoint, now, sameTargetCooldownMs)) {
            long elapsedMs = now - lastDispatchAtMs;
            return host.accept(
                "mining_same_target_reclick_cooldown",
                host.details(
                    "targetWorldX", targetWorldPoint == null ? -1 : targetWorldPoint.getX(),
                    "targetWorldY", targetWorldPoint == null ? -1 : targetWorldPoint.getY(),
                    "waitMsRemaining", Math.max(0L, sameTargetCooldownMs - Math.max(0L, elapsedMs)),
                    "reroutedFromSameTargetCooldown", false
                )
            );
        }

        long targetReclickCooldownMs = variedMiningTargetReclickCooldownMs(
            host.miningTargetReclickCooldownMs(),
            now,
            targetWorldPoint
        );
        int targetId = resolvedTargetObject.getId();
        MotorDispatchResult dispatchResult = host.dispatchMiningMoveAndClick(
            targetCanvas,
            motion,
            resolvedTargetObject
        );
        MotorDispatchStatus status = dispatchResult.getStatus();
        if (status == MotorDispatchStatus.COMPLETE) {
            host.noteInteractionActivityNow();
            noteDispatchTarget(targetWorldPoint, now);
            if (targetWorldPoint != null) {
                host.suppressMiningRockTarget(targetWorldPoint, targetReclickCooldownMs);
            }
            host.beginMiningOutcomeWaitWindow();
            host.incrementClicksDispatched();
            return host.accept(
                "mining_left_click_dispatched",
                host.details(
                    "target", "locked_rock",
                    "objectId", resolvedTargetObject.getId(),
                    "targetReclickCooldownMs", targetReclickCooldownMs,
                    "reroutedFromSameTargetCooldown", reroutedFromSameTargetCooldown,
                    "motorGestureId", dispatchResult.getId()
                )
            );
        }
        if (status == MotorDispatchStatus.FAILED || status == MotorDispatchStatus.CANCELLED) {
            host.clearMiningInteractionWindows();
            host.clearMiningTargetLock();
            host.clearMiningHoverPoint();
            return host.reject("mining_motor_gesture_" + safeString(dispatchResult.getReason()));
        }
        noteDispatchTarget(targetWorldPoint, now);
        if (targetWorldPoint != null) {
            host.suppressMiningRockTarget(targetWorldPoint, targetReclickCooldownMs);
        }
        host.beginMiningOutcomeWaitWindow();
        return host.accept(
            "mining_motor_gesture_in_flight",
            host.details(
                "target", "locked_rock",
                "objectId", targetId,
                "targetReclickCooldownMs", targetReclickCooldownMs,
                "reroutedFromSameTargetCooldown", reroutedFromSameTargetCooldown,
                "motorGestureId", dispatchResult.getId(),
                "motorStatus", status.name(),
                "motorReason", dispatchResult.getReason()
            )
        );
    }

    private static boolean isAnimationActive(int animation) {
        return animation != -1 && animation != 0;
    }

    private String safeString(String value) {
        if (host != null) {
            return host.safeString(value);
        }
        return value == null ? "" : value;
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

    private Optional<TileObject> resolveAlternateTargetForSameTargetCooldown(
        String targetCategory,
        WorldPoint currentTargetWorldPoint
    ) {
        if (currentTargetWorldPoint == null) {
            return Optional.empty();
        }
        Optional<TileObject> alternateTarget;
        if ("SELECTED".equals(targetCategory)) {
            alternateTarget = host.resolveNearestSelectedRockTargetExcludingLocked();
            if (isSameWorldPoint(alternateTarget, currentTargetWorldPoint)) {
                alternateTarget = host.resolveNearestSelectedRockTarget();
            }
        } else {
            alternateTarget = host.resolveNearestRockTargetExcluding(currentTargetWorldPoint);
        }
        if (!isSameWorldPoint(alternateTarget, currentTargetWorldPoint)) {
            return alternateTarget;
        }
        return Optional.empty();
    }

    private static boolean isSameWorldPoint(Optional<TileObject> candidate, WorldPoint worldPoint) {
        if (candidate == null || candidate.isEmpty() || worldPoint == null) {
            return false;
        }
        WorldPoint candidateWorldPoint = candidate.get().getWorldLocation();
        return candidateWorldPoint != null && candidateWorldPoint.equals(worldPoint);
    }

    private boolean isSameTargetCooldownActive(WorldPoint targetWorldPoint, long now, long sameTargetCooldownMs) {
        if (targetWorldPoint == null || lastDispatchWorldPoint == null || lastDispatchAtMs <= 0L) {
            return false;
        }
        if (!targetWorldPoint.equals(lastDispatchWorldPoint)) {
            return false;
        }
        long elapsedMs = now - lastDispatchAtMs;
        return elapsedMs >= 0L && elapsedMs < sameTargetCooldownMs;
    }

    private void noteDispatchTarget(WorldPoint targetWorldPoint, long now) {
        lastDispatchWorldPoint = targetWorldPoint;
        lastDispatchAtMs = now;
    }

    private static long variedMiningTargetReclickCooldownMs(
        long baseCooldownMs,
        long dispatchAtMs,
        WorldPoint targetWorldPoint
    ) {
        long base = Math.max(0L, baseCooldownMs);
        if (base <= 0L) {
            return MINING_RECLICK_COOLDOWN_MIN_MS;
        }
        long seed = dispatchAtMs ^ 0xA8A7D5B13E57C2D9L;
        if (targetWorldPoint != null) {
            seed ^= ((long) targetWorldPoint.getX() * 73856093L);
            seed ^= ((long) targetWorldPoint.getY() * 19349663L);
            seed ^= ((long) targetWorldPoint.getPlane() * 83492791L);
        }
        double unit = normalizedHashUnit(seed);
        double scaleRange = MINING_RECLICK_JITTER_MAX_SCALE - MINING_RECLICK_JITTER_MIN_SCALE;
        double scale = MINING_RECLICK_JITTER_MIN_SCALE + (scaleRange * unit);
        long scaled = Math.round((double) base * scale);
        return Math.max(MINING_RECLICK_COOLDOWN_MIN_MS, scaled);
    }

    private static double normalizedHashUnit(long seed) {
        long z = seed + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= (z >>> 31);
        long positive = z & Long.MAX_VALUE;
        return (double) positive / (double) Long.MAX_VALUE;
    }
}
