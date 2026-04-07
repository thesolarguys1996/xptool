package com.xptool.bridge;

final class ExecutorBridgeDispatchSettings implements BridgeDispatchSettings {
    @Override
    public boolean isBridgeRuntimeEnabled() {
        return BridgeDispatchSettings.RuntimeState.isBridgeRuntimeEnabled();
    }

    @Override
    public void setBridgeRuntimeEnabled(boolean enabled) {
        BridgeDispatchSettings.RuntimeState.setBridgeRuntimeEnabled(enabled);
    }

    @Override
    public boolean isLiveDispatchEnabled() {
        return BridgeDispatchSettings.RuntimeState.isLiveDispatchEnabled();
    }

    @Override
    public void setLiveDispatchEnabled(boolean enabled) {
        BridgeDispatchSettings.RuntimeState.setLiveDispatchEnabled(enabled);
    }

    @Override
    public String liveDispatchAllowlistCsv() {
        return BridgeDispatchSettings.RuntimeState.liveDispatchAllowlistCsv();
    }

    @Override
    public void setLiveDispatchAllowlistCsv(String csv) {
        BridgeDispatchSettings.RuntimeState.setLiveDispatchAllowlistCsv(csv);
    }

    @Override
    public void setLiveDispatchAllowlistFromIterable(Iterable<String> values) {
        BridgeDispatchSettings.RuntimeState.setLiveDispatchAllowlistFromIterable(values);
    }

    @Override
    public Iterable<String> liveDispatchAllowlistSnapshot() {
        return BridgeDispatchSettings.RuntimeState.liveDispatchAllowlistSnapshot();
    }
}
