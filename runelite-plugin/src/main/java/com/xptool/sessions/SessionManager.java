package com.xptool.sessions;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class SessionManager {
    private static final long MIN_MOTOR_LEASE_MS = 1L;
    private final Map<String, Integer> activeSessionCounts = new LinkedHashMap<>();
    private String motorOwner = "";
    private long motorLeaseExpiresAtMs = 0L;

    public synchronized Registration registerSession(String sessionName) {
        String key = normalize(sessionName);
        if (key.isEmpty()) {
            return Registration.noop();
        }
        Integer count = activeSessionCounts.get(key);
        if (count == null) {
            activeSessionCounts.put(key, 1);
        } else {
            activeSessionCounts.put(key, count + 1);
        }
        return new Registration(this, key);
    }

    public synchronized void unregisterSession(String sessionName) {
        String key = normalize(sessionName);
        if (key.isEmpty()) {
            return;
        }
        Integer count = activeSessionCounts.get(key);
        if (count == null) {
            return;
        }
        if (count <= 1) {
            activeSessionCounts.remove(key);
            return;
        }
        activeSessionCounts.put(key, count - 1);
    }

    public synchronized boolean hasActiveSession() {
        return !activeSessionCounts.isEmpty();
    }

    public synchronized Optional<String> getActiveSession() {
        for (String name : activeSessionCounts.keySet()) {
            return Optional.of(name);
        }
        return Optional.empty();
    }

    public synchronized boolean hasActiveSessionOtherThan(String sessionName) {
        String self = normalize(sessionName);
        for (String name : activeSessionCounts.keySet()) {
            if (!name.equals(self)) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean tryAcquireMotor(String owner, long leaseMs) {
        String key = normalize(owner);
        if (key.isEmpty()) {
            return false;
        }
        expireMotorLeaseIfNeeded();
        if (motorOwner.isEmpty() || motorOwner.equals(key)) {
            motorOwner = key;
            motorLeaseExpiresAtMs = nextLeaseExpiry(leaseMs);
            return true;
        }
        return false;
    }

    public synchronized boolean renewMotor(String owner, long leaseMs) {
        String key = normalize(owner);
        if (key.isEmpty()) {
            return false;
        }
        expireMotorLeaseIfNeeded();
        if (!motorOwner.equals(key)) {
            return false;
        }
        motorLeaseExpiresAtMs = nextLeaseExpiry(leaseMs);
        return true;
    }

    public synchronized void releaseMotor(String owner) {
        String key = normalize(owner);
        if (key.isEmpty()) {
            return;
        }
        expireMotorLeaseIfNeeded();
        if (!motorOwner.equals(key)) {
            return;
        }
        motorOwner = "";
        motorLeaseExpiresAtMs = 0L;
    }

    public synchronized boolean isMotorOwner(String owner) {
        String key = normalize(owner);
        if (key.isEmpty()) {
            return false;
        }
        expireMotorLeaseIfNeeded();
        return motorOwner.equals(key);
    }

    private void expireMotorLeaseIfNeeded() {
        if (motorOwner.isEmpty()) {
            motorLeaseExpiresAtMs = 0L;
            return;
        }
        long now = System.currentTimeMillis();
        if (now <= motorLeaseExpiresAtMs) {
            return;
        }
        motorOwner = "";
        motorLeaseExpiresAtMs = 0L;
    }

    private static long nextLeaseExpiry(long leaseMs) {
        long safeLeaseMs = Math.max(MIN_MOTOR_LEASE_MS, leaseMs);
        return System.currentTimeMillis() + safeLeaseMs;
    }

    private static String normalize(String sessionName) {
        return sessionName == null ? "" : sessionName.trim();
    }

    public static final class Registration implements AutoCloseable {
        private final SessionManager manager;
        private final String sessionName;
        private boolean closed = false;

        private Registration(SessionManager manager, String sessionName) {
            this.manager = manager;
            this.sessionName = sessionName;
        }

        private static Registration noop() {
            return new Registration(null, "");
        }

        @Override
        public void close() {
            SessionManager owner;
            String key;
            synchronized (this) {
                if (closed) {
                    return;
                }
                closed = true;
                owner = manager;
                key = sessionName;
            }
            if (owner != null) {
                owner.unregisterSession(key);
            }
        }
    }
}
