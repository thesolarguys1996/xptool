package com.xptool.executor;

final class BreakProfile {
    private final double workProgressStepPctPerTick;
    private final double breakProgressStepPctPerTick;
    private final int breakStartChancePct;
    private final int breakEndChancePct;
    private final int resumeCooldownTicks;
    private final boolean logoutOnBreakStart;

    BreakProfile(
        double workProgressStepPctPerTick,
        double breakProgressStepPctPerTick,
        int breakStartChancePct,
        int breakEndChancePct,
        int resumeCooldownTicks,
        boolean logoutOnBreakStart
    ) {
        this.workProgressStepPctPerTick = clampPositive(workProgressStepPctPerTick, 0.01);
        this.breakProgressStepPctPerTick = clampPositive(breakProgressStepPctPerTick, 0.01);
        this.breakStartChancePct = clampPercent(breakStartChancePct);
        this.breakEndChancePct = clampPercent(breakEndChancePct);
        this.resumeCooldownTicks = Math.max(0, resumeCooldownTicks);
        this.logoutOnBreakStart = logoutOnBreakStart;
    }

    static BreakProfile defaults() {
        return new BreakProfile(
            0.08,
            0.45,
            22,
            35,
            3,
            true
        );
    }

    double workProgressStepPctPerTick() {
        return workProgressStepPctPerTick;
    }

    double breakProgressStepPctPerTick() {
        return breakProgressStepPctPerTick;
    }

    int breakStartChancePct() {
        return breakStartChancePct;
    }

    int breakEndChancePct() {
        return breakEndChancePct;
    }

    int resumeCooldownTicks() {
        return resumeCooldownTicks;
    }

    boolean logoutOnBreakStart() {
        return logoutOnBreakStart;
    }

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static double clampPositive(double value, double fallback) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0.0) {
            return fallback;
        }
        return value;
    }
}
