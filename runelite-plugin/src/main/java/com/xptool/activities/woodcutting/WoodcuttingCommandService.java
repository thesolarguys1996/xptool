package com.xptool.activities.woodcutting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xptool.core.motor.MotorDispatchResult;
import com.xptool.core.motor.MotorDispatchStatus;
import com.xptool.core.runtime.FatigueSnapshot;
import com.xptool.core.runtime.RuntimeDecision;
import com.xptool.motion.MotionProfile;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

public final class WoodcuttingCommandService {
    private static final int WOODCUT_WALK_APPROACH_DISTANCE_TILES = 6;
    private static final int WOODCUT_FAST_RETARGET_DISTANCE_TILES = 7;
    private static final int WOODCUT_FAR_RETARGET_DISTANCE_TILES = 10;
    private static final long WOODCUT_FAR_RETARGET_RECENT_DISPATCH_WINDOW_MS = 5_200L;
    private static final long WOODCUT_FAR_RETARGET_DEFER_MIN_MS = 140L;
    private static final long WOODCUT_FAR_RETARGET_DEFER_MAX_MS = 510L;
    private static final long WOODCUT_PING_PONG_GUARD_WINDOW_MS = 6_800L;
    private static final long WOODCUT_PING_PONG_DEFER_MIN_MS = 180L;
    private static final long WOODCUT_PING_PONG_DEFER_MAX_MS = 620L;
    private static final int WOODCUT_RECLICK_EXTRA_DISTANCE_START_TILES = 4;
    private static final long WOODCUT_RECLICK_EXTRA_PER_TILE_MS = 180L;
    private static final long WOODCUT_RECLICK_EXTRA_MAX_MS = 1400L;
    private static final double WOODCUT_RECLICK_JITTER_MIN_SCALE = 0.82;
    private static final double WOODCUT_RECLICK_JITTER_MAX_SCALE = 1.18;
    private static final int WOODCUT_FATIGUE_RECLICK_COOLDOWN_BIAS_MAX_MS = 520;
    private static final int RECENT_CLICK_POINT_HISTORY_SIZE = 10;
    private static final int RECENT_CLICK_REGION_HISTORY_SIZE = 10;
    private static final double RECENT_CLICK_POINT_REPEAT_EXCLUSION_PX = 6.8;
    private static final int RECENT_CLICK_REGION_CELL_SIZE_PX = 22;
    private static final int WOODCUT_CLICK_POINT_RESAMPLE_ATTEMPTS = 5;
    private static final int WOODCUT_INITIAL_DISPATCH_CLICK_POINT_ATTEMPTS_MIN = 7;
    private static final int WOODCUT_INITIAL_DISPATCH_CLICK_POINT_ATTEMPTS_MAX = 12;
    private static final long POST_DROP_FAST_RETRY_GRACE_MS = 2200L;
    private static final long POST_DROP_REACQUIRE_DELAY_MIN_MS = 180L;
    private static final long POST_DROP_REACQUIRE_DELAY_MAX_MS = 1511L;
    private static final long IDLE_OFFSCREEN_SUPPRESSION_MAX_MS = 600L;
    private static final long COLD_START_HOLD_MIN_MS = 1100L;
    private static final long COLD_START_HOLD_MAX_MS = 2400L;
    private WorldPoint implicitAreaAnchorWorldPoint = null;
    private long postDropReacquireDelayForDropEndedAtMs = Long.MIN_VALUE;
    private long postDropReacquireDelayUntilMs = 0L;

    public interface Host {
        boolean isDropSweepSessionActive();

        void endDropSweepSession();

        long lastDropSweepSessionEndedAtMs();

        void extendWoodcutRetryWindow();

        ClickMotionSettings resolveClickMotion(JsonObject payload, MotionProfile motionProfile);

        int currentPlayerAnimation();

        WorldPoint localPlayerWorldPoint();

        void clearWoodcutOutcomeWaitWindow();

        void clearWoodcutTargetAttempt();

        void clearWoodcutDispatchAttempt();

        long woodcutOutcomeWaitUntilMs();

        WorldPoint woodcutLastAttemptWorldPoint();

        long woodcutApproachWaitUntilMs();

        WorldPoint woodcutLastDispatchWorldPoint();

        long woodcutLastDispatchAtMs();

        long woodcutSameTargetReclickCooldownMs();

        Optional<TileObject> resolveLockedOakTreeTarget();

        Optional<TileObject> resolveNearestOakTreeTarget(int targetWorldX, int targetWorldY, int targetMaxDistance);

        Optional<TileObject> resolveLockedWillowTreeTarget();

        Optional<TileObject> resolveNearestWillowTreeTarget(int targetWorldX, int targetWorldY, int targetMaxDistance);

        Optional<TileObject> resolveLockedSelectedTreeTarget();

        Optional<TileObject> resolvePreferredSelectedTreeTarget();

        Optional<TileObject> resolveNearestSelectedTreeTarget();

        Optional<TileObject> resolveNearestTreeTargetInArea(int targetWorldX, int targetWorldY, int targetMaxDistance);

        Optional<TileObject> resolveLockedNormalTreeTarget();

        Optional<TileObject> resolveNearestNormalTreeTarget(int targetWorldX, int targetWorldY, int targetMaxDistance);

        void lockWoodcutTarget(TileObject targetObject);

        void clearWoodcutInteractionWindows();

        int selectedWoodcutTargetCount();

        Point resolveWoodcutHoverPoint(TileObject targetObject);

        boolean isUsableCanvasPoint(Point point);

        void clearWoodcutTargetLock();

        void clearWoodcutHoverPoint();

        void updateWoodcutBoundary(int targetWorldX, int targetWorldY, int targetMaxDistance);

        void clearWoodcutBoundary();

        void rememberInteractionAnchorForTileObject(TileObject targetObject, Point point);

        MotorDispatchResult dispatchWoodcutMoveAndClick(
            Point canvasPoint,
            ClickMotionSettings motion,
            TileObject targetObject
        );

        void noteInteractionActivityNow();

        void noteWoodcutTargetAttempt(TileObject targetObject);

        void noteWoodcutDispatchAttempt(TileObject targetObject, long now);

        void beginWoodcutOutcomeWaitWindow();

        void incrementClicksDispatched();

        FatigueSnapshot fatigueSnapshot();

        JsonObject details(Object... kvPairs);

        String safeString(String value);

        RuntimeDecision accept(String reason, JsonObject details);

        RuntimeDecision reject(String reason);
    }

    private final Host host;
    private final WoodcutNoticeDelayController woodcutNoticeDelayController = new WoodcutNoticeDelayController();
    private final WoodcutHoldDebugTelemetry holdDebugTelemetry = new WoodcutHoldDebugTelemetry();
    private final long[] recentClickPointHistory = new long[RECENT_CLICK_POINT_HISTORY_SIZE];
    private int recentClickPointHistoryWriteIndex = 0;
    private final long[] recentClickRegionHistory = new long[RECENT_CLICK_REGION_HISTORY_SIZE];
    private int recentClickRegionHistoryWriteIndex = 0;
    private boolean coldStartHoldAppliedForCurrentEpisode = false;
    private long coldStartHoldUntilMs = 0L;
    private long lastSeenDispatchSignalAtMs = Long.MIN_VALUE;
    private volatile long idleOffscreenSuppressionUntilMs = 0L;
    private WorldPoint localRecentDispatchWorldPoint = null;
    private WorldPoint localPriorDispatchWorldPoint = null;
    private long localRecentDispatchAtMs = 0L;

    public WoodcuttingCommandService(Host host) {
        this.host = host;
        initializeRecentClickHistories();
    }

    public boolean isOffscreenIdleSuppressedNow() {
        return idleOffscreenSuppressionRemainingMs() > 0L;
    }

    public long idleOffscreenSuppressionRemainingMs() {
        long now = System.currentTimeMillis();
        long remaining = idleOffscreenSuppressionUntilMs - now;
        return Math.max(0L, remaining);
    }

    public RuntimeDecision executeChopNearestTree(JsonObject payload, MotionProfile motionProfile) {
        if (host.isDropSweepSessionActive()) {
            host.endDropSweepSession();
        }
        host.extendWoodcutRetryWindow();
        ClickMotionSettings motion = host.resolveClickMotion(payload, motionProfile);
        long now = System.currentTimeMillis();
        long lastDispatchAtMs = host.woodcutLastDispatchAtMs();
        long lastDropEndedAtMs = host.lastDropSweepSessionEndedAtMs();
        boolean recentDropCompletion = lastDropEndedAtMs > 0L
            && now >= lastDropEndedAtMs
            && (now - lastDropEndedAtMs) <= POST_DROP_FAST_RETRY_GRACE_MS;
        FatigueSnapshot fatigue = fatigueSnapshot();
        int animation = host.currentPlayerAnimation();
        if (isAnimationActive(animation)) {
            woodcutNoticeDelayController.noteAnimationActive(now);
            host.clearWoodcutOutcomeWaitWindow();
            host.clearWoodcutTargetAttempt();
            return acceptProgress("woodcut_busy_animation_active", now, host.details("animation", animation));
        }
        if (now <= host.woodcutOutcomeWaitUntilMs()) {
            long waitMs = Math.max(0L, host.woodcutOutcomeWaitUntilMs() - now);
            return acceptHold(
                "woodcut_waiting_outcome_window",
                now,
                waitMs,
                host.details("waitMsRemaining", waitMs)
            );
        }
        if (recentDropCompletion) {
            woodcutNoticeDelayController.resetForDropTeardown(lastDispatchAtMs);
        } else {
            WoodcutNoticeDelayController.Decision noticeDelayDecision = woodcutNoticeDelayController.maybeDeferAfterAnimationEnd(
                now,
                lastDispatchAtMs,
                fatigue
            );
            if (noticeDelayDecision.defer) {
                long waitMs = Math.max(0L, noticeDelayDecision.waitMsRemaining);
                return acceptHold(
                    "woodcut_post_chop_notice_delay",
                    now,
                    waitMs,
                    host.details(
                        "waitMsRemaining", waitMs,
                        "newlyArmed", noticeDelayDecision.newlyArmed,
                        "fatigueLoad", fatigue.loadPercent(),
                        "fatigueBand", fatigue.band()
                    )
                );
            }
        }
        RuntimeDecision preTargetDelayDecision = maybeAcceptPreTargetDelay(now, lastDispatchAtMs);
        if (preTargetDelayDecision != null) {
            return preTargetDelayDecision;
        }

        String targetCategory = safeString(asString(payload == null ? null : payload.get("targetCategory")))
            .trim()
            .toUpperCase(Locale.ROOT);
        int targetWorldX = asInt(payload == null ? null : payload.get("targetWorldX"), -1);
        int targetWorldY = asInt(payload == null ? null : payload.get("targetWorldY"), -1);
        int targetMaxDistance = Math.max(1, asInt(payload == null ? null : payload.get("targetMaxDistance"), 12));
        WorldPoint localWorldPoint = host.localPlayerWorldPoint();
        boolean hasExplicitAreaAnchor = targetWorldX > 0 && targetWorldY > 0;
        boolean areaAnchorRequestedByPayload = hasAreaAnchorRequest(payload);
        int effectiveTargetWorldX = targetWorldX;
        int effectiveTargetWorldY = targetWorldY;
        boolean usePlayerAreaAnchor = false;
        if (hasExplicitAreaAnchor) {
            clearImplicitAreaAnchor();
        } else if (areaAnchorRequestedByPayload
            && shouldUsePlayerAreaAnchor(targetCategory, host.selectedWoodcutTargetCount())) {
            WorldPoint anchoredWorldPoint = implicitAreaAnchorWorldPoint;
            if (anchoredWorldPoint == null && localWorldPoint != null) {
                anchoredWorldPoint = localWorldPoint;
                implicitAreaAnchorWorldPoint = anchoredWorldPoint;
            }
            if (anchoredWorldPoint != null) {
                usePlayerAreaAnchor = true;
                effectiveTargetWorldX = anchoredWorldPoint.getX();
                effectiveTargetWorldY = anchoredWorldPoint.getY();
            }
        } else {
            clearImplicitAreaAnchor();
        }
        boolean hasAreaAnchor = hasExplicitAreaAnchor || usePlayerAreaAnchor;
        if (hasAreaAnchor) {
            host.updateWoodcutBoundary(effectiveTargetWorldX, effectiveTargetWorldY, targetMaxDistance);
        } else {
            host.clearWoodcutBoundary();
        }
        Optional<TileObject> targetObject;
        if ("OAK".equals(targetCategory)) {
            if (hasAreaAnchor) {
                targetObject = host.resolveNearestOakTreeTarget(effectiveTargetWorldX, effectiveTargetWorldY, targetMaxDistance);
            } else {
                targetObject = host.resolveNearestOakTreeTarget(-1, -1, -1);
                if (targetObject.isEmpty()) {
                    targetObject = host.resolveLockedOakTreeTarget();
                }
            }
            targetObject.ifPresent(host::lockWoodcutTarget);
        } else if ("WILLOW".equals(targetCategory)) {
            if (hasAreaAnchor) {
                targetObject = host.resolveNearestWillowTreeTarget(effectiveTargetWorldX, effectiveTargetWorldY, targetMaxDistance);
            } else {
                targetObject = host.resolveNearestWillowTreeTarget(-1, -1, -1);
                if (targetObject.isEmpty()) {
                    targetObject = host.resolveLockedWillowTreeTarget();
                }
            }
            targetObject.ifPresent(host::lockWoodcutTarget);
        } else if ("SELECTED".equals(targetCategory)) {
            if (hasAreaAnchor) {
                // Area mode does not require manual right-click tree selection.
                targetObject = host.resolveNearestTreeTargetInArea(effectiveTargetWorldX, effectiveTargetWorldY, targetMaxDistance);
                targetObject.ifPresent(host::lockWoodcutTarget);
            } else {
                targetObject = host.resolveNearestSelectedTreeTarget();
                if (targetObject.isEmpty()) {
                    targetObject = host.resolvePreferredSelectedTreeTarget();
                    targetObject.ifPresent(host::lockWoodcutTarget);
                }
                if (targetObject.isEmpty()) {
                    targetObject = host.resolveLockedSelectedTreeTarget();
                    targetObject.ifPresent(host::lockWoodcutTarget);
                }
            }
        } else {
            if (hasAreaAnchor) {
                targetObject = host.resolveNearestNormalTreeTarget(
                    effectiveTargetWorldX,
                    effectiveTargetWorldY,
                    targetMaxDistance
                );
            } else {
                targetObject = host.resolveNearestNormalTreeTarget(-1, -1, -1);
                if (targetObject.isEmpty()) {
                    targetObject = host.resolveLockedNormalTreeTarget();
                }
            }
            targetObject.ifPresent(host::lockWoodcutTarget);
        }
        if (targetObject.isEmpty()) {
            host.clearWoodcutInteractionWindows();
            if ("SELECTED".equals(targetCategory)) {
                if (hasAreaAnchor) {
                    return acceptProgress(
                        "woodcut_area_target_unavailable",
                        now,
                        host.details(
                            "targetWorldX", effectiveTargetWorldX,
                            "targetWorldY", effectiveTargetWorldY,
                            "targetMaxDistance", targetMaxDistance,
                            "areaAnchorSource", usePlayerAreaAnchor ? "player" : "explicit"
                        )
                    );
                }
                return acceptProgress(
                    "selected_tree_target_unavailable",
                    now,
                    host.details("selectedTargetCount", host.selectedWoodcutTargetCount())
                );
            }
            return host.reject("normal_tree_target_point_unavailable");
        }
        TileObject resolvedTargetObject = targetObject.get();
        boolean initialDispatchSelection = host.woodcutLastDispatchAtMs() <= 0L;
        Point targetCanvas = initialDispatchSelection
            ? resolveRetrySafeWoodcutHoverPoint(resolvedTargetObject, true)
            : resolveRetrySafeWoodcutHoverPoint(resolvedTargetObject, false);
        if (targetCanvas == null || !host.isUsableCanvasPoint(targetCanvas)) {
            host.clearWoodcutInteractionWindows();
            host.clearWoodcutTargetLock();
            host.clearWoodcutHoverPoint();
            return host.reject("normal_tree_click_point_unavailable");
        }
        host.rememberInteractionAnchorForTileObject(resolvedTargetObject, targetCanvas);

        WorldPoint targetWorldPoint = resolvedTargetObject.getWorldLocation();
        int localDistanceToTarget = -1;
        if (localWorldPoint != null && targetWorldPoint != null) {
            int distance = localWorldPoint.distanceTo(targetWorldPoint);
            if (distance >= 0) {
                localDistanceToTarget = distance;
            }
        }
        RuntimeDecision postDropDelayDecision = maybeAcceptPostDropReacquireDelay(now, localDistanceToTarget);
        if (postDropDelayDecision != null) {
            return postDropDelayDecision;
        }
        WorldPoint lastAttemptWorldPoint = host.woodcutLastAttemptWorldPoint();
        boolean sameTargetAsAttempt = targetWorldPoint != null
            && lastAttemptWorldPoint != null
            && targetWorldPoint.equals(lastAttemptWorldPoint);
        WorldPoint lastDispatchWorldPoint = host.woodcutLastDispatchWorldPoint();
        boolean fastRetargetEligible = targetWorldPoint != null
            && lastDispatchWorldPoint != null
            && !targetWorldPoint.equals(lastDispatchWorldPoint)
            && localDistanceToTarget >= 0
            && localDistanceToTarget <= WOODCUT_FAST_RETARGET_DISTANCE_TILES;
        int targetWorldPointX = targetWorldPoint == null ? -1 : targetWorldPoint.getX();
        int targetWorldPointY = targetWorldPoint == null ? -1 : targetWorldPoint.getY();
        long approachWaitUntilMs = host.woodcutApproachWaitUntilMs();
        if (localDistanceToTarget >= WOODCUT_WALK_APPROACH_DISTANCE_TILES && !fastRetargetEligible) {
            if (!sameTargetAsAttempt) {
                host.noteWoodcutTargetAttempt(targetObject.get());
                approachWaitUntilMs = host.woodcutApproachWaitUntilMs();
                long waitMs = Math.max(0L, approachWaitUntilMs - now);
                return acceptHold(
                    "woodcut_approaching_target",
                    now,
                    waitMs,
                    host.details(
                        "targetWorldX", targetWorldPointX,
                        "targetWorldY", targetWorldPointY,
                        "localDistance", localDistanceToTarget,
                        "waitMsRemaining", waitMs
                    )
                );
            }
            if (now <= approachWaitUntilMs) {
                long waitMs = Math.max(0L, approachWaitUntilMs - now);
                return acceptHold(
                    "woodcut_approaching_target",
                    now,
                    waitMs,
                    host.details(
                        "targetWorldX", targetWorldPointX,
                        "targetWorldY", targetWorldPointY,
                        "localDistance", localDistanceToTarget,
                        "waitMsRemaining", waitMs
                    )
                );
            }
        }
        long sinceLastSameTargetDispatchMs = now - lastDispatchAtMs;
        long sameTargetCooldownMs = variedSameTargetReclickCooldownMs(
            host.woodcutSameTargetReclickCooldownMs(),
            lastDispatchAtMs,
            targetWorldPoint
        );
        int fatigueReclickBiasMs = fatigue.woodcutReclickCooldownBiasMs(WOODCUT_FATIGUE_RECLICK_COOLDOWN_BIAS_MAX_MS);
        sameTargetCooldownMs += fatigueReclickBiasMs;
        if (localDistanceToTarget >= WOODCUT_RECLICK_EXTRA_DISTANCE_START_TILES) {
            int extraTiles = localDistanceToTarget - (WOODCUT_RECLICK_EXTRA_DISTANCE_START_TILES - 1);
            long distanceExtraMs = Math.min(
                WOODCUT_RECLICK_EXTRA_MAX_MS,
                (long) Math.max(0, extraTiles) * WOODCUT_RECLICK_EXTRA_PER_TILE_MS
            );
            sameTargetCooldownMs += distanceExtraMs;
        }
        boolean reroutedFromSameTargetCooldown = false;
        boolean reroutedFromFarRetarget = false;
        boolean reroutedFromPingPongGuard = false;
        if (targetWorldPoint != null
            && lastDispatchWorldPoint != null
            && targetWorldPoint.equals(lastDispatchWorldPoint)
            && lastDispatchAtMs > 0L
            && sinceLastSameTargetDispatchMs >= 0L
            && sinceLastSameTargetDispatchMs < sameTargetCooldownMs) {
            Optional<TileObject> reroutedTarget = resolveAlternateTargetForSameTargetCooldown(
                targetCategory,
                hasAreaAnchor,
                effectiveTargetWorldX,
                effectiveTargetWorldY,
                targetMaxDistance,
                targetWorldPoint,
                lastDispatchWorldPoint
            );
            if (reroutedTarget.isPresent()) {
                TileObject alternateTarget = reroutedTarget.get();
                Point alternateCanvas = resolveRetrySafeWoodcutHoverPoint(alternateTarget, false);
                if (alternateCanvas != null && host.isUsableCanvasPoint(alternateCanvas)) {
                    resolvedTargetObject = alternateTarget;
                    targetCanvas = alternateCanvas;
                    host.lockWoodcutTarget(resolvedTargetObject);
                    host.rememberInteractionAnchorForTileObject(resolvedTargetObject, targetCanvas);
                    targetWorldPoint = resolvedTargetObject.getWorldLocation();
                    localDistanceToTarget = resolveLocalDistance(localWorldPoint, targetWorldPoint);
                    targetWorldPointX = targetWorldPoint == null ? -1 : targetWorldPoint.getX();
                    targetWorldPointY = targetWorldPoint == null ? -1 : targetWorldPoint.getY();
                    reroutedFromSameTargetCooldown = true;
                }
            }
        }

        if (targetWorldPoint != null && lastAttemptWorldPoint != null && !targetWorldPoint.equals(lastAttemptWorldPoint)) {
            host.clearWoodcutTargetAttempt();
        }

        long sinceLastDispatchMs = lastDispatchAtMs > 0L ? (now - lastDispatchAtMs) : Long.MAX_VALUE;
        boolean withinRecentRetargetWindow = sinceLastDispatchMs >= 0L
            && sinceLastDispatchMs <= WOODCUT_FAR_RETARGET_RECENT_DISPATCH_WINDOW_MS;
        boolean farRetargetCandidate = targetWorldPoint != null
            && lastDispatchWorldPoint != null
            && !targetWorldPoint.equals(lastDispatchWorldPoint)
            && localDistanceToTarget >= WOODCUT_FAR_RETARGET_DISTANCE_TILES
            && withinRecentRetargetWindow;
        if (farRetargetCandidate) {
            Optional<TileObject> reroutedTarget = resolveAlternateTargetForSameTargetCooldown(
                targetCategory,
                hasAreaAnchor,
                effectiveTargetWorldX,
                effectiveTargetWorldY,
                targetMaxDistance,
                targetWorldPoint,
                lastDispatchWorldPoint
            );
            if (reroutedTarget.isPresent()) {
                TileObject alternateTarget = reroutedTarget.get();
                Point alternateCanvas = resolveRetrySafeWoodcutHoverPoint(alternateTarget, false);
                if (alternateCanvas != null && host.isUsableCanvasPoint(alternateCanvas)) {
                    resolvedTargetObject = alternateTarget;
                    targetCanvas = alternateCanvas;
                    host.lockWoodcutTarget(resolvedTargetObject);
                    host.rememberInteractionAnchorForTileObject(resolvedTargetObject, targetCanvas);
                    targetWorldPoint = resolvedTargetObject.getWorldLocation();
                    localDistanceToTarget = resolveLocalDistance(localWorldPoint, targetWorldPoint);
                    targetWorldPointX = targetWorldPoint == null ? -1 : targetWorldPoint.getX();
                    targetWorldPointY = targetWorldPoint == null ? -1 : targetWorldPoint.getY();
                    reroutedFromFarRetarget = true;
                }
            }
            if (!reroutedFromFarRetarget) {
                long waitMs = randomLongInclusive(WOODCUT_FAR_RETARGET_DEFER_MIN_MS, WOODCUT_FAR_RETARGET_DEFER_MAX_MS);
                return acceptHold(
                    "woodcut_far_retarget_deferred",
                    now,
                    waitMs,
                    host.details(
                        "targetWorldX", targetWorldPointX,
                        "targetWorldY", targetWorldPointY,
                        "localDistance", localDistanceToTarget,
                        "waitMsRemaining", waitMs,
                        "sinceLastDispatchMs", sinceLastDispatchMs,
                        "movementCause", "far_retarget_deferred"
                    )
                );
            }
        }

        boolean pingPongTargetCandidate = isPingPongTarget(targetWorldPoint, now);
        if (pingPongTargetCandidate) {
            Optional<TileObject> reroutedTarget = resolveAlternateTargetForSameTargetCooldown(
                targetCategory,
                hasAreaAnchor,
                effectiveTargetWorldX,
                effectiveTargetWorldY,
                targetMaxDistance,
                targetWorldPoint,
                lastDispatchWorldPoint
            );
            if (reroutedTarget.isPresent()) {
                TileObject alternateTarget = reroutedTarget.get();
                Point alternateCanvas = resolveRetrySafeWoodcutHoverPoint(alternateTarget, false);
                if (alternateCanvas != null && host.isUsableCanvasPoint(alternateCanvas)) {
                    resolvedTargetObject = alternateTarget;
                    targetCanvas = alternateCanvas;
                    host.lockWoodcutTarget(resolvedTargetObject);
                    host.rememberInteractionAnchorForTileObject(resolvedTargetObject, targetCanvas);
                    targetWorldPoint = resolvedTargetObject.getWorldLocation();
                    localDistanceToTarget = resolveLocalDistance(localWorldPoint, targetWorldPoint);
                    targetWorldPointX = targetWorldPoint == null ? -1 : targetWorldPoint.getX();
                    targetWorldPointY = targetWorldPoint == null ? -1 : targetWorldPoint.getY();
                    reroutedFromPingPongGuard = true;
                }
            }
            if (!reroutedFromPingPongGuard) {
                long waitMs = randomLongInclusive(WOODCUT_PING_PONG_DEFER_MIN_MS, WOODCUT_PING_PONG_DEFER_MAX_MS);
                return acceptHold(
                    "woodcut_target_ping_pong_cooldown",
                    now,
                    waitMs,
                    host.details(
                        "targetWorldX", targetWorldPointX,
                        "targetWorldY", targetWorldPointY,
                        "localDistance", localDistanceToTarget,
                        "waitMsRemaining", waitMs,
                        "recentDispatchWorldX",
                        localRecentDispatchWorldPoint == null ? -1 : localRecentDispatchWorldPoint.getX(),
                        "recentDispatchWorldY",
                        localRecentDispatchWorldPoint == null ? -1 : localRecentDispatchWorldPoint.getY(),
                        "priorDispatchWorldX",
                        localPriorDispatchWorldPoint == null ? -1 : localPriorDispatchWorldPoint.getX(),
                        "priorDispatchWorldY",
                        localPriorDispatchWorldPoint == null ? -1 : localPriorDispatchWorldPoint.getY(),
                        "movementCause", "ping_pong_guard_deferred"
                    )
                );
            }
        }

        sinceLastSameTargetDispatchMs = now - lastDispatchAtMs;
        sameTargetCooldownMs = variedSameTargetReclickCooldownMs(
            host.woodcutSameTargetReclickCooldownMs(),
            lastDispatchAtMs,
            targetWorldPoint
        );
        sameTargetCooldownMs += fatigueReclickBiasMs;
        if (localDistanceToTarget >= WOODCUT_RECLICK_EXTRA_DISTANCE_START_TILES) {
            int extraTiles = localDistanceToTarget - (WOODCUT_RECLICK_EXTRA_DISTANCE_START_TILES - 1);
            long distanceExtraMs = Math.min(
                WOODCUT_RECLICK_EXTRA_MAX_MS,
                (long) Math.max(0, extraTiles) * WOODCUT_RECLICK_EXTRA_PER_TILE_MS
            );
            sameTargetCooldownMs += distanceExtraMs;
        }
        if (targetWorldPoint != null
            && lastDispatchWorldPoint != null
            && targetWorldPoint.equals(lastDispatchWorldPoint)
            && lastDispatchAtMs > 0L
            && sinceLastSameTargetDispatchMs >= 0L
            && sinceLastSameTargetDispatchMs < sameTargetCooldownMs) {
            long waitMs = sameTargetCooldownMs - sinceLastSameTargetDispatchMs;
            return acceptHold(
                "woodcut_same_target_reclick_cooldown",
                now,
                waitMs,
                host.details(
                    "targetWorldX", targetWorldPointX,
                    "targetWorldY", targetWorldPointY,
                    "localDistance", localDistanceToTarget,
                    "waitMsRemaining", waitMs,
                    "fatigueReclickBiasMs", fatigueReclickBiasMs,
                    "fatigueLoad", fatigue.loadPercent(),
                    "fatigueBand", fatigue.band(),
                    "reroutedFromSameTargetCooldown", false,
                    "movementCause", "same_target_reclick_cooldown"
                )
            );
        }

        String movementCause = resolveDispatchMovementCause(
            reroutedFromSameTargetCooldown,
            reroutedFromFarRetarget,
            reroutedFromPingPongGuard,
            fastRetargetEligible,
            hasAreaAnchor,
            localDistanceToTarget
        );

        int targetId = resolvedTargetObject.getId();
        MotorDispatchResult dispatchResult = host.dispatchWoodcutMoveAndClick(
            targetCanvas,
            motion,
            resolvedTargetObject
        );
        MotorDispatchStatus status = dispatchResult.getStatus();
        if (status == MotorDispatchStatus.COMPLETE) {
            host.noteInteractionActivityNow();
            host.noteWoodcutTargetAttempt(resolvedTargetObject);
            host.noteWoodcutDispatchAttempt(resolvedTargetObject, now);
            noteLocalDispatchTarget(resolvedTargetObject.getWorldLocation(), now);
            woodcutNoticeDelayController.armForDispatch(now, fatigue);
            host.beginWoodcutOutcomeWaitWindow();
            host.incrementClicksDispatched();
            rememberRecentDispatchClickPoint(targetCanvas);
            return acceptProgress(
                "woodcut_left_click_dispatched",
                now,
                host.details(
                    "target", "locked_normal_tree",
                    "objectId", resolvedTargetObject.getId(),
                    "reroutedFromSameTargetCooldown", reroutedFromSameTargetCooldown,
                    "reroutedFromFarRetarget", reroutedFromFarRetarget,
                    "reroutedFromPingPongGuard", reroutedFromPingPongGuard,
                    "movementCause", movementCause,
                    "motorGestureId", dispatchResult.getId()
                )
            );
        }
        if (status == MotorDispatchStatus.FAILED || status == MotorDispatchStatus.CANCELLED) {
            host.clearWoodcutInteractionWindows();
            host.clearWoodcutTargetLock();
            host.clearWoodcutHoverPoint();
            return host.reject("woodcut_motor_gesture_" + safeString(dispatchResult.getReason()));
        }
        host.noteWoodcutTargetAttempt(resolvedTargetObject);
        host.noteWoodcutDispatchAttempt(resolvedTargetObject, now);
        noteLocalDispatchTarget(resolvedTargetObject.getWorldLocation(), now);
        woodcutNoticeDelayController.armForDispatch(now, fatigue);
        host.beginWoodcutOutcomeWaitWindow();
        rememberRecentDispatchClickPoint(targetCanvas);
        return acceptProgress(
            "woodcut_motor_gesture_in_flight",
            now,
            host.details(
                "target", "locked_normal_tree",
                "objectId", targetId,
                "reroutedFromSameTargetCooldown", reroutedFromSameTargetCooldown,
                "reroutedFromFarRetarget", reroutedFromFarRetarget,
                "reroutedFromPingPongGuard", reroutedFromPingPongGuard,
                "movementCause", movementCause,
                "motorGestureId", dispatchResult.getId(),
                "motorStatus", status.name(),
                "motorReason", dispatchResult.getReason()
            )
        );
    }

    private static boolean isAnimationActive(int animation) {
        return animation != -1 && animation != 0;
    }

    private FatigueSnapshot fatigueSnapshot() {
        FatigueSnapshot snapshot = host.fatigueSnapshot();
        return snapshot == null ? FatigueSnapshot.neutral() : snapshot;
    }

    private RuntimeDecision acceptHold(String reason, long nowMs, long waitMs, JsonObject details) {
        long suppressionWaitMs = resolveIdleOffscreenSuppressionWaitMs(waitMs);
        noteIdleOffscreenSuppression(nowMs, suppressionWaitMs);
        JsonObject enriched = holdDebugTelemetry.decorateHold(details, reason, nowMs, waitMs);
        enriched.addProperty("idleOffscreenSuppressionWaitMs", suppressionWaitMs);
        enriched.addProperty("idleOffscreenSuppressionCapMs", IDLE_OFFSCREEN_SUPPRESSION_MAX_MS);
        return host.accept(reason, enriched);
    }

    private static long resolveIdleOffscreenSuppressionWaitMs(long waitMs) {
        if (waitMs <= 0L) {
            return 0L;
        }
        return Math.min(waitMs, IDLE_OFFSCREEN_SUPPRESSION_MAX_MS);
    }

    private void noteIdleOffscreenSuppression(long nowMs, long waitMs) {
        if (waitMs <= 0L) {
            return;
        }
        long baseNow = nowMs > 0L ? nowMs : System.currentTimeMillis();
        long until = baseNow + waitMs;
        if (until > idleOffscreenSuppressionUntilMs) {
            idleOffscreenSuppressionUntilMs = until;
        }
    }

    private RuntimeDecision acceptProgress(String reason, long nowMs, JsonObject details) {
        JsonObject enriched = holdDebugTelemetry.decorateProgress(details, reason, nowMs);
        return host.accept(reason, enriched);
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

    private static boolean hasAreaAnchorRequest(JsonObject payload) {
        if (payload == null) {
            return false;
        }
        return payload.has("targetWorldX")
            || payload.has("targetWorldY")
            || payload.has("targetMaxDistance")
            || payload.has("targetCategory");
    }

    private static boolean shouldUsePlayerAreaAnchor(String targetCategory, int selectedTargetCount) {
        if ("SELECTED".equals(targetCategory) && selectedTargetCount > 0) {
            return false;
        }
        return true;
    }

    private Point resolveRetrySafeWoodcutHoverPoint(TileObject targetObject, boolean initialDispatchSelection) {
        Point fallback = null;
        List<Point> nonRecentCandidates = new ArrayList<>();
        List<Point> usableCandidates = new ArrayList<>();
        int attempts = Math.max(1, WOODCUT_CLICK_POINT_RESAMPLE_ATTEMPTS);
        if (initialDispatchSelection) {
            attempts = randomIntInclusive(
                WOODCUT_INITIAL_DISPATCH_CLICK_POINT_ATTEMPTS_MIN,
                WOODCUT_INITIAL_DISPATCH_CLICK_POINT_ATTEMPTS_MAX
            );
        }
        for (int i = 0; i < attempts; i++) {
            Point candidate = host.resolveWoodcutHoverPoint(targetObject);
            if (candidate == null || !host.isUsableCanvasPoint(candidate)) {
                continue;
            }
            fallback = candidate;
            if (!containsEquivalentPoint(usableCandidates, candidate, 0.9)) {
                usableCandidates.add(candidate);
            }
            if (!isRecentlyUsedClickPoint(candidate)
                && !containsEquivalentPoint(nonRecentCandidates, candidate, 0.9)) {
                nonRecentCandidates.add(candidate);
            }
        }
        if (!nonRecentCandidates.isEmpty()) {
            return copyPoint(selectDiverseClickPoint(nonRecentCandidates));
        }
        if (!usableCandidates.isEmpty()) {
            return copyPoint(selectDiverseClickPoint(usableCandidates));
        }
        return fallback;
    }

    private void noteLocalDispatchTarget(WorldPoint targetWorldPoint, long nowMs) {
        if (targetWorldPoint == null || nowMs <= 0L) {
            return;
        }
        if (localRecentDispatchWorldPoint == null || !targetWorldPoint.equals(localRecentDispatchWorldPoint)) {
            localPriorDispatchWorldPoint = localRecentDispatchWorldPoint;
        }
        localRecentDispatchWorldPoint = targetWorldPoint;
        localRecentDispatchAtMs = nowMs;
    }

    private boolean isPingPongTarget(WorldPoint targetWorldPoint, long nowMs) {
        if (targetWorldPoint == null || nowMs <= 0L) {
            return false;
        }
        if (localRecentDispatchWorldPoint == null || localPriorDispatchWorldPoint == null) {
            return false;
        }
        if (localRecentDispatchAtMs <= 0L || nowMs < localRecentDispatchAtMs) {
            return false;
        }
        long sinceRecentDispatchMs = nowMs - localRecentDispatchAtMs;
        if (sinceRecentDispatchMs > WOODCUT_PING_PONG_GUARD_WINDOW_MS) {
            return false;
        }
        if (targetWorldPoint.equals(localRecentDispatchWorldPoint)) {
            return false;
        }
        return targetWorldPoint.equals(localPriorDispatchWorldPoint);
    }

    private static String resolveDispatchMovementCause(
        boolean reroutedFromSameTargetCooldown,
        boolean reroutedFromFarRetarget,
        boolean reroutedFromPingPongGuard,
        boolean fastRetargetEligible,
        boolean hasAreaAnchor,
        int localDistanceToTarget
    ) {
        if (reroutedFromPingPongGuard) {
            return "ping_pong_reroute";
        }
        if (reroutedFromFarRetarget) {
            return "far_retarget_reroute";
        }
        if (reroutedFromSameTargetCooldown) {
            return "same_target_cooldown_reroute";
        }
        if (fastRetargetEligible) {
            return "fast_retarget";
        }
        if (localDistanceToTarget >= WOODCUT_WALK_APPROACH_DISTANCE_TILES) {
            return "approach_reacquire";
        }
        if (hasAreaAnchor) {
            return "area_anchor_reacquire";
        }
        return "direct_reacquire";
    }

    private void clearImplicitAreaAnchor() {
        implicitAreaAnchorWorldPoint = null;
    }

    private Point selectDiverseClickPoint(List<Point> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        double totalWeight = 0.0;
        double[] weights = new double[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            Point candidate = candidates.get(i);
            if (candidate == null) {
                continue;
            }
            double nearestRecentDistance = nearestRecentClickPointDistance(candidate);
            double weight = 1.0
                + Math.min(12.0, nearestRecentDistance * 0.85)
                + randomBetweenInclusive(0.10, 1.70);
            if (isRecentlyUsedClickRegion(candidate)) {
                weight *= 0.55;
            }
            weights[i] = Math.max(0.001, weight);
            totalWeight += weights[i];
        }
        if (!(totalWeight > 0.0)) {
            return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        }
        double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
        double cumulative = 0.0;
        for (int i = 0; i < candidates.size(); i++) {
            cumulative += weights[i];
            if (roll <= cumulative && candidates.get(i) != null) {
                return candidates.get(i);
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    private void initializeRecentClickHistories() {
        for (int i = 0; i < recentClickPointHistory.length; i++) {
            recentClickPointHistory[i] = Long.MIN_VALUE;
        }
        for (int i = 0; i < recentClickRegionHistory.length; i++) {
            recentClickRegionHistory[i] = Long.MIN_VALUE;
        }
    }

    private void rememberRecentDispatchClickPoint(Point point) {
        if (point == null) {
            return;
        }
        rememberClickPointHistoryKey(clickPointKey(point));
        rememberClickRegionHistoryKey(clickRegionKey(point));
    }

    private boolean isRecentlyUsedClickPoint(Point point) {
        long key = clickPointKey(point);
        if (key == Long.MIN_VALUE) {
            return false;
        }
        if (isRecentlyUsedClickRegion(point)) {
            return true;
        }
        for (long recentKey : recentClickPointHistory) {
            if (recentKey == key) {
                return true;
            }
            Point recentPoint = decodeClickPointKey(recentKey);
            if (recentPoint != null && pixelDistance(recentPoint, point) <= RECENT_CLICK_POINT_REPEAT_EXCLUSION_PX) {
                return true;
            }
        }
        return false;
    }

    private boolean isRecentlyUsedClickRegion(Point point) {
        long regionKey = clickRegionKey(point);
        if (regionKey == Long.MIN_VALUE) {
            return false;
        }
        for (long recentRegionKey : recentClickRegionHistory) {
            if (recentRegionKey == regionKey) {
                return true;
            }
        }
        return false;
    }

    private double nearestRecentClickPointDistance(Point point) {
        if (point == null) {
            return 0.0;
        }
        double nearest = Double.MAX_VALUE;
        for (long recentKey : recentClickPointHistory) {
            Point recentPoint = decodeClickPointKey(recentKey);
            if (recentPoint == null) {
                continue;
            }
            nearest = Math.min(nearest, pixelDistance(point, recentPoint));
        }
        return nearest == Double.MAX_VALUE ? 8.0 : nearest;
    }

    private void rememberClickPointHistoryKey(long key) {
        if (key == Long.MIN_VALUE || recentClickPointHistory.length <= 0) {
            return;
        }
        recentClickPointHistory[recentClickPointHistoryWriteIndex] = key;
        recentClickPointHistoryWriteIndex = (recentClickPointHistoryWriteIndex + 1) % recentClickPointHistory.length;
    }

    private void rememberClickRegionHistoryKey(long key) {
        if (key == Long.MIN_VALUE || recentClickRegionHistory.length <= 0) {
            return;
        }
        recentClickRegionHistory[recentClickRegionHistoryWriteIndex] = key;
        recentClickRegionHistoryWriteIndex = (recentClickRegionHistoryWriteIndex + 1)
            % recentClickRegionHistory.length;
    }

    private static Point copyPoint(Point point) {
        return point == null ? null : new Point(point);
    }

    private static boolean containsEquivalentPoint(List<Point> points, Point candidate, double tolerancePx) {
        if (points == null || points.isEmpty() || candidate == null) {
            return false;
        }
        double tolerance = Math.max(0.0, tolerancePx);
        for (Point point : points) {
            if (point == null) {
                continue;
            }
            if (pixelDistance(point, candidate) <= tolerance) {
                return true;
            }
        }
        return false;
    }

    private Optional<TileObject> resolveAlternateTargetForSameTargetCooldown(
        String targetCategory,
        boolean hasAreaAnchor,
        int targetWorldX,
        int targetWorldY,
        int targetMaxDistance,
        WorldPoint currentTargetWorldPoint,
        WorldPoint lastDispatchWorldPoint
    ) {
        Optional<TileObject> alternateTarget;
        if ("OAK".equals(targetCategory)) {
            if (hasAreaAnchor) {
                alternateTarget = host.resolveNearestOakTreeTarget(targetWorldX, targetWorldY, targetMaxDistance);
            } else {
                alternateTarget = host.resolveNearestOakTreeTarget(-1, -1, -1);
            }
        } else if ("WILLOW".equals(targetCategory)) {
            if (hasAreaAnchor) {
                alternateTarget = host.resolveNearestWillowTreeTarget(targetWorldX, targetWorldY, targetMaxDistance);
            } else {
                alternateTarget = host.resolveNearestWillowTreeTarget(-1, -1, -1);
            }
        } else if ("SELECTED".equals(targetCategory)) {
            if (hasAreaAnchor) {
                alternateTarget = host.resolveNearestTreeTargetInArea(targetWorldX, targetWorldY, targetMaxDistance);
            } else {
                alternateTarget = host.resolvePreferredSelectedTreeTarget();
                if (alternateTarget.isEmpty()) {
                    alternateTarget = host.resolveNearestSelectedTreeTarget();
                }
            }
        } else if (hasAreaAnchor) {
            alternateTarget = host.resolveNearestNormalTreeTarget(targetWorldX, targetWorldY, targetMaxDistance);
        } else {
            alternateTarget = host.resolveNearestNormalTreeTarget(-1, -1, -1);
        }
        if (alternateTarget.isEmpty()) {
            return Optional.empty();
        }
        TileObject candidate = alternateTarget.get();
        WorldPoint candidateWorldPoint = candidate.getWorldLocation();
        if (candidateWorldPoint.equals(currentTargetWorldPoint) || candidateWorldPoint.equals(lastDispatchWorldPoint)) {
            return Optional.empty();
        }
        return Optional.of(candidate);
    }

    private static int resolveLocalDistance(WorldPoint localWorldPoint, WorldPoint targetWorldPoint) {
        if (localWorldPoint == null || targetWorldPoint == null) {
            return -1;
        }
        int distance = localWorldPoint.distanceTo(targetWorldPoint);
        return distance >= 0 ? distance : -1;
    }

    private RuntimeDecision maybeAcceptPreTargetDelay(long nowMs, long lastDispatchAtMs) {
        if (nowMs <= 0L) {
            return null;
        }
        long lastDropEndedAtMs = host.lastDropSweepSessionEndedAtMs();
        boolean recentDropCompletion =
            lastDropEndedAtMs > 0L
                && nowMs > 0L
                && (nowMs - lastDropEndedAtMs) <= POST_DROP_FAST_RETRY_GRACE_MS;
        if (recentDropCompletion) {
            coldStartHoldAppliedForCurrentEpisode = true;
            coldStartHoldUntilMs = 0L;
            return null;
        }
        long dispatchSignalAtMs = Math.max(0L, lastDispatchAtMs);
        if (dispatchSignalAtMs > 0L) {
            lastSeenDispatchSignalAtMs = dispatchSignalAtMs;
            coldStartHoldAppliedForCurrentEpisode = false;
            coldStartHoldUntilMs = 0L;
            return null;
        }
        if (lastSeenDispatchSignalAtMs > 0L) {
            lastSeenDispatchSignalAtMs = 0L;
            coldStartHoldAppliedForCurrentEpisode = false;
            coldStartHoldUntilMs = 0L;
        }
        if (coldStartHoldAppliedForCurrentEpisode) {
            return null;
        }
        if (coldStartHoldUntilMs <= 0L) {
            coldStartHoldUntilMs = nowMs + randomLongInclusive(COLD_START_HOLD_MIN_MS, COLD_START_HOLD_MAX_MS);
        }
        if (nowMs < coldStartHoldUntilMs) {
            long waitMs = coldStartHoldUntilMs - nowMs;
            return acceptHold(
                "woodcut_cold_start_hold_window",
                nowMs,
                waitMs,
                host.details(
                    "waitMsRemaining", waitMs,
                    "coldStartHoldWindowMinMs", COLD_START_HOLD_MIN_MS,
                    "coldStartHoldWindowMaxMs", COLD_START_HOLD_MAX_MS
                )
            );
        }
        coldStartHoldAppliedForCurrentEpisode = true;
        coldStartHoldUntilMs = 0L;
        return null;
    }

    private RuntimeDecision maybeAcceptPostDropReacquireDelay(long now, int localDistanceToTarget) {
        if (now <= 0L) {
            return null;
        }
        long lastDropEndedAtMs = host.lastDropSweepSessionEndedAtMs();
        if (lastDropEndedAtMs <= 0L || now < lastDropEndedAtMs) {
            return null;
        }
        long sinceDropEndedMs = now - lastDropEndedAtMs;
        if (sinceDropEndedMs > POST_DROP_FAST_RETRY_GRACE_MS) {
            return null;
        }
        if (lastDropEndedAtMs != postDropReacquireDelayForDropEndedAtMs) {
            postDropReacquireDelayForDropEndedAtMs = lastDropEndedAtMs;
            postDropReacquireDelayUntilMs = now + samplePostDropReacquireDelayMs(localDistanceToTarget);
        }
        if (now < postDropReacquireDelayUntilMs) {
            long waitMs = postDropReacquireDelayUntilMs - now;
            return acceptHold(
                "woodcut_post_drop_reacquire_delay",
                now,
                waitMs,
                host.details(
                    "waitMsRemaining", waitMs,
                    "sinceDropEndedMs", sinceDropEndedMs,
                    "localDistance", localDistanceToTarget,
                    "delayMinMs", POST_DROP_REACQUIRE_DELAY_MIN_MS,
                    "delayMaxMs", POST_DROP_REACQUIRE_DELAY_MAX_MS
                )
            );
        }
        return null;
    }

    private static long samplePostDropReacquireDelayMs(int localDistanceToTarget) {
        long base = randomLongInclusive(POST_DROP_REACQUIRE_DELAY_MIN_MS, POST_DROP_REACQUIRE_DELAY_MAX_MS);
        int distance = Math.max(0, localDistanceToTarget);
        long distanceBias = Math.min(260L, (long) distance * 34L);
        return Math.max(POST_DROP_REACQUIRE_DELAY_MIN_MS, base + distanceBias);
    }

    private static long variedSameTargetReclickCooldownMs(long baseCooldownMs, long dispatchAtMs, WorldPoint targetWorldPoint) {
        long base = Math.max(0L, baseCooldownMs);
        if (base <= 0L) {
            return 0L;
        }
        long seed = dispatchAtMs;
        if (targetWorldPoint != null) {
            seed ^= ((long) targetWorldPoint.getX() * 73856093L);
            seed ^= ((long) targetWorldPoint.getY() * 19349663L);
            seed ^= ((long) targetWorldPoint.getPlane() * 83492791L);
        }
        double normalized = normalizedHashUnit(seed);
        double scaleRange = WOODCUT_RECLICK_JITTER_MAX_SCALE - WOODCUT_RECLICK_JITTER_MIN_SCALE;
        double scale = WOODCUT_RECLICK_JITTER_MIN_SCALE + (scaleRange * normalized);
        return Math.max(120L, Math.round((double) base * scale));
    }

    private static double normalizedHashUnit(long seed) {
        long z = seed + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= (z >>> 31);
        long positive = z & Long.MAX_VALUE;
        return (double) positive / (double) Long.MAX_VALUE;
    }

    private static long clickPointKey(Point point) {
        if (point == null) {
            return Long.MIN_VALUE;
        }
        long x = ((long) point.x) & 0xFFFFFFFFL;
        long y = ((long) point.y) & 0xFFFFFFFFL;
        return (x << 32) | y;
    }

    private static long clickRegionKey(Point point) {
        if (point == null) {
            return Long.MIN_VALUE;
        }
        int regionSize = Math.max(1, RECENT_CLICK_REGION_CELL_SIZE_PX);
        int regionX = Math.floorDiv(point.x, regionSize);
        int regionY = Math.floorDiv(point.y, regionSize);
        long x = ((long) regionX) & 0xFFFFFFFFL;
        long y = ((long) regionY) & 0xFFFFFFFFL;
        return (x << 32) | y;
    }

    private static Point decodeClickPointKey(long key) {
        if (key == Long.MIN_VALUE) {
            return null;
        }
        int x = (int) (key >>> 32);
        int y = (int) key;
        return new Point(x, y);
    }

    private static double pixelDistance(Point a, Point b) {
        if (a == null || b == null) {
            return Double.MAX_VALUE;
        }
        double dx = (double) a.x - (double) b.x;
        double dy = (double) a.y - (double) b.y;
        return Math.hypot(dx, dy);
    }

    private static int randomIntInclusive(int minInclusive, int maxInclusive) {
        int lo = Math.min(minInclusive, maxInclusive);
        int hi = Math.max(minInclusive, maxInclusive);
        if (hi <= lo) {
            return lo;
        }
        return ThreadLocalRandom.current().nextInt(lo, hi + 1);
    }

    private static long randomLongInclusive(long minInclusive, long maxInclusive) {
        long lo = Math.min(minInclusive, maxInclusive);
        long hi = Math.max(minInclusive, maxInclusive);
        if (hi <= lo) {
            return lo;
        }
        return ThreadLocalRandom.current().nextLong(lo, hi + 1L);
    }

    private static double randomBetweenInclusive(double min, double max) {
        if (max <= min) {
            return min;
        }
        return ThreadLocalRandom.current().nextDouble(min, Math.nextUp(max));
    }
}
