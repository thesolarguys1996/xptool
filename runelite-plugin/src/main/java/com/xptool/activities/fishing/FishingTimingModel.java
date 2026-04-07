package com.xptool.activities.fishing;

import com.xptool.core.runtime.FatigueSnapshot;
import net.runelite.api.coords.WorldPoint;

public final class FishingTimingModel {
    public static final int WALK_APPROACH_DISTANCE_TILES = 7;
    public static final int ATTEMPT_WORLDPOINT_MATCH_RADIUS_TILES = 1;
    public static final int POST_DISPATCH_LOCAL_TRAVEL_MIN_DISTANCE_TILES = 2;
    public static final long SETTLE_GATE_ACTIVE_MAX_MS = 1500L;
    public static final long CADENCE_GATE_ACTIVE_MAX_MS = 2400L;
    public static final long CADENCE_WAIT_CAP_MS = 220L;
    public static final int WORLDPOINT_RECLICK_RADIUS_TILES = 1;

    private static final double RECLICK_JITTER_MIN_SCALE = 0.84;
    private static final double RECLICK_JITTER_MAX_SCALE = 1.16;
    private static final double GLOBAL_RECLICK_JITTER_MIN_SCALE = 0.52;
    private static final double GLOBAL_RECLICK_JITTER_MAX_SCALE = 0.78;
    private static final long GLOBAL_RECLICK_COOLDOWN_MIN_MS = 900L;
    private static final int FATIGUE_RECLICK_COOLDOWN_BIAS_MAX_MS = 400;
    private static final long NO_ANIMATION_SAME_TARGET_BIAS_MIN_MS = 520L;
    private static final long NO_ANIMATION_SAME_TARGET_BIAS_MAX_MS = 1320L;
    private static final long NO_ANIMATION_SAME_TARGET_BIAS_STREAK_STEP_MS = 420L;
    private static final long NO_ANIMATION_SAME_TARGET_BIAS_CAP_MS = 2800L;
    private static final long NO_ANIMATION_RETRY_BACKOFF_MIN_MS = 1700L;
    private static final long NO_ANIMATION_RETRY_BACKOFF_STREAK_STEP_MS = 900L;
    private static final long NO_ANIMATION_RETRY_BACKOFF_CAP_MS = 5200L;
    private static final int NO_ANIMATION_RETRY_BACKOFF_FATIGUE_EXTRA_MAX_MS = 220;

    private FishingTimingModel() {
    }

    public static boolean isAnimationActive(int animation) {
        return animation != -1 && animation != 0;
    }

    public static long variedSameTargetReclickCooldownMs(long baseCooldownMs, long dispatchAtMs, int targetNpcIndex) {
        long base = Math.max(0L, baseCooldownMs);
        if (base <= 0L) {
            return 0L;
        }
        long seed = dispatchAtMs ^ ((long) targetNpcIndex * 73856093L);
        double normalized = normalizedHashUnit(seed);
        double scaleRange = RECLICK_JITTER_MAX_SCALE - RECLICK_JITTER_MIN_SCALE;
        double scale = RECLICK_JITTER_MIN_SCALE + (scaleRange * normalized);
        return Math.max(120L, Math.round((double) base * scale));
    }

    public static long variedGlobalReclickCooldownMs(long baseCooldownMs, long dispatchAtMs, int targetNpcIndex) {
        long base = Math.max(0L, baseCooldownMs);
        if (base <= 0L) {
            return GLOBAL_RECLICK_COOLDOWN_MIN_MS;
        }
        long seed = (dispatchAtMs ^ 0xA5A5A5A55A5A5A5AL) + ((long) targetNpcIndex * 19349663L);
        double normalized = normalizedHashUnit(seed);
        double scaleRange = GLOBAL_RECLICK_JITTER_MAX_SCALE - GLOBAL_RECLICK_JITTER_MIN_SCALE;
        double scale = GLOBAL_RECLICK_JITTER_MIN_SCALE + (scaleRange * normalized);
        long scaled = Math.max(120L, Math.round((double) base * scale));
        return Math.max(GLOBAL_RECLICK_COOLDOWN_MIN_MS, scaled);
    }

    public static long variedNoAnimationSameTargetBiasMs(long dispatchAtMs, int targetNpcIndex, int retryStreak) {
        int streak = Math.max(1, retryStreak);
        long streakExtra = (long) Math.max(0, streak - 1) * NO_ANIMATION_SAME_TARGET_BIAS_STREAK_STEP_MS;
        long minMs = NO_ANIMATION_SAME_TARGET_BIAS_MIN_MS + streakExtra;
        long maxMs = NO_ANIMATION_SAME_TARGET_BIAS_MAX_MS + streakExtra;
        long seed = (dispatchAtMs ^ 0xB492B66FBE98F273L) + ((long) targetNpcIndex * 0x9E3779B97F4A7C15L) + streak;
        long varied = rangedHashDuration(seed, minMs, maxMs);
        return Math.min(NO_ANIMATION_SAME_TARGET_BIAS_CAP_MS, Math.max(260L, varied));
    }

    public static long variedNoAnimationRetryBackoffMs(
        long dispatchAtMs,
        int targetNpcIndex,
        int retryStreak,
        FatigueSnapshot fatigue
    ) {
        int streak = Math.max(1, retryStreak);
        long streakExtra = (long) Math.max(0, streak - 1) * NO_ANIMATION_RETRY_BACKOFF_STREAK_STEP_MS;
        long baseMs = NO_ANIMATION_RETRY_BACKOFF_MIN_MS + streakExtra;
        long seed = (dispatchAtMs ^ 0x8CB92BA72F3D8DD7L) + ((long) targetNpcIndex * 0x9E3779B97F4A7C15L) + streak;
        long varied = rangedHashDuration(seed, (long) (baseMs * 0.88), (long) (baseMs * 1.16));
        int fatigueExtra = fatigue == null
            ? 0
            : fatigue.fishingReclickCooldownBiasMs(NO_ANIMATION_RETRY_BACKOFF_FATIGUE_EXTRA_MAX_MS);
        long total = varied + Math.max(0L, fatigueExtra);
        return Math.min(NO_ANIMATION_RETRY_BACKOFF_CAP_MS, Math.max(1200L, total));
    }

    public static boolean worldPointsNear(WorldPoint a, WorldPoint b, int radiusTiles) {
        if (a == null || b == null) {
            return false;
        }
        if (a.getPlane() != b.getPlane()) {
            return false;
        }
        int distance = a.distanceTo(b);
        int radius = Math.max(0, radiusTiles);
        return distance >= 0 && distance <= radius;
    }

    public static int worldDistance(WorldPoint a, WorldPoint b) {
        if (a == null || b == null) {
            return -1;
        }
        if (a.getPlane() != b.getPlane()) {
            return -1;
        }
        int distance = a.distanceTo(b);
        return distance < 0 ? -1 : distance;
    }

    public static long variedPostDispatchLocalTravelGraceMs(
        long dispatchAtMs,
        int dispatchNpcIndex,
        int localDistanceFromDispatchTarget,
        FatigueSnapshot fatigue
    ) {
        if (dispatchAtMs <= 0L
            || localDistanceFromDispatchTarget < POST_DISPATCH_LOCAL_TRAVEL_MIN_DISTANCE_TILES) {
            return 0L;
        }
        int distanceSteps = Math.max(
            0,
            Math.min(6, localDistanceFromDispatchTarget - POST_DISPATCH_LOCAL_TRAVEL_MIN_DISTANCE_TILES)
        );
        long minMs = 900L + (distanceSteps * 220L);
        long maxMs = 1450L + (distanceSteps * 320L);
        long seed = (dispatchAtMs ^ 0xC13FA9A902A6328FL) + ((long) dispatchNpcIndex * 0x9E3779B97F4A7C15L);
        long varied = rangedHashDuration(seed, minMs, maxMs);
        int fatigueExtra = fatigue == null ? 0 : fatigue.fishingReclickCooldownBiasMs(140);
        return Math.max(760L, varied + Math.max(0, fatigueExtra));
    }

    public static long variedPostDispatchTravelGraceMs(
        long dispatchAtMs,
        int targetNpcIndex,
        int localDistanceToTarget,
        FatigueSnapshot fatigue
    ) {
        if (dispatchAtMs <= 0L || localDistanceToTarget < WALK_APPROACH_DISTANCE_TILES) {
            return 0L;
        }
        int distanceSteps = Math.max(0, Math.min(8, localDistanceToTarget - (WALK_APPROACH_DISTANCE_TILES - 1)));
        long minMs = 620L + (distanceSteps * 110L);
        long maxMs = 980L + (distanceSteps * 170L);
        long seed = dispatchAtMs ^ ((long) targetNpcIndex * 0x9E3779B97F4A7C15L);
        long varied = rangedHashDuration(seed, minMs, maxMs);
        int fatigueExtra = fatigue == null ? 0 : fatigue.fishingReclickCooldownBiasMs(120);
        return Math.max(600L, varied + Math.max(0, fatigueExtra));
    }

    public static int fatigueReclickCooldownBiasMs(FatigueSnapshot fatigue) {
        return fatigue == null ? 0 : fatigue.fishingReclickCooldownBiasMs(FATIGUE_RECLICK_COOLDOWN_BIAS_MAX_MS);
    }

    private static long rangedHashDuration(long seed, long minMs, long maxMs) {
        long lo = Math.min(minMs, maxMs);
        long hi = Math.max(minMs, maxMs);
        if (hi <= lo) {
            return Math.max(0L, lo);
        }
        double unit = normalizedHashUnit(seed);
        return lo + Math.round((double) (hi - lo) * unit);
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
