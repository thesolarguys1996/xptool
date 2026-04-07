package com.xptool.executor;

import java.util.Locale;
import java.util.function.Predicate;

final class CommandRoutingPolicy {
    private CommandRoutingPolicy() {
    }

    static String resolveMotorOwnerForCommandType(
        String commandType,
        String sessionDropSweep,
        String motorOwnerBank,
        String motorOwnerInteraction,
        Predicate<String> dropSupports,
        Predicate<String> bankSupports,
        Predicate<String> interactionSupports
    ) {
        String type = normalizeCommandType(commandType);
        if (dropSupports.test(type)) {
            return sessionDropSweep;
        }
        if (bankSupports.test(type)) {
            return motorOwnerBank;
        }
        if (interactionSupports.test(type)) {
            return motorOwnerInteraction;
        }
        return "";
    }

    static String resolveClickTypeForCommandType(String commandType, String worldClickType, String nonWorldClickType) {
        String type = normalizeCommandType(commandType);
        if ("WOODCUT_CHOP_NEAREST_TREE_SAFE".equals(type)
            || "MINE_NEAREST_ROCK_SAFE".equals(type)
            || "FISH_NEAREST_SPOT_SAFE".equals(type)
            || "AGILITY_OBSTACLE_ACTION_SAFE".equals(type)
            || "SCENE_OBJECT_ACTION_SAFE".equals(type)
            || "GROUND_ITEM_ACTION_SAFE".equals(type)
            || "WALK_TO_WORLDPOINT_SAFE".equals(type)
            || "COMBAT_ATTACK_NEAREST_NPC_SAFE".equals(type)) {
            return worldClickType;
        }
        return nonWorldClickType;
    }

    private static String normalizeCommandType(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
