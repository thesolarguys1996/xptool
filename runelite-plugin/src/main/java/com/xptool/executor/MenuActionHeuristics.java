package com.xptool.executor;

import java.util.Locale;
import net.runelite.api.MenuAction;

final class MenuActionHeuristics {
    private MenuActionHeuristics() {
    }

    static boolean isChopOption(String optionRaw) {
        String option = normalizeMenuToken(optionRaw);
        return option.contains("chop");
    }

    static boolean isMineOption(String optionRaw) {
        String option = normalizeMenuToken(optionRaw);
        return option.equals("mine") || option.startsWith("mine ");
    }

    static boolean isBankOption(String optionRaw) {
        String option = normalizeMenuToken(optionRaw);
        return option.contains("bank");
    }

    static boolean isAttackOption(String optionRaw) {
        String option = normalizeMenuToken(optionRaw);
        return option.equals("attack") || option.startsWith("attack ");
    }

    static boolean isGameObjectMenuAction(MenuAction action) {
        if (action == null) {
            return false;
        }
        return action == MenuAction.GAME_OBJECT_FIRST_OPTION
            || action == MenuAction.GAME_OBJECT_SECOND_OPTION
            || action == MenuAction.GAME_OBJECT_THIRD_OPTION
            || action == MenuAction.GAME_OBJECT_FOURTH_OPTION
            || action == MenuAction.GAME_OBJECT_FIFTH_OPTION;
    }

    static boolean isNpcMenuAction(MenuAction action) {
        if (action == null) {
            return false;
        }
        return action == MenuAction.NPC_FIRST_OPTION
            || action == MenuAction.NPC_SECOND_OPTION
            || action == MenuAction.NPC_THIRD_OPTION
            || action == MenuAction.NPC_FOURTH_OPTION
            || action == MenuAction.NPC_FIFTH_OPTION;
    }

    static boolean isGroundItemMenuAction(MenuAction action) {
        if (action == null) {
            return false;
        }
        return action == MenuAction.GROUND_ITEM_FIRST_OPTION
            || action == MenuAction.GROUND_ITEM_SECOND_OPTION
            || action == MenuAction.GROUND_ITEM_THIRD_OPTION
            || action == MenuAction.GROUND_ITEM_FOURTH_OPTION
            || action == MenuAction.GROUND_ITEM_FIFTH_OPTION;
    }

    static boolean hasChopAction(String[] actions) {
        if (actions == null || actions.length == 0) {
            return false;
        }
        for (String action : actions) {
            if (action == null) {
                continue;
            }
            String normalized = action.trim().toLowerCase(Locale.ROOT);
            if (normalized.equals("chop down") || normalized.equals("chop") || normalized.startsWith("chop ")) {
                return true;
            }
        }
        return false;
    }

    static boolean hasMineAction(String[] actions) {
        if (actions == null || actions.length == 0) {
            return false;
        }
        for (String action : actions) {
            if (action == null) {
                continue;
            }
            String normalized = action.trim().toLowerCase(Locale.ROOT);
            if (normalized.equals("mine") || normalized.startsWith("mine ")) {
                return true;
            }
        }
        return false;
    }

    static boolean hasFishingAction(String[] actions) {
        if (actions == null || actions.length == 0) {
            return false;
        }
        for (String action : actions) {
            if (action == null) {
                continue;
            }
            String normalized = action.trim().toLowerCase(Locale.ROOT);
            if (normalized.equals("net")
                || normalized.equals("bait")
                || normalized.equals("lure")
                || normalized.equals("cage")
                || normalized.equals("harpoon")
                || normalized.equals("big net")
                || normalized.equals("small net")
                || normalized.equals("fish")
                || normalized.equals("rod")
                || normalized.startsWith("harpoon ")
                || normalized.startsWith("net ")
                || normalized.startsWith("fish ")) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeMenuToken(String raw) {
        String s = safeString(raw).toLowerCase(Locale.ROOT).trim();
        return s.replaceAll("<[^>]*>", "").trim();
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}
