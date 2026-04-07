package com.xptool.bridge;

import com.google.gson.JsonObject;

final class BridgeHeartbeatService {
    private final BridgeRuntime runtime;
    private final BridgeDispatchSettings dispatchSettings;

    BridgeHeartbeatService(BridgeRuntime runtime, BridgeDispatchSettings dispatchSettings) {
        this.runtime = runtime;
        this.dispatchSettings = dispatchSettings;
    }

    JsonObject heartbeat() {
        JsonObject payload = basePayload();
        payload.addProperty("status", runtime.isBridgeReady() ? "bridge_ipc_ready" : "bridge_ipc_not_ready");
        payload.addProperty("tickCount", -1);
        payload.addProperty("gameState", "UNAVAILABLE");
        payload.addProperty("loggedIn", false);
        return payload;
    }

    private JsonObject basePayload() {
        JsonObject payload = new JsonObject();
        payload.addProperty("ok", true);
        payload.addProperty("bridgeReady", runtime.isBridgeReady());
        payload.addProperty("bridgeRuntimeEnabled", dispatchSettings.isBridgeRuntimeEnabled());
        payload.addProperty("liveDispatchEnabled", dispatchSettings.isLiveDispatchEnabled());
        payload.addProperty("liveDispatchAllowlist", dispatchSettings.liveDispatchAllowlistCsv());
        payload.addProperty("runtimeStartedAtUnixMs", runtime.runtimeStartedAtUnixMs());
        return payload;
    }
}
