package com.xptool.executor;

import com.xptool.sessions.idle.FishingIdleMode;
import com.xptool.sessions.idle.IdleSkillContext;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class IdleArmingService {
    static final String SOURCE_NONE = "none";
    static final String SOURCE_UNSPECIFIED = "unspecified";

    private final Set<String> armedActivities = new HashSet<>();
    private final Set<String> offscreenArmedActivities = new HashSet<>();
    private final Map<String, String> armSourceByActivity = new HashMap<>();
    private final Map<String, String> offscreenArmSourceByActivity = new HashMap<>();

    boolean isArmedForContext(IdleSkillContext context, FishingIdleMode mode) {
        String activityKey = activityKeyFromContext(context);
        if (mode == FishingIdleMode.OFF) {
            return false;
        }
        if (mode == FishingIdleMode.OFFSCREEN_BIASED) {
            return offscreenArmedActivities.contains(activityKey);
        }
        return armedActivities.contains(activityKey);
    }

    boolean isArmedForActivity(String activityKey) {
        return armedActivities.contains(activityKeyFromPlannerTag(activityKey));
    }

    boolean isOffscreenArmedForActivity(String activityKey) {
        return offscreenArmedActivities.contains(activityKeyFromPlannerTag(activityKey));
    }

    int armedActivityCount() {
        return armedActivities.size();
    }

    int offscreenArmedActivityCount() {
        return offscreenArmedActivities.size();
    }

    boolean hasAnyArmedActivity() {
        return !armedActivities.isEmpty() || !offscreenArmedActivities.isEmpty();
    }

    void armActivity(String activityKey, FishingIdleMode mode) {
        armActivity(activityKey, mode, SOURCE_UNSPECIFIED);
    }

    void armActivity(String activityKey, FishingIdleMode mode, String source) {
        String normalized = activityKeyFromPlannerTag(activityKey);
        String normalizedSource = normalizeSource(source);
        if (mode == FishingIdleMode.OFF) {
            disarmActivity(normalized);
            return;
        }
        armedActivities.add(normalized);
        armSourceByActivity.put(normalized, normalizedSource);
        if (mode == FishingIdleMode.OFFSCREEN_BIASED) {
            offscreenArmedActivities.add(normalized);
            offscreenArmSourceByActivity.put(normalized, normalizedSource);
        } else {
            offscreenArmedActivities.remove(normalized);
            offscreenArmSourceByActivity.remove(normalized);
        }
    }

    void disarmActivity(String activityKey) {
        String normalized = activityKeyFromPlannerTag(activityKey);
        armedActivities.remove(normalized);
        offscreenArmedActivities.remove(normalized);
        armSourceByActivity.remove(normalized);
        offscreenArmSourceByActivity.remove(normalized);
    }

    void disarmAll() {
        armedActivities.clear();
        offscreenArmedActivities.clear();
        armSourceByActivity.clear();
        offscreenArmSourceByActivity.clear();
    }

    String armSourceForActivity(String activityKey) {
        String normalized = activityKeyFromPlannerTag(activityKey);
        return armSourceByActivity.getOrDefault(normalized, SOURCE_NONE);
    }

    String offscreenArmSourceForActivity(String activityKey) {
        String normalized = activityKeyFromPlannerTag(activityKey);
        return offscreenArmSourceByActivity.getOrDefault(normalized, SOURCE_NONE);
    }

    static String activityKeyFromPlannerTag(String plannerTag) {
        String normalized = ActivityIdlePolicyRegistry.normalizeActivityKey(plannerTag);
        if (normalized.isEmpty()) {
            return ActivityIdlePolicyRegistry.ACTIVITY_GLOBAL;
        }
        if (ActivityIdlePolicyRegistry.ACTIVITY_FISHING.equals(normalized)
            || ActivityIdlePolicyRegistry.ACTIVITY_WOODCUTTING.equals(normalized)
            || ActivityIdlePolicyRegistry.ACTIVITY_MINING.equals(normalized)
            || ActivityIdlePolicyRegistry.ACTIVITY_COMBAT.equals(normalized)
            || ActivityIdlePolicyRegistry.ACTIVITY_AGILITY.equals(normalized)
            || ActivityIdlePolicyRegistry.ACTIVITY_STORE_BANK.equals(normalized)
            || ActivityIdlePolicyRegistry.ACTIVITY_GLOBAL.equals(normalized)) {
            return normalized;
        }
        return ActivityIdlePolicyRegistry.ACTIVITY_GLOBAL;
    }

    static String activityKeyFromContext(IdleSkillContext context) {
        return ActivityIdlePolicyRegistry.activityKeyForContext(context);
    }

    static IdleSkillContext idleContextFromActivityKey(String activityKey) {
        String normalized = ActivityIdlePolicyRegistry.normalizeActivityKey(activityKey);
        if (ActivityIdlePolicyRegistry.ACTIVITY_FISHING.equals(normalized)) {
            return IdleSkillContext.FISHING;
        }
        if (ActivityIdlePolicyRegistry.ACTIVITY_WOODCUTTING.equals(normalized)) {
            return IdleSkillContext.WOODCUTTING;
        }
        if (ActivityIdlePolicyRegistry.ACTIVITY_MINING.equals(normalized)) {
            return IdleSkillContext.MINING;
        }
        if (ActivityIdlePolicyRegistry.ACTIVITY_COMBAT.equals(normalized)) {
            return IdleSkillContext.COMBAT;
        }
        return IdleSkillContext.GLOBAL;
    }

    static IdleSkillContext idleContextFromPlannerTag(String plannerTag) {
        return idleContextFromActivityKey(activityKeyFromPlannerTag(plannerTag));
    }

    private static String normalizeSource(String source) {
        String normalized = ActivityIdlePolicyRegistry.normalizeActivityKey(source);
        return normalized.isEmpty() ? SOURCE_UNSPECIFIED : normalized;
    }
}
