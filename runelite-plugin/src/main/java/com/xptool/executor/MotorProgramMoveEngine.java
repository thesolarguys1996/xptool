package com.xptool.executor;

import com.xptool.motion.MotionProfile.MotorGestureMode;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

final class MotorProgramMoveEngine {
    private static final double DROP_TERMINAL_ALIGNMENT_TOLERANCE_PX = 1.35;
    private static final int DROP_TERMINAL_FINALIZE_JITTER_RADIUS_PX = 1;
    private static final int DROP_TERMINAL_ALIGNMENT_MAX_RETRIES = 3;
    private static final int PATH_VARIANT_REPEAT_AVOID_CHANCE_PERCENT = 72;
    private static final int CURVE_SIGN_REPEAT_AVOID_CHANCE_PERCENT = 74;
    private static final double FISHING_MOVE_PAUSE_TICK_CHANCE = 0.10;
    private static final double FISHING_MOVE_SINGLE_STEP_TICK_CHANCE = 0.28;
    private static final double FISHING_MOVE_TICK_CHANCE_JITTER = 0.06;
    private static final double FISHING_MOVE_TRIPLE_STEP_TICK_CHANCE = 0.035;
    private static final double FISHING_TRUE_OVERSHOOT_CHANCE = 0.23;
    private static final double WOODCUT_SHORT_APPROACH_DISTANCE_PX = 72.0;
    private static final double WOODCUT_SHORT_CORRECTION_DISTANCE_PX = 18.0;
    private static final String MOTOR_MOVE_REASON_IN_FLIGHT = "motor_move_in_flight";
    private static final String MOTOR_MOVE_REASON_TERMINAL_ALIGNMENT_DEFERRED =
        "motor_move_terminal_alignment_deferred";
    private static final String MOTOR_MOVE_REASON_TERMINAL_ALIGNMENT_RETRY_EXHAUSTED =
        "motor_move_terminal_alignment_retry_exhausted";

    interface Host {
        Optional<Point> toScreenPoint(Point canvasPoint);

        Rectangle dropSweepRegionScreen();

        Point dropSweepLastTargetScreen();

        void setDropSweepLastTargetScreen(Point point);

        boolean dropSweepAwaitingFirstCursorSync();

        Point motorCursorScreenPoint();

        Point motorCursorLocationOr(Point fallback);

        Robot getOrCreateRobot();

        void failMotorProgram(MotorProgram program, String reason);

        boolean tryConsumeMouseMutationBudget();

        Point currentPointerLocationOr(Point fallback);

        void noteMouseMutation(Point point);

        void noteInteractionActivityNow();

        void noteMotorProgramFirstMouseMutation(MotorProgram program);

        boolean isCursorNearScreenPoint(Point point, double tolerancePx);

        void updateMotorCursorState(Point point);

        void completeMotorProgram(MotorProgram program, String reason);

        String normalizedMotorOwnerName(String owner);

        boolean isWoodcutWorldClickType(String clickType);

        boolean isFishingWorldClickType(String clickType);
    }

    static final class Config {
        final double pendingMoveTargetMatchTolerancePx;
        final double moveTargetTolerancePx;
        final int dropCursorMotorMinSteps;
        final int dropCursorMotorMaxSteps;
        final int cursorMotorMinSteps;
        final int cursorMotorMaxSteps;
        final double woodcutStepScaleMin;
        final double woodcutStepScaleMax;
        final double idleStepScaleMin;
        final double idleStepScaleMax;
        final double idleMovePauseTickChance;
        final double woodcutMovePauseTickChance;
        final double woodcutMoveSingleStepTickChance;
        final boolean humanizedTimingEnabled;
        final String motorOwnerIdle;
        final String motorOwnerBank;
        final double dropCurveScale;
        final double dropCurveMinPx;
        final double dropCurveMaxPx;
        final int dropControlJitterPx;
        final int dropMicroCorrectionChancePercent;
        final int dropMicroCorrectionMinDistancePx;
        final int dropMicroCorrectionMaxRadiusPx;
        final double curveScaleSettle;
        final double curveMinPxSettle;
        final double curveMaxPxSettle;

        Config(
            double pendingMoveTargetMatchTolerancePx,
            double moveTargetTolerancePx,
            int dropCursorMotorMinSteps,
            int dropCursorMotorMaxSteps,
            int cursorMotorMinSteps,
            int cursorMotorMaxSteps,
            double woodcutStepScaleMin,
            double woodcutStepScaleMax,
            double idleStepScaleMin,
            double idleStepScaleMax,
            double idleMovePauseTickChance,
            double woodcutMovePauseTickChance,
            double woodcutMoveSingleStepTickChance,
            boolean humanizedTimingEnabled,
            String motorOwnerIdle,
            String motorOwnerBank,
            double dropCurveScale,
            double dropCurveMinPx,
            double dropCurveMaxPx,
            int dropControlJitterPx,
            int dropMicroCorrectionChancePercent,
            int dropMicroCorrectionMinDistancePx,
            int dropMicroCorrectionMaxRadiusPx,
            double curveScaleSettle,
            double curveMinPxSettle,
            double curveMaxPxSettle
        ) {
            this.pendingMoveTargetMatchTolerancePx = pendingMoveTargetMatchTolerancePx;
            this.moveTargetTolerancePx = moveTargetTolerancePx;
            this.dropCursorMotorMinSteps = Math.max(1, dropCursorMotorMinSteps);
            this.dropCursorMotorMaxSteps = Math.max(this.dropCursorMotorMinSteps, dropCursorMotorMaxSteps);
            this.cursorMotorMinSteps = Math.max(1, cursorMotorMinSteps);
            this.cursorMotorMaxSteps = Math.max(this.cursorMotorMinSteps, cursorMotorMaxSteps);
            this.woodcutStepScaleMin = woodcutStepScaleMin;
            this.woodcutStepScaleMax = Math.max(woodcutStepScaleMin, woodcutStepScaleMax);
            this.idleStepScaleMin = idleStepScaleMin;
            this.idleStepScaleMax = Math.max(idleStepScaleMin, idleStepScaleMax);
            this.idleMovePauseTickChance = idleMovePauseTickChance;
            this.woodcutMovePauseTickChance = woodcutMovePauseTickChance;
            this.woodcutMoveSingleStepTickChance = woodcutMoveSingleStepTickChance;
            this.humanizedTimingEnabled = humanizedTimingEnabled;
            this.motorOwnerIdle = safeString(motorOwnerIdle);
            this.motorOwnerBank = safeString(motorOwnerBank);
            this.dropCurveScale = dropCurveScale;
            this.dropCurveMinPx = dropCurveMinPx;
            this.dropCurveMaxPx = dropCurveMaxPx;
            this.dropControlJitterPx = dropControlJitterPx;
            this.dropMicroCorrectionChancePercent = dropMicroCorrectionChancePercent;
            this.dropMicroCorrectionMinDistancePx = dropMicroCorrectionMinDistancePx;
            this.dropMicroCorrectionMaxRadiusPx = dropMicroCorrectionMaxRadiusPx;
            this.curveScaleSettle = curveScaleSettle;
            this.curveMinPxSettle = curveMinPxSettle;
            this.curveMaxPxSettle = curveMaxPxSettle;
        }

        private static String safeString(String value) {
            return value == null ? "" : value;
        }
    }

    private final Host host;
    private final Config config;
    private final WoodcutPreClickSettlePlanner woodcutPreClickSettlePlanner;
    private Point lastDropTerminalFinalizePoint;
    private int lastDropPathVariant = -1;
    private int lastWoodcutPathVariant = -1;
    private int lastFishingPathVariant = -1;
    private int lastDropPrimaryCurveSign = 0;
    private int lastWoodcutPrimaryCurveSign = 0;
    private int lastFishingPrimaryCurveSign = 0;
    private int lastFishingSplitBucket = -1;
    private int lastFishingLegBucket = -1;

    MotorProgramMoveEngine(Host host, Config config) {
        this.host = host;
        this.config = config;
        this.woodcutPreClickSettlePlanner = new WoodcutPreClickSettlePlanner();
    }

    void advanceMotorProgramMove(MotorProgram program) {
        if (program == null || program.targetCanvas == null) {
            host.failMotorProgram(program, "motor_move_program_invalid");
            return;
        }
        Point targetCanvasPoint = program.targetCanvas.toAwtPoint();
        Optional<Point> screenOpt = host.toScreenPoint(targetCanvasPoint);
        if (screenOpt.isEmpty()) {
            host.failMotorProgram(program, "motor_move_target_offscreen");
            return;
        }
        Rectangle dropRegion = host.dropSweepRegionScreen();
        Point target = new Point(screenOpt.get());
        boolean dropSweepMove = program.profile.gestureMode == MotorGestureMode.DROP_SWEEP;
        boolean firstSyncDropMove = dropSweepMove && host.dropSweepAwaitingFirstCursorSync();
        if (dropSweepMove && dropRegion != null) {
            target = clampScreenPointToRegion(target, dropRegion);
        }

        Robot robot = host.getOrCreateRobot();
        if (robot == null) {
            host.failMotorProgram(program, "motor_robot_unavailable");
            return;
        }

        if (program.totalSteps <= 0 || program.stepIndex >= program.totalSteps
            || !pointsNear(program.toScreen, target, config.pendingMoveTargetMatchTolerancePx)) {
            resetTerminalAlignmentRetry(program);
            Point from;
            if (dropSweepMove) {
                if (firstSyncDropMove) {
                    // First drop-sweep move must anchor to actual pointer location
                    // to avoid visible jump when cached motor cursor state is stale.
                    // Never default straight to target here; that can look like teleport.
                    Point observedPointer = host.currentPointerLocationOr(null);
                    from = observedPointer != null
                        ? observedPointer
                        : host.motorCursorLocationOr(target);
                } else if (host.dropSweepLastTargetScreen() != null) {
                    from = new Point(host.dropSweepLastTargetScreen());
                } else {
                    from = host.motorCursorLocationOr(target);
                }
            } else {
                from = host.motorCursorLocationOr(target);
            }
            if (dropSweepMove && dropRegion != null && !firstSyncDropMove) {
                from = clampScreenPointToRegion(from, dropRegion);
            }
            program.fromScreen = from;
            program.toScreen = target;
            program.scriptedPathScreen = null;
            program.controlScreen = dropSweepMove
                ? curveControlPoint(from, target, config.dropCurveScale, config.dropCurveMinPx, config.dropCurveMaxPx)
                : curveControlPoint(from, target);
            if (dropSweepMove && config.humanizedTimingEnabled && !firstSyncDropMove) {
                program.controlScreen = jitterControlPoint(
                    program.controlScreen,
                    randomIntInclusive(3, 8)
                );
            }
            if (!dropSweepMove && host.isWoodcutWorldClickType(program.profile.clickType)) {
                program.controlScreen = jitterControlPoint(program.controlScreen, 9);
            } else if (!dropSweepMove && host.isFishingWorldClickType(program.profile.clickType)) {
                program.controlScreen = jitterControlPoint(program.controlScreen, randomIntInclusive(4, 10));
            }

            int minSteps = dropSweepMove ? config.dropCursorMotorMinSteps : config.cursorMotorMinSteps;
            int maxSteps = dropSweepMove ? config.dropCursorMotorMaxSteps : config.cursorMotorMaxSteps;
            boolean woodcutWorldMove = !dropSweepMove && host.isWoodcutWorldClickType(program.profile.clickType);
            boolean fishingWorldMove = !dropSweepMove && host.isFishingWorldClickType(program.profile.clickType);
            boolean initialWoodcutApproach = woodcutWorldMove
                && (host.motorCursorScreenPoint() == null || pixelDistance(from, target) >= 110.0);
            boolean initialFishingApproach = fishingWorldMove
                && (host.motorCursorScreenPoint() == null || pixelDistance(from, target) >= 94.0);
            if (firstSyncDropMove) {
                // First transition into inventory should be visibly smooth, not a short snap.
                minSteps = Math.max(minSteps, 20);
                maxSteps = Math.max(maxSteps, 44);
            }
            if (initialWoodcutApproach) {
                minSteps = Math.max(minSteps, config.cursorMotorMinSteps + 4);
                maxSteps = Math.max(maxSteps, config.cursorMotorMaxSteps + 8);
            }
            if (initialFishingApproach) {
                minSteps = Math.max(minSteps, config.cursorMotorMinSteps + 3);
                maxSteps = Math.max(maxSteps, config.cursorMotorMaxSteps + 9);
            }
            boolean idleMove = !dropSweepMove
                && config.motorOwnerIdle.equals(host.normalizedMotorOwnerName(program.profile.owner));
            if (idleMove) {
                minSteps = Math.max(minSteps, config.cursorMotorMinSteps + 2);
                maxSteps = Math.max(maxSteps, config.cursorMotorMaxSteps + 10);
            }
            boolean bankMove = !dropSweepMove
                && config.motorOwnerBank.equals(host.normalizedMotorOwnerName(program.profile.owner));
            if (bankMove) {
                minSteps = Math.max(minSteps, config.cursorMotorMinSteps + 1);
                maxSteps = Math.max(maxSteps, config.cursorMotorMaxSteps + 6);
            }
            double distanceDivisor;
            int distanceBase;
            if (dropSweepMove) {
                distanceDivisor = 9.0;
                distanceBase = 9;
            } else if (woodcutWorldMove) {
                // Drop-like smoothness preset for world interactions.
                distanceDivisor = 12.0;
                distanceBase = 7;
            } else if (fishingWorldMove) {
                distanceDivisor = 12.0;
                distanceBase = 7;
            } else {
                distanceDivisor = 13.0;
                distanceBase = 6;
            }
            int computedSteps = (int) Math.round(pixelDistance(from, target) / distanceDivisor) + distanceBase;
            if (dropSweepMove) {
                double scale = config.humanizedTimingEnabled
                    ? ThreadLocalRandom.current().nextDouble(0.96, 1.12)
                    : 1.04;
                computedSteps = (int) Math.round((double) computedSteps * scale);
                if (firstSyncDropMove) {
                    computedSteps = (int) Math.round((double) computedSteps * 1.35);
                }
            }
            if (!dropSweepMove && host.isWoodcutWorldClickType(program.profile.clickType)) {
                double scale = config.humanizedTimingEnabled
                    ? ThreadLocalRandom.current().nextDouble(config.woodcutStepScaleMin, config.woodcutStepScaleMax)
                    : 1.10;
                computedSteps = (int) Math.round((double) computedSteps * scale);
                if (initialWoodcutApproach) {
                    computedSteps = (int) Math.round((double) computedSteps * 1.14);
                }
            }
            if (fishingWorldMove) {
                double scale = config.humanizedTimingEnabled
                    ? ThreadLocalRandom.current().nextDouble(0.92, 1.28)
                    : 1.08;
                computedSteps = (int) Math.round((double) computedSteps * scale);
                if (initialFishingApproach) {
                    computedSteps = (int) Math.round((double) computedSteps * 1.10);
                }
            }
            if (idleMove) {
                double scale = config.humanizedTimingEnabled
                    ? ThreadLocalRandom.current().nextDouble(config.idleStepScaleMin, config.idleStepScaleMax)
                    : 1.35;
                computedSteps = (int) Math.round((double) computedSteps * scale);
            }
            program.totalSteps = Math.max(minSteps, Math.min(maxSteps, computedSteps));
            if (dropSweepMove) {
                Rectangle pathClampRegion = firstSyncDropMove ? null : dropRegion;
                List<Point> scriptedDropPath = buildDropSweepPathPoints(
                    from,
                    target,
                    program.totalSteps,
                    pathClampRegion,
                    firstSyncDropMove
                );
                if (!scriptedDropPath.isEmpty()) {
                    program.scriptedPathScreen = scriptedDropPath;
                    // Keep temporal pacing from computed step budget even when
                    // very short hops collapse to few unique integer points.
                    program.totalSteps = Math.max(program.totalSteps, scriptedDropPath.size());
                }
            } else if (woodcutWorldMove) {
                List<Point> scriptedWoodcutPath = buildWoodcutWorldPathPoints(
                    from,
                    target,
                    program.totalSteps,
                    initialWoodcutApproach
                );
                if (!scriptedWoodcutPath.isEmpty()) {
                    program.scriptedPathScreen = scriptedWoodcutPath;
                    program.totalSteps = scriptedWoodcutPath.size();
                }
            } else if (fishingWorldMove) {
                List<Point> scriptedFishingPath = buildFishingWorldPathPoints(
                    from,
                    target,
                    program.totalSteps,
                    initialFishingApproach
                );
                if (!scriptedFishingPath.isEmpty()) {
                    program.scriptedPathScreen = scriptedFishingPath;
                    program.totalSteps = scriptedFishingPath.size();
                }
            } else if (bankMove) {
                List<Point> scriptedBankPath = buildBankPathPoints(
                    from,
                    target,
                    program.totalSteps
                );
                if (!scriptedBankPath.isEmpty()) {
                    program.scriptedPathScreen = scriptedBankPath;
                    program.totalSteps = scriptedBankPath.size();
                }
            }
            program.stepIndex = 0;
            if (host.isCursorNearScreenPoint(target, config.moveTargetTolerancePx)) {
                program.stepIndex = program.totalSteps;
            }
        }

        int stepsPerTick = resolveStepsPerTickForMove(program.profile);
        if (stepsPerTick <= 0) {
            return;
        }
        for (int i = 0; i < stepsPerTick; i++) {
            if (program.stepIndex >= program.totalSteps) {
                break;
            }
            if (program.profile.enforceMutationBudget && !host.tryConsumeMouseMutationBudget()) {
                return;
            }
            program.stepIndex++;
            Point p;
            if (program.scriptedPathScreen != null && !program.scriptedPathScreen.isEmpty()) {
                int pathSize = program.scriptedPathScreen.size();
                int pathIndex;
                if (pathSize <= 1 || program.totalSteps <= 1) {
                    pathIndex = 0;
                } else {
                    double ratio = (double) (program.stepIndex - 1) / (double) Math.max(1, program.totalSteps - 1);
                    ratio = Math.max(0.0, Math.min(1.0, ratio));
                    pathIndex = (int) Math.round(ratio * (double) (pathSize - 1));
                }
                pathIndex = Math.max(0, Math.min(pathSize - 1, pathIndex));
                p = program.scriptedPathScreen.get(pathIndex);
            } else {
                double t = (double) program.stepIndex / (double) Math.max(1, program.totalSteps);
                double progress = resolveMoveProgress(
                    t,
                    program.fromScreen,
                    program.toScreen,
                    program.profile == null ? null : program.profile.clickSettings
                );
                p = quadraticBezier(program.fromScreen, program.controlScreen, program.toScreen, progress);
            }
            robot.mouseMove(p.x, p.y);
            Point after = host.currentPointerLocationOr(program.toScreen);
            if (!program.firstMouseMutationObserved) {
                program.firstMouseMutationObserved = true;
                program.firstMouseMutationAtMs = System.currentTimeMillis();
                host.noteMotorProgramFirstMouseMutation(program);
            }
            host.noteMouseMutation(after);
            host.noteInteractionActivityNow();
        }

        if (program.stepIndex < program.totalSteps
            && !host.isCursorNearScreenPoint(program.toScreen, config.moveTargetTolerancePx)) {
            return;
        }

        Point finalPoint = new Point(program.toScreen);
        if (dropSweepMove && dropRegion != null) {
            finalPoint = clampScreenPointToRegion(finalPoint, dropRegion);
            Point terminalTarget = resolveDropTerminalFinalizeTarget(finalPoint, dropRegion);
            Point current = host.currentPointerLocationOr(finalPoint);
            boolean nearTarget = pointsNear(current, terminalTarget, DROP_TERMINAL_ALIGNMENT_TOLERANCE_PX);
            if (!nearTarget) {
                if (!host.dropSweepAwaitingFirstCursorSync()) {
                    Point correctionStart = current == null ? terminalTarget : current;
                    Point corrected = applyDropTerminalMicroCorrection(
                        robot,
                        correctionStart,
                        terminalTarget,
                        dropRegion,
                        true
                    );
                    if (!pointsNear(corrected, terminalTarget, DROP_TERMINAL_ALIGNMENT_TOLERANCE_PX)) {
                        corrected = applyDropTerminalMicroCorrection(
                            robot,
                            corrected,
                            terminalTarget,
                            dropRegion,
                            false
                        );
                    }
                    current = corrected;
                    if (!pointsNear(current, terminalTarget, DROP_TERMINAL_ALIGNMENT_TOLERANCE_PX)) {
                        if (!deferOrFailTerminalAlignment(program)) {
                            return;
                        }
                        return;
                    }
                } else if (current != null) {
                    // During first sync, avoid hard final snap; allow another smooth pass.
                    current = clampScreenPointToRegion(current, dropRegion);
                }
            }
            if (!pointsNear(current, terminalTarget, DROP_TERMINAL_ALIGNMENT_TOLERANCE_PX)) {
                if (!deferOrFailTerminalAlignment(program)) {
                    return;
                }
                return;
            }
            lastDropTerminalFinalizePoint = new Point(terminalTarget);
            if (current != null) {
                finalPoint = current;
            }
            host.setDropSweepLastTargetScreen(new Point(finalPoint));
        }
        resetTerminalAlignmentRetry(program);
        host.updateMotorCursorState(finalPoint);
        transitionAfterMove(program);
    }

    private void transitionAfterMove(MotorProgram program) {
        if (program == null) {
            return;
        }
        if (program.type == MotorGestureType.MOVE_ONLY) {
            host.completeMotorProgram(program, "motor_move_complete");
            return;
        }
        program.hoverSettleTicksRemaining = Math.max(0, program.profile.hoverSettleTicks);
        if (program.hoverSettleTicksRemaining > 0) {
            program.phase = MotorProgramPhase.HOVER_SETTLE;
            return;
        }
        if (program.profile.menuValidationMode != MotorMenuValidationMode.NONE) {
            program.phase = MotorProgramPhase.MENU_VALIDATE;
            return;
        }
        program.phase = MotorProgramPhase.CLICKING;
    }

    private int resolveStepsPerTickForMove(MotorProfile profile) {
        int maxSteps = profile == null ? 1 : Math.max(1, profile.maxMoveStepsPerTick);
        if (profile == null) {
            return maxSteps;
        }
        if (profile.gestureMode == MotorGestureMode.DROP_SWEEP) {
            if (host.dropSweepAwaitingFirstCursorSync()) {
                if (!config.humanizedTimingEnabled) {
                    return Math.max(1, Math.min(maxSteps, 2));
                }
                int capped = Math.max(1, Math.min(maxSteps, 3));
                if (capped <= 1) {
                    return 1;
                }
                if (capped == 2) {
                    return 2;
                }
                return ThreadLocalRandom.current().nextInt(100) < 18 ? 3 : 2;
            }
            if (!config.humanizedTimingEnabled) {
                return Math.max(1, Math.min(maxSteps, 2));
            }
            int capped = Math.max(1, Math.min(maxSteps, 3));
            if (capped <= 1) {
                return 1;
            }
            if (capped == 2) {
                return ThreadLocalRandom.current().nextInt(100) < 22 ? 1 : 2;
            }
            int roll = ThreadLocalRandom.current().nextInt(100);
            if (roll < 14) {
                return 1;
            }
            if (roll < 82) {
                return 2;
            }
            return 3;
        }
        if (config.motorOwnerIdle.equals(host.normalizedMotorOwnerName(profile.owner))) {
            if (!config.humanizedTimingEnabled) {
                return Math.max(1, Math.min(maxSteps, 2));
            }
            double roll = ThreadLocalRandom.current().nextDouble();
            if (roll < config.idleMovePauseTickChance) {
                return 0;
            }
            int cappedMaxSteps = Math.max(1, Math.min(maxSteps, 2));
            if (cappedMaxSteps <= 1) {
                return 1;
            }
            return roll < 0.42 ? 1 : 2;
        }
        if (host.isFishingWorldClickType(profile.clickType)) {
            if (!config.humanizedTimingEnabled) {
                return Math.max(1, Math.min(maxSteps, 2));
            }
            double roll = ThreadLocalRandom.current().nextDouble();
            double pauseChance = clampUnit(
                FISHING_MOVE_PAUSE_TICK_CHANCE
                    + ThreadLocalRandom.current().nextDouble(
                        -FISHING_MOVE_TICK_CHANCE_JITTER,
                        FISHING_MOVE_TICK_CHANCE_JITTER
                    )
            );
            double singleStepChance = clampUnit(
                FISHING_MOVE_SINGLE_STEP_TICK_CHANCE
                    + ThreadLocalRandom.current().nextDouble(
                        -FISHING_MOVE_TICK_CHANCE_JITTER,
                        FISHING_MOVE_TICK_CHANCE_JITTER
                    )
            );
            if (roll < pauseChance) {
                return 0;
            }
            if (roll < (pauseChance + singleStepChance)) {
                return 1;
            }
            if (maxSteps >= 3 && roll > (1.0 - FISHING_MOVE_TRIPLE_STEP_TICK_CHANCE)) {
                return 3;
            }
            int cappedMax = Math.max(1, Math.min(maxSteps, 2));
            if (cappedMax <= 1) {
                return 1;
            }
            return ThreadLocalRandom.current().nextDouble() < 0.63 ? 2 : 1;
        }
        if (!host.isWoodcutWorldClickType(profile.clickType)) {
            return maxSteps;
        }
        if (!config.humanizedTimingEnabled) {
            return Math.max(1, Math.min(maxSteps, 2));
        }
        double roll = ThreadLocalRandom.current().nextDouble();
        if (roll < config.woodcutMovePauseTickChance) {
            return 0;
        }
        if (roll < (config.woodcutMovePauseTickChance + config.woodcutMoveSingleStepTickChance)) {
            return 1;
        }
        if (maxSteps >= 3 && roll > 0.90) {
            return 3;
        }
        return Math.max(1, Math.min(maxSteps, 2));
    }

    private List<Point> buildDropSweepPathPoints(
        Point from,
        Point to,
        int baseSteps,
        Rectangle clampRegion,
        boolean firstSyncEntry
    ) {
        if (from == null || to == null) {
            return List.of();
        }
        Point finalTarget = clampScreenPointToRegion(new Point(to), clampRegion);
        int totalSteps = Math.max(4, baseSteps);
        List<Point> out = new ArrayList<>(totalSteps + 8);
        int selectedVariant = selectPathVariant(
            lastDropPathVariant,
            2,
            PATH_VARIANT_REPEAT_AVOID_CHANCE_PERCENT
        );
        lastDropPathVariant = selectedVariant;
        boolean useSCurve = selectedVariant == 0;
        if (useSCurve) {
            double splitT = firstSyncEntry
                ? ThreadLocalRandom.current().nextDouble(0.40, 0.62)
                : ThreadLocalRandom.current().nextDouble(0.44, 0.58);
            Point split = linearInterpolate(from, finalTarget, splitT);
            int sign = selectCurveSign(lastDropPrimaryCurveSign, CURVE_SIGN_REPEAT_AVOID_CHANCE_PERCENT);
            lastDropPrimaryCurveSign = sign;
            Point c1 = curveControlPoint(
                from,
                split,
                config.dropCurveScale,
                config.dropCurveMinPx,
                config.dropCurveMaxPx,
                sign
            );
            Point c2 = curveControlPoint(
                split,
                finalTarget,
                config.dropCurveScale,
                config.dropCurveMinPx,
                config.dropCurveMaxPx,
                -sign
            );
            int controlJitter = firstSyncEntry
                ? randomIntInclusive(1, 3)
                : config.dropControlJitterPx;
            c1 = jitterControlPoint(c1, controlJitter);
            c2 = jitterControlPoint(c2, controlJitter);
            int leadSteps = Math.max(2, (int) Math.round(totalSteps * 0.52));
            int settleSteps = Math.max(2, totalSteps - leadSteps);
            appendBezierSegmentPoints(out, from, c1, split, leadSteps, clampRegion);
            appendBezierSegmentPoints(out, split, c2, finalTarget, settleSteps, clampRegion);
        } else {
            int sign = selectCurveSign(lastDropPrimaryCurveSign, CURVE_SIGN_REPEAT_AVOID_CHANCE_PERCENT);
            lastDropPrimaryCurveSign = sign;
            Point control;
            if (firstSyncEntry) {
                control = curveControlPoint(
                    from,
                    finalTarget,
                    config.dropCurveScale * 1.08,
                    config.dropCurveMinPx + 2.0,
                    config.dropCurveMaxPx + 8.0,
                    sign
                );
            } else {
                control = curveControlPoint(
                    from,
                    finalTarget,
                    config.dropCurveScale,
                    config.dropCurveMinPx,
                    config.dropCurveMaxPx,
                    sign
                );
            }
            if (config.humanizedTimingEnabled) {
                int jitterRadius = firstSyncEntry
                    ? randomIntInclusive(1, 3)
                    : randomIntInclusive(1, config.dropControlJitterPx);
                control = jitterControlPoint(control, jitterRadius);
            }
            appendBezierSegmentPoints(out, from, control, finalTarget, totalSteps, clampRegion);
        }
        if (!firstSyncEntry
            && config.humanizedTimingEnabled
            && pixelDistance(from, finalTarget) >= config.dropMicroCorrectionMinDistancePx
            && ThreadLocalRandom.current().nextInt(100) < config.dropMicroCorrectionChancePercent) {
            Point correction = maybeDropMicroCorrectionTarget(finalTarget, clampRegion);
            if (correction != null) {
                Point outControl = curveControlPoint(
                    finalTarget,
                    correction,
                    config.curveScaleSettle,
                    config.curveMinPxSettle,
                    config.curveMaxPxSettle
                );
                Point backControl = curveControlPoint(
                    correction,
                    finalTarget,
                    config.curveScaleSettle,
                    config.curveMinPxSettle,
                    config.curveMaxPxSettle
                );
                appendBezierSegmentPoints(out, finalTarget, outControl, correction, 2, clampRegion);
                appendBezierSegmentPoints(out, correction, backControl, finalTarget, 2, clampRegion);
            }
        }
        if (out.isEmpty()) {
            out.add(finalTarget);
            return out;
        }
        Point last = out.get(out.size() - 1);
        if (!pointsNear(last, finalTarget, 0.0)) {
            out.add(finalTarget);
        }
        return out;
    }

    private List<Point> buildWoodcutWorldPathPoints(
        Point from,
        Point to,
        int baseSteps,
        boolean initialApproach
    ) {
        if (from == null || to == null) {
            return List.of();
        }
        Point approachTarget = to;
        int preClickCorrectionSteps = 0;
        Optional<Point> settleAnchor = woodcutPreClickSettlePlanner.resolveApproachAnchor(
            from,
            to,
            config.humanizedTimingEnabled
        );
        if (settleAnchor.isPresent()) {
            approachTarget = settleAnchor.get();
            preClickCorrectionSteps = woodcutPreClickSettlePlanner.resolveCorrectionSteps(
                initialApproach,
                config.humanizedTimingEnabled
            );
        }
        int totalSteps = Math.max(4, baseSteps);
        if (preClickCorrectionSteps > 0) {
            totalSteps = Math.max(4, totalSteps - preClickCorrectionSteps);
        }
        List<Point> out = new ArrayList<>(totalSteps + 6);
        int selectedVariant = selectPathVariant(
            lastWoodcutPathVariant,
            2,
            PATH_VARIANT_REPEAT_AVOID_CHANCE_PERCENT
        );
        lastWoodcutPathVariant = selectedVariant;
        boolean shortApproach = pixelDistance(from, approachTarget) <= WOODCUT_SHORT_APPROACH_DISTANCE_PX;
        boolean useSCurve = shortApproach || selectedVariant == 0;
        if (useSCurve) {
            double splitT;
            if (shortApproach) {
                splitT = ThreadLocalRandom.current().nextDouble(0.44, 0.56);
            } else if (initialApproach) {
                splitT = ThreadLocalRandom.current().nextDouble(0.42, 0.62);
            } else {
                splitT = ThreadLocalRandom.current().nextDouble(0.46, 0.58);
            }
            Point split = linearInterpolate(from, approachTarget, splitT);
            int sign = selectCurveSign(lastWoodcutPrimaryCurveSign, CURVE_SIGN_REPEAT_AVOID_CHANCE_PERCENT);
            lastWoodcutPrimaryCurveSign = sign;
            Point c1 = curveControlPoint(
                from,
                split,
                shortApproach ? 0.24 : 0.18,
                shortApproach ? 9.0 : 8.0,
                shortApproach ? 44.0 : 38.0,
                sign
            );
            Point c2 = curveControlPoint(
                split,
                approachTarget,
                shortApproach ? 0.22 : 0.16,
                shortApproach ? 8.0 : 6.0,
                shortApproach ? 40.0 : 34.0,
                -sign
            );
            int jitterRadius;
            if (shortApproach) {
                jitterRadius = initialApproach ? randomIntInclusive(3, 7) : randomIntInclusive(2, 5);
            } else {
                jitterRadius = initialApproach ? randomIntInclusive(2, 6) : randomIntInclusive(1, 4);
            }
            c1 = jitterControlPoint(c1, jitterRadius);
            c2 = jitterControlPoint(c2, jitterRadius);
            int leadSteps = Math.max(2, (int) Math.round(totalSteps * 0.54));
            int settleSteps = Math.max(2, totalSteps - leadSteps);
            appendBezierSegmentPoints(out, from, c1, split, leadSteps, null);
            appendBezierSegmentPoints(out, split, c2, approachTarget, settleSteps, null);
        } else {
            int sign = selectCurveSign(lastWoodcutPrimaryCurveSign, CURVE_SIGN_REPEAT_AVOID_CHANCE_PERCENT);
            lastWoodcutPrimaryCurveSign = sign;
            Point control = curveControlPoint(
                from,
                approachTarget,
                initialApproach ? 0.22 : 0.18,
                shortApproach ? 8.0 : 7.0,
                shortApproach ? 46.0 : 42.0,
                sign
            );
            if (config.humanizedTimingEnabled) {
                int jitterRadius = initialApproach
                    ? randomIntInclusive(3, 8)
                    : randomIntInclusive(2, 6);
                control = jitterControlPoint(control, jitterRadius);
            }
            appendBezierSegmentPoints(out, from, control, approachTarget, totalSteps, null);
        }
        if (preClickCorrectionSteps > 0) {
            int correctionSign = selectCurveSign(lastWoodcutPrimaryCurveSign, CURVE_SIGN_REPEAT_AVOID_CHANCE_PERCENT);
            lastWoodcutPrimaryCurveSign = correctionSign;
            boolean shortCorrection = pixelDistance(approachTarget, to) <= WOODCUT_SHORT_CORRECTION_DISTANCE_PX;
            Point correctionControl = curveControlPoint(
                approachTarget,
                to,
                shortCorrection ? 0.16 : 0.10,
                shortCorrection ? 3.0 : 2.0,
                shortCorrection ? 14.0 : 10.0,
                correctionSign
            );
            if (config.humanizedTimingEnabled) {
                correctionControl = jitterControlPoint(
                    correctionControl,
                    shortCorrection ? randomIntInclusive(2, 4) : randomIntInclusive(1, 2)
                );
            }
            appendBezierSegmentPoints(out, approachTarget, correctionControl, to, preClickCorrectionSteps, null);
        }
        if (out.isEmpty()) {
            out.add(new Point(to));
            return out;
        }
        Point last = out.get(out.size() - 1);
        if (!pointsNear(last, to, 0.0)) {
            out.add(new Point(to));
        }
        return out;
    }

    private List<Point> buildFishingWorldPathPoints(
        Point from,
        Point to,
        int baseSteps,
        boolean initialApproach
    ) {
        if (from == null || to == null) {
            return List.of();
        }
        int totalSteps = Math.max(4, baseSteps);
        List<Point> out = new ArrayList<>(totalSteps + 7);
        int selectedVariant = selectPathVariant(
            lastFishingPathVariant,
            3,
            PATH_VARIANT_REPEAT_AVOID_CHANCE_PERCENT
        );
        lastFishingPathVariant = selectedVariant;
        if (selectedVariant == 0) {
            double splitSample = initialApproach
                ? ThreadLocalRandom.current().nextDouble(0.40, 0.62)
                : ThreadLocalRandom.current().nextDouble(0.44, 0.59);
            double splitT = selectFishingSplitWithRepeatAvoid(splitSample);
            Point split = linearInterpolate(from, to, splitT);
            int sign = selectCurveSign(lastFishingPrimaryCurveSign, CURVE_SIGN_REPEAT_AVOID_CHANCE_PERCENT);
            lastFishingPrimaryCurveSign = sign;
            Point c1 = curveControlPoint(
                from,
                split,
                initialApproach ? 0.22 : 0.19,
                7.0,
                42.0,
                sign
            );
            Point c2 = curveControlPoint(
                split,
                to,
                initialApproach ? 0.18 : 0.16,
                6.0,
                34.0,
                -sign
            );
            int jitterRadius = initialApproach ? randomIntInclusive(3, 8) : randomIntInclusive(2, 6);
            c1 = jitterControlPoint(c1, jitterRadius);
            c2 = jitterControlPoint(c2, jitterRadius);
            int leadSteps = Math.max(
                2,
                (int) Math.round(
                    totalSteps * (initialApproach
                        ? ThreadLocalRandom.current().nextDouble(0.50, 0.62)
                        : ThreadLocalRandom.current().nextDouble(0.52, 0.60))
                )
            );
            int settleSteps = Math.max(2, totalSteps - leadSteps);
            appendBezierSegmentPoints(out, from, c1, split, leadSteps, null);
            appendBezierSegmentPoints(out, split, c2, to, settleSteps, null);
        } else if (selectedVariant == 1) {
            int sign = selectCurveSign(lastFishingPrimaryCurveSign, CURVE_SIGN_REPEAT_AVOID_CHANCE_PERCENT);
            lastFishingPrimaryCurveSign = sign;
            Point control = curveControlPoint(
                from,
                to,
                initialApproach ? 0.24 : 0.20,
                8.0,
                48.0,
                sign
            );
            if (config.humanizedTimingEnabled) {
                int jitterRadius = initialApproach
                    ? randomIntInclusive(3, 9)
                    : randomIntInclusive(2, 7);
                control = jitterControlPoint(control, jitterRadius);
            }
            appendBezierSegmentPoints(out, from, control, to, totalSteps, null);
        } else {
            double legSample = initialApproach
                ? ThreadLocalRandom.current().nextDouble(0.62, 0.78)
                : ThreadLocalRandom.current().nextDouble(0.58, 0.72);
            double legT = selectFishingLegWithRepeatAvoid(legSample);
            Point legPoint = linearInterpolate(from, to, legT);
            int sign = selectCurveSign(lastFishingPrimaryCurveSign, CURVE_SIGN_REPEAT_AVOID_CHANCE_PERCENT);
            lastFishingPrimaryCurveSign = sign;
            Point c1 = curveControlPoint(
                from,
                legPoint,
                initialApproach ? 0.18 : 0.15,
                5.0,
                30.0,
                sign
            );
            Point c2 = curveControlPoint(
                legPoint,
                to,
                0.14,
                4.0,
                22.0,
                -sign
            );
            int jitterRadius = config.humanizedTimingEnabled
                ? (initialApproach ? randomIntInclusive(2, 6) : randomIntInclusive(1, 5))
                : 1;
            c1 = jitterControlPoint(c1, jitterRadius);
            c2 = jitterControlPoint(c2, jitterRadius);
            int leadSteps = Math.max(
                2,
                (int) Math.round(
                    totalSteps * (initialApproach
                        ? ThreadLocalRandom.current().nextDouble(0.62, 0.76)
                        : ThreadLocalRandom.current().nextDouble(0.60, 0.72))
                )
            );
            int finishSteps = Math.max(2, totalSteps - leadSteps);
            appendBezierSegmentPoints(out, from, c1, legPoint, leadSteps, null);
            appendBezierSegmentPoints(out, legPoint, c2, to, finishSteps, null);
        }
        if (!out.isEmpty()
            && config.humanizedTimingEnabled
            && pixelDistance(from, to) >= 24.0
            && ThreadLocalRandom.current().nextDouble() < FISHING_TRUE_OVERSHOOT_CHANCE) {
            Point anchor = out.get(out.size() - 1);
            Point overshoot = resolveFishingOvershootPoint(from, to);
            if (overshoot != null && !pointsNear(overshoot, to, 0.0)) {
                Point toOvershootControl = curveControlPoint(
                    anchor,
                    overshoot,
                    0.11,
                    3.0,
                    12.0
                );
                Point returnControl = curveControlPoint(
                    overshoot,
                    to,
                    0.13,
                    4.0,
                    14.0
                );
                appendBezierSegmentPoints(out, anchor, toOvershootControl, overshoot, randomIntInclusive(2, 4), null);
                appendBezierSegmentPoints(out, overshoot, returnControl, to, randomIntInclusive(2, 4), null);
            }
        }
        if (out.isEmpty()) {
            out.add(new Point(to));
            return out;
        }
        Point last = out.get(out.size() - 1);
        if (!pointsNear(last, to, 0.0)) {
            out.add(new Point(to));
        }
        return out;
    }

    private List<Point> buildBankPathPoints(Point from, Point to, int baseSteps) {
        if (from == null || to == null) {
            return List.of();
        }
        int totalSteps = Math.max(4, baseSteps);
        List<Point> out = new ArrayList<>(totalSteps + 4);
        double splitT = config.humanizedTimingEnabled
            ? ThreadLocalRandom.current().nextDouble(0.46, 0.60)
            : 0.53;
        Point split = linearInterpolate(from, to, splitT);
        int sign = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
        Point c1 = curveControlPoint(
            from,
            split,
            0.16,
            6.0,
            32.0,
            sign
        );
        Point c2 = curveControlPoint(
            split,
            to,
            0.14,
            5.0,
            28.0,
            -sign
        );
        int jitterRadius = config.humanizedTimingEnabled ? randomIntInclusive(1, 4) : 1;
        c1 = jitterControlPoint(c1, jitterRadius);
        c2 = jitterControlPoint(c2, jitterRadius);
        int leadSteps = Math.max(2, (int) Math.round(totalSteps * 0.52));
        int settleSteps = Math.max(2, totalSteps - leadSteps);
        appendBezierSegmentPoints(out, from, c1, split, leadSteps, null);
        appendBezierSegmentPoints(out, split, c2, to, settleSteps, null);
        if (out.isEmpty()) {
            out.add(new Point(to));
            return out;
        }
        Point last = out.get(out.size() - 1);
        if (!pointsNear(last, to, 0.0)) {
            out.add(new Point(to));
        }
        return out;
    }

    private void appendBezierSegmentPoints(
        List<Point> out,
        Point from,
        Point control,
        Point to,
        int steps,
        Rectangle clampRegion
    ) {
        if (out == null || from == null || to == null) {
            return;
        }
        int total = Math.max(1, steps);
        Point controlPoint = control == null
            ? curveControlPoint(from, to, config.dropCurveScale, config.dropCurveMinPx, config.dropCurveMaxPx)
            : control;
        Point last = out.isEmpty() ? null : out.get(out.size() - 1);
        for (int i = 1; i <= total; i++) {
            double t = (double) i / (double) total;
            double progress = smoothstep(t);
            Point p = quadraticBezier(from, controlPoint, to, progress);
            if (clampRegion != null) {
                p = clampScreenPointToRegion(p, clampRegion);
            }
            if (last == null || !pointsNear(last, p, 0.0)) {
                out.add(p);
                last = p;
            }
        }
    }

    private Point maybeDropMicroCorrectionTarget(Point target, Rectangle clampRegion) {
        if (target == null) {
            return null;
        }
        int radius = randomIntInclusive(1, config.dropMicroCorrectionMaxRadiusPx);
        int dx = randomIntInclusive(-radius, radius);
        int dy = randomIntInclusive(-radius, radius);
        if (dx == 0 && dy == 0) {
            return null;
        }
        Point correction = new Point(target.x + dx, target.y + dy);
        if (clampRegion != null) {
            correction = clampScreenPointToRegion(correction, clampRegion);
        }
        return pointsNear(correction, target, 0.0) ? null : correction;
    }

    private Point applyDropTerminalMicroCorrection(
        Robot robot,
        Point from,
        Point to,
        Rectangle clampRegion,
        boolean jitterControl
    ) {
        if (robot == null || to == null) {
            return from;
        }
        Point target = clampScreenPointToRegion(new Point(to), clampRegion);
        Point start = from == null ? target : clampScreenPointToRegion(new Point(from), clampRegion);
        if (pointsNear(start, target, DROP_TERMINAL_ALIGNMENT_TOLERANCE_PX)) {
            return start;
        }
        int steps = pixelDistance(start, target) >= 3.0 ? 4 : 3;
        Point control = curveControlPoint(
            start,
            target,
            config.curveScaleSettle,
            config.curveMinPxSettle,
            config.curveMaxPxSettle
        );
        if (jitterControl && config.humanizedTimingEnabled) {
            control = jitterControlPoint(control, randomIntInclusive(1, 2));
        }
        Point last = start;
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / (double) steps;
            Point p = quadraticBezier(start, control, target, smoothstep(t));
            p = clampScreenPointToRegion(p, clampRegion);
            if (!pointsNear(last, p, 0.0)) {
                robot.mouseMove(p.x, p.y);
                last = p;
            }
        }
        return host.currentPointerLocationOr(last);
    }

    private Point resolveDropTerminalFinalizeTarget(Point target, Rectangle clampRegion) {
        Point base = clampScreenPointToRegion(target == null ? null : new Point(target), clampRegion);
        if (base == null) {
            return target;
        }
        Point last = lastDropTerminalFinalizePoint == null ? null : new Point(lastDropTerminalFinalizePoint);
        Point selected = null;
        for (int i = 0; i < 10; i++) {
            int dx = randomIntInclusive(
                -DROP_TERMINAL_FINALIZE_JITTER_RADIUS_PX,
                DROP_TERMINAL_FINALIZE_JITTER_RADIUS_PX
            );
            int dy = randomIntInclusive(
                -DROP_TERMINAL_FINALIZE_JITTER_RADIUS_PX,
                DROP_TERMINAL_FINALIZE_JITTER_RADIUS_PX
            );
            Point candidate = clampScreenPointToRegion(new Point(base.x + dx, base.y + dy), clampRegion);
            if (last == null || !pointsNear(last, candidate, 0.0)) {
                selected = candidate;
                break;
            }
        }
        if (selected == null) {
            return null;
        }
        return selected;
    }

    private boolean deferOrFailTerminalAlignment(MotorProgram program) {
        if (program == null) {
            return false;
        }
        int retries = Math.max(0, program.terminalAlignmentRetryCount) + 1;
        program.terminalAlignmentRetryCount = retries;
        if (retries > DROP_TERMINAL_ALIGNMENT_MAX_RETRIES) {
            host.failMotorProgram(program, MOTOR_MOVE_REASON_TERMINAL_ALIGNMENT_RETRY_EXHAUSTED);
            return false;
        }
        program.resultReason = MOTOR_MOVE_REASON_TERMINAL_ALIGNMENT_DEFERRED;
        return true;
    }

    private int selectPathVariant(int lastVariant, int variantCount, int repeatAvoidChancePercent) {
        int count = Math.max(1, variantCount);
        if (count <= 1) {
            return 0;
        }
        int candidate = ThreadLocalRandom.current().nextInt(count);
        int avoidChance = clampPercent(repeatAvoidChancePercent);
        if (lastVariant >= 0
            && lastVariant < count
            && candidate == lastVariant
            && ThreadLocalRandom.current().nextInt(100) < avoidChance) {
            candidate = (candidate + 1 + ThreadLocalRandom.current().nextInt(count - 1)) % count;
        }
        return candidate;
    }

    private int selectCurveSign(int previousSign, int repeatAvoidChancePercent) {
        int sign = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
        int avoidChance = clampPercent(repeatAvoidChancePercent);
        if (previousSign != 0
            && sign == previousSign
            && ThreadLocalRandom.current().nextInt(100) < avoidChance) {
            sign = -sign;
        }
        return sign;
    }

    private double selectFishingSplitWithRepeatAvoid(double sampledSplitT) {
        return selectFishingSegmentFractionWithRepeatAvoid(
            sampledSplitT,
            lastFishingSplitBucket,
            bucket -> lastFishingSplitBucket = bucket
        );
    }

    private double selectFishingLegWithRepeatAvoid(double sampledLegT) {
        return selectFishingSegmentFractionWithRepeatAvoid(
            sampledLegT,
            lastFishingLegBucket,
            bucket -> lastFishingLegBucket = bucket
        );
    }

    private double selectFishingSegmentFractionWithRepeatAvoid(
        double sampledFraction,
        int previousBucket,
        java.util.function.IntConsumer updateBucket
    ) {
        double clamped = Math.max(0.18, Math.min(0.84, sampledFraction));
        int bucket = clampInt((int) Math.floor(clamped * 10.0), 0, 9);
        if (previousBucket >= 0
            && bucket == previousBucket
            && ThreadLocalRandom.current().nextInt(100) < PATH_VARIANT_REPEAT_AVOID_CHANCE_PERCENT) {
            int direction = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
            bucket = clampInt(bucket + direction, 0, 9);
            double jitter = ThreadLocalRandom.current().nextDouble(-0.035, 0.035);
            clamped = Math.max(0.18, Math.min(0.84, ((double) bucket + 0.5) / 10.0 + jitter));
        }
        updateBucket.accept(bucket);
        return clamped;
    }

    private static void resetTerminalAlignmentRetry(MotorProgram program) {
        if (program == null) {
            return;
        }
        program.terminalAlignmentRetryCount = 0;
        if (program.status == MotorGestureStatus.IN_FLIGHT) {
            program.resultReason = MOTOR_MOVE_REASON_IN_FLIGHT;
        }
    }

    private static Point jitterControlPoint(Point controlPoint, int radiusPx) {
        if (controlPoint == null || radiusPx <= 0) {
            return controlPoint;
        }
        int dx = randomIntInclusive(-radiusPx, radiusPx);
        int dy = randomIntInclusive(-radiusPx, radiusPx);
        return new Point(controlPoint.x + dx, controlPoint.y + dy);
    }

    private static Point clampScreenPointToRegion(Point point, Rectangle region) {
        if (point == null || region == null) {
            return point;
        }
        int x = Math.max(region.x, Math.min(region.x + Math.max(0, region.width - 1), point.x));
        int y = Math.max(region.y, Math.min(region.y + Math.max(0, region.height - 1), point.y));
        return new Point(x, y);
    }

    private static Point curveControlPoint(Point from, Point to) {
        return curveControlPoint(from, to, 0.20, 6.0, 36.0);
    }

    private static Point curveControlPoint(
        Point from,
        Point to,
        double curveScale,
        double curveMinPx,
        double curveMaxPx
    ) {
        return curveControlPoint(from, to, curveScale, curveMinPx, curveMaxPx, 0);
    }

    private static Point curveControlPoint(
        Point from,
        Point to,
        double curveScale,
        double curveMinPx,
        double curveMaxPx,
        int preferredSign
    ) {
        double midX = (from.x + to.x) / 2.0;
        double midY = (from.y + to.y) / 2.0;
        double dx = (double) to.x - from.x;
        double dy = (double) to.y - from.y;
        double len = Math.max(1.0, Math.hypot(dx, dy));
        double nx = -dy / len;
        double ny = dx / len;
        double curve = Math.max(curveMinPx, Math.min(curveMaxPx, len * curveScale));
        int sign;
        if (preferredSign > 0) {
            sign = 1;
        } else if (preferredSign < 0) {
            sign = -1;
        } else {
            sign = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
        }
        int cx = (int) Math.round(midX + (nx * curve * sign));
        int cy = (int) Math.round(midY + (ny * curve * sign));
        return new Point(cx, cy);
    }

    private static Point quadraticBezier(Point p0, Point p1, Point p2, double t) {
        double u = 1.0 - t;
        double x = (u * u * p0.x) + (2.0 * u * t * p1.x) + (t * t * p2.x);
        double y = (u * u * p0.y) + (2.0 * u * t * p1.y) + (t * t * p2.y);
        return new Point((int) Math.round(x), (int) Math.round(y));
    }

    private static Point linearInterpolate(Point from, Point to, double t) {
        double clamped = Math.max(0.0, Math.min(1.0, t));
        int x = (int) Math.round(from.x + ((to.x - from.x) * clamped));
        int y = (int) Math.round(from.y + ((to.y - from.y) * clamped));
        return new Point(x, y);
    }

    private static Point resolveFishingOvershootPoint(Point from, Point to) {
        if (from == null || to == null) {
            return null;
        }
        double dx = (double) to.x - from.x;
        double dy = (double) to.y - from.y;
        double len = Math.hypot(dx, dy);
        if (len < 10.0) {
            return null;
        }
        double ux = dx / len;
        double uy = dy / len;
        double nx = -uy;
        double ny = ux;
        double overshootMagnitude = ThreadLocalRandom.current().nextDouble(3.5, Math.min(18.0, (len * 0.16) + 4.5));
        double lateralMagnitude = ThreadLocalRandom.current().nextDouble(-6.0, 6.0);
        int ox = (int) Math.round(to.x + (ux * overshootMagnitude) + (nx * lateralMagnitude));
        int oy = (int) Math.round(to.y + (uy * overshootMagnitude) + (ny * lateralMagnitude));
        return new Point(ox, oy);
    }

    private static double smoothstep(double t) {
        double clamped = Math.max(0.0, Math.min(1.0, t));
        return clamped * clamped * (3.0 - (2.0 * clamped));
    }

    private static double resolveMoveProgress(
        double t,
        Point from,
        Point to,
        ClickMotionSettings motion
    ) {
        double clamped = clampUnit(t);
        double progress = smoothstep(clamped);
        if (motion == null) {
            return progress;
        }
        int accelPercent = clampPercent(motion.moveAccelPercent);
        int decelPercent = clampPercent(motion.moveDecelPercent);
        if (accelPercent > 0) {
            double accelExponent = 1.0 + (1.8 * ((double) accelPercent / 100.0));
            double easeIn = Math.pow(clamped, accelExponent);
            double accelBlend = Math.min(0.85, (double) accelPercent / 100.0);
            progress = mix(progress, easeIn, accelBlend);
        }
        if (decelPercent > 0) {
            double decelExponent = 1.0 + (2.2 * ((double) decelPercent / 100.0));
            double easeOut = 1.0 - Math.pow(1.0 - clamped, decelExponent);
            double decelBlend = Math.min(0.90, (double) decelPercent / 100.0);
            int terminalRadius = Math.max(0, motion.terminalSlowdownRadiusPx);
            if (terminalRadius > 0 && from != null && to != null) {
                double distance = pixelDistance(from, to);
                if (distance > 0.0) {
                    double terminalInfluence = Math.min(1.0, Math.max(0.0, (double) terminalRadius / distance));
                    decelBlend = Math.min(0.95, Math.max(decelBlend, decelBlend + (0.35 * terminalInfluence)));
                }
            }
            progress = mix(progress, easeOut, decelBlend);
        }
        return clampUnit(progress);
    }

    private static double mix(double a, double b, double weight) {
        double w = clampUnit(weight);
        return (a * (1.0 - w)) + (b * w);
    }

    private static double clampUnit(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static boolean pointsNear(Point a, Point b, double tolerancePx) {
        if (a == null || b == null) {
            return false;
        }
        return pixelDistance(a, b) <= Math.max(0.0, tolerancePx);
    }

    private static double pixelDistance(Point a, Point b) {
        if (a == null || b == null) {
            return 0.0;
        }
        double dx = (double) a.x - b.x;
        double dy = (double) a.y - b.y;
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

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static int clampInt(int value, int minInclusive, int maxInclusive) {
        int lo = Math.min(minInclusive, maxInclusive);
        int hi = Math.max(minInclusive, maxInclusive);
        return Math.max(lo, Math.min(hi, value));
    }
}
