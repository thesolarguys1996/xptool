package com.xptool.executor;

import com.google.gson.JsonObject;
import com.xptool.sessions.idle.IdleSkillContext;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

final class IdleCursorTargetPlanner {
    private static final int SIDE_LEFT = 0;
    private static final int SIDE_RIGHT = 1;
    private static final int SIDE_TOP = 2;
    private static final int SIDE_BOTTOM = 3;
    private static final double OFFSCREEN_WINDOW_MARGIN_MIN_INCHES = 1.0;
    private static final double OFFSCREEN_WINDOW_MARGIN_MAX_INCHES = 2.0;
    private static final double OFFSCREEN_FAR_TARGET_MIN_GAP_INCHES = 1.75;
    private static final double OFFSCREEN_NEAR_TARGET_MAX_GAP_INCHES = 2.85;
    private static final double OFFSCREEN_FAR_TARGET_MAX_GAP_INCHES = 6.20;
    private static final double OFFSCREEN_FAR_TARGET_SIDE_ROOM_MIN_RATIO = 0.30;
    private static final int OFFSCREEN_FAR_TARGET_FREEFORM_SAMPLE_CHANCE_PERCENT = 65;
    private static final boolean OFFSCREEN_FAR_TARGET_MAX_ROOM_ONLY = false;
    private static final int OFFSCREEN_USABLE_SCREEN_INSET_PX = 2;
    private static final boolean OFFSCREEN_NEAR_TARGET_FALLBACK_ENABLED = true;
    private static final boolean OFFSCREEN_CONSTRAIN_TO_CURRENT_MONITOR = true;
    private static final int OFFSCREEN_FAR_TARGET_MAX_ATTEMPTS = 36;
    private static final int OFFSCREEN_NEAR_TARGET_MAX_ATTEMPTS = 16;
    private static final int OFFSCREEN_SIDE_REPEAT_AVOID_BASE_CHANCE_PERCENT = 56;
    private static final int OFFSCREEN_SIDE_REPEAT_AVOID_STREAK_STEP_PERCENT = 12;
    private static final int OFFSCREEN_SIDE_REPEAT_AVOID_MAX_CHANCE_PERCENT = 90;
    private static final int OFFSCREEN_RECENT_REGION_HISTORY_SIZE = 8;
    private static final int OFFSCREEN_REGION_GRID_SIZE = 4;
    private static final int OFFSCREEN_WOODCUT_MONITOR_WIDE_MAX_ATTEMPTS = 52;
    private static final double OFFSCREEN_WOODCUT_VERTICAL_SIDE_WEIGHT_MULTIPLIER = 1.65;
    private static final double OFFSCREEN_WOODCUT_HORIZONTAL_SIDE_WEIGHT_MULTIPLIER = 0.62;
    private static final int OFFSCREEN_WOODCUT_OPPOSITE_SIDE_AVOID_BASE_CHANCE_PERCENT = 64;
    private static final int OFFSCREEN_WOODCUT_OPPOSITE_SIDE_AVOID_STREAK_STEP_PERCENT = 8;
    private static final int OFFSCREEN_WOODCUT_OPPOSITE_SIDE_AVOID_MAX_CHANCE_PERCENT = 92;
    private static final int OFFSCREEN_WOODCUT_SAME_SIDE_SOFT_PENALTY_PERCENT = 32;
    private static final int OFFSCREEN_WOODCUT_EDGE_BAND_MIN_PX = 42;
    private static final int OFFSCREEN_WOODCUT_EDGE_BAND_MAX_PX = 420;
    private static final double OFFSCREEN_WOODCUT_EDGE_BAND_RATIO = 0.24;
    private static final int OFFSCREEN_WOODCUT_EDGE_BAND_FREEFORM_SAMPLE_CHANCE_PERCENT = 30;
    private static final int OFFSCREEN_WOODCUT_CENTER_AVOID_CHANCE_PERCENT = 74;
    private static final double OFFSCREEN_WOODCUT_CENTER_AVOID_RATIO = 0.16;
    private static final int IDLE_DRIFT_LOCAL_SAMPLE_CHANCE_PERCENT = 90;
    private static final double IDLE_DRIFT_MAX_JUMP_RATIO = 0.19;
    private static final int IDLE_DRIFT_MAX_JUMP_MIN_PX = 90;
    private static final int IDLE_DRIFT_MAX_JUMP_MAX_PX = 280;
    private static final int IDLE_DRIFT_RADIUS_JITTER_PX = 10;
    private static final int IDLE_PARK_NEAREST_SIDE_BIAS_CHANCE_PERCENT = 72;
    private static final double IDLE_PARK_CURSOR_ORTHOGONAL_BAND_RATIO = 0.09;
    private static final double IDLE_PARK_MAX_JUMP_RATIO = 0.24;
    private static final int IDLE_PARK_MAX_JUMP_MIN_PX = 140;
    private static final int IDLE_PARK_MAX_JUMP_MAX_PX = 340;
    private static final boolean OFFSCREEN_PLANNER_DEBUG_TELEMETRY_ENABLED =
        Boolean.parseBoolean(System.getProperty("xptool.idleOffscreenPlannerDebugTelemetryEnabled", "true"));
    private static final int FALLBACK_SCREEN_DPI = 96;

    interface Host {
        Optional<Rectangle> resolveInventoryInteractionRegionCanvas();

        Optional<Point> randomCanvasPointInRegion(Rectangle region, int insetPx);

        boolean isUsableCanvasPoint(Point point);

        int canvasWidth();

        int canvasHeight();

        Point currentMouseCanvasPoint();

        Optional<Rectangle> resolveClientCanvasBoundsScreen();

        Optional<Rectangle> resolveClientWindowBoundsScreen();

        Optional<Rectangle> resolveScreenBoundsForPoint(Point point);

        Point currentPointerLocationOr(Point fallback);

        IdleSkillContext resolveIdleSkillContext();

        IdleCadenceTuning activeIdleCadenceTuning();

        void emitIdleEvent(String reason, JsonObject details);
    }

    private final Host host;
    private String lastFarFailureReason = "";
    private int lastOffscreenSelectedSide = -1;
    private int offscreenSideRepeatStreak = 0;
    private final long[] recentOffscreenRegionKeys = new long[OFFSCREEN_RECENT_REGION_HISTORY_SIZE];
    private int recentOffscreenRegionWriteIndex = 0;

    IdleCursorTargetPlanner(Host host) {
        this.host = host;
        initializeOffscreenRegionHistory();
    }

    Optional<Point> resolveIdleHoverTargetCanvasPoint() {
        Optional<Rectangle> regionOpt = host.resolveInventoryInteractionRegionCanvas();
        if (regionOpt.isPresent() && ThreadLocalRandom.current().nextInt(100) < 78) {
            return host.randomCanvasPointInRegion(regionOpt.get(), 10);
        }
        return resolveIdleDriftTargetCanvasPoint();
    }

    Optional<Point> resolveIdleDriftTargetCanvasPoint() {
        int width = host.canvasWidth();
        int height = host.canvasHeight();
        if (width <= 0 || height <= 0) {
            return Optional.empty();
        }
        int minX = Math.max(8, (int) Math.round(width * 0.24));
        int maxX = Math.max(minX, (int) Math.round(width * 0.78));
        int minY = Math.max(8, (int) Math.round(height * 0.26));
        int maxY = Math.max(minY, (int) Math.round(height * 0.78));
        Rectangle driftBounds = new Rectangle(
            minX,
            minY,
            Math.max(1, (maxX - minX) + 1),
            Math.max(1, (maxY - minY) + 1)
        );
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        Point cursorCanvas = host.currentMouseCanvasPoint();
        boolean cursorUsable = host.isUsableCanvasPoint(cursorCanvas);
        int maxJumpPx = resolveIdleMaxJumpPx(
            width,
            height,
            IDLE_DRIFT_MAX_JUMP_RATIO,
            IDLE_DRIFT_MAX_JUMP_MIN_PX,
            IDLE_DRIFT_MAX_JUMP_MAX_PX
        );
        Point p = null;
        if (cursorUsable && rng.nextInt(100) < IDLE_DRIFT_LOCAL_SAMPLE_CHANCE_PERCENT) {
            p = sampleLocalCandidateAroundCursor(cursorCanvas, driftBounds, maxJumpPx);
        }
        if (p == null && cursorUsable) {
            p = sampleLocalCandidateAroundCursor(cursorCanvas, driftBounds, maxJumpPx);
        }
        if (p == null) {
            int x = rng.nextInt(minX, maxX + 1);
            int y = rng.nextInt(minY, maxY + 1);
            p = new Point(x, y);
        }
        if (cursorUsable) {
            p = constrainPointToMaxDistance(p, cursorCanvas, maxJumpPx, driftBounds);
        }
        return host.isUsableCanvasPoint(p) ? Optional.of(p) : Optional.empty();
    }

    Optional<Point> resolveIdleParkingTargetCanvasPoint() {
        int width = host.canvasWidth();
        int height = host.canvasHeight();
        if (width <= 0 || height <= 0) {
            return Optional.empty();
        }
        int minX = 8;
        int maxX = Math.max(minX, width - 9);
        int minY = 8;
        int maxY = Math.max(minY, height - 9);
        Rectangle parkingBounds = new Rectangle(
            minX,
            minY,
            Math.max(1, (maxX - minX) + 1),
            Math.max(1, (maxY - minY) + 1)
        );
        Point cursorCanvas = host.currentMouseCanvasPoint();
        boolean cursorUsable = host.isUsableCanvasPoint(cursorCanvas);
        int maxJumpPx = resolveIdleMaxJumpPx(
            width,
            height,
            IDLE_PARK_MAX_JUMP_RATIO,
            IDLE_PARK_MAX_JUMP_MIN_PX,
            IDLE_PARK_MAX_JUMP_MAX_PX
        );

        int side = cursorUsable
            && ThreadLocalRandom.current().nextInt(100) < IDLE_PARK_NEAREST_SIDE_BIAS_CHANCE_PERCENT
            ? nearestEdgeSide(cursorCanvas, width, height)
            : ThreadLocalRandom.current().nextInt(4);
        if (side < 0) {
            side = ThreadLocalRandom.current().nextInt(4);
        }
        int edgeBandX = Math.max(10, (int) Math.round(width * 0.14));
        int edgeBandY = Math.max(10, (int) Math.round(height * 0.14));
        int cursorBandX = Math.max(24, (int) Math.round(width * IDLE_PARK_CURSOR_ORTHOGONAL_BAND_RATIO));
        int cursorBandY = Math.max(24, (int) Math.round(height * IDLE_PARK_CURSOR_ORTHOGONAL_BAND_RATIO));

        int x;
        int y;
        if (side == 0) {
            int leftMax = Math.min(maxX, minX + edgeBandX);
            x = ThreadLocalRandom.current().nextInt(minX, leftMax + 1);
            int yLow = cursorUsable
                ? Math.max(minY, cursorCanvas.y - cursorBandY)
                : Math.max(minY, (int) Math.round(height * 0.18));
            int yHigh = cursorUsable
                ? Math.min(maxY, cursorCanvas.y + cursorBandY)
                : Math.min(maxY, (int) Math.round(height * 0.86));
            if (yHigh < yLow) {
                return resolveIdleDriftTargetCanvasPoint();
            }
            y = ThreadLocalRandom.current().nextInt(yLow, yHigh + 1);
        } else if (side == 1) {
            int rightMin = Math.max(minX, maxX - edgeBandX);
            x = ThreadLocalRandom.current().nextInt(rightMin, maxX + 1);
            int yLow = cursorUsable
                ? Math.max(minY, cursorCanvas.y - cursorBandY)
                : Math.max(minY, (int) Math.round(height * 0.18));
            int yHigh = cursorUsable
                ? Math.min(maxY, cursorCanvas.y + cursorBandY)
                : Math.min(maxY, (int) Math.round(height * 0.86));
            if (yHigh < yLow) {
                return resolveIdleDriftTargetCanvasPoint();
            }
            y = ThreadLocalRandom.current().nextInt(yLow, yHigh + 1);
        } else if (side == 2) {
            int topMax = Math.min(maxY, minY + edgeBandY);
            y = ThreadLocalRandom.current().nextInt(minY, topMax + 1);
            int xLow = cursorUsable
                ? Math.max(minX, cursorCanvas.x - cursorBandX)
                : Math.max(minX, (int) Math.round(width * 0.20));
            int xHigh = cursorUsable
                ? Math.min(maxX, cursorCanvas.x + cursorBandX)
                : Math.min(maxX, (int) Math.round(width * 0.86));
            if (xHigh < xLow) {
                return resolveIdleDriftTargetCanvasPoint();
            }
            x = ThreadLocalRandom.current().nextInt(xLow, xHigh + 1);
        } else {
            int bottomMin = Math.max(minY, maxY - edgeBandY);
            y = ThreadLocalRandom.current().nextInt(bottomMin, maxY + 1);
            int xLow = cursorUsable
                ? Math.max(minX, cursorCanvas.x - cursorBandX)
                : Math.max(minX, (int) Math.round(width * 0.20));
            int xHigh = cursorUsable
                ? Math.min(maxX, cursorCanvas.x + cursorBandX)
                : Math.min(maxX, (int) Math.round(width * 0.86));
            if (xHigh < xLow) {
                return resolveIdleDriftTargetCanvasPoint();
            }
            x = ThreadLocalRandom.current().nextInt(xLow, xHigh + 1);
        }

        Point p = new Point(x, y);
        if (cursorUsable) {
            p = constrainPointToMaxDistance(p, cursorCanvas, maxJumpPx, parkingBounds);
        }
        if (!host.isUsableCanvasPoint(p)) {
            return resolveIdleDriftTargetCanvasPoint();
        }
        return Optional.of(p);
    }

    private static Point sampleLocalCandidateAroundCursor(Point cursorCanvas, Rectangle bounds, int maxRadiusPx) {
        if (cursorCanvas == null || bounds == null) {
            return null;
        }
        int boundedMaxRadius = Math.max(24, maxRadiusPx);
        int minRadius = Math.max(10, Math.min(40, boundedMaxRadius / 3));
        int radial = randomIntInclusive(minRadius, boundedMaxRadius);
        double angle = ThreadLocalRandom.current().nextDouble(0.0, Math.PI * 2.0);
        int jitterX = randomIntInclusive(-IDLE_DRIFT_RADIUS_JITTER_PX, IDLE_DRIFT_RADIUS_JITTER_PX);
        int jitterY = randomIntInclusive(-IDLE_DRIFT_RADIUS_JITTER_PX, IDLE_DRIFT_RADIUS_JITTER_PX);
        int localX = cursorCanvas.x + (int) Math.round(Math.cos(angle) * radial) + jitterX;
        int localY = cursorCanvas.y + (int) Math.round(Math.sin(angle) * radial) + jitterY;
        return clampPointToRectangle(new Point(localX, localY), bounds);
    }

    private static Point constrainPointToMaxDistance(
        Point point,
        Point anchor,
        int maxDistancePx,
        Rectangle clampBounds
    ) {
        if (point == null || anchor == null) {
            return point;
        }
        int cap = Math.max(8, maxDistancePx);
        double dx = (double) point.x - (double) anchor.x;
        double dy = (double) point.y - (double) anchor.y;
        double distance = Math.hypot(dx, dy);
        if (!(distance > (double) cap)) {
            return point;
        }
        double scale = (double) cap / distance;
        int limitedX = anchor.x + (int) Math.round(dx * scale);
        int limitedY = anchor.y + (int) Math.round(dy * scale);
        Point limited = new Point(limitedX, limitedY);
        return clampPointToRectangle(limited, clampBounds);
    }

    private static int resolveIdleMaxJumpPx(
        int width,
        int height,
        double ratio,
        int minPx,
        int maxPx
    ) {
        int base = (int) Math.round(Math.min(width, height) * Math.max(0.05, ratio));
        return Math.max(minPx, Math.min(maxPx, base));
    }

    private static int nearestEdgeSide(Point cursorCanvas, int width, int height) {
        if (cursorCanvas == null || width <= 0 || height <= 0) {
            return -1;
        }
        int left = Math.max(0, cursorCanvas.x);
        int right = Math.max(0, (width - 1) - cursorCanvas.x);
        int top = Math.max(0, cursorCanvas.y);
        int bottom = Math.max(0, (height - 1) - cursorCanvas.y);
        int min = Math.min(Math.min(left, right), Math.min(top, bottom));
        int[] matches = new int[4];
        int count = 0;
        if (left == min) {
            matches[count++] = SIDE_LEFT;
        }
        if (right == min) {
            matches[count++] = SIDE_RIGHT;
        }
        if (top == min) {
            matches[count++] = SIDE_TOP;
        }
        if (bottom == min) {
            matches[count++] = SIDE_BOTTOM;
        }
        if (count <= 0) {
            return -1;
        }
        return matches[ThreadLocalRandom.current().nextInt(count)];
    }

    Optional<Point> resolveIdleOffscreenTargetScreenPoint() {
        lastFarFailureReason = "";
        IdleSkillContext idleContext = resolveIdleSkillContext();
        Optional<Rectangle> windowBoundsOpt = host.resolveClientWindowBoundsScreen();
        if (windowBoundsOpt.isEmpty()) {
            emitPlannerTelemetry(
                "idle_offscreen_planner_unavailable",
                jsonDetails("stage", "window_bounds_missing")
            );
            emitPlannerTelemetry(
                "offscreen_target_unavailable",
                jsonDetails("failureReason", "missing_window_bounds")
            );
            return Optional.empty();
        }
        Rectangle windowBounds = windowBoundsOpt.get();
        Point windowCenter = new Point(
            (int) Math.round(windowBounds.getCenterX()),
            (int) Math.round(windowBounds.getCenterY())
        );
        Optional<Rectangle> screenBoundsOpt = host.resolveScreenBoundsForPoint(windowCenter);
        if (screenBoundsOpt.isEmpty()) {
            emitPlannerTelemetry(
                "idle_offscreen_planner_unavailable",
                jsonDetails(
                    "stage", "screen_bounds_missing",
                    "windowX", windowBounds.x,
                    "windowY", windowBounds.y,
                    "windowWidth", windowBounds.width,
                    "windowHeight", windowBounds.height
                )
            );
            emitPlannerTelemetry(
                "offscreen_target_unavailable",
                jsonDetails(
                    "failureReason", "missing_screen_bounds",
                    "windowX", windowBounds.x,
                    "windowY", windowBounds.y,
                    "windowWidth", windowBounds.width,
                    "windowHeight", windowBounds.height
                )
            );
            return Optional.empty();
        }
        Rectangle screenBounds = screenBoundsOpt.get();
        Rectangle planningBounds = OFFSCREEN_CONSTRAIN_TO_CURRENT_MONITOR
            ? new Rectangle(screenBounds)
            : resolveVirtualDesktopBounds(screenBounds);
        String boundsSource = OFFSCREEN_CONSTRAIN_TO_CURRENT_MONITOR ? "current_monitor" : "virtual_desktop";
        Rectangle usable = insetRectangle(planningBounds, OFFSCREEN_USABLE_SCREEN_INSET_PX);
        if (usable.width <= 0 || usable.height <= 0) {
            emitPlannerTelemetry(
                "idle_offscreen_planner_unavailable",
                jsonDetails(
                    "stage", "usable_bounds_invalid",
                    "boundsSource", boundsSource,
                    "usableX", usable.x,
                    "usableY", usable.y,
                    "usableWidth", usable.width,
                    "usableHeight", usable.height
                )
            );
            emitPlannerTelemetry(
                "offscreen_target_unavailable",
                jsonDetails(
                    "failureReason", "usable_area_invalid",
                    "boundsSource", boundsSource,
                    "usableX", usable.x,
                    "usableY", usable.y,
                    "usableWidth", usable.width,
                    "usableHeight", usable.height
                )
            );
            return Optional.empty();
        }

        int windowMinX = windowBounds.x;
        int windowMaxX = windowBounds.x + Math.max(0, windowBounds.width - 1);
        int windowMinY = windowBounds.y;
        int windowMaxY = windowBounds.y + Math.max(0, windowBounds.height - 1);
        int usableMinX = usable.x;
        int usableMaxX = usable.x + Math.max(0, usable.width - 1);
        int usableMinY = usable.y;
        int usableMaxY = usable.y + Math.max(0, usable.height - 1);
        int leftRoom = Math.max(0, windowMinX - usableMinX);
        int rightRoom = Math.max(0, usableMaxX - windowMaxX);
        int topRoom = Math.max(0, windowMinY - usableMinY);
        int bottomRoom = Math.max(0, usableMaxY - windowMaxY);

        int dpi = resolveScreenDpi();
        int defaultMinMarginPx = Math.max(12, (int) Math.round(dpi * OFFSCREEN_WINDOW_MARGIN_MIN_INCHES));
        int defaultMaxMarginPx = Math.max(defaultMinMarginPx, (int) Math.round(dpi * OFFSCREEN_WINDOW_MARGIN_MAX_INCHES));
        int defaultMinGapPx = Math.max(
            defaultMinMarginPx + 18,
            (int) Math.round(Math.max(0.0, dpi) * OFFSCREEN_FAR_TARGET_MIN_GAP_INCHES)
        );
        int defaultNearMaxGapPx = Math.max(
            defaultMinGapPx,
            (int) Math.round(Math.max(0.0, dpi) * OFFSCREEN_NEAR_TARGET_MAX_GAP_INCHES)
        );
        int defaultFarMaxGapPx = Math.max(
            defaultMinGapPx,
            (int) Math.round(Math.max(0.0, dpi) * OFFSCREEN_FAR_TARGET_MAX_GAP_INCHES)
        );
        IdleCadenceTuning tuning = host.activeIdleCadenceTuning();
        if (tuning == null) {
            tuning = IdleCadenceTuning.none();
        }
        int minMarginPx = tuning.resolveOffscreenWindowMarginMinPx(defaultMinMarginPx);
        int maxMarginPx = tuning.resolveOffscreenWindowMarginMaxPx(defaultMaxMarginPx, minMarginPx);
        int minGapPx = tuning.resolveOffscreenFarTargetMinGapPx(defaultMinGapPx, minMarginPx);
        int maxGapPx = tuning.resolveOffscreenNearTargetMaxGapPx(defaultNearMaxGapPx, minGapPx);
        int farMaxGapPx = tuning.resolveOffscreenFarTargetMaxGapPx(defaultFarMaxGapPx, maxGapPx);
        emitPlannerTelemetry(
            "idle_offscreen_planner_context",
            jsonDetails(
                "idleContext", idleContext.name(),
                "windowX", windowBounds.x,
                "windowY", windowBounds.y,
                "windowWidth", windowBounds.width,
                "windowHeight", windowBounds.height,
                "boundsSource", boundsSource,
                "screenX", screenBounds.x,
                "screenY", screenBounds.y,
                "screenWidth", screenBounds.width,
                "screenHeight", screenBounds.height,
                "planningBoundsX", planningBounds.x,
                "planningBoundsY", planningBounds.y,
                "planningBoundsWidth", planningBounds.width,
                "planningBoundsHeight", planningBounds.height,
                "usableX", usable.x,
                "usableY", usable.y,
                "usableWidth", usable.width,
                "usableHeight", usable.height,
                "leftRoom", leftRoom,
                "rightRoom", rightRoom,
                "topRoom", topRoom,
                "bottomRoom", bottomRoom,
                "dpi", dpi,
                "minMarginPx", minMarginPx,
                "maxMarginPx", maxMarginPx,
                "minGapPx", minGapPx,
                "maxGapPx", maxGapPx,
                "farMaxGapPx", farMaxGapPx,
                "defaultMinMarginPx", defaultMinMarginPx,
                "defaultMaxMarginPx", defaultMaxMarginPx,
                "defaultMinGapPx", defaultMinGapPx,
                "defaultNearMaxGapPx", defaultNearMaxGapPx,
                "defaultFarMaxGapPx", defaultFarMaxGapPx,
                "farMaxAttempts", OFFSCREEN_FAR_TARGET_MAX_ATTEMPTS,
                "farMinGapInches", OFFSCREEN_FAR_TARGET_MIN_GAP_INCHES,
                "maxGapInches", OFFSCREEN_NEAR_TARGET_MAX_GAP_INCHES,
                "farMaxGapInches", OFFSCREEN_FAR_TARGET_MAX_GAP_INCHES,
                "farSideRoomMinRatio", OFFSCREEN_FAR_TARGET_SIDE_ROOM_MIN_RATIO,
                "farFreeformSampleChancePercent", OFFSCREEN_FAR_TARGET_FREEFORM_SAMPLE_CHANCE_PERCENT,
                "farMaxRoomOnly", OFFSCREEN_FAR_TARGET_MAX_ROOM_ONLY,
                "nearFallbackEnabled", OFFSCREEN_NEAR_TARGET_FALLBACK_ENABLED
            )
        );
        if (idleContext == IdleSkillContext.WOODCUTTING) {
            Optional<Point> monitorWideTarget = resolveWoodcuttingMonitorWideOffscreenTargetScreenPoint(
                windowBounds,
                usable,
                minGapPx,
                minMarginPx
            );
            if (monitorWideTarget.isPresent()) {
                Point target = monitorWideTarget.get();
                emitPlannerTelemetry(
                    "idle_offscreen_planner_result",
                    jsonDetails(
                        "path", "woodcut_monitor_wide",
                        "targetX", target.x,
                        "targetY", target.y,
                        "farFailed", false,
                        "nearFallbackRan", false
                    )
                );
                emitPlannerTelemetry(
                    "offscreen_target_resolved",
                    jsonDetails(
                        "path", "woodcut_monitor_wide",
                        "targetX", target.x,
                        "targetY", target.y
                    )
                );
                return monitorWideTarget;
            }
        }
        Optional<Point> farTarget =
            resolveFarOffscreenTargetScreenPoint(windowBounds, usable, minGapPx, minMarginPx, farMaxGapPx);
        if (farTarget.isPresent()) {
            Point target = farTarget.get();
            emitPlannerTelemetry(
                "idle_offscreen_planner_result",
                jsonDetails(
                    "path", "far",
                    "targetX", target.x,
                    "targetY", target.y,
                    "farFailed", false,
                    "nearFallbackRan", false
                )
            );
            emitPlannerTelemetry(
                "offscreen_target_resolved",
                jsonDetails(
                    "path", "far",
                    "targetX", target.x,
                    "targetY", target.y
                )
            );
            return farTarget;
        }
        String farFailureReason = lastFarFailureReason == null || lastFarFailureReason.isBlank()
            ? "far_target_unavailable"
            : lastFarFailureReason;
        emitPlannerTelemetry(
            "idle_offscreen_far_target_failed",
            jsonDetails(
                "farFailed", true,
                "nearFallbackEnabled", OFFSCREEN_NEAR_TARGET_FALLBACK_ENABLED,
                "minGapPx", minGapPx,
                "farFailureReason", farFailureReason
            )
        );
        if (!OFFSCREEN_NEAR_TARGET_FALLBACK_ENABLED) {
            emitPlannerTelemetry(
                "idle_offscreen_planner_result",
                jsonDetails(
                    "path", "none",
                    "farFailed", true,
                    "nearFallbackRan", false
                )
            );
            emitPlannerTelemetry(
                "offscreen_target_unavailable",
                jsonDetails(
                    "failureReason", "far_attempts_exhausted".equals(farFailureReason)
                        ? "far_attempts_exhausted"
                        : "far_target_unavailable",
                    "internalFailureReason", farFailureReason
                )
            );
            return Optional.empty();
        }

        int yBandMin = windowBounds.y + (int) Math.round(windowBounds.height * 0.18);
        int yBandMax = windowBounds.y + (int) Math.round(windowBounds.height * 0.86);
        int xBandMin = windowBounds.x + (int) Math.round(windowBounds.width * 0.20);
        int xBandMax = windowBounds.x + (int) Math.round(windowBounds.width * 0.86);
        emitPlannerTelemetry(
            "idle_offscreen_near_fallback_start",
            jsonDetails(
                "nearFallbackRan", true,
                "nearMaxAttempts", OFFSCREEN_NEAR_TARGET_MAX_ATTEMPTS,
                "maxGapPx", maxGapPx,
                "leftRoom", leftRoom,
                "rightRoom", rightRoom,
                "topRoom", topRoom,
                "bottomRoom", bottomRoom,
                "yBandMin", yBandMin,
                "yBandMax", yBandMax,
                "xBandMin", xBandMin,
                "xBandMax", xBandMax
            )
        );
        if (leftRoom <= 0 && rightRoom <= 0 && topRoom <= 0 && bottomRoom <= 0) {
            emitPlannerTelemetry(
                "idle_offscreen_near_fallback_failed",
                jsonDetails(
                    "reason", "no_room",
                    "nearFallbackRan", true,
                    "farFailed", true
                )
            );
            emitPlannerTelemetry(
                "idle_offscreen_planner_result",
                jsonDetails(
                    "path", "none",
                    "farFailed", true,
                    "nearFallbackRan", true
                )
            );
            emitPlannerTelemetry(
                "offscreen_target_unavailable",
                jsonDetails(
                    "failureReason", "near_fallback_exhausted",
                    "internalFailureReason", "no_room",
                    "farFailureReason", farFailureReason
                )
            );
            return Optional.empty();
        }

        for (int attempt = 0; attempt < OFFSCREEN_NEAR_TARGET_MAX_ATTEMPTS; attempt++) {
            int side = pickSideByRoomAvoidRecent(leftRoom, rightRoom, topRoom, bottomRoom);
            Integer orthogonal = null;
            int sideRoom = 0;

            if (side == SIDE_LEFT) {
                sideRoom = leftRoom;
                int minY = Math.max(usable.y, yBandMin);
                int maxY = Math.min(usable.y + usable.height - 1, yBandMax);
                if (maxY >= minY) {
                    orthogonal = randomIntInclusive(minY, maxY);
                }
            } else if (side == SIDE_RIGHT) {
                sideRoom = rightRoom;
                int minY = Math.max(usable.y, yBandMin);
                int maxY = Math.min(usable.y + usable.height - 1, yBandMax);
                if (maxY >= minY) {
                    orthogonal = randomIntInclusive(minY, maxY);
                }
            } else if (side == SIDE_TOP) {
                sideRoom = topRoom;
                int minX = Math.max(usable.x, xBandMin);
                int maxX = Math.min(usable.x + usable.width - 1, xBandMax);
                if (maxX >= minX) {
                    orthogonal = randomIntInclusive(minX, maxX);
                }
            } else {
                sideRoom = bottomRoom;
                int minX = Math.max(usable.x, xBandMin);
                int maxX = Math.min(usable.x + usable.width - 1, xBandMax);
                if (maxX >= minX) {
                    orthogonal = randomIntInclusive(minX, maxX);
                }
            }
            if (sideRoom <= 0) {
                emitPlannerTelemetry(
                    "idle_offscreen_near_candidate_rejected",
                    jsonDetails(
                        "attempt", attempt,
                        "side", sideName(side),
                        "sideRoom", sideRoom,
                        "reason", "no_side_room"
                    )
                );
                continue;
            }
            if (orthogonal == null) {
                emitPlannerTelemetry(
                    "idle_offscreen_near_candidate_rejected",
                    jsonDetails(
                        "attempt", attempt,
                        "side", sideName(side),
                        "sideRoom", sideRoom,
                        "reason", "orthogonal_band_unavailable"
                    )
                );
                continue;
            }
            int requestedSideMinMargin = sideRoom >= minMarginPx ? minMarginPx : 1;
            int sideMaxMargin = Math.min(maxMarginPx, sideRoom);
            sideMaxMargin = Math.min(sideMaxMargin, maxGapPx);
            if (sideMaxMargin <= 0) {
                emitPlannerTelemetry(
                    "idle_offscreen_near_candidate_rejected",
                    jsonDetails(
                        "attempt", attempt,
                        "side", sideName(side),
                        "sideRoom", sideRoom,
                        "sideMinMargin", requestedSideMinMargin,
                        "sideMaxMargin", sideMaxMargin,
                        "maxGapPx", maxGapPx,
                        "reason", "no_valid_margin_after_max_gap_cap"
                    )
                );
                continue;
            }
            int sideMinMargin = Math.min(requestedSideMinMargin, sideMaxMargin);
            if (requestedSideMinMargin > sideMinMargin) {
                emitPlannerTelemetry(
                    "idle_offscreen_near_candidate_adjusted",
                    jsonDetails(
                        "attempt", attempt,
                        "side", sideName(side),
                        "sideRoom", sideRoom,
                        "requestedSideMinMargin", requestedSideMinMargin,
                        "sideMinMargin", sideMinMargin,
                        "sideMaxMargin", sideMaxMargin,
                        "maxGapPx", maxGapPx,
                        "reason", "side_min_margin_capped_by_max_gap"
                    )
                );
            }
            int margin = randomIntInclusive(sideMinMargin, sideMaxMargin);
            Point candidate = resolveOffWindowPoint(windowBounds, side, margin, orthogonal);
            if (candidate == null) {
                emitPlannerTelemetry(
                    "idle_offscreen_near_candidate_rejected",
                    jsonDetails(
                        "attempt", attempt,
                        "side", sideName(side),
                        "sideRoom", sideRoom,
                        "margin", margin,
                        "orthogonal", orthogonal,
                        "reason", "candidate_null"
                    )
                );
                continue;
            }
            if (!usable.contains(candidate)) {
                emitPlannerTelemetry(
                    "idle_offscreen_near_candidate_rejected",
                    jsonDetails(
                        "attempt", attempt,
                        "side", sideName(side),
                        "sideRoom", sideRoom,
                        "margin", margin,
                        "orthogonal", orthogonal,
                        "candidateX", candidate.x,
                        "candidateY", candidate.y,
                        "reason", "candidate_outside_usable"
                    )
                );
                continue;
            }
            if (windowBounds.contains(candidate)) {
                emitPlannerTelemetry(
                    "idle_offscreen_near_candidate_rejected",
                    jsonDetails(
                        "attempt", attempt,
                        "side", sideName(side),
                        "sideRoom", sideRoom,
                        "margin", margin,
                        "orthogonal", orthogonal,
                        "candidateX", candidate.x,
                        "candidateY", candidate.y,
                        "reason", "candidate_inside_window"
                    )
                );
                continue;
            }
            int gap = gapOutsideWindowPixels(candidate, windowBounds);
            if (gap < sideMinMargin) {
                emitPlannerTelemetry(
                    "idle_offscreen_near_candidate_rejected",
                    jsonDetails(
                        "attempt", attempt,
                        "side", sideName(side),
                        "sideRoom", sideRoom,
                        "margin", margin,
                        "orthogonal", orthogonal,
                        "candidateX", candidate.x,
                        "candidateY", candidate.y,
                        "gap", gap,
                        "sideMinMargin", sideMinMargin,
                        "sideMaxMargin", sideMaxMargin,
                        "reason", "gap_below_side_min_margin"
                    )
                );
                continue;
            }
            if (gap > sideMaxMargin) {
                emitPlannerTelemetry(
                    "idle_offscreen_near_candidate_rejected",
                    jsonDetails(
                        "attempt", attempt,
                        "side", sideName(side),
                        "sideRoom", sideRoom,
                        "margin", margin,
                        "orthogonal", orthogonal,
                        "candidateX", candidate.x,
                        "candidateY", candidate.y,
                        "gap", gap,
                        "sideMinMargin", sideMinMargin,
                        "sideMaxMargin", sideMaxMargin,
                        "reason", "gap_above_side_max_margin"
                    )
                );
                continue;
            }
            if (gap > maxGapPx) {
                emitPlannerTelemetry(
                    "idle_offscreen_near_candidate_rejected",
                    jsonDetails(
                        "attempt", attempt,
                        "side", sideName(side),
                        "sideRoom", sideRoom,
                        "margin", margin,
                        "orthogonal", orthogonal,
                        "candidateX", candidate.x,
                        "candidateY", candidate.y,
                        "gap", gap,
                        "maxGapPx", maxGapPx,
                        "reason", "gap_above_max_gap"
                    )
                );
                continue;
            }
            if (isRecentOffscreenRegion(side, candidate, usable)) {
                emitPlannerTelemetry(
                    "idle_offscreen_near_candidate_rejected",
                    jsonDetails(
                        "attempt", attempt,
                        "side", sideName(side),
                        "sideRoom", sideRoom,
                        "margin", margin,
                        "orthogonal", orthogonal,
                        "candidateX", candidate.x,
                        "candidateY", candidate.y,
                        "reason", "recent_region_repeat"
                    )
                );
                continue;
            }
            emitPlannerTelemetry(
                "idle_offscreen_near_candidate_selected",
                jsonDetails(
                    "attempt", attempt,
                    "side", sideName(side),
                    "sideRoom", sideRoom,
                    "margin", margin,
                    "orthogonal", orthogonal,
                    "candidateX", candidate.x,
                    "candidateY", candidate.y,
                    "gap", gap
                )
            );
            noteOffscreenSelection(side, candidate, usable);
            emitPlannerTelemetry(
                "idle_offscreen_planner_result",
                jsonDetails(
                    "path", "near",
                    "targetX", candidate.x,
                    "targetY", candidate.y,
                    "farFailed", true,
                    "nearFallbackRan", true
                )
            );
            emitPlannerTelemetry(
                "offscreen_target_resolved",
                jsonDetails(
                    "path", "near_fallback",
                    "targetX", candidate.x,
                    "targetY", candidate.y
                )
            );
            return Optional.of(candidate);
        }
        emitPlannerTelemetry(
            "idle_offscreen_near_fallback_failed",
            jsonDetails(
                "reason", "attempts_exhausted",
                "nearFallbackRan", true,
                "nearMaxAttempts", OFFSCREEN_NEAR_TARGET_MAX_ATTEMPTS,
                "farFailed", true
            )
        );
        emitPlannerTelemetry(
            "idle_offscreen_planner_result",
            jsonDetails(
                "path", "none",
                "farFailed", true,
                "nearFallbackRan", true
            )
        );
        emitPlannerTelemetry(
            "offscreen_target_unavailable",
            jsonDetails(
                "failureReason", "near_fallback_exhausted",
                "internalFailureReason", "attempts_exhausted",
                "farFailureReason", farFailureReason
            )
        );
        return Optional.empty();
    }

    private Optional<Point> resolveWoodcuttingMonitorWideOffscreenTargetScreenPoint(
        Rectangle windowBounds,
        Rectangle usable,
        int minGapPx,
        int minMarginPx
    ) {
        if (windowBounds == null || usable == null || usable.width <= 0 || usable.height <= 0) {
            emitPlannerTelemetry(
                "idle_offscreen_woodcut_monitor_wide_failed",
                jsonDetails(
                    "reason", "invalid_bounds",
                    "windowPresent", windowBounds != null,
                    "usablePresent", usable != null
                )
            );
            return Optional.empty();
        }
        int requiredMinGapPx = Math.max(1, Math.max(minGapPx, minMarginPx));
        int usableMinX = usable.x;
        int usableMaxX = usable.x + Math.max(0, usable.width - 1);
        int usableMinY = usable.y;
        int usableMaxY = usable.y + Math.max(0, usable.height - 1);
        int windowMinX = windowBounds.x;
        int windowMaxX = windowBounds.x + Math.max(0, windowBounds.width - 1);
        int windowMinY = windowBounds.y;
        int windowMaxY = windowBounds.y + Math.max(0, windowBounds.height - 1);
        int leftRoom = Math.max(0, windowMinX - usableMinX);
        int rightRoom = Math.max(0, usableMaxX - windowMaxX);
        int topRoom = Math.max(0, windowMinY - usableMinY);
        int bottomRoom = Math.max(0, usableMaxY - windowMaxY);
        if (leftRoom <= 0 && rightRoom <= 0 && topRoom <= 0 && bottomRoom <= 0) {
            emitPlannerTelemetry(
                "idle_offscreen_woodcut_monitor_wide_failed",
                jsonDetails(
                    "reason", "no_room",
                    "requiredMinGapPx", requiredMinGapPx
                )
            );
            return Optional.empty();
        }
        emitPlannerTelemetry(
            "idle_offscreen_woodcut_monitor_wide_search_start",
            jsonDetails(
                "attempts", OFFSCREEN_WOODCUT_MONITOR_WIDE_MAX_ATTEMPTS,
                "requiredMinGapPx", requiredMinGapPx,
                "leftRoom", leftRoom,
                "rightRoom", rightRoom,
                "topRoom", topRoom,
                "bottomRoom", bottomRoom,
                "usableX", usable.x,
                "usableY", usable.y,
                "usableWidth", usable.width,
                "usableHeight", usable.height
            )
        );
        for (int attempt = 0; attempt < OFFSCREEN_WOODCUT_MONITOR_WIDE_MAX_ATTEMPTS; attempt++) {
            int side = pickWoodcutMonitorWideSide(leftRoom, rightRoom, topRoom, bottomRoom);
            if (side < 0) {
                emitPlannerTelemetry(
                    "idle_offscreen_woodcut_monitor_wide_candidate_rejected",
                    jsonDetails(
                        "attempt", attempt,
                        "reason", "side_unavailable"
                    )
                );
                continue;
            }
            Point candidate = sampleWoodcutMonitorWideCandidateForSide(
                side,
                usable,
                windowBounds,
                requiredMinGapPx
            );
            if (candidate == null) {
                emitPlannerTelemetry(
                    "idle_offscreen_woodcut_monitor_wide_candidate_rejected",
                    jsonDetails(
                        "attempt", attempt,
                        "side", sideName(side),
                        "reason", "candidate_null"
                    )
                );
                continue;
            }
            if (windowBounds.contains(candidate)) {
                emitPlannerTelemetry(
                    "idle_offscreen_woodcut_monitor_wide_candidate_rejected",
                    jsonDetails(
                        "attempt", attempt,
                        "side", sideName(side),
                        "candidateX", candidate.x,
                        "candidateY", candidate.y,
                        "reason", "candidate_inside_window"
                    )
                );
                continue;
            }
            int gap = gapOutsideWindowPixels(candidate, windowBounds);
            if (gap < requiredMinGapPx) {
                emitPlannerTelemetry(
                    "idle_offscreen_woodcut_monitor_wide_candidate_rejected",
                    jsonDetails(
                        "attempt", attempt,
                        "side", sideName(side),
                        "candidateX", candidate.x,
                        "candidateY", candidate.y,
                        "gap", gap,
                        "requiredMinGapPx", requiredMinGapPx,
                        "reason", "gap_below_required_min"
                    )
                );
                continue;
            }
            int classifiedSide = classifyOffscreenSide(candidate, windowBounds);
            if (classifiedSide < 0) {
                emitPlannerTelemetry(
                    "idle_offscreen_woodcut_monitor_wide_candidate_rejected",
                    jsonDetails(
                        "attempt", attempt,
                        "candidateX", candidate.x,
                        "candidateY", candidate.y,
                        "requestedSide", sideName(side),
                        "reason", "side_unknown"
                    )
                );
                continue;
            }
            if (isRecentOffscreenRegion(classifiedSide, candidate, usable)) {
                emitPlannerTelemetry(
                    "idle_offscreen_woodcut_monitor_wide_candidate_rejected",
                    jsonDetails(
                        "attempt", attempt,
                        "candidateX", candidate.x,
                        "candidateY", candidate.y,
                        "side", sideName(classifiedSide),
                        "reason", "recent_region_repeat"
                    )
                );
                continue;
            }
            emitPlannerTelemetry(
                "idle_offscreen_woodcut_monitor_wide_candidate_selected",
                jsonDetails(
                    "attempt", attempt,
                    "candidateX", candidate.x,
                    "candidateY", candidate.y,
                    "requestedSide", sideName(side),
                    "side", sideName(classifiedSide),
                    "gap", gap
                )
            );
            noteOffscreenSelection(classifiedSide, candidate, usable);
            return Optional.of(candidate);
        }
        emitPlannerTelemetry(
            "idle_offscreen_woodcut_monitor_wide_failed",
            jsonDetails(
                "reason", "attempts_exhausted",
                "attempts", OFFSCREEN_WOODCUT_MONITOR_WIDE_MAX_ATTEMPTS,
                "requiredMinGapPx", requiredMinGapPx
            )
        );
        return Optional.empty();
    }

    private int pickWoodcutMonitorWideSide(int leftRoom, int rightRoom, int topRoom, int bottomRoom) {
        int left = Math.max(0, leftRoom);
        int right = Math.max(0, rightRoom);
        int top = Math.max(0, topRoom);
        int bottom = Math.max(0, bottomRoom);
        if (left <= 0 && right <= 0 && top <= 0 && bottom <= 0) {
            return -1;
        }
        double[] weights = new double[4];
        weights[SIDE_LEFT] = weightedWoodcutSide(left, false);
        weights[SIDE_RIGHT] = weightedWoodcutSide(right, false);
        weights[SIDE_TOP] = weightedWoodcutSide(top, true);
        weights[SIDE_BOTTOM] = weightedWoodcutSide(bottom, true);

        if (lastOffscreenSelectedSide >= 0) {
            weights[lastOffscreenSelectedSide] *= (100.0 - OFFSCREEN_WOODCUT_SAME_SIDE_SOFT_PENALTY_PERCENT) / 100.0;
            int opposite = oppositeSide(lastOffscreenSelectedSide);
            if (opposite >= 0) {
                int avoidChance = clampPercent(
                    OFFSCREEN_WOODCUT_OPPOSITE_SIDE_AVOID_BASE_CHANCE_PERCENT
                        + (offscreenSideRepeatStreak * OFFSCREEN_WOODCUT_OPPOSITE_SIDE_AVOID_STREAK_STEP_PERCENT),
                    OFFSCREEN_WOODCUT_OPPOSITE_SIDE_AVOID_MAX_CHANCE_PERCENT
                );
                if (ThreadLocalRandom.current().nextInt(100) < avoidChance) {
                    weights[opposite] *= 0.18;
                }
            }
        }

        double total = 0.0;
        for (int side = 0; side < weights.length; side++) {
            if (weights[side] < 0.0) {
                weights[side] = 0.0;
            }
            total += weights[side];
        }
        if (!(total > 0.0)) {
            return pickSideByRoomAvoidRecent(left, right, top, bottom);
        }
        double roll = ThreadLocalRandom.current().nextDouble(total);
        for (int side = 0; side < weights.length; side++) {
            if (roll < weights[side]) {
                return side;
            }
            roll -= weights[side];
        }
        return SIDE_BOTTOM;
    }

    private static double weightedWoodcutSide(int roomPx, boolean vertical) {
        if (roomPx <= 0) {
            return 0.0;
        }
        double multiplier = vertical
            ? OFFSCREEN_WOODCUT_VERTICAL_SIDE_WEIGHT_MULTIPLIER
            : OFFSCREEN_WOODCUT_HORIZONTAL_SIDE_WEIGHT_MULTIPLIER;
        double jitter = ThreadLocalRandom.current().nextDouble(0.86, 1.18);
        return Math.max(0.0, ((double) roomPx) * multiplier * jitter);
    }

    private static int oppositeSide(int side) {
        if (side == SIDE_LEFT) {
            return SIDE_RIGHT;
        }
        if (side == SIDE_RIGHT) {
            return SIDE_LEFT;
        }
        if (side == SIDE_TOP) {
            return SIDE_BOTTOM;
        }
        if (side == SIDE_BOTTOM) {
            return SIDE_TOP;
        }
        return -1;
    }

    private static Point sampleWoodcutMonitorWideCandidateForSide(
        int side,
        Rectangle usable,
        Rectangle windowBounds,
        int requiredMinGapPx
    ) {
        if (usable == null || windowBounds == null || usable.width <= 0 || usable.height <= 0) {
            return null;
        }
        int usableMinX = usable.x;
        int usableMaxX = usable.x + Math.max(0, usable.width - 1);
        int usableMinY = usable.y;
        int usableMaxY = usable.y + Math.max(0, usable.height - 1);
        int windowMinX = windowBounds.x;
        int windowMaxX = windowBounds.x + Math.max(0, windowBounds.width - 1);
        int windowMinY = windowBounds.y;
        int windowMaxY = windowBounds.y + Math.max(0, windowBounds.height - 1);
        int centerX = (int) Math.round(windowBounds.getCenterX());
        int centerY = (int) Math.round(windowBounds.getCenterY());
        int deadZoneX = Math.max(24, (int) Math.round(usable.width * OFFSCREEN_WOODCUT_CENTER_AVOID_RATIO));
        int deadZoneY = Math.max(24, (int) Math.round(usable.height * OFFSCREEN_WOODCUT_CENTER_AVOID_RATIO));
        int bandX = resolveWoodcutEdgeBandPx(usable.width);
        int bandY = resolveWoodcutEdgeBandPx(usable.height);

        if (side == SIDE_LEFT) {
            int maxX = Math.min(usableMaxX, windowMinX - Math.max(1, requiredMinGapPx));
            if (maxX < usableMinX) {
                return null;
            }
            int bandMaxX = Math.min(maxX, usableMinX + bandX);
            int x = sampleWoodcutEdgeCoordinate(usableMinX, bandMaxX, usableMinX, maxX);
            int y = sampleWithCenterAvoid(
                usableMinY,
                usableMaxY,
                centerY,
                deadZoneY,
                OFFSCREEN_WOODCUT_CENTER_AVOID_CHANCE_PERCENT
            );
            return new Point(x, y);
        }
        if (side == SIDE_RIGHT) {
            int minX = Math.max(usableMinX, windowMaxX + Math.max(1, requiredMinGapPx));
            if (minX > usableMaxX) {
                return null;
            }
            int bandMinX = Math.max(minX, usableMaxX - bandX);
            int x = sampleWoodcutEdgeCoordinate(bandMinX, usableMaxX, minX, usableMaxX);
            int y = sampleWithCenterAvoid(
                usableMinY,
                usableMaxY,
                centerY,
                deadZoneY,
                OFFSCREEN_WOODCUT_CENTER_AVOID_CHANCE_PERCENT
            );
            return new Point(x, y);
        }
        if (side == SIDE_TOP) {
            int maxY = Math.min(usableMaxY, windowMinY - Math.max(1, requiredMinGapPx));
            if (maxY < usableMinY) {
                return null;
            }
            int bandMaxY = Math.min(maxY, usableMinY + bandY);
            int y = sampleWoodcutEdgeCoordinate(usableMinY, bandMaxY, usableMinY, maxY);
            int x = sampleWithCenterAvoid(usableMinX, usableMaxX, centerX, deadZoneX, 58);
            return new Point(x, y);
        }
        if (side == SIDE_BOTTOM) {
            int minY = Math.max(usableMinY, windowMaxY + Math.max(1, requiredMinGapPx));
            if (minY > usableMaxY) {
                return null;
            }
            int bandMinY = Math.max(minY, usableMaxY - bandY);
            int y = sampleWoodcutEdgeCoordinate(bandMinY, usableMaxY, minY, usableMaxY);
            int x = sampleWithCenterAvoid(usableMinX, usableMaxX, centerX, deadZoneX, 58);
            return new Point(x, y);
        }
        return null;
    }

    private static int resolveWoodcutEdgeBandPx(int spanPx) {
        int span = Math.max(1, spanPx);
        int byRatio = (int) Math.round(span * OFFSCREEN_WOODCUT_EDGE_BAND_RATIO);
        return Math.max(OFFSCREEN_WOODCUT_EDGE_BAND_MIN_PX, Math.min(OFFSCREEN_WOODCUT_EDGE_BAND_MAX_PX, byRatio));
    }

    private static int sampleWoodcutEdgeCoordinate(int edgeMin, int edgeMax, int fullMin, int fullMax) {
        int min = Math.min(edgeMin, edgeMax);
        int max = Math.max(edgeMin, edgeMax);
        if (ThreadLocalRandom.current().nextInt(100) < OFFSCREEN_WOODCUT_EDGE_BAND_FREEFORM_SAMPLE_CHANCE_PERCENT) {
            return randomIntInclusive(Math.min(fullMin, fullMax), Math.max(fullMin, fullMax));
        }
        return randomIntInclusive(min, max);
    }

    private static int sampleWithCenterAvoid(
        int min,
        int max,
        int center,
        int deadZoneHalfWidth,
        int avoidChancePercent
    ) {
        int lo = Math.min(min, max);
        int hi = Math.max(min, max);
        if (hi <= lo) {
            return lo;
        }
        int chance = clampPercent(avoidChancePercent, 100);
        if (ThreadLocalRandom.current().nextInt(100) >= chance) {
            return randomIntInclusive(lo, hi);
        }
        int dead = Math.max(1, deadZoneHalfWidth);
        int leftMax = Math.min(hi, center - dead);
        int rightMin = Math.max(lo, center + dead);
        boolean hasLeft = leftMax >= lo;
        boolean hasRight = rightMin <= hi;
        if (!hasLeft && !hasRight) {
            return randomIntInclusive(lo, hi);
        }
        if (hasLeft && !hasRight) {
            return randomIntInclusive(lo, leftMax);
        }
        if (!hasLeft) {
            return randomIntInclusive(rightMin, hi);
        }
        int leftSpan = Math.max(1, leftMax - lo + 1);
        int rightSpan = Math.max(1, hi - rightMin + 1);
        int total = leftSpan + rightSpan;
        if (ThreadLocalRandom.current().nextInt(total) < leftSpan) {
            return randomIntInclusive(lo, leftMax);
        }
        return randomIntInclusive(rightMin, hi);
    }

    private Optional<Point> resolveFarOffscreenTargetScreenPoint(
        Rectangle windowBounds,
        Rectangle usable,
        int minGapPx,
        int minMarginPx,
        int farMaxGapPx
    ) {
        if (windowBounds == null || usable == null || usable.width <= 0 || usable.height <= 0) {
            lastFarFailureReason = "invalid_bounds";
            emitPlannerTelemetry(
                "idle_offscreen_far_candidate_failed",
                jsonDetails(
                    "reason", "invalid_bounds",
                    "windowPresent", windowBounds != null,
                    "usablePresent", usable != null
                )
            );
            return Optional.empty();
        }
        int windowMinX = windowBounds.x;
        int windowMaxX = windowBounds.x + Math.max(0, windowBounds.width - 1);
        int windowMinY = windowBounds.y;
        int windowMaxY = windowBounds.y + Math.max(0, windowBounds.height - 1);
        int usableMinX = usable.x;
        int usableMaxX = usable.x + Math.max(0, usable.width - 1);
        int usableMinY = usable.y;
        int usableMaxY = usable.y + Math.max(0, usable.height - 1);
        int leftRoom = Math.max(0, windowMinX - usableMinX);
        int rightRoom = Math.max(0, usableMaxX - windowMaxX);
        int topRoom = Math.max(0, windowMinY - usableMinY);
        int bottomRoom = Math.max(0, usableMaxY - windowMaxY);
        if (leftRoom <= 0 && rightRoom <= 0 && topRoom <= 0 && bottomRoom <= 0) {
            lastFarFailureReason = "no_room";
            emitPlannerTelemetry(
                "idle_offscreen_far_candidate_failed",
                jsonDetails(
                    "reason", "no_room",
                    "leftRoom", leftRoom,
                    "rightRoom", rightRoom,
                    "topRoom", topRoom,
                    "bottomRoom", bottomRoom
                )
            );
            return Optional.empty();
        }
        int resolvedMinGapPx = Math.max(minGapPx, minMarginPx + 8);
        emitPlannerTelemetry(
            "idle_offscreen_far_search_start",
            jsonDetails(
                "leftRoom", leftRoom,
                "rightRoom", rightRoom,
                "topRoom", topRoom,
                "bottomRoom", bottomRoom,
                "minGapPx", resolvedMinGapPx,
                "maxGapPx", farMaxGapPx,
                "minMarginPx", minMarginPx,
                "attempts", OFFSCREEN_FAR_TARGET_MAX_ATTEMPTS,
                "freeformSampleChancePercent", OFFSCREEN_FAR_TARGET_FREEFORM_SAMPLE_CHANCE_PERCENT
            )
        );

        for (int attempt = 0; attempt < OFFSCREEN_FAR_TARGET_MAX_ATTEMPTS; attempt++) {
            int side = pickSideByRoomAvoidRecent(leftRoom, rightRoom, topRoom, bottomRoom);
            if (side < 0) {
                lastFarFailureReason = "no_side_available";
                emitPlannerTelemetry(
                    "idle_offscreen_far_candidate_rejected",
                    jsonDetails(
                        "attempt", attempt,
                        "reason", "no_side_available"
                    )
                );
                return Optional.empty();
            }
            int sideRoom = sideRoomFor(side, leftRoom, rightRoom, topRoom, bottomRoom);
            if (sideRoom <= 0) {
                emitPlannerTelemetry(
                    "idle_offscreen_far_candidate_rejected",
                    jsonDetails(
                        "attempt", attempt,
                        "side", sideName(side),
                        "sideRoom", sideRoom,
                        "reason", "no_side_room"
                    )
                );
                continue;
            }
            int requestedFarMinMargin = Math.max(
                resolvedMinGapPx,
                (int) Math.ceil(sideRoom * OFFSCREEN_FAR_TARGET_SIDE_ROOM_MIN_RATIO)
            );
            int farMaxMargin = Math.min(sideRoom, farMaxGapPx);
            if (farMaxMargin <= 0) {
                emitPlannerTelemetry(
                    "idle_offscreen_far_candidate_rejected",
                    jsonDetails(
                        "attempt", attempt,
                        "side", sideName(side),
                        "sideRoom", sideRoom,
                        "farMinMargin", requestedFarMinMargin,
                        "farMaxMargin", farMaxMargin,
                        "minGapPx", resolvedMinGapPx,
                        "maxGapPx", farMaxGapPx,
                        "reason", "no_valid_margin_after_max_gap_cap"
                    )
                );
                continue;
            }
            int farMinMargin = Math.min(requestedFarMinMargin, farMaxMargin);
            if (requestedFarMinMargin > farMinMargin) {
                emitPlannerTelemetry(
                    "idle_offscreen_far_candidate_adjusted",
                    jsonDetails(
                        "attempt", attempt,
                        "side", sideName(side),
                        "sideRoom", sideRoom,
                        "requestedFarMinMargin", requestedFarMinMargin,
                        "farMinMargin", farMinMargin,
                        "farMaxMargin", farMaxMargin,
                        "minGapPx", resolvedMinGapPx,
                        "maxGapPx", farMaxGapPx,
                        "reason", "far_min_margin_capped_by_max_gap"
                    )
                );
            }
            boolean useFreeformSampling =
                ThreadLocalRandom.current().nextInt(100) < OFFSCREEN_FAR_TARGET_FREEFORM_SAMPLE_CHANCE_PERCENT;
            String samplingMode = useFreeformSampling ? "freeform" : "side_band";
            int margin = randomIntInclusive(farMinMargin, farMaxMargin);
            Integer orthogonal = null;
            Point candidate;
            if (useFreeformSampling) {
                candidate = randomPointInRectangle(usable);
            } else {
                orthogonal = (side == SIDE_LEFT || side == SIDE_RIGHT)
                    ? randomIntInclusive(usableMinY, usableMaxY)
                    : randomIntInclusive(usableMinX, usableMaxX);
                candidate = resolveOffWindowPoint(windowBounds, side, margin, orthogonal);
            }
            if (candidate == null) {
                emitPlannerTelemetry(
                    "idle_offscreen_far_candidate_rejected",
                    jsonDetails(
                        "attempt", attempt,
                        "side", sideName(side),
                        "sideRoom", sideRoom,
                        "farMinMargin", farMinMargin,
                        "farMaxMargin", farMaxMargin,
                        "margin", margin,
                        "samplingMode", samplingMode,
                        "orthogonal", orthogonal,
                        "reason", "candidate_null"
                    )
                );
                continue;
            }
            if (!usable.contains(candidate)) {
                emitPlannerTelemetry(
                    "idle_offscreen_far_candidate_rejected",
                    jsonDetails(
                        "attempt", attempt,
                        "side", sideName(side),
                        "sideRoom", sideRoom,
                        "farMinMargin", farMinMargin,
                        "farMaxMargin", farMaxMargin,
                        "margin", margin,
                        "samplingMode", samplingMode,
                        "orthogonal", orthogonal,
                        "candidateX", candidate.x,
                        "candidateY", candidate.y,
                        "reason", "candidate_outside_usable"
                    )
                );
                continue;
            }
            if (windowBounds.contains(candidate)) {
                emitPlannerTelemetry(
                    "idle_offscreen_far_candidate_rejected",
                    jsonDetails(
                        "attempt", attempt,
                        "side", sideName(side),
                        "sideRoom", sideRoom,
                        "farMinMargin", farMinMargin,
                        "farMaxMargin", farMaxMargin,
                        "margin", margin,
                        "samplingMode", samplingMode,
                        "orthogonal", orthogonal,
                        "candidateX", candidate.x,
                        "candidateY", candidate.y,
                        "reason", "candidate_inside_window"
                    )
                );
                continue;
            }
            int gap = gapOutsideWindowPixels(candidate, windowBounds);
            if (gap < farMinMargin) {
                emitPlannerTelemetry(
                    "idle_offscreen_far_candidate_rejected",
                    jsonDetails(
                        "attempt", attempt,
                        "side", sideName(side),
                        "sideRoom", sideRoom,
                        "farMinMargin", farMinMargin,
                        "farMaxMargin", farMaxMargin,
                        "margin", margin,
                        "samplingMode", samplingMode,
                        "orthogonal", orthogonal,
                        "candidateX", candidate.x,
                        "candidateY", candidate.y,
                        "gap", gap,
                        "reason", "gap_below_far_min_margin"
                    )
                );
                continue;
            }
            if (gap > farMaxGapPx) {
                emitPlannerTelemetry(
                    "idle_offscreen_far_candidate_rejected",
                    jsonDetails(
                        "attempt", attempt,
                        "side", sideName(side),
                        "sideRoom", sideRoom,
                        "farMinMargin", farMinMargin,
                        "farMaxMargin", farMaxMargin,
                        "margin", margin,
                        "samplingMode", samplingMode,
                        "orthogonal", orthogonal,
                        "candidateX", candidate.x,
                        "candidateY", candidate.y,
                        "gap", gap,
                        "maxGapPx", farMaxGapPx,
                        "reason", "gap_above_max_gap"
                    )
                );
                continue;
            }
            if (isRecentOffscreenRegion(side, candidate, usable)) {
                emitPlannerTelemetry(
                    "idle_offscreen_far_candidate_rejected",
                    jsonDetails(
                        "attempt", attempt,
                        "side", sideName(side),
                        "sideRoom", sideRoom,
                        "farMinMargin", farMinMargin,
                        "farMaxMargin", farMaxMargin,
                        "margin", margin,
                        "samplingMode", samplingMode,
                        "orthogonal", orthogonal,
                        "candidateX", candidate.x,
                        "candidateY", candidate.y,
                        "reason", "recent_region_repeat"
                    )
                );
                continue;
            }
            emitPlannerTelemetry(
                "idle_offscreen_far_candidate_selected",
                jsonDetails(
                    "attempt", attempt,
                    "side", sideName(side),
                    "sideRoom", sideRoom,
                    "farMinMargin", farMinMargin,
                    "farMaxMargin", farMaxMargin,
                    "margin", margin,
                    "samplingMode", samplingMode,
                    "orthogonal", orthogonal,
                    "candidateX", candidate.x,
                    "candidateY", candidate.y,
                    "gap", gap
                )
            );
            lastFarFailureReason = "";
            noteOffscreenSelection(side, candidate, usable);
            return Optional.of(candidate);
        }
        lastFarFailureReason = "far_attempts_exhausted";
        emitPlannerTelemetry(
            "idle_offscreen_far_candidate_failed",
            jsonDetails(
                "reason", "attempts_exhausted",
                "attempts", OFFSCREEN_FAR_TARGET_MAX_ATTEMPTS,
                "minGapPx", resolvedMinGapPx
            )
        );
        return Optional.empty();
    }

    private static int sideRoomFor(int side, int leftRoom, int rightRoom, int topRoom, int bottomRoom) {
        if (side == SIDE_LEFT) {
            return leftRoom;
        }
        if (side == SIDE_RIGHT) {
            return rightRoom;
        }
        if (side == SIDE_TOP) {
            return topRoom;
        }
        if (side == SIDE_BOTTOM) {
            return bottomRoom;
        }
        return 0;
    }

    private static String sideName(int side) {
        if (side == SIDE_LEFT) {
            return "left";
        }
        if (side == SIDE_RIGHT) {
            return "right";
        }
        if (side == SIDE_TOP) {
            return "top";
        }
        if (side == SIDE_BOTTOM) {
            return "bottom";
        }
        return "unknown";
    }

    private void emitPlannerTelemetry(String reason, JsonObject details) {
        if (!OFFSCREEN_PLANNER_DEBUG_TELEMETRY_ENABLED) {
            return;
        }
        String safeReason = reason == null ? "" : reason.trim();
        if (safeReason.isEmpty()) {
            safeReason = "idle_offscreen_planner_debug";
        }
        host.emitIdleEvent(safeReason, details == null ? new JsonObject() : details);
    }

    private static JsonObject jsonDetails(Object... kvPairs) {
        JsonObject out = new JsonObject();
        if (kvPairs == null || kvPairs.length < 2) {
            return out;
        }
        int count = kvPairs.length - (kvPairs.length % 2);
        for (int i = 0; i < count; i += 2) {
            String key = kvPairs[i] == null ? "" : String.valueOf(kvPairs[i]);
            if (key.isBlank()) {
                continue;
            }
            Object value = kvPairs[i + 1];
            if (value == null) {
                out.addProperty(key, (String) null);
            } else if (value instanceof Number) {
                out.addProperty(key, (Number) value);
            } else if (value instanceof Boolean) {
                out.addProperty(key, (Boolean) value);
            } else if (value instanceof Character) {
                out.addProperty(key, (Character) value);
            } else {
                out.addProperty(key, String.valueOf(value));
            }
        }
        return out;
    }

    private static Point resolveOffWindowPoint(Rectangle windowBounds, int side, int margin, Integer orthogonal) {
        if (windowBounds == null || orthogonal == null) {
            return null;
        }
        int m = Math.max(0, margin);
        int windowMinX = windowBounds.x;
        int windowMaxX = windowBounds.x + Math.max(0, windowBounds.width - 1);
        int windowMinY = windowBounds.y;
        int windowMaxY = windowBounds.y + Math.max(0, windowBounds.height - 1);
        if (side == SIDE_LEFT) {
            return new Point(windowMinX - m, orthogonal);
        }
        if (side == SIDE_RIGHT) {
            return new Point(windowMaxX + m, orthogonal);
        }
        if (side == SIDE_TOP) {
            return new Point(orthogonal, windowMinY - m);
        }
        if (side == SIDE_BOTTOM) {
            return new Point(orthogonal, windowMaxY + m);
        }
        return null;
    }

    boolean isCursorInsideClientCanvasScreen() {
        Optional<Rectangle> canvasBoundsOpt = host.resolveClientCanvasBoundsScreen();
        if (canvasBoundsOpt.isEmpty()) {
            return false;
        }
        Point cursor = host.currentPointerLocationOr(null);
        return cursor != null && canvasBoundsOpt.get().contains(cursor);
    }

    private static Point clampPointToRectangle(Point point, Rectangle bounds) {
        if (point == null || bounds == null) {
            return point;
        }
        int x = Math.max(bounds.x, Math.min(bounds.x + Math.max(0, bounds.width - 1), point.x));
        int y = Math.max(bounds.y, Math.min(bounds.y + Math.max(0, bounds.height - 1), point.y));
        return new Point(x, y);
    }

    private static Point randomPointInRectangle(Rectangle bounds) {
        if (bounds == null || bounds.width <= 0 || bounds.height <= 0) {
            return null;
        }
        int minX = bounds.x;
        int maxX = bounds.x + Math.max(0, bounds.width - 1);
        int minY = bounds.y;
        int maxY = bounds.y + Math.max(0, bounds.height - 1);
        int x = randomIntInclusive(minX, maxX);
        int y = randomIntInclusive(minY, maxY);
        return new Point(x, y);
    }

    private static int randomIntInclusive(int minInclusive, int maxInclusive) {
        int lo = Math.min(minInclusive, maxInclusive);
        int hi = Math.max(minInclusive, maxInclusive);
        if (hi <= lo) {
            return lo;
        }
        return ThreadLocalRandom.current().nextInt(lo, hi + 1);
    }

    private void initializeOffscreenRegionHistory() {
        for (int i = 0; i < recentOffscreenRegionKeys.length; i++) {
            recentOffscreenRegionKeys[i] = Long.MIN_VALUE;
        }
    }

    private int pickSideByRoomAvoidRecent(int leftRoom, int rightRoom, int topRoom, int bottomRoom) {
        int side = pickSideByRoom(leftRoom, rightRoom, topRoom, bottomRoom);
        if (side < 0 || lastOffscreenSelectedSide < 0 || side != lastOffscreenSelectedSide) {
            return side;
        }
        int alternative = pickAlternativeSideByRoom(side, leftRoom, rightRoom, topRoom, bottomRoom);
        if (alternative < 0) {
            return side;
        }
        int avoidChance = clampPercent(
            OFFSCREEN_SIDE_REPEAT_AVOID_BASE_CHANCE_PERCENT
                + (offscreenSideRepeatStreak * OFFSCREEN_SIDE_REPEAT_AVOID_STREAK_STEP_PERCENT),
            OFFSCREEN_SIDE_REPEAT_AVOID_MAX_CHANCE_PERCENT
        );
        if (ThreadLocalRandom.current().nextInt(100) < avoidChance) {
            return alternative;
        }
        return side;
    }

    private static int pickAlternativeSideByRoom(
        int excludedSide,
        int leftRoom,
        int rightRoom,
        int topRoom,
        int bottomRoom
    ) {
        int[] sides = new int[4];
        int[] weights = new int[4];
        int count = 0;
        int total = 0;
        int left = Math.max(0, leftRoom);
        int right = Math.max(0, rightRoom);
        int top = Math.max(0, topRoom);
        int bottom = Math.max(0, bottomRoom);
        if (excludedSide != SIDE_LEFT && left > 0) {
            sides[count] = SIDE_LEFT;
            weights[count] = left;
            total += left;
            count++;
        }
        if (excludedSide != SIDE_RIGHT && right > 0) {
            sides[count] = SIDE_RIGHT;
            weights[count] = right;
            total += right;
            count++;
        }
        if (excludedSide != SIDE_TOP && top > 0) {
            sides[count] = SIDE_TOP;
            weights[count] = top;
            total += top;
            count++;
        }
        if (excludedSide != SIDE_BOTTOM && bottom > 0) {
            sides[count] = SIDE_BOTTOM;
            weights[count] = bottom;
            total += bottom;
            count++;
        }
        if (count <= 0 || total <= 0) {
            return -1;
        }
        int roll = ThreadLocalRandom.current().nextInt(total);
        for (int i = 0; i < count; i++) {
            if (roll < weights[i]) {
                return sides[i];
            }
            roll -= weights[i];
        }
        return sides[count - 1];
    }

    private static int clampPercent(int value, int cap) {
        int max = Math.max(0, cap);
        return Math.max(0, Math.min(max, value));
    }

    private boolean isRecentOffscreenRegion(int side, Point candidate, Rectangle usableBounds) {
        long key = offscreenRegionKey(side, candidate, usableBounds);
        if (key == Long.MIN_VALUE) {
            return false;
        }
        for (long recentKey : recentOffscreenRegionKeys) {
            if (recentKey == key) {
                return true;
            }
        }
        return false;
    }

    private void noteOffscreenSelection(int side, Point candidate, Rectangle usableBounds) {
        if (side >= 0) {
            if (side == lastOffscreenSelectedSide) {
                offscreenSideRepeatStreak = Math.min(6, offscreenSideRepeatStreak + 1);
            } else {
                lastOffscreenSelectedSide = side;
                offscreenSideRepeatStreak = 0;
            }
        }
        rememberOffscreenRegionKey(offscreenRegionKey(side, candidate, usableBounds));
    }

    private void rememberOffscreenRegionKey(long key) {
        if (key == Long.MIN_VALUE || recentOffscreenRegionKeys.length <= 0) {
            return;
        }
        recentOffscreenRegionKeys[recentOffscreenRegionWriteIndex] = key;
        recentOffscreenRegionWriteIndex = (recentOffscreenRegionWriteIndex + 1) % recentOffscreenRegionKeys.length;
    }

    private static long offscreenRegionKey(int side, Point candidate, Rectangle usableBounds) {
        if (side < 0 || candidate == null || usableBounds == null || usableBounds.width <= 0 || usableBounds.height <= 0) {
            return Long.MIN_VALUE;
        }
        int grid = Math.max(2, OFFSCREEN_REGION_GRID_SIZE);
        int relativeX = candidate.x - usableBounds.x;
        int relativeY = candidate.y - usableBounds.y;
        int binX = Math.max(
            0,
            Math.min(grid - 1, (int) Math.floor((double) relativeX * (double) grid / (double) Math.max(1, usableBounds.width)))
        );
        int binY = Math.max(
            0,
            Math.min(grid - 1, (int) Math.floor((double) relativeY * (double) grid / (double) Math.max(1, usableBounds.height)))
        );
        return (((long) side & 0xFFL) << 40) | (((long) binX & 0xFFFFL) << 20) | ((long) binY & 0xFFFFL);
    }

    private static int pickSideByRoom(int leftRoom, int rightRoom, int topRoom, int bottomRoom) {
        int left = Math.max(0, leftRoom);
        int right = Math.max(0, rightRoom);
        int top = Math.max(0, topRoom);
        int bottom = Math.max(0, bottomRoom);
        int total = left + right + top + bottom;
        if (total <= 0) {
            return -1;
        }
        if (OFFSCREEN_FAR_TARGET_MAX_ROOM_ONLY) {
            int maxRoom = Math.max(Math.max(left, right), Math.max(top, bottom));
            if (maxRoom <= 0) {
                return -1;
            }
            int[] sides = new int[4];
            int count = 0;
            if (left == maxRoom) {
                sides[count++] = SIDE_LEFT;
            }
            if (right == maxRoom) {
                sides[count++] = SIDE_RIGHT;
            }
            if (top == maxRoom) {
                sides[count++] = SIDE_TOP;
            }
            if (bottom == maxRoom) {
                sides[count++] = SIDE_BOTTOM;
            }
            if (count <= 0) {
                return -1;
            }
            return sides[ThreadLocalRandom.current().nextInt(count)];
        }
        int roll = ThreadLocalRandom.current().nextInt(total);
        if (roll < left) {
            return SIDE_LEFT;
        }
        roll -= left;
        if (roll < right) {
            return SIDE_RIGHT;
        }
        roll -= right;
        if (roll < top) {
            return SIDE_TOP;
        }
        return SIDE_BOTTOM;
    }

    private static int classifyOffscreenSide(Point candidate, Rectangle windowBounds) {
        if (candidate == null || windowBounds == null) {
            return -1;
        }
        int windowMinX = windowBounds.x;
        int windowMaxX = windowBounds.x + Math.max(0, windowBounds.width - 1);
        int windowMinY = windowBounds.y;
        int windowMaxY = windowBounds.y + Math.max(0, windowBounds.height - 1);

        int leftGap = candidate.x < windowMinX ? windowMinX - candidate.x : 0;
        int rightGap = candidate.x > windowMaxX ? candidate.x - windowMaxX : 0;
        int topGap = candidate.y < windowMinY ? windowMinY - candidate.y : 0;
        int bottomGap = candidate.y > windowMaxY ? candidate.y - windowMaxY : 0;
        int maxGap = Math.max(Math.max(leftGap, rightGap), Math.max(topGap, bottomGap));
        if (maxGap <= 0) {
            return -1;
        }
        int[] matches = new int[4];
        int count = 0;
        if (leftGap == maxGap) {
            matches[count++] = SIDE_LEFT;
        }
        if (rightGap == maxGap) {
            matches[count++] = SIDE_RIGHT;
        }
        if (topGap == maxGap) {
            matches[count++] = SIDE_TOP;
        }
        if (bottomGap == maxGap) {
            matches[count++] = SIDE_BOTTOM;
        }
        if (count <= 0) {
            return -1;
        }
        return matches[ThreadLocalRandom.current().nextInt(count)];
    }

    private static int gapOutsideWindowPixels(Point candidate, Rectangle windowBounds) {
        if (candidate == null || windowBounds == null) {
            return 0;
        }
        int windowMinX = windowBounds.x;
        int windowMaxX = windowBounds.x + Math.max(0, windowBounds.width - 1);
        int windowMinY = windowBounds.y;
        int windowMaxY = windowBounds.y + Math.max(0, windowBounds.height - 1);
        int gapX = 0;
        if (candidate.x < windowMinX) {
            gapX = windowMinX - candidate.x;
        } else if (candidate.x > windowMaxX) {
            gapX = candidate.x - windowMaxX;
        }
        int gapY = 0;
        if (candidate.y < windowMinY) {
            gapY = windowMinY - candidate.y;
        } else if (candidate.y > windowMaxY) {
            gapY = candidate.y - windowMaxY;
        }
        return Math.max(gapX, gapY);
    }

    private static Rectangle insetRectangle(Rectangle bounds, int insetPx) {
        if (bounds == null) {
            return new Rectangle(0, 0, 1, 1);
        }
        int inset = Math.max(0, insetPx);
        int x = bounds.x + inset;
        int y = bounds.y + inset;
        int width = Math.max(1, bounds.width - (inset * 2));
        int height = Math.max(1, bounds.height - (inset * 2));
        return new Rectangle(x, y, width, height);
    }

    private static Rectangle resolveVirtualDesktopBounds(Rectangle fallbackBounds) {
        Rectangle union = fallbackBounds == null ? null : new Rectangle(fallbackBounds);
        try {
            GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] devices = env.getScreenDevices();
            if (devices != null) {
                for (GraphicsDevice device : devices) {
                    if (device == null || device.getDefaultConfiguration() == null) {
                        continue;
                    }
                    Rectangle bounds = device.getDefaultConfiguration().getBounds();
                    if (bounds == null || bounds.width <= 0 || bounds.height <= 0) {
                        continue;
                    }
                    union = union == null ? new Rectangle(bounds) : union.union(bounds);
                }
            }
        } catch (RuntimeException ex) {
            // Keep fallback bounds when desktop enumeration is unavailable.
        }
        if (union == null || union.width <= 0 || union.height <= 0) {
            return new Rectangle(0, 0, 1, 1);
        }
        return union;
    }

    private static int resolveScreenDpi() {
        try {
            int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
            if (dpi <= 0) {
                return FALLBACK_SCREEN_DPI;
            }
            return dpi;
        } catch (RuntimeException ex) {
            return FALLBACK_SCREEN_DPI;
        }
    }

    private IdleSkillContext resolveIdleSkillContext() {
        IdleSkillContext context = host.resolveIdleSkillContext();
        return context == null ? IdleSkillContext.GLOBAL : context;
    }
}
