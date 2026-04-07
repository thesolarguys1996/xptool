package com.xptool.executor;

import com.xptool.motion.MotionProfile;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.concurrent.ThreadLocalRandom;

final class MotorProgramClickEngine {
    interface Host {
        boolean tryConsumeMouseMutationBudget();

        Robot getOrCreateRobot();

        void failMotorProgram(MotorProgram program, String reason);

        void completeMotorProgram(MotorProgram program, String reason);

        void noteInteractionClickSuccess(String clickType);
    }

    static final class Config {
        final long microSettleBeforeClickMs;
        final long microButtonDownMs;
        final String clickTypeWoodcutWorld;
        final boolean humanizedTimingEnabled;

        Config(
            long microSettleBeforeClickMs,
            long microButtonDownMs,
            String clickTypeWoodcutWorld,
            boolean humanizedTimingEnabled
        ) {
            this.microSettleBeforeClickMs = Math.max(0L, microSettleBeforeClickMs);
            this.microButtonDownMs = Math.max(0L, microButtonDownMs);
            this.clickTypeWoodcutWorld = clickTypeWoodcutWorld == null ? "" : clickTypeWoodcutWorld;
            this.humanizedTimingEnabled = humanizedTimingEnabled;
        }
    }

    private final Host host;
    private final Config config;

    MotorProgramClickEngine(Host host, Config config) {
        this.host = host;
        this.config = config;
    }

    void runMotorProgramClick(MotorProgram program) {
        if (program == null) {
            return;
        }
        long now = System.currentTimeMillis();
        switch (program.clickPhase) {
            case PREPARE:
                if (program.profile.enforceMutationBudget && !host.tryConsumeMouseMutationBudget()) {
                    return;
                }
                if (host.getOrCreateRobot() == null) {
                    host.failMotorProgram(program, "motor_click_robot_unavailable");
                    return;
                }
                ClickTiming timing = resolveClickTiming(program);
                program.clickButtonDownDurationMs = timing.buttonDownMs;
                program.clickPhaseReadyAtMs = now + Math.max(0L, timing.settleMs);
                program.clickPhase = MotorClickPhase.WAIT_SETTLE;
                return;
            case WAIT_SETTLE:
                if (now < program.clickPhaseReadyAtMs) {
                    return;
                }
                Robot robotForPress = host.getOrCreateRobot();
                if (robotForPress == null) {
                    host.failMotorProgram(program, "motor_click_robot_unavailable");
                    return;
                }
                robotForPress.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                program.clickPhaseReadyAtMs = now + Math.max(0L, program.clickButtonDownDurationMs);
                program.clickPhase = MotorClickPhase.WAIT_RELEASE;
                return;
            case WAIT_RELEASE:
                if (now < program.clickPhaseReadyAtMs) {
                    return;
                }
                Robot robotForRelease = host.getOrCreateRobot();
                if (robotForRelease == null) {
                    host.failMotorProgram(program, "motor_click_robot_unavailable");
                    return;
                }
                robotForRelease.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                program.clickPhase = MotorClickPhase.COMPLETE;
                host.noteInteractionClickSuccess(program.profile.clickType);
                host.completeMotorProgram(program, "motor_click_complete");
                return;
            case COMPLETE:
            default:
                return;
        }
    }

    private ClickTiming resolveClickTiming(MotorProgram program) {
        ClickMotionSettings motion = program.profile.clickSettings == null
            ? MotionProfile.GENERIC_INTERACT.directClickSettings
            : program.profile.clickSettings;
        long settleMs = Math.max(config.microSettleBeforeClickMs, motion.preClickDelayMs);
        long downMs = Math.max(config.microButtonDownMs, motion.postClickDelayMs);
        if (config.clickTypeWoodcutWorld.equals(program.profile.clickType)) {
            long settleBase = Math.max(config.microSettleBeforeClickMs, Math.round(motion.preClickDelayMs * 0.84));
            long downBase = Math.max(config.microButtonDownMs, Math.round(motion.postClickDelayMs * 0.92));
            long settleVariance = config.humanizedTimingEnabled ? randomBetween(0L, 18L) : 0L;
            long downVariance = config.humanizedTimingEnabled ? randomBetween(0L, 12L) : 0L;
            settleMs = settleBase + settleVariance;
            downMs = downBase + downVariance;
        }
        return new ClickTiming(settleMs, downMs);
    }

    private static final class ClickTiming {
        private final long settleMs;
        private final long buttonDownMs;

        private ClickTiming(long settleMs, long buttonDownMs) {
            this.settleMs = settleMs;
            this.buttonDownMs = buttonDownMs;
        }
    }

    private static long randomBetween(long minInclusive, long maxInclusive) {
        long lo = Math.min(minInclusive, maxInclusive);
        long hi = Math.max(minInclusive, maxInclusive);
        if (hi <= lo) {
            return lo;
        }
        if (hi == Long.MAX_VALUE) {
            long sampled = ThreadLocalRandom.current().nextLong(lo, hi);
            return ThreadLocalRandom.current().nextBoolean() ? sampled : hi;
        }
        return ThreadLocalRandom.current().nextLong(lo, hi + 1L);
    }
}
