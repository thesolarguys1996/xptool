package com.xptool.executor;

import java.util.Locale;
import net.runelite.api.widgets.Widget;

final class WidgetActionResolver {
    private WidgetActionResolver() {
    }

    static int actionIndex(String[] actions, String targetAction) {
        if (actions == null || targetAction == null) {
            return -1;
        }
        for (int i = 0; i < actions.length; i++) {
            String action = actions[i];
            if (action != null && action.equalsIgnoreCase(targetAction)) {
                return i;
            }
        }
        return -1;
    }

    static int chooseWidgetOpByKeywordPriority(Widget widget, String... preferredKeywords) {
        if (widget == null) {
            return -1;
        }
        String[] actions = widget.getActions();
        if (actions == null || actions.length == 0 || preferredKeywords == null || preferredKeywords.length == 0) {
            return -1;
        }
        for (String keyword : preferredKeywords) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
            for (int i = 0; i < actions.length; i++) {
                String action = actions[i];
                if (action != null && action.toLowerCase(Locale.ROOT).contains(normalizedKeyword)) {
                    return i + 1;
                }
            }
        }
        return -1;
    }

    static String summarizeWidgetActions(Widget widget) {
        if (widget == null) {
            return "";
        }
        String[] actions = widget.getActions();
        if (actions == null || actions.length == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < actions.length; i++) {
            String action = actions[i];
            if (action == null || action.isBlank()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(" | ");
            }
            out.append(i + 1).append(":").append(action);
        }
        return out.toString();
    }
}
