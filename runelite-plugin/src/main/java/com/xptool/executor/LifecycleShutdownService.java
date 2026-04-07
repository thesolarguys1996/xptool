package com.xptool.executor;

final class LifecycleShutdownService {
    interface Host {
        void releasePendingIdleCameraDrag();
        void stopCommandIngestor();
        void clearPendingCommands();
        void clearDropSweepSessionRegistration();
        boolean isLogoutRuntimeActiveOrSuccessful();
        void requestLogoutRuntimeStop();
        boolean isLoginBreakRuntimeEnabled();
        void disarmBreakRuntime();
        void requestLoginRuntimeStop();
        void cancelResumePlanner();
        void cancelHumanTyping();
        void emitPendingTelemetryRollup();
        void shutdownInteractionSession();
    }

    private final Host host;

    LifecycleShutdownService(Host host) {
        this.host = host;
    }

    void shutdown() {
        host.releasePendingIdleCameraDrag();
        host.stopCommandIngestor();
        host.clearPendingCommands();
        host.clearDropSweepSessionRegistration();
        if (host.isLogoutRuntimeActiveOrSuccessful()) {
            host.requestLogoutRuntimeStop();
        }
        if (host.isLoginBreakRuntimeEnabled()) {
            host.disarmBreakRuntime();
            host.requestLoginRuntimeStop();
            host.cancelResumePlanner();
            host.cancelHumanTyping();
        }
        host.emitPendingTelemetryRollup();
        host.shutdownInteractionSession();
    }
}

