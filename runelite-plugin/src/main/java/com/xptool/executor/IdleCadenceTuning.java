package com.xptool.executor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xptool.sessions.idle.IdleBehaviorProfile;

final class IdleCadenceTuning {
    private static final String IDLE_CADENCE_TUNING_PAYLOAD_KEY = "idleCadenceTuning";
    private static final IdleCadenceTuning NONE = new IdleCadenceTuning(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );

    private final Integer fishingIdleMinIntervalTicks;
    private final Integer fishingIdleMaxIntervalTicks;
    private final Integer fishingIdleRetryMinIntervalTicks;
    private final Integer fishingIdleRetryMaxIntervalTicks;
    private final Integer fishingDbParityIdleMinIntervalTicks;
    private final Integer fishingDbParityIdleMaxIntervalTicks;
    private final Integer fishingDbParityIdleRetryMinIntervalTicks;
    private final Integer fishingDbParityIdleRetryMaxIntervalTicks;
    private final Integer postDropIdleCooldownMinTicks;
    private final Integer postDropIdleCooldownMaxTicks;
    private final Integer postDropIdleDbParityCooldownMinTicks;
    private final Integer postDropIdleDbParityCooldownMaxTicks;
    private final Integer offscreenWindowMarginMinPx;
    private final Integer offscreenWindowMarginMaxPx;
    private final Integer offscreenNearTargetMaxGapPx;
    private final Integer offscreenFarTargetMinGapPx;
    private final Integer offscreenFarTargetMaxGapPx;
    private final Integer profileHoverChancePercent;
    private final Integer profileDriftChancePercent;
    private final Integer profileCameraChancePercent;
    private final Integer profileNoopChancePercent;
    private final Integer profileParkAfterBurstMinActions;
    private final Integer profileParkAfterBurstChancePercent;

    private IdleCadenceTuning(
        Integer fishingIdleMinIntervalTicks,
        Integer fishingIdleMaxIntervalTicks,
        Integer fishingIdleRetryMinIntervalTicks,
        Integer fishingIdleRetryMaxIntervalTicks,
        Integer fishingDbParityIdleMinIntervalTicks,
        Integer fishingDbParityIdleMaxIntervalTicks,
        Integer fishingDbParityIdleRetryMinIntervalTicks,
        Integer fishingDbParityIdleRetryMaxIntervalTicks,
        Integer postDropIdleCooldownMinTicks,
        Integer postDropIdleCooldownMaxTicks,
        Integer postDropIdleDbParityCooldownMinTicks,
        Integer postDropIdleDbParityCooldownMaxTicks,
        Integer offscreenWindowMarginMinPx,
        Integer offscreenWindowMarginMaxPx,
        Integer offscreenNearTargetMaxGapPx,
        Integer offscreenFarTargetMinGapPx,
        Integer offscreenFarTargetMaxGapPx,
        Integer profileHoverChancePercent,
        Integer profileDriftChancePercent,
        Integer profileCameraChancePercent,
        Integer profileNoopChancePercent,
        Integer profileParkAfterBurstMinActions,
        Integer profileParkAfterBurstChancePercent
    ) {
        this.fishingIdleMinIntervalTicks = fishingIdleMinIntervalTicks;
        this.fishingIdleMaxIntervalTicks = fishingIdleMaxIntervalTicks;
        this.fishingIdleRetryMinIntervalTicks = fishingIdleRetryMinIntervalTicks;
        this.fishingIdleRetryMaxIntervalTicks = fishingIdleRetryMaxIntervalTicks;
        this.fishingDbParityIdleMinIntervalTicks = fishingDbParityIdleMinIntervalTicks;
        this.fishingDbParityIdleMaxIntervalTicks = fishingDbParityIdleMaxIntervalTicks;
        this.fishingDbParityIdleRetryMinIntervalTicks = fishingDbParityIdleRetryMinIntervalTicks;
        this.fishingDbParityIdleRetryMaxIntervalTicks = fishingDbParityIdleRetryMaxIntervalTicks;
        this.postDropIdleCooldownMinTicks = postDropIdleCooldownMinTicks;
        this.postDropIdleCooldownMaxTicks = postDropIdleCooldownMaxTicks;
        this.postDropIdleDbParityCooldownMinTicks = postDropIdleDbParityCooldownMinTicks;
        this.postDropIdleDbParityCooldownMaxTicks = postDropIdleDbParityCooldownMaxTicks;
        this.offscreenWindowMarginMinPx = offscreenWindowMarginMinPx;
        this.offscreenWindowMarginMaxPx = offscreenWindowMarginMaxPx;
        this.offscreenNearTargetMaxGapPx = offscreenNearTargetMaxGapPx;
        this.offscreenFarTargetMinGapPx = offscreenFarTargetMinGapPx;
        this.offscreenFarTargetMaxGapPx = offscreenFarTargetMaxGapPx;
        this.profileHoverChancePercent = profileHoverChancePercent;
        this.profileDriftChancePercent = profileDriftChancePercent;
        this.profileCameraChancePercent = profileCameraChancePercent;
        this.profileNoopChancePercent = profileNoopChancePercent;
        this.profileParkAfterBurstMinActions = profileParkAfterBurstMinActions;
        this.profileParkAfterBurstChancePercent = profileParkAfterBurstChancePercent;
    }

    static IdleCadenceTuning none() {
        return NONE;
    }

    boolean hasOverrides() {
        return fishingIdleMinIntervalTicks != null
            || fishingIdleMaxIntervalTicks != null
            || fishingIdleRetryMinIntervalTicks != null
            || fishingIdleRetryMaxIntervalTicks != null
            || fishingDbParityIdleMinIntervalTicks != null
            || fishingDbParityIdleMaxIntervalTicks != null
            || fishingDbParityIdleRetryMinIntervalTicks != null
            || fishingDbParityIdleRetryMaxIntervalTicks != null
            || postDropIdleCooldownMinTicks != null
            || postDropIdleCooldownMaxTicks != null
            || postDropIdleDbParityCooldownMinTicks != null
            || postDropIdleDbParityCooldownMaxTicks != null
            || offscreenWindowMarginMinPx != null
            || offscreenWindowMarginMaxPx != null
            || offscreenNearTargetMaxGapPx != null
            || offscreenFarTargetMinGapPx != null
            || offscreenFarTargetMaxGapPx != null
            || profileHoverChancePercent != null
            || profileDriftChancePercent != null
            || profileCameraChancePercent != null
            || profileNoopChancePercent != null
            || profileParkAfterBurstMinActions != null
            || profileParkAfterBurstChancePercent != null;
    }

    boolean hasFishingDbParityIdleCadenceWindow() {
        return fishingDbParityIdleMinIntervalTicks != null
            && fishingDbParityIdleMaxIntervalTicks != null;
    }

    boolean hasPostDropIdleDbParityCooldownWindow() {
        return postDropIdleDbParityCooldownMinTicks != null
            && postDropIdleDbParityCooldownMaxTicks != null;
    }

    static IdleCadenceTuning fromPayload(JsonObject payload) {
        if (payload == null) {
            return none();
        }
        JsonObject tuning = asJsonObject(payload.get(IDLE_CADENCE_TUNING_PAYLOAD_KEY));
        if (tuning == null) {
            return none();
        }
        Integer fishingIdleMinIntervalTicks = asIntBounded(tuning.get("fishingIdleMinIntervalTicks"), 1, 60);
        Integer fishingIdleMaxIntervalTicks = asIntBounded(tuning.get("fishingIdleMaxIntervalTicks"), 1, 90);
        Integer fishingIdleRetryMinIntervalTicks = asIntBounded(tuning.get("fishingIdleRetryMinIntervalTicks"), 1, 60);
        Integer fishingIdleRetryMaxIntervalTicks = asIntBounded(tuning.get("fishingIdleRetryMaxIntervalTicks"), 1, 90);
        Integer fishingDbParityIdleMinIntervalTicks =
            asIntBounded(tuning.get("fishingDbParityIdleMinIntervalTicks"), 1, 60);
        Integer fishingDbParityIdleMaxIntervalTicks =
            asIntBounded(tuning.get("fishingDbParityIdleMaxIntervalTicks"), 1, 90);
        Integer fishingDbParityIdleRetryMinIntervalTicks =
            asIntBounded(tuning.get("fishingDbParityIdleRetryMinIntervalTicks"), 1, 60);
        Integer fishingDbParityIdleRetryMaxIntervalTicks =
            asIntBounded(tuning.get("fishingDbParityIdleRetryMaxIntervalTicks"), 1, 90);
        Integer postDropIdleCooldownMinTicks = asIntBounded(tuning.get("postDropIdleCooldownMinTicks"), 1, 80);
        Integer postDropIdleCooldownMaxTicks = asIntBounded(tuning.get("postDropIdleCooldownMaxTicks"), 1, 120);
        Integer postDropIdleDbParityCooldownMinTicks =
            asIntBounded(tuning.get("postDropIdleDbParityCooldownMinTicks"), 1, 80);
        Integer postDropIdleDbParityCooldownMaxTicks =
            asIntBounded(tuning.get("postDropIdleDbParityCooldownMaxTicks"), 1, 120);
        Integer offscreenWindowMarginMinPx =
            asIntBounded(tuning.get("offscreenWindowMarginMinPx"), 1, 600);
        Integer offscreenWindowMarginMaxPx =
            asIntBounded(tuning.get("offscreenWindowMarginMaxPx"), 1, 1000);
        Integer offscreenNearTargetMaxGapPx =
            asIntBounded(tuning.get("offscreenNearTargetMaxGapPx"), 1, 3000);
        Integer offscreenFarTargetMinGapPx =
            asIntBounded(tuning.get("offscreenFarTargetMinGapPx"), 1, 3000);
        Integer offscreenFarTargetMaxGapPx =
            asIntBounded(tuning.get("offscreenFarTargetMaxGapPx"), 1, 5000);
        Integer profileHoverChancePercent = asIntBounded(tuning.get("profileHoverChancePercent"), 0, 100);
        Integer profileDriftChancePercent = asIntBounded(tuning.get("profileDriftChancePercent"), 0, 100);
        Integer profileCameraChancePercent = asIntBounded(tuning.get("profileCameraChancePercent"), 0, 100);
        Integer profileNoopChancePercent = asIntBounded(tuning.get("profileNoopChancePercent"), 0, 100);
        Integer profileParkAfterBurstMinActions =
            asIntBounded(tuning.get("profileParkAfterBurstMinActions"), 0, 24);
        Integer profileParkAfterBurstChancePercent =
            asIntBounded(tuning.get("profileParkAfterBurstChancePercent"), 0, 100);
        return new IdleCadenceTuning(
            fishingIdleMinIntervalTicks,
            fishingIdleMaxIntervalTicks,
            fishingIdleRetryMinIntervalTicks,
            fishingIdleRetryMaxIntervalTicks,
            fishingDbParityIdleMinIntervalTicks,
            fishingDbParityIdleMaxIntervalTicks,
            fishingDbParityIdleRetryMinIntervalTicks,
            fishingDbParityIdleRetryMaxIntervalTicks,
            postDropIdleCooldownMinTicks,
            postDropIdleCooldownMaxTicks,
            postDropIdleDbParityCooldownMinTicks,
            postDropIdleDbParityCooldownMaxTicks,
            offscreenWindowMarginMinPx,
            offscreenWindowMarginMaxPx,
            offscreenNearTargetMaxGapPx,
            offscreenFarTargetMinGapPx,
            offscreenFarTargetMaxGapPx,
            profileHoverChancePercent,
            profileDriftChancePercent,
            profileCameraChancePercent,
            profileNoopChancePercent,
            profileParkAfterBurstMinActions,
            profileParkAfterBurstChancePercent
        );
    }

    int resolveFishingIdleMinIntervalTicks(int fallback) {
        return fishingIdleMinIntervalTicks == null ? fallback : clampInt(fishingIdleMinIntervalTicks.intValue(), 1, 60);
    }

    int resolveFishingIdleMaxIntervalTicks(int fallback, int resolvedMin) {
        int base = fishingIdleMaxIntervalTicks == null ? fallback : fishingIdleMaxIntervalTicks.intValue();
        return Math.max(resolvedMin, clampInt(base, 1, 90));
    }

    int resolveFishingIdleRetryMinIntervalTicks(int fallback) {
        return fishingIdleRetryMinIntervalTicks == null
            ? fallback
            : clampInt(fishingIdleRetryMinIntervalTicks.intValue(), 1, 60);
    }

    int resolveFishingIdleRetryMaxIntervalTicks(int fallback, int resolvedMin) {
        int base = fishingIdleRetryMaxIntervalTicks == null ? fallback : fishingIdleRetryMaxIntervalTicks.intValue();
        return Math.max(resolvedMin, clampInt(base, 1, 90));
    }

    int resolveFishingDbParityIdleMinIntervalTicks(int fallback) {
        return fishingDbParityIdleMinIntervalTicks == null
            ? fallback
            : clampInt(fishingDbParityIdleMinIntervalTicks.intValue(), 1, 60);
    }

    int resolveFishingDbParityIdleMaxIntervalTicks(int fallback, int resolvedMin) {
        int base = fishingDbParityIdleMaxIntervalTicks == null ? fallback : fishingDbParityIdleMaxIntervalTicks.intValue();
        return Math.max(resolvedMin, clampInt(base, 1, 90));
    }

    int resolveFishingDbParityIdleRetryMinIntervalTicks(int fallback) {
        return fishingDbParityIdleRetryMinIntervalTicks == null
            ? fallback
            : clampInt(fishingDbParityIdleRetryMinIntervalTicks.intValue(), 1, 60);
    }

    int resolveFishingDbParityIdleRetryMaxIntervalTicks(int fallback, int resolvedMin) {
        int base = fishingDbParityIdleRetryMaxIntervalTicks == null
            ? fallback
            : fishingDbParityIdleRetryMaxIntervalTicks.intValue();
        return Math.max(resolvedMin, clampInt(base, 1, 90));
    }

    int resolvePostDropIdleCooldownMinTicks(int fallback, boolean dbParityCadence) {
        Integer override = dbParityCadence ? postDropIdleDbParityCooldownMinTicks : postDropIdleCooldownMinTicks;
        if (override == null) {
            return fallback;
        }
        return clampInt(override.intValue(), 1, 80);
    }

    int resolvePostDropIdleCooldownMaxTicks(int fallback, int resolvedMin, boolean dbParityCadence) {
        Integer override = dbParityCadence ? postDropIdleDbParityCooldownMaxTicks : postDropIdleCooldownMaxTicks;
        int candidate = override == null ? fallback : clampInt(override.intValue(), 1, 120);
        return Math.max(resolvedMin, candidate);
    }

    int resolveOffscreenWindowMarginMinPx(int fallback) {
        if (offscreenWindowMarginMinPx == null) {
            return Math.max(1, fallback);
        }
        return clampInt(offscreenWindowMarginMinPx.intValue(), 1, 600);
    }

    int resolveOffscreenWindowMarginMaxPx(int fallback, int resolvedMin) {
        int candidate = offscreenWindowMarginMaxPx == null
            ? fallback
            : clampInt(offscreenWindowMarginMaxPx.intValue(), 1, 1000);
        return Math.max(resolvedMin, candidate);
    }

    int resolveOffscreenFarTargetMinGapPx(int fallback, int resolvedMarginMin) {
        int marginBasedFloor = Math.max(1, resolvedMarginMin + 8);
        int candidate = offscreenFarTargetMinGapPx == null
            ? fallback
            : clampInt(offscreenFarTargetMinGapPx.intValue(), 1, 3000);
        return Math.max(marginBasedFloor, candidate);
    }

    int resolveOffscreenNearTargetMaxGapPx(int fallback, int resolvedMinGap) {
        int candidate = offscreenNearTargetMaxGapPx == null
            ? fallback
            : clampInt(offscreenNearTargetMaxGapPx.intValue(), 1, 3000);
        return Math.max(resolvedMinGap, candidate);
    }

    int resolveOffscreenFarTargetMaxGapPx(int fallback, int resolvedNearMaxGap) {
        int candidate = offscreenFarTargetMaxGapPx == null
            ? fallback
            : clampInt(offscreenFarTargetMaxGapPx.intValue(), 1, 5000);
        return Math.max(resolvedNearMaxGap, candidate);
    }

    IdleBehaviorProfile applyFishingProfile(IdleBehaviorProfile base) {
        if (base == null) {
            return null;
        }
        int hover = profileHoverChancePercent == null ? base.hoverChancePercent() : profileHoverChancePercent.intValue();
        int drift = profileDriftChancePercent == null ? base.driftChancePercent() : profileDriftChancePercent.intValue();
        int offscreen = base.offscreenParkChancePercent();
        int camera = profileCameraChancePercent == null ? base.cameraChancePercent() : profileCameraChancePercent.intValue();
        int noop = profileNoopChancePercent == null ? base.noopChancePercent() : profileNoopChancePercent.intValue();
        int parkMin = profileParkAfterBurstMinActions == null
            ? base.parkAfterBurstMinActions()
            : profileParkAfterBurstMinActions.intValue();
        int parkChance = profileParkAfterBurstChancePercent == null
            ? base.parkAfterBurstChancePercent()
            : profileParkAfterBurstChancePercent.intValue();
        return new IdleBehaviorProfile(hover, drift, offscreen, camera, noop, parkMin, parkChance);
    }

    private static JsonObject asJsonObject(JsonElement element) {
        if (element == null || element.isJsonNull() || !element.isJsonObject()) {
            return null;
        }
        try {
            return element.getAsJsonObject();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Integer asIntBounded(JsonElement element, int min, int max) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        try {
            int value = element.getAsInt();
            return clampInt(value, min, max);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int clampInt(int value, int min, int max) {
        int lo = Math.min(min, max);
        int hi = Math.max(min, max);
        return Math.max(lo, Math.min(hi, value));
    }
}
