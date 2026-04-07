package com.xptool.executor;

final class TypingProfile {
    private final int baseKeyDelayMs;
    private final double keyJitterPct;
    private final int chunkPauseChancePct;
    private final int chunkPauseScalePct;
    private final double typoChancePct;
    private final int correctionScalePct;
    private final int submitHesitationChancePct;
    private final int submitHesitationScalePct;
    private final int focusReclickChancePct;

    TypingProfile(
        int baseKeyDelayMs,
        double keyJitterPct,
        int chunkPauseChancePct,
        int chunkPauseScalePct,
        double typoChancePct,
        int correctionScalePct,
        int submitHesitationChancePct,
        int submitHesitationScalePct,
        int focusReclickChancePct
    ) {
        this.baseKeyDelayMs = Math.max(1, baseKeyDelayMs);
        this.keyJitterPct = clampNonNegative(keyJitterPct);
        this.chunkPauseChancePct = clampPercent(chunkPauseChancePct);
        this.chunkPauseScalePct = Math.max(0, chunkPauseScalePct);
        this.typoChancePct = clampNonNegative(typoChancePct);
        this.correctionScalePct = Math.max(0, correctionScalePct);
        this.submitHesitationChancePct = clampPercent(submitHesitationChancePct);
        this.submitHesitationScalePct = Math.max(0, submitHesitationScalePct);
        this.focusReclickChancePct = clampPercent(focusReclickChancePct);
    }

    static TypingProfile defaults() {
        return new TypingProfile(
            120,
            0.32,
            14,
            180,
            2.5,
            130,
            22,
            210,
            9
        );
    }

    int baseKeyDelayMs() {
        return baseKeyDelayMs;
    }

    double keyJitterPct() {
        return keyJitterPct;
    }

    int chunkPauseChancePct() {
        return chunkPauseChancePct;
    }

    int chunkPauseScalePct() {
        return chunkPauseScalePct;
    }

    double typoChancePct() {
        return typoChancePct;
    }

    int correctionScalePct() {
        return correctionScalePct;
    }

    int submitHesitationChancePct() {
        return submitHesitationChancePct;
    }

    int submitHesitationScalePct() {
        return submitHesitationScalePct;
    }

    int focusReclickChancePct() {
        return focusReclickChancePct;
    }

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static double clampNonNegative(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0) {
            return 0.0;
        }
        return value;
    }
}
