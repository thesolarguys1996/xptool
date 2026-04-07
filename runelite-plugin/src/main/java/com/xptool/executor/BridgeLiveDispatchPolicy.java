package com.xptool.executor;

interface BridgeLiveDispatchPolicy {
    boolean isBridgeRuntimeEnabled();

    boolean isLiveDispatchEnabled();

    String liveDispatchAllowlistCsv();

    boolean isAllowedLiveDispatchCommand(String normalizedCommandType);
}
