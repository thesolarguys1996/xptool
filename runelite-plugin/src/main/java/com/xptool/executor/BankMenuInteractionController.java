package com.xptool.executor;

import com.xptool.motion.MotionProfile.ClickMotionSettings;
import java.awt.Point;
import net.runelite.api.MenuEntry;

final class BankMenuInteractionController {
    interface Host {
        boolean rightClickCanvasPointBank(Point canvasPoint, ClickMotionSettings motion);

        boolean waitForMenuOpen(long timeoutMs);

        MenuEntry[] menuEntries();

        int menuX();

        int menuY();

        int menuWidth();

        boolean clickCanvasPointNoRefocus(Point canvasPoint, int clickSettleMs, int clickDownMs);

        void sleepCritical(long ms);

        boolean isMenuOpen();

        ClickMotionSettings defaultMenuInteractionMotion();

        void emitContextMenuOptionClicked(
            String option,
            String target,
            String matchedKeyword,
            int row,
            int menuX,
            int menuY
        );
    }

    static final class Config {
        final long rightClickPreMaxMs;
        final long rightClickPostMaxMs;
        final int initialOpenWaitBudgetMs;
        final int initialClickSettleMs;
        final int initialClickDownMs;
        final int initialPostClickCheckMs;
        final int minOpenWaitBudgetMs;
        final int maxOpenWaitBudgetMs;
        final int minClickSettleMs;
        final int maxClickSettleMs;
        final int minClickDownMs;
        final int maxClickDownMs;
        final int minPostClickCheckMs;
        final int maxPostClickCheckMs;
        final int successOpenWaitDeltaMs;
        final int failOpenWaitDeltaMs;
        final int clickXInsetPx;
        final int clickXRightPaddingPx;
        final int clickYBaseOffsetPx;
        final int clickYRowStepPx;
        final int clickYCenterOffsetPx;

        Config(
            long rightClickPreMaxMs,
            long rightClickPostMaxMs,
            int initialOpenWaitBudgetMs,
            int initialClickSettleMs,
            int initialClickDownMs,
            int initialPostClickCheckMs,
            int minOpenWaitBudgetMs,
            int maxOpenWaitBudgetMs,
            int minClickSettleMs,
            int maxClickSettleMs,
            int minClickDownMs,
            int maxClickDownMs,
            int minPostClickCheckMs,
            int maxPostClickCheckMs,
            int successOpenWaitDeltaMs,
            int failOpenWaitDeltaMs,
            int clickXInsetPx,
            int clickXRightPaddingPx,
            int clickYBaseOffsetPx,
            int clickYRowStepPx,
            int clickYCenterOffsetPx
        ) {
            this.rightClickPreMaxMs = Math.max(1L, rightClickPreMaxMs);
            this.rightClickPostMaxMs = Math.max(1L, rightClickPostMaxMs);
            this.initialOpenWaitBudgetMs = Math.max(1, initialOpenWaitBudgetMs);
            this.initialClickSettleMs = Math.max(1, initialClickSettleMs);
            this.initialClickDownMs = Math.max(1, initialClickDownMs);
            this.initialPostClickCheckMs = Math.max(1, initialPostClickCheckMs);
            this.minOpenWaitBudgetMs = Math.max(1, minOpenWaitBudgetMs);
            this.maxOpenWaitBudgetMs = Math.max(this.minOpenWaitBudgetMs, maxOpenWaitBudgetMs);
            this.minClickSettleMs = Math.max(1, minClickSettleMs);
            this.maxClickSettleMs = Math.max(this.minClickSettleMs, maxClickSettleMs);
            this.minClickDownMs = Math.max(1, minClickDownMs);
            this.maxClickDownMs = Math.max(this.minClickDownMs, maxClickDownMs);
            this.minPostClickCheckMs = Math.max(1, minPostClickCheckMs);
            this.maxPostClickCheckMs = Math.max(this.minPostClickCheckMs, maxPostClickCheckMs);
            this.successOpenWaitDeltaMs = Math.max(0, successOpenWaitDeltaMs);
            this.failOpenWaitDeltaMs = Math.max(0, failOpenWaitDeltaMs);
            this.clickXInsetPx = Math.max(0, clickXInsetPx);
            this.clickXRightPaddingPx = Math.max(0, clickXRightPaddingPx);
            this.clickYBaseOffsetPx = clickYBaseOffsetPx;
            this.clickYRowStepPx = Math.max(1, clickYRowStepPx);
            this.clickYCenterOffsetPx = clickYCenterOffsetPx;
        }
    }

    private final Host host;
    private final Config config;
    private int openWaitBudgetMs;
    private int clickSettleMs;
    private int clickDownMs;
    private int postClickCheckMs;

    BankMenuInteractionController(Host host, Config config) {
        this.host = host;
        this.config = config;
        this.openWaitBudgetMs = config.initialOpenWaitBudgetMs;
        this.clickSettleMs = config.initialClickSettleMs;
        this.clickDownMs = config.initialClickDownMs;
        this.postClickCheckMs = config.initialPostClickCheckMs;
    }

    boolean selectBankContextMenuOptionAt(Point canvasPoint, ClickMotionSettings motion, String... optionKeywords) {
        ClickMotionSettings bankMotion = motion == null
            ? host.defaultMenuInteractionMotion()
            : motion;
        ClickMotionSettings bankRightClickMotion = new ClickMotionSettings(
            bankMotion.driftRadiusPx,
            Math.max(1L, Math.min(bankMotion.preClickDelayMs, config.rightClickPreMaxMs)),
            Math.max(1L, Math.min(bankMotion.postClickDelayMs, config.rightClickPostMaxMs))
        );
        if (!host.rightClickCanvasPointBank(canvasPoint, bankRightClickMotion)) {
            tuneBankMenuTiming(false);
            return false;
        }
        if (!host.waitForMenuOpen(openWaitBudgetMs)) {
            tuneBankMenuTiming(false);
            return false;
        }
        MenuEntry[] entries = host.menuEntries();
        if (entries == null || entries.length == 0) {
            tuneBankMenuTiming(false);
            return false;
        }

        ExecutorMenuClickSupport.MenuMatch match = ExecutorMenuClickSupport.findMenuMatchFromTop(entries, optionKeywords);
        if (match == null) {
            tuneBankMenuTiming(false);
            return false;
        }

        boolean clicked = clickMatchedMenuRow(match.row, clickSettleMs, clickDownMs, postClickCheckMs);
        if (clicked) {
            host.emitContextMenuOptionClicked(
                match.option,
                match.target,
                match.matchedKeyword,
                match.row,
                host.menuX(),
                host.menuY()
            );
        }
        tuneBankMenuTiming(clicked);
        return clicked;
    }

    ContextMenuSelectionStatus selectBankContextMenuOptionAtDeferred(
        Point canvasPoint,
        ClickMotionSettings motion,
        String... optionKeywords
    ) {
        ClickMotionSettings bankMotion = motion == null
            ? host.defaultMenuInteractionMotion()
            : motion;
        ClickMotionSettings bankRightClickMotion = new ClickMotionSettings(
            bankMotion.driftRadiusPx,
            Math.max(1L, Math.min(bankMotion.preClickDelayMs, config.rightClickPreMaxMs)),
            Math.max(1L, Math.min(bankMotion.postClickDelayMs, config.rightClickPostMaxMs))
        );

        ContextMenuSelectionStatus preOpenStatus = selectFromOpenMenu(optionKeywords, clickSettleMs, clickDownMs, postClickCheckMs);
        if (preOpenStatus == ContextMenuSelectionStatus.SUCCESS) {
            tuneBankMenuTiming(true);
            return ContextMenuSelectionStatus.SUCCESS;
        }

        if (!host.rightClickCanvasPointBank(canvasPoint, bankRightClickMotion)) {
            tuneBankMenuTiming(false);
            return ContextMenuSelectionStatus.RIGHT_CLICK_FAILED;
        }

        ContextMenuSelectionStatus status = selectFromOpenMenu(optionKeywords, clickSettleMs, clickDownMs, postClickCheckMs);
        tuneBankMenuTiming(status == ContextMenuSelectionStatus.SUCCESS);
        return status;
    }

    private boolean clickMatchedMenuRow(int row, int clickSettleMs, int clickDownMs, int postClickCheckMs) {
        if (row < 0) {
            return false;
        }
        int menuX = host.menuX();
        int menuY = host.menuY();
        int menuW = Math.max(40, host.menuWidth());
        int clickX = menuX + Math.min(menuW - config.clickXRightPaddingPx, config.clickXInsetPx);
        int clickY = menuY
            + config.clickYBaseOffsetPx
            + (row * config.clickYRowStepPx)
            + config.clickYCenterOffsetPx;
        Point p = new Point(clickX, clickY);

        for (int i = 0; i < 1; i++) {
            if (!host.clickCanvasPointNoRefocus(p, clickSettleMs, clickDownMs)) {
                return false;
            }
            host.sleepCritical(Math.max(1L, (long) postClickCheckMs));
            if (!host.isMenuOpen()) {
                return true;
            }
        }
        return !host.isMenuOpen();
    }

    private ContextMenuSelectionStatus selectFromOpenMenu(
        String[] optionKeywords,
        int clickSettleMs,
        int clickDownMs,
        int postClickCheckMs
    ) {
        if (!host.isMenuOpen()) {
            return ContextMenuSelectionStatus.MENU_PENDING;
        }
        MenuEntry[] entries = host.menuEntries();
        if (entries == null || entries.length == 0) {
            return ContextMenuSelectionStatus.MENU_ENTRIES_UNAVAILABLE;
        }
        ExecutorMenuClickSupport.MenuMatch match = ExecutorMenuClickSupport.findMenuMatchFromTop(entries, optionKeywords);
        if (match == null) {
            return ContextMenuSelectionStatus.MENU_OPTION_NOT_FOUND;
        }
        boolean clicked = clickMatchedMenuRow(match.row, clickSettleMs, clickDownMs, postClickCheckMs);
        if (!clicked) {
            return ContextMenuSelectionStatus.MENU_ROW_CLICK_FAILED;
        }
        host.emitContextMenuOptionClicked(
            match.option,
            match.target,
            match.matchedKeyword,
            match.row,
            host.menuX(),
            host.menuY()
        );
        return ContextMenuSelectionStatus.SUCCESS;
    }

    private void tuneBankMenuTiming(boolean success) {
        if (success) {
            openWaitBudgetMs = Math.max(config.minOpenWaitBudgetMs, openWaitBudgetMs - config.successOpenWaitDeltaMs);
            clickSettleMs = Math.max(config.minClickSettleMs, clickSettleMs - 1);
            clickDownMs = Math.max(config.minClickDownMs, clickDownMs - 1);
            postClickCheckMs = Math.max(config.minPostClickCheckMs, postClickCheckMs - 1);
            return;
        }
        openWaitBudgetMs = Math.min(config.maxOpenWaitBudgetMs, openWaitBudgetMs + config.failOpenWaitDeltaMs);
        clickSettleMs = Math.min(config.maxClickSettleMs, clickSettleMs + 1);
        clickDownMs = Math.min(config.maxClickDownMs, clickDownMs + 1);
        postClickCheckMs = Math.min(config.maxPostClickCheckMs, postClickCheckMs + 1);
    }
}
