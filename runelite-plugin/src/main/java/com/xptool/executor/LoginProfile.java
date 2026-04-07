package com.xptool.executor;

import java.util.concurrent.ThreadLocalRandom;

final class LoginProfile {
    private final int maxRetries;
    private final int retryCooldownMinTicks;
    private final int retryCooldownMaxTicks;
    private final int waitResultTimeoutMinTicks;
    private final int waitResultTimeoutMaxTicks;
    private final int worldSelectChancePct;

    LoginProfile(
        int maxRetries,
        int retryCooldownTicks,
        int waitResultTimeoutTicks,
        int worldSelectChancePct
    ) {
        this(
            maxRetries,
            retryCooldownTicks,
            retryCooldownTicks,
            waitResultTimeoutTicks,
            waitResultTimeoutTicks,
            worldSelectChancePct
        );
    }

    LoginProfile(
        int maxRetries,
        int retryCooldownMinTicks,
        int retryCooldownMaxTicks,
        int waitResultTimeoutMinTicks,
        int waitResultTimeoutMaxTicks,
        int worldSelectChancePct
    ) {
        this.maxRetries = Math.max(0, maxRetries);
        int retryMin = Math.max(1, retryCooldownMinTicks);
        int retryMax = Math.max(1, retryCooldownMaxTicks);
        if (retryMin > retryMax) {
            int tmp = retryMin;
            retryMin = retryMax;
            retryMax = tmp;
        }
        int waitMin = Math.max(1, waitResultTimeoutMinTicks);
        int waitMax = Math.max(1, waitResultTimeoutMaxTicks);
        if (waitMin > waitMax) {
            int tmp = waitMin;
            waitMin = waitMax;
            waitMax = tmp;
        }
        this.retryCooldownMinTicks = retryMin;
        this.retryCooldownMaxTicks = retryMax;
        this.waitResultTimeoutMinTicks = waitMin;
        this.waitResultTimeoutMaxTicks = waitMax;
        this.worldSelectChancePct = clampPercent(worldSelectChancePct);
    }

    static LoginProfile defaults() {
        return new LoginProfile(3, 6, 11, 120, 165, 14);
    }

    int maxRetries() {
        return maxRetries;
    }

    int retryCooldownTicks() {
        return retryCooldownMinTicks;
    }

    int waitResultTimeoutTicks() {
        return waitResultTimeoutMinTicks;
    }

    int worldSelectChancePct() {
        return worldSelectChancePct;
    }

    int rollRetryCooldownTicks() {
        if (retryCooldownMaxTicks <= retryCooldownMinTicks) {
            return retryCooldownMinTicks;
        }
        return ThreadLocalRandom.current().nextInt(retryCooldownMinTicks, retryCooldownMaxTicks + 1);
    }

    int rollWaitResultTimeoutTicks() {
        if (waitResultTimeoutMaxTicks <= waitResultTimeoutMinTicks) {
            return waitResultTimeoutMinTicks;
        }
        return ThreadLocalRandom.current().nextInt(waitResultTimeoutMinTicks, waitResultTimeoutMaxTicks + 1);
    }

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
