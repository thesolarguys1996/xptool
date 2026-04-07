package com.xptool.executor;

import java.awt.Rectangle;
import java.util.Locale;
import net.runelite.api.widgets.Widget;

final class LoginWidgetHeuristics {
    private LoginWidgetHeuristics() {
    }

    static boolean matchesKeywords(Widget widget, String[] keywords) {
        if (widget == null || widget.isHidden()) {
            return false;
        }
        Rectangle bounds = widget.getBounds();
        if (bounds == null || bounds.width <= 0 || bounds.height <= 0) {
            return false;
        }

        StringBuilder haystack = new StringBuilder();
        String text = widget.getText();
        if (text != null) {
            haystack.append(text).append(' ');
        }
        String name = widget.getName();
        if (name != null) {
            haystack.append(name).append(' ');
        }
        String[] actions = widget.getActions();
        if (actions != null) {
            for (String action : actions) {
                if (action != null) {
                    haystack.append(action).append(' ');
                }
            }
        }
        String corpus = haystack.toString().toLowerCase(Locale.ROOT);
        if (corpus.isBlank()) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && corpus.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    static long scoreWidget(Widget widget) {
        if (widget == null || widget.getBounds() == null) {
            return Long.MIN_VALUE;
        }
        Rectangle bounds = widget.getBounds();
        long area = (long) bounds.width * (long) bounds.height;
        long centerBias = 2000L - (Math.abs(bounds.x - 380L) + Math.abs(bounds.y - 260L));
        return area + centerBias;
    }

    static String joinKeywords(String... keywords) {
        if (keywords == null || keywords.length == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (String keyword : keywords) {
            String safe = safeString(keyword).trim();
            if (safe.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append('|');
            }
            out.append(safe);
        }
        return out.toString();
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}
