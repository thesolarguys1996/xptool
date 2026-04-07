package com.xptool.executor;

import com.xptool.sessions.idle.FishingIdleMode;
import com.xptool.sessions.idle.IdleBehaviorProfile;
import java.util.Locale;

final class ActivityIdlePolicy {
    private static final String DEFAULT_PROFILE_KEY = "DB_PARITY";

    private final String profileKey;
    private final FishingIdleMode fishingIdleMode;
    private final IdleBehaviorProfile behaviorProfile;
    private final ActivityIdleCadenceWindow cadenceWindow;
    private final IdleBehaviorProfile offscreenFishingBehaviorProfile;
    private final ActivityIdleCadenceWindow offscreenFishingCadenceWindow;

    private ActivityIdlePolicy(
        String profileKey,
        FishingIdleMode fishingIdleMode,
        IdleBehaviorProfile behaviorProfile,
        ActivityIdleCadenceWindow cadenceWindow,
        IdleBehaviorProfile offscreenFishingBehaviorProfile,
        ActivityIdleCadenceWindow offscreenFishingCadenceWindow
    ) {
        this.profileKey = normalizeProfileKey(profileKey);
        this.fishingIdleMode = fishingIdleMode == null ? FishingIdleMode.STANDARD : fishingIdleMode;
        this.behaviorProfile = behaviorProfile == null
            ? new IdleBehaviorProfile(45, 35, 0, 10, 10, 2, 42)
            : behaviorProfile;
        this.cadenceWindow = cadenceWindow == null
            ? ActivityIdleCadenceWindow.of(10, 30, 5, 11)
            : cadenceWindow;
        this.offscreenFishingBehaviorProfile = offscreenFishingBehaviorProfile;
        this.offscreenFishingCadenceWindow = offscreenFishingCadenceWindow;
    }

    static ActivityIdlePolicy of(String profileKey, FishingIdleMode fishingIdleMode) {
        return new ActivityIdlePolicy(profileKey, fishingIdleMode, null, null, null, null);
    }

    static ActivityIdlePolicy of(
        String profileKey,
        FishingIdleMode fishingIdleMode,
        IdleBehaviorProfile behaviorProfile,
        ActivityIdleCadenceWindow cadenceWindow
    ) {
        return new ActivityIdlePolicy(profileKey, fishingIdleMode, behaviorProfile, cadenceWindow, null, null);
    }

    static ActivityIdlePolicy fishing(
        String profileKey,
        FishingIdleMode fishingIdleMode,
        IdleBehaviorProfile behaviorProfile,
        ActivityIdleCadenceWindow cadenceWindow,
        IdleBehaviorProfile offscreenBehaviorProfile,
        ActivityIdleCadenceWindow offscreenCadenceWindow
    ) {
        return new ActivityIdlePolicy(
            profileKey,
            fishingIdleMode,
            behaviorProfile,
            cadenceWindow,
            offscreenBehaviorProfile,
            offscreenCadenceWindow
        );
    }

    String profileKey() {
        return profileKey;
    }

    FishingIdleMode fishingIdleMode() {
        return fishingIdleMode;
    }

    IdleBehaviorProfile behaviorProfile() {
        return behaviorProfile;
    }

    ActivityIdleCadenceWindow cadenceWindow() {
        return cadenceWindow;
    }

    IdleBehaviorProfile resolveBehaviorProfile(FishingIdleMode mode) {
        if (mode == FishingIdleMode.OFFSCREEN_BIASED && offscreenFishingBehaviorProfile != null) {
            return offscreenFishingBehaviorProfile;
        }
        return behaviorProfile;
    }

    ActivityIdleCadenceWindow resolveCadenceWindow(FishingIdleMode mode) {
        if (mode == FishingIdleMode.OFFSCREEN_BIASED && offscreenFishingCadenceWindow != null) {
            return offscreenFishingCadenceWindow;
        }
        return cadenceWindow;
    }

    private static String normalizeProfileKey(String raw) {
        if (raw == null) {
            return DEFAULT_PROFILE_KEY;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? DEFAULT_PROFILE_KEY : normalized;
    }
}
