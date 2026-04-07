package com.xptool.executor;

import java.util.Set;

final class CommandSupportPolicy {
    private static final Set<String> SUPPORTED_COMMAND_TYPES = Set.of(
        "OPEN_BANK",
        "BANK_OPEN_SAFE",
        "ENTER_BANK_PIN",
        "SEARCH_BANK_ITEM",
        "DEPOSIT_ITEM",
        "DEPOSIT_ALL_EXCEPT",
        "BANK_DEPOSIT_ALL_EXCEPT_TOOL_SAFE",
        "WITHDRAW_ITEM",
        "BANK_WITHDRAW_LOGS_SAFE",
        "WOODCUT_CHOP_NEAREST_TREE_SAFE",
        "MINE_NEAREST_ROCK_SAFE",
        "FISH_NEAREST_SPOT_SAFE",
        "WALK_TO_WORLDPOINT_SAFE",
        "CAMERA_NUDGE_SAFE",
        "SET_FISHING_IDLE_MODE_SAFE",
        "COMBAT_ATTACK_NEAREST_NPC_SAFE",
        "NPC_CONTEXT_MENU_TEST",
        "SCENE_OBJECT_ACTION_SAFE",
        "AGILITY_OBSTACLE_ACTION_SAFE",
        "GROUND_ITEM_ACTION_SAFE",
        "SHOP_BUY_ITEM_SAFE",
        "WORLD_HOP_SAFE",
        "DROP_START_SESSION",
        "DROP_STOP_SESSION",
        "DROP_ITEM_SAFE",
        "WOODCUT_START_DROP_SESSION",
        "WOODCUT_STOP_DROP_SESSION",
        "WOODCUT_DROP_ITEM_SAFE",
        "EAT_FOOD_SAFE",
        "CLOSE_BANK",
        "LOGOUT_SAFE",
        "STOP_ALL_RUNTIME",
        "LOGIN_START_TEST"
    );

    private CommandSupportPolicy() {
    }

    static boolean isSupported(String commandType) {
        return SUPPORTED_COMMAND_TYPES.contains(commandType);
    }
}

