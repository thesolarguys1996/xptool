package com.xptool.executor;

import com.xptool.sessions.idle.FishingIdleMode;
import com.xptool.sessions.idle.IdleSkillContext;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

final class CommandQueueIdleArmingService {
    private final IdleArmingService idleArmingService;
    private final Function<IdleSkillContext, FishingIdleMode> fishingIdleModeResolver;
    private final boolean idleActivityGateEnabled;
    private final Set<String> idleActivityAllowlist;
    private final String idleArmSource;

    CommandQueueIdleArmingService(
        IdleArmingService idleArmingService,
        Function<IdleSkillContext, FishingIdleMode> fishingIdleModeResolver,
        boolean idleActivityGateEnabled,
        Set<String> idleActivityAllowlist,
        String idleArmSource
    ) {
        this.idleArmingService = idleArmingService;
        this.fishingIdleModeResolver = fishingIdleModeResolver;
        this.idleActivityGateEnabled = idleActivityGateEnabled;
        this.idleActivityAllowlist = idleActivityAllowlist;
        this.idleArmSource = idleArmSource;
    }

    void onQueuedCommand(CommandRow row) {
        if (row == null) {
            return;
        }
        String plannerTag = CommandRowPlannerTagPolicy.resolvePlannerTag(row);
        if (shouldSuppressIdleRuntimeForCommandRow(row, plannerTag)) {
            if (plannerTag.isEmpty()) {
                idleArmingService.disarmAll();
            } else {
                idleArmingService.disarmActivity(IdleArmingService.activityKeyFromPlannerTag(plannerTag));
            }
            return;
        }
        if (!shouldArmIdleRuntimeForCommandRow(row, plannerTag)) {
            return;
        }
        String activityKey = plannerTag.isEmpty()
            ? ActivityIdlePolicyRegistry.ACTIVITY_GLOBAL
            : IdleArmingService.activityKeyFromPlannerTag(plannerTag);
        IdleSkillContext context = plannerTag.isEmpty()
            ? IdleSkillContext.GLOBAL
            : IdleArmingService.idleContextFromPlannerTag(plannerTag);
        FishingIdleMode mode = fishingIdleModeResolver.apply(context);
        idleArmingService.armActivity(activityKey, mode, idleArmSource);
    }

    private boolean shouldSuppressIdleRuntimeForCommandRow(CommandRow row, String plannerTag) {
        if (row == null) {
            return false;
        }
        String normalizedType = safeString(row.commandType).trim().toUpperCase(Locale.ROOT);
        if ("WORLD_HOP_SAFE".equals(normalizedType)) {
            return true;
        }
        if (!idleActivityGateEnabled) {
            return false;
        }
        if (plannerTag.isEmpty()) {
            return false;
        }
        return !idleActivityAllowlist.contains(plannerTag);
    }

    private boolean shouldArmIdleRuntimeForCommandRow(CommandRow row, String plannerTag) {
        if (row == null) {
            return false;
        }
        if (!shouldArmIdleRuntimeForCommandType(row.commandType)) {
            return false;
        }
        if (!idleActivityGateEnabled) {
            return true;
        }
        if (plannerTag.isEmpty()) {
            return false;
        }
        return idleActivityAllowlist.contains(plannerTag);
    }

    private static boolean shouldArmIdleRuntimeForCommandType(String commandType) {
        String normalized = safeString(commandType).trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return false;
        }
        if ("STOP_ALL_RUNTIME".equals(normalized)) {
            return false;
        }
        if ("LOGIN_START_TEST".equals(normalized)) {
            return false;
        }
        if ("LOGOUT_SAFE".equals(normalized)) {
            return false;
        }
        if ("NPC_CONTEXT_MENU_TEST".equals(normalized)) {
            return false;
        }
        if ("SCENE_OBJECT_ACTION_SAFE".equals(normalized)) {
            return false;
        }
        if ("AGILITY_OBSTACLE_ACTION_SAFE".equals(normalized)) {
            return false;
        }
        if ("GROUND_ITEM_ACTION_SAFE".equals(normalized)) {
            return false;
        }
        if ("CAMERA_NUDGE_SAFE".equals(normalized)) {
            return false;
        }
        return true;
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}
