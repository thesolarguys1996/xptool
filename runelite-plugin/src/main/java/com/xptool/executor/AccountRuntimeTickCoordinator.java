package com.xptool.executor;

import net.runelite.api.GameState;

final class AccountRuntimeTickCoordinator {
    interface Host {
        GameState gameState();
        boolean isLoginRuntimeActive();
        boolean isLogoutRuntimeActive();
        long currentTimeMs();

        long loginClientTickAdvanceMinIntervalMs();
        long logoutClientTickAdvanceMinIntervalMs();
        long lastLoginClientAdvanceAtMs();
        void setLastLoginClientAdvanceAtMs(long value);
        long lastLogoutClientAdvanceAtMs();
        void setLastLogoutClientAdvanceAtMs(long value);
        int lastLoginRuntimeAdvanceTick();
        void setLastLoginRuntimeAdvanceTick(int value);
        int lastLogoutRuntimeAdvanceTick();
        void setLastLogoutRuntimeAdvanceTick(int value);

        void onLoginRuntimeGameTick(int runtimeTick);
        void onLogoutRuntimeGameTick(int runtimeTick);
    }

    private final Host host;

    AccountRuntimeTickCoordinator(Host host) {
        this.host = host;
    }

    void advanceLoginRuntimeOnClientTickWhenLoggedOut(int observedTick) {
        if (host.gameState() == GameState.LOGGED_IN) {
            host.setLastLoginRuntimeAdvanceTick(Integer.MIN_VALUE);
            return;
        }
        if (!host.isLoginRuntimeActive()) {
            return;
        }
        long now = host.currentTimeMs();
        if ((now - host.lastLoginClientAdvanceAtMs()) < host.loginClientTickAdvanceMinIntervalMs()) {
            return;
        }
        host.setLastLoginClientAdvanceAtMs(now);
        host.onLoginRuntimeGameTick(nextLoginRuntimeTick(observedTick));
    }

    void advanceLogoutRuntimeOnClientTick(int observedTick) {
        if (!host.isLogoutRuntimeActive()) {
            host.setLastLogoutRuntimeAdvanceTick(Integer.MIN_VALUE);
            return;
        }
        long now = host.currentTimeMs();
        if ((now - host.lastLogoutClientAdvanceAtMs()) < host.logoutClientTickAdvanceMinIntervalMs()) {
            return;
        }
        host.setLastLogoutClientAdvanceAtMs(now);
        host.onLogoutRuntimeGameTick(nextLogoutRuntimeTick(observedTick));
    }

    void advanceLogoutRuntimeOnObservedTick(int observedTick) {
        host.onLogoutRuntimeGameTick(nextLogoutRuntimeTick(observedTick));
    }

    private int nextLoginRuntimeTick(int observedTick) {
        if (host.lastLoginRuntimeAdvanceTick() == Integer.MIN_VALUE) {
            host.setLastLoginRuntimeAdvanceTick(observedTick);
            return observedTick;
        }
        int next = observedTick;
        if (next <= host.lastLoginRuntimeAdvanceTick()) {
            next = host.lastLoginRuntimeAdvanceTick() + 1;
        }
        host.setLastLoginRuntimeAdvanceTick(next);
        return next;
    }

    private int nextLogoutRuntimeTick(int observedTick) {
        if (host.lastLogoutRuntimeAdvanceTick() == Integer.MIN_VALUE) {
            host.setLastLogoutRuntimeAdvanceTick(observedTick);
            return observedTick;
        }
        int next = observedTick;
        if (next <= host.lastLogoutRuntimeAdvanceTick()) {
            next = host.lastLogoutRuntimeAdvanceTick() + 1;
        }
        host.setLastLogoutRuntimeAdvanceTick(next);
        return next;
    }
}
