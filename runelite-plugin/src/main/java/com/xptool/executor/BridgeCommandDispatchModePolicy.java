package com.xptool.executor;

import java.util.Locale;

final class BridgeCommandDispatchModePolicy {
    private final BridgeLiveDispatchPolicy liveDispatchPolicy;
    private final boolean shadowOnly;

    BridgeCommandDispatchModePolicy(BridgeLiveDispatchPolicy liveDispatchPolicy, boolean shadowOnly) {
        this.liveDispatchPolicy = liveDispatchPolicy;
        this.shadowOnly = shadowOnly;
    }

    boolean isBridgeRuntimeEnabled() {
        return liveDispatchPolicy.isBridgeRuntimeEnabled();
    }

    boolean isLiveDispatchEnabled() {
        return liveDispatchPolicy.isLiveDispatchEnabled();
    }

    String liveDispatchAllowlistCsv() {
        return liveDispatchPolicy.liveDispatchAllowlistCsv();
    }

    boolean isShadowQueueOnlyMode() {
        return shadowOnly && !isLiveDispatchActive();
    }

    boolean shouldEvaluateLiveCommand(String commandType) {
        if (!shadowOnly) {
            return true;
        }
        return isLiveDispatchCommand(normalizeCommandType(commandType));
    }

    String normalizeCommandType(String commandType) {
        return safeString(commandType).trim().toUpperCase(Locale.ROOT);
    }

    boolean isLiveDispatchCommand(String normalizedCommandType) {
        if (!isLiveDispatchActive()) {
            return false;
        }
        return liveDispatchPolicy.isAllowedLiveDispatchCommand(normalizedCommandType);
    }

    String telemetryMode(String commandType, String reason) {
        if (!shadowOnly) {
            return "live";
        }
        String normalizedCommandType = normalizeCommandType(commandType);
        if (isLiveDispatchCommand(normalizedCommandType) && !"shadow_would_dispatch".equals(safeString(reason))) {
            return "live";
        }
        return "shadow";
    }

    boolean isShadowWouldDispatchOutcome(ExecutionOutcome outcome) {
        if (outcome == null) {
            return false;
        }
        return "shadow_would_dispatch".equals(safeString(outcome.reason));
    }

    private boolean isLiveDispatchActive() {
        return shadowOnly
            && liveDispatchPolicy.isBridgeRuntimeEnabled()
            && liveDispatchPolicy.isLiveDispatchEnabled();
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}
