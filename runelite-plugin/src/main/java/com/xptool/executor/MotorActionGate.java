package com.xptool.executor;

import com.xptool.sessions.SessionManager;

final class MotorActionGate {
    private static volatile long globalNextAllowedMotorActionAtMs = 0L;

    private final int maxMouseMutationsPerPump;
    private int mouseMutationsThisPump = 0;
    private long actionSerial = 0L;
    private long nextAllowedMotorActionAtMs = 0L;

    MotorActionGate(int maxMouseMutationsPerPump) {
        this.maxMouseMutationsPerPump = Math.max(1, maxMouseMutationsPerPump);
    }

    boolean canPerformActionNow(SessionManager sessionManager, String owner) {
        if (sessionManager == null) {
            return false;
        }
        String key = owner == null ? "" : owner.trim();
        if (key.isEmpty()) {
            return false;
        }
        if (!sessionManager.isMotorOwner(key)) {
            return false;
        }
        return isActionReadyNow();
    }

    boolean isActionReadyNow() {
        long now = System.currentTimeMillis();
        long gate = Math.max(nextAllowedMotorActionAtMs, globalNextAllowedMotorActionAtMs);
        return now >= gate;
    }

    void reserveCooldown(long delayMs) {
        if (delayMs <= 0L) {
            return;
        }
        long base = Math.max(System.currentTimeMillis(), nextAllowedMotorActionAtMs);
        nextAllowedMotorActionAtMs = base + delayMs;
        reserveGlobalCooldown(delayMs);
    }

    void noteAction() {
        actionSerial++;
    }

    long actionSerial() {
        return actionSerial;
    }

    boolean tryConsumeMouseMutationBudget() {
        if (mouseMutationsThisPump >= maxMouseMutationsPerPump) {
            return false;
        }
        mouseMutationsThisPump++;
        return true;
    }

    void resetMouseMutationBudget() {
        mouseMutationsThisPump = 0;
    }

    static void reserveGlobalCooldownOnly(long delayMs) {
        reserveGlobalCooldown(delayMs);
    }

    private static synchronized void reserveGlobalCooldown(long delayMs) {
        if (delayMs <= 0L) {
            return;
        }
        long base = Math.max(System.currentTimeMillis(), globalNextAllowedMotorActionAtMs);
        globalNextAllowedMotorActionAtMs = base + delayMs;
    }
}
