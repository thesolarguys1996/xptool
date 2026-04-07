package com.xptool.executor;

import com.xptool.sessions.idle.FishingIdleMode;
import com.xptool.sessions.idle.IdleBehaviorProfile;
import com.xptool.sessions.idle.IdleSkillContext;
import com.xptool.sessions.idle.IdleSkillPolicies;
import com.xptool.sessions.idle.IdleSkillPolicy;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class ActivityIdlePolicyRegistry {
    static final String ACTIVITY_GLOBAL = "global";
    static final String ACTIVITY_FISHING = "fishing";
    static final String ACTIVITY_WOODCUTTING = "woodcutting";
    static final String ACTIVITY_MINING = "mining";
    static final String ACTIVITY_COMBAT = "combat";
    static final String ACTIVITY_AGILITY = "agility";
    static final String ACTIVITY_STORE_BANK = "store_bank";
    private static final String PROFILE_DB_PARITY = "DB_PARITY";
    private static final ActivityIdleCadenceWindow GLOBAL_CADENCE_WINDOW =
        ActivityIdleCadenceWindow.of(10, 30, 5, 11);
    private static final ActivityIdleCadenceWindow FISHING_DB_PARITY_CADENCE_WINDOW =
        ActivityIdleCadenceWindow.of(5, 14, 3, 8);
    private static final ActivityIdleCadenceWindow WOODCUT_DB_PARITY_CADENCE_WINDOW =
        ActivityIdleCadenceWindow.of(10, 26, 8, 18);
    private static final ActivityIdleCadenceWindow FISHING_OFFSCREEN_CADENCE_WINDOW =
        ActivityIdleCadenceWindow.of(7, 18, 5, 12);
    private static final IdleBehaviorProfile FALLBACK_BEHAVIOR_PROFILE =
        new IdleBehaviorProfile(45, 35, 0, 10, 10, 2, 42);
    private static final IdleBehaviorProfile FISHING_DB_PARITY_BEHAVIOR_PROFILE =
        new IdleBehaviorProfile(26, 22, 0, 10, 42, 2, 26);
    private static final IdleBehaviorProfile WOODCUT_DB_PARITY_BEHAVIOR_PROFILE =
        new IdleBehaviorProfile(10, 4, 0, 6, 80, 4, 8);
    private static final IdleBehaviorProfile FISHING_OFFSCREEN_BEHAVIOR_PROFILE =
        new IdleBehaviorProfile(2, 2, 84, 4, 8, 3, 20);

    private final ActivityIdlePolicy fallbackPolicy;
    private final Map<String, ActivityIdlePolicy> policiesByActivity;

    private ActivityIdlePolicyRegistry(
        ActivityIdlePolicy fallbackPolicy,
        Map<String, ActivityIdlePolicy> policiesByActivity
    ) {
        this.fallbackPolicy = fallbackPolicy == null
            ? ActivityIdlePolicy.of("DB_PARITY", FishingIdleMode.STANDARD)
            : fallbackPolicy;
        this.policiesByActivity = Map.copyOf(policiesByActivity == null ? Map.of() : policiesByActivity);
    }

    static ActivityIdlePolicyRegistry defaults() {
        Map<IdleSkillContext, IdleSkillPolicy> skillPolicies = IdleSkillPolicies.defaults();
        IdleBehaviorProfile globalProfile = profileFor(skillPolicies, IdleSkillContext.GLOBAL, FALLBACK_BEHAVIOR_PROFILE);
        IdleBehaviorProfile miningProfile = profileFor(skillPolicies, IdleSkillContext.MINING, globalProfile);
        IdleBehaviorProfile combatProfile = profileFor(skillPolicies, IdleSkillContext.COMBAT, globalProfile);

        ActivityIdlePolicy dbParityStandard = ActivityIdlePolicy.of(
            PROFILE_DB_PARITY,
            FishingIdleMode.STANDARD,
            globalProfile,
            GLOBAL_CADENCE_WINDOW
        );
        ActivityIdlePolicy fishingPolicy = ActivityIdlePolicy.fishing(
            PROFILE_DB_PARITY,
            FishingIdleMode.STANDARD,
            FISHING_DB_PARITY_BEHAVIOR_PROFILE,
            FISHING_DB_PARITY_CADENCE_WINDOW,
            FISHING_OFFSCREEN_BEHAVIOR_PROFILE,
            FISHING_OFFSCREEN_CADENCE_WINDOW
        );
        ActivityIdlePolicy woodcuttingBaselinePolicy = ActivityIdlePolicy.of(
            PROFILE_DB_PARITY,
            FishingIdleMode.OFFSCREEN_BIASED,
            WOODCUT_DB_PARITY_BEHAVIOR_PROFILE,
            WOODCUT_DB_PARITY_CADENCE_WINDOW
        );
        Map<String, ActivityIdlePolicy> defaults = new HashMap<>();
        defaults.put(ACTIVITY_GLOBAL, dbParityStandard);
        defaults.put(ACTIVITY_FISHING, fishingPolicy);
        defaults.put(ACTIVITY_WOODCUTTING, woodcuttingBaselinePolicy);
        defaults.put(
            ACTIVITY_MINING,
            ActivityIdlePolicy.of(PROFILE_DB_PARITY, FishingIdleMode.STANDARD, miningProfile, GLOBAL_CADENCE_WINDOW)
        );
        defaults.put(
            ACTIVITY_COMBAT,
            ActivityIdlePolicy.of(PROFILE_DB_PARITY, FishingIdleMode.STANDARD, combatProfile, GLOBAL_CADENCE_WINDOW)
        );
        defaults.put(ACTIVITY_AGILITY, dbParityStandard);
        defaults.put(ACTIVITY_STORE_BANK, dbParityStandard);
        return new ActivityIdlePolicyRegistry(dbParityStandard, defaults);
    }

    static ActivityIdlePolicyRegistry of(
        ActivityIdlePolicy fallbackPolicy,
        Map<String, ActivityIdlePolicy> policiesByActivity
    ) {
        return new ActivityIdlePolicyRegistry(fallbackPolicy, policiesByActivity);
    }

    ActivityIdlePolicy resolveForActivity(String activityKey) {
        String normalized = normalizeActivityKey(activityKey);
        ActivityIdlePolicy policy = policiesByActivity.get(normalized);
        return policy == null ? fallbackPolicy : policy;
    }

    ActivityIdlePolicy resolveForContext(IdleSkillContext context) {
        return resolveForActivity(activityKeyForContext(context));
    }

    static String activityKeyForContext(IdleSkillContext context) {
        if (context == null) {
            return ACTIVITY_GLOBAL;
        }
        switch (context) {
            case FISHING:
                return ACTIVITY_FISHING;
            case WOODCUTTING:
                return ACTIVITY_WOODCUTTING;
            case MINING:
                return ACTIVITY_MINING;
            case COMBAT:
                return ACTIVITY_COMBAT;
            case GLOBAL:
            default:
                return ACTIVITY_GLOBAL;
        }
    }

    static String normalizeActivityKey(String raw) {
        if (raw == null) {
            return "";
        }
        return raw
            .trim()
            .toLowerCase(Locale.ROOT)
            .replace('-', '_')
            .replace(' ', '_');
    }

    private static IdleBehaviorProfile profileFor(
        Map<IdleSkillContext, IdleSkillPolicy> policies,
        IdleSkillContext context,
        IdleBehaviorProfile fallback
    ) {
        if (policies == null || context == null) {
            return fallback;
        }
        IdleSkillPolicy policy = policies.get(context);
        if (policy == null || policy.profile() == null) {
            return fallback;
        }
        return policy.profile();
    }
}
