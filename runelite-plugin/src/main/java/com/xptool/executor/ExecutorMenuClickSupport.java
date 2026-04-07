package com.xptool.executor;

import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import net.runelite.api.MenuEntry;

final class ExecutorMenuClickSupport {
    private ExecutorMenuClickSupport() {
    }

    static boolean clickAtCritical(Robot robot, Point p, long settleMs, long downMs, int jitterRadiusPx) {
        if (robot == null || p == null) {
            return false;
        }
        Point target = ExecutorCursorMotion.withMicroJitter(p, Math.max(0, jitterRadiusPx));
        ExecutorCursorMotion.moveMouseCurve(robot, target);
        sleepCritical(settleMs);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        sleepCritical(downMs);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        return true;
    }

    static boolean clickAt(
        Robot robot,
        Point p,
        long settleMs,
        long downMs,
        double jitterRadiusPx,
        int minSettleMs,
        int minDownMs
    ) {
        if (robot == null || p == null) {
            return false;
        }
        int radius = Math.max(0, (int) Math.round(jitterRadiusPx));
        Point target = ExecutorCursorMotion.withMicroJitter(p, radius);
        ExecutorCursorMotion.moveMouseCurve(robot, target);
        sleepCritical(Math.max(minSettleMs, settleMs));
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        sleepCritical(Math.max(minDownMs, downMs));
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        return true;
    }

    static MenuMatch findMenuMatchFromTop(MenuEntry[] entries, String... optionKeywords) {
        if (entries == null || entries.length == 0 || optionKeywords == null || optionKeywords.length == 0) {
            return null;
        }
        for (String keywordRaw : optionKeywords) {
            String keyword = normalizeMenuText(keywordRaw);
            if (keyword.isBlank()) {
                continue;
            }
            int row = 0;
            for (int i = entries.length - 1; i >= 0; i--, row++) {
                MenuEntry entry = entries[i];
                if (entry == null) {
                    continue;
                }
                String option = safeString(entry.getOption());
                String target = safeString(entry.getTarget());
                String optionCorpus = normalizeMenuText(option);
                if (containsPhraseWithBoundaries(optionCorpus, keyword)) {
                    return new MenuMatch(row, option, target, keywordRaw);
                }
            }
        }
        return null;
    }

    private static String normalizeMenuText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        boolean lastWasSpace = true;
        for (int i = 0; i < value.length(); i++) {
            char c = Character.toLowerCase(value.charAt(i));
            if (Character.isLetterOrDigit(c)) {
                out.append(c);
                lastWasSpace = false;
                continue;
            }
            if (!lastWasSpace) {
                out.append(' ');
                lastWasSpace = true;
            }
        }
        int len = out.length();
        if (len > 0 && out.charAt(len - 1) == ' ') {
            out.setLength(len - 1);
        }
        return out.toString();
    }

    private static boolean containsPhraseWithBoundaries(String corpus, String phrase) {
        if (corpus == null || phrase == null || corpus.isBlank() || phrase.isBlank()) {
            return false;
        }
        int from = 0;
        while (from <= corpus.length()) {
            int idx = corpus.indexOf(phrase, from);
            if (idx < 0) {
                return false;
            }
            int before = idx - 1;
            int after = idx + phrase.length();
            boolean leftBoundary = before < 0 || corpus.charAt(before) == ' ';
            boolean rightBoundary = after >= corpus.length() || corpus.charAt(after) == ' ';
            if (leftBoundary && rightBoundary) {
                return true;
            }
            from = idx + 1;
        }
        return false;
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }

    static final class MenuMatch {
        final int row;
        final String option;
        final String target;
        final String matchedKeyword;

        MenuMatch(int row, String option, String target, String matchedKeyword) {
            this.row = row;
            this.option = option;
            this.target = target;
            this.matchedKeyword = matchedKeyword;
        }
    }

    private static void sleepCritical(long ms) {
        MotorActionGate.reserveGlobalCooldownOnly(ms);
    }
}
