package com.xptool.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BridgeAgentConfigTest {
    private static final String PROP_ENABLED = "xptool.bridge.enabled";
    private static final String PROP_PORT = "xptool.bridge.ipcPort";
    private static final String PROP_BIND_ADDRESS = "xptool.bridge.bindAddress";
    private static final String PROP_AUTH_TOKEN = "xptool.bridge.authToken";

    @AfterEach
    void clearProperties() {
        System.clearProperty(PROP_ENABLED);
        System.clearProperty(PROP_PORT);
        System.clearProperty(PROP_BIND_ADDRESS);
        System.clearProperty(PROP_AUTH_TOKEN);
    }

    @Test
    void readsSystemProperties() {
        System.setProperty(PROP_ENABLED, "false");
        System.setProperty(PROP_PORT, "18888");
        System.setProperty(PROP_BIND_ADDRESS, "127.0.0.2");
        System.setProperty(PROP_AUTH_TOKEN, "secret-token");

        BridgeAgentConfig config = BridgeAgentConfig.fromRuntime("");
        assertFalse(config.enabled());
        assertEquals(18888, config.ipcPort());
        assertEquals("127.0.0.2", config.bindAddress());
        assertEquals("secret-token", config.authToken());
    }

    @Test
    void agentArgsOverrideSystemProperties() {
        System.setProperty(PROP_ENABLED, "false");
        System.setProperty(PROP_PORT, "18888");
        System.setProperty(PROP_BIND_ADDRESS, "127.0.0.2");
        System.setProperty(PROP_AUTH_TOKEN, "property-token");

        BridgeAgentConfig config = BridgeAgentConfig.fromRuntime("enabled=true,port=19999,bindAddress=127.0.0.1,authToken=arg-token");

        assertTrue(config.enabled());
        assertEquals(19999, config.ipcPort());
        assertEquals("127.0.0.1", config.bindAddress());
        assertEquals("arg-token", config.authToken());
    }
}
