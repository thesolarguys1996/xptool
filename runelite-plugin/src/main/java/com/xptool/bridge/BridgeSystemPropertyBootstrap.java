package com.xptool.bridge;

final class BridgeSystemPropertyBootstrap {
    private static final String PROPERTY_BRIDGE_ENABLED = "xptool.bridge.enabled";
    private static final String PROPERTY_PORT = "xptool.bridge.ipcPort";
    private static final String PROPERTY_BIND_ADDRESS = "xptool.bridge.bindAddress";
    private static final String PROPERTY_AUTH_TOKEN = "xptool.bridge.authToken";
    private static final String PROPERTY_COMMAND_EXECUTOR_SHADOW_ONLY = "xptool.commandExecutorShadowOnly";
    private static final String PROPERTY_BRIDGE_LIVE_DISPATCH = "xptool.bridge.liveDispatch";
    private static final String PROPERTY_BRIDGE_ALLOWLIST = "xptool.bridge.liveDispatchCommandAllowlist";

    private static final String DEFAULT_ALLOWLIST =
        "FISH_NEAREST_SPOT_SAFE,WOODCUT_CHOP_NEAREST_TREE_SAFE,SET_FISHING_IDLE_MODE_SAFE,WALK_TO_WORLDPOINT_SAFE,CAMERA_NUDGE_SAFE,DROP_START_SESSION,DROP_STOP_SESSION,DROP_ITEM_SAFE,STOP_ALL_RUNTIME,LOGOUT_SAFE,LOGIN_START_TEST";

    private BridgeSystemPropertyBootstrap() {
        // Static helper.
    }

    static void apply(BridgeAgentConfig config) {
        System.setProperty(PROPERTY_BRIDGE_ENABLED, "true");
        setIfMissing(PROPERTY_COMMAND_EXECUTOR_SHADOW_ONLY, "true");
        setIfMissing(PROPERTY_BRIDGE_LIVE_DISPATCH, "false");
        setIfMissing(PROPERTY_BRIDGE_ALLOWLIST, DEFAULT_ALLOWLIST);

        if (config != null) {
            setIfMissing(PROPERTY_PORT, Integer.toString(Math.max(1, config.ipcPort())));
            setIfMissing(PROPERTY_BIND_ADDRESS, safe(config.bindAddress()));
            String authToken = safe(config.authToken());
            if (!authToken.isEmpty()) {
                setIfMissing(PROPERTY_AUTH_TOKEN, authToken);
            }
        }
    }

    private static void setIfMissing(String key, String value) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            return;
        }
        String existing = System.getProperty(key);
        if (existing == null || existing.isBlank()) {
            System.setProperty(key, value);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
