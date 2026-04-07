package com.xptool.executor;

import com.xptool.bridge.BridgeDispatchSettings;

final class BridgeRuntimeStateLiveDispatchPolicy implements BridgeLiveDispatchPolicy {
    @Override
    public boolean isBridgeRuntimeEnabled() {
        return BridgeDispatchSettings.RuntimeState.isBridgeRuntimeEnabled();
    }

    @Override
    public boolean isLiveDispatchEnabled() {
        return BridgeDispatchSettings.RuntimeState.isLiveDispatchEnabled();
    }

    @Override
    public String liveDispatchAllowlistCsv() {
        return BridgeDispatchSettings.RuntimeState.liveDispatchAllowlistCsv();
    }

    @Override
    public boolean isAllowedLiveDispatchCommand(String normalizedCommandType) {
        return BridgeDispatchSettings.RuntimeState.isAllowedLiveDispatchCommand(normalizedCommandType);
    }
}
