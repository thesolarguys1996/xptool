package com.xptool.executor;

public final class FatigueSnapshot {
    private static final FatigueSnapshot NEUTRAL = new FatigueSnapshot(0.0);

    private final double load01;
    private final String band;

    private FatigueSnapshot(double load01) {
        this.load01 = clamp01(load01);
        this.band = resolveBand(this.load01);
    }

    public static FatigueSnapshot neutral() {
        return NEUTRAL;
    }

    public static FatigueSnapshot fromLoad(double load01) {
        if (load01 <= 0.0) {
            return NEUTRAL;
        }
        return new FatigueSnapshot(load01);
    }

    public double load01() {
        return load01;
    }

    public int loadPercent() {
        return (int) Math.round(load01 * 100.0);
    }

    public String band() {
        return band;
    }

    public int dropCooldownBiasMs(int maxMs) {
        return scaledInt(maxMs);
    }

    public int dropSecondDispatchPenaltyPercent(int maxPercent) {
        return scaledInt(maxPercent);
    }

    public int dropRhythmPauseBiasPercent(int maxPercent) {
        return scaledInt(maxPercent);
    }

    public int dropHesitationChanceBiasPercent(int maxPercent) {
        return scaledInt(maxPercent);
    }

    public int idleNoopBiasPercent(int maxPercent) {
        return scaledInt(maxPercent);
    }

    public int idleIntervalExtraTicks(int maxTicks) {
        return scaledInt(maxTicks);
    }

    public int woodcutNoticeChanceBonusPercent(int maxPercent) {
        return scaledInt(maxPercent);
    }

    public int woodcutNoticeDelayExtraMs(int maxMs) {
        return scaledInt(maxMs);
    }

    public int woodcutReclickCooldownBiasMs(int maxMs) {
        return scaledInt(maxMs);
    }

    public int fishingNoticeChanceBonusPercent(int maxPercent) {
        return scaledInt(maxPercent);
    }

    public int fishingNoticeDelayExtraMs(int maxMs) {
        return scaledInt(maxMs);
    }

    public int fishingReclickCooldownBiasMs(int maxMs) {
        return scaledInt(maxMs);
    }

    private int scaledInt(int maxValue) {
        int boundedMax = Math.max(0, maxValue);
        if (boundedMax == 0 || load01 <= 0.0) {
            return 0;
        }
        return (int) Math.round(load01 * boundedMax);
    }

    private static String resolveBand(double load01) {
        if (load01 >= 0.70) {
            return "high";
        }
        if (load01 >= 0.38) {
            return "medium";
        }
        return "low";
    }

    private static double clamp01(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }
}
