package com.xptool.sessions;

import com.xptool.executor.InteractionClickEvent;
import java.awt.Point;

final class InteractionPostClickSettleService {
    interface Host {
        boolean hasActiveSessionOtherThanInteraction();

        boolean hasPendingCommandRows();

        long currentMotorActionSerial();

        boolean performInteractionPostClickSettleMove(Point settleAnchor);

        long nowMs();

        int randomPercentRoll();

        long randomLongInclusive(long minInclusive, long maxInclusive);
    }

    private static final long POST_CLICK_SETTLE_DELAY_FAST_MIN_MS = 56L;
    private static final long POST_CLICK_SETTLE_DELAY_FAST_MAX_MS = 130L;
    private static final long POST_CLICK_SETTLE_DELAY_BASE_MIN_MS = 92L;
    private static final long POST_CLICK_SETTLE_DELAY_BASE_MAX_MS = 250L;
    private static final int POST_CLICK_SETTLE_SKIP_CHANCE_PERCENT = 42;
    private static final int POST_CLICK_SETTLE_EXTRA_HESITATION_CHANCE_PERCENT = 16;
    private static final long POST_CLICK_SETTLE_EXTRA_HESITATION_MIN_MS = 70L;
    private static final long POST_CLICK_SETTLE_EXTRA_HESITATION_MAX_MS = 190L;
    private static final long POST_CLICK_SETTLE_MAX_CLICK_AGE_MS = 350L;
    private static final long POST_CLICK_SETTLE_EXPIRY_MS = 1300L;

    private final Host host;
    private long pendingSettleClickSerial = -1L;
    private long settledClickSerial = -1L;
    private Point pendingSettleAnchorCanvasPoint = null;
    private long pendingSettleReadyAtMs = 0L;
    private long pendingSettleExpiresAtMs = 0L;
    private long pendingSettleMotorActionSerial = -1L;

    InteractionPostClickSettleService(Host host) {
        this.host = host;
    }

    void onInteractionClickEvent(InteractionClickEvent clickEvent) {
        if (clickEvent == null || !clickEvent.isSettleEligible()) {
            return;
        }
        long clickSerial = clickEvent.getClickSerial();
        if (clickSerial <= 0L || clickSerial == pendingSettleClickSerial || clickSerial == settledClickSerial) {
            return;
        }
        long now = host.nowMs();
        if ((now - clickEvent.getClickedAtMs()) > POST_CLICK_SETTLE_MAX_CLICK_AGE_MS) {
            return;
        }
        Point clickAnchor = clickEvent.getAnchorCanvasPoint();
        if (clickAnchor == null) {
            clickAnchor = clickEvent.getClickCanvasPoint();
        }
        if (clickAnchor == null) {
            return;
        }
        if (host.randomPercentRoll() < POST_CLICK_SETTLE_SKIP_CHANCE_PERCENT) {
            return;
        }
        long delayMs = samplePostClickSettleDelayMs();
        pendingSettleClickSerial = clickSerial;
        pendingSettleAnchorCanvasPoint = new Point(clickAnchor);
        pendingSettleReadyAtMs = now + delayMs;
        pendingSettleExpiresAtMs = now + POST_CLICK_SETTLE_EXPIRY_MS;
        pendingSettleMotorActionSerial = clickEvent.getMotorActionSerialAtClick();
    }

    boolean hasPendingPostClickSettle() {
        return pendingSettleAnchorCanvasPoint != null && pendingSettleClickSerial > 0L;
    }

    void clearPendingPostClickSettle() {
        pendingSettleClickSerial = -1L;
        pendingSettleAnchorCanvasPoint = null;
        pendingSettleReadyAtMs = 0L;
        pendingSettleExpiresAtMs = 0L;
        pendingSettleMotorActionSerial = -1L;
    }

    boolean shouldAcquireMotorForPendingSettle() {
        if (!hasPendingPostClickSettle()) {
            return false;
        }
        if (host.hasActiveSessionOtherThanInteraction()) {
            clearPendingPostClickSettle();
            return false;
        }
        if (host.hasPendingCommandRows()) {
            clearPendingPostClickSettle();
            return false;
        }
        if (host.currentMotorActionSerial() > pendingSettleMotorActionSerial) {
            clearPendingPostClickSettle();
            return false;
        }
        long now = host.nowMs();
        if (now > pendingSettleExpiresAtMs) {
            clearPendingPostClickSettle();
            return false;
        }
        return now >= pendingSettleReadyAtMs;
    }

    void tryRunPostClickSettle() {
        if (!hasPendingPostClickSettle()) {
            return;
        }
        if (host.hasActiveSessionOtherThanInteraction()) {
            clearPendingPostClickSettle();
            return;
        }
        if (host.hasPendingCommandRows()) {
            clearPendingPostClickSettle();
            return;
        }
        if (host.currentMotorActionSerial() > pendingSettleMotorActionSerial) {
            clearPendingPostClickSettle();
            return;
        }
        long now = host.nowMs();
        if (now < pendingSettleReadyAtMs) {
            return;
        }
        if (now > pendingSettleExpiresAtMs) {
            clearPendingPostClickSettle();
            return;
        }

        Point settleAnchor = new Point(pendingSettleAnchorCanvasPoint);
        boolean settled = host.performInteractionPostClickSettleMove(settleAnchor);
        if (settled) {
            settledClickSerial = pendingSettleClickSerial;
        }
        clearPendingPostClickSettle();
    }

    private long samplePostClickSettleDelayMs() {
        int profileRoll = host.randomPercentRoll();
        long delayMs;
        if (profileRoll < 34) {
            delayMs = randomLongInclusive(POST_CLICK_SETTLE_DELAY_FAST_MIN_MS, POST_CLICK_SETTLE_DELAY_FAST_MAX_MS);
        } else {
            delayMs = randomLongInclusive(POST_CLICK_SETTLE_DELAY_BASE_MIN_MS, POST_CLICK_SETTLE_DELAY_BASE_MAX_MS);
        }
        if (host.randomPercentRoll() < POST_CLICK_SETTLE_EXTRA_HESITATION_CHANCE_PERCENT) {
            delayMs += randomLongInclusive(
                POST_CLICK_SETTLE_EXTRA_HESITATION_MIN_MS,
                POST_CLICK_SETTLE_EXTRA_HESITATION_MAX_MS
            );
        }
        return delayMs;
    }

    private long randomLongInclusive(long min, long max) {
        long lo = Math.min(min, max);
        long hi = Math.max(min, max);
        if (lo == hi) {
            return lo;
        }
        return host.randomLongInclusive(lo, hi);
    }
}
