package com.xptool.bridge;

import java.util.concurrent.atomic.AtomicBoolean;

final class BridgeRuntime {
    private final BridgeAgentConfig config;
    private final BridgeDispatchSettings dispatchSettings;
    private final long runtimeStartedAtUnixMs = System.currentTimeMillis();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean bridgeReady = new AtomicBoolean(false);
    private final BridgeHeartbeatService heartbeatService;
    private final BridgeIpcServer ipcServer;

    BridgeRuntime(BridgeAgentConfig config) {
        this.config = config;
        this.dispatchSettings = new ExecutorBridgeDispatchSettings();
        this.heartbeatService = new BridgeHeartbeatService(this, dispatchSettings);
        this.ipcServer = new BridgeIpcServer(config, this, heartbeatService, dispatchSettings);
    }

    void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        ipcServer.start();
        bridgeReady.set(config.isLoopbackBindAddress() && config.hasAuthToken());
    }

    boolean isBridgeReady() {
        return bridgeReady.get();
    }

    long runtimeStartedAtUnixMs() {
        return runtimeStartedAtUnixMs;
    }
}
