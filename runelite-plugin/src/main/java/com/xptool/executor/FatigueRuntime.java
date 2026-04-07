package com.xptool.executor;

final class FatigueRuntime {
    private static final double ACTION_LOAD_BASE = 0.070;
    private static final double ACTION_LOAD_BURST_BONUS = 0.040;
    private static final double ACTION_LOAD_RATIO = boundedPositiveDoubleProperty(
        "xptool.fatigueLoadRatio",
        1.45,
        0.10,
        4.0
    );
    private static final long BURST_WINDOW_MS = 1800L;
    private static final double DECAY_PER_SECOND = 0.010;
    private static final double RECOVERY_PER_SECOND = 0.018;
    private static final long RECOVERY_IDLE_THRESHOLD_MS = 3500L;

    private double load01 = 0.0;
    private long lastUpdatedAtMs = 0L;
    private long lastActionAtMs = 0L;

    void noteActionDispatch() {
        long now = System.currentTimeMillis();
        applyDecay(now);
        double loadDelta = ACTION_LOAD_BASE;
        if (lastActionAtMs > 0L && now - lastActionAtMs <= BURST_WINDOW_MS) {
            loadDelta += ACTION_LOAD_BURST_BONUS;
        }
        loadDelta *= ACTION_LOAD_RATIO;
        load01 = clamp01(load01 + loadDelta);
        lastActionAtMs = now;
    }

    FatigueSnapshot snapshot() {
        applyDecay(System.currentTimeMillis());
        return FatigueSnapshot.fromLoad(load01);
    }

    void reset() {
        load01 = 0.0;
        lastUpdatedAtMs = 0L;
        lastActionAtMs = 0L;
    }

    private void applyDecay(long now) {
        if (now <= 0L) {
            return;
        }
        if (lastUpdatedAtMs <= 0L) {
            lastUpdatedAtMs = now;
            return;
        }
        long elapsedMs = now - lastUpdatedAtMs;
        if (elapsedMs <= 0L) {
            return;
        }
        double elapsedSeconds = elapsedMs / 1000.0;
        double decayPerSecond = DECAY_PER_SECOND;
        if (lastActionAtMs <= 0L || now - lastActionAtMs >= RECOVERY_IDLE_THRESHOLD_MS) {
            decayPerSecond += RECOVERY_PER_SECOND;
        }
        load01 = clamp01(load01 - (elapsedSeconds * decayPerSecond));
        lastUpdatedAtMs = now;
    }

    private static double clamp01(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double boundedPositiveDoubleProperty(
        String key,
        double defaultValue,
        double minValue,
        double maxValue
    ) {
        String raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            double parsed = Double.parseDouble(raw.trim());
            if (Double.isNaN(parsed) || Double.isInfinite(parsed)) {
                return defaultValue;
            }
            return Math.max(minValue, Math.min(maxValue, parsed));
        } catch (RuntimeException ignored) {
            return defaultValue;
        }
    }
}
