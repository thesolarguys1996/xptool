package com.xptool.bridge;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class BridgeAgentConfig {
    private static final String PROPERTY_ENABLED = "xptool.bridge.enabled";
    private static final String PROPERTY_PORT = "xptool.bridge.ipcPort";
    private static final String PROPERTY_BIND_ADDRESS = "xptool.bridge.bindAddress";
    private static final String PROPERTY_AUTH_TOKEN = "xptool.bridge.authToken";

    private static final String ENV_ENABLED = "XPTOOL_BRIDGE_ENABLED";
    private static final String ENV_PORT = "XPTOOL_BRIDGE_PORT";
    private static final String ENV_BIND_ADDRESS = "XPTOOL_BRIDGE_BIND_ADDRESS";
    private static final String ENV_AUTH_TOKEN = "XPTOOL_BRIDGE_AUTH_TOKEN";

    private static final int DEFAULT_PORT = 17888;
    private static final String DEFAULT_BIND_ADDRESS = "127.0.0.1";

    private final boolean enabled;
    private final int ipcPort;
    private final String bindAddress;
    private final String authToken;

    private BridgeAgentConfig(
        boolean enabled,
        int ipcPort,
        String bindAddress,
        String authToken
    ) {
        this.enabled = enabled;
        this.ipcPort = ipcPort;
        this.bindAddress = bindAddress;
        this.authToken = authToken;
    }

    static BridgeAgentConfig fromRuntime(String agentArgs) {
        Map<String, String> args = parseAgentArgs(agentArgs);

        boolean enabled = parseBoolean(
            firstNonEmpty(args, "enabled", PROPERTY_ENABLED),
            parseBoolean(System.getProperty(PROPERTY_ENABLED), parseBoolean(System.getenv(ENV_ENABLED), true))
        );
        int ipcPort = parsePort(
            firstNonEmpty(args, "port", "ipcPort", PROPERTY_PORT),
            parsePort(System.getProperty(PROPERTY_PORT), parsePort(System.getenv(ENV_PORT), DEFAULT_PORT))
        );
        String bindAddress = normalizeText(
            firstNonEmpty(args, "bindAddress", PROPERTY_BIND_ADDRESS),
            normalizeText(System.getProperty(PROPERTY_BIND_ADDRESS), normalizeText(System.getenv(ENV_BIND_ADDRESS), DEFAULT_BIND_ADDRESS))
        );
        String authToken = normalizeText(
            firstNonEmpty(args, "authToken", PROPERTY_AUTH_TOKEN),
            normalizeText(System.getProperty(PROPERTY_AUTH_TOKEN), normalizeText(System.getenv(ENV_AUTH_TOKEN), ""))
        );

        return new BridgeAgentConfig(
            enabled,
            ipcPort,
            bindAddress,
            authToken
        );
    }

    boolean enabled() {
        return enabled;
    }

    int ipcPort() {
        return ipcPort;
    }

    String bindAddress() {
        return bindAddress;
    }

    String authToken() {
        return authToken;
    }

    boolean hasAuthToken() {
        return authToken != null && !authToken.isBlank();
    }

    boolean isLoopbackBindAddress() {
        try {
            InetAddress address = InetAddress.getByName(bindAddress);
            return address != null && address.isLoopbackAddress();
        } catch (Exception ex) {
            return false;
        }
    }

    private static Map<String, String> parseAgentArgs(String agentArgs) {
        String raw = normalizeText(agentArgs, "");
        if (raw.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> values = new HashMap<>();
        String[] pairs = raw.split("[,;]");
        for (String pair : pairs) {
            if (pair == null) {
                continue;
            }
            String token = pair.trim();
            if (token.isEmpty()) {
                continue;
            }
            int separator = token.indexOf('=');
            if (separator <= 0 || separator >= token.length() - 1) {
                continue;
            }
            String key = token.substring(0, separator).trim();
            String value = token.substring(separator + 1).trim();
            if (!key.isEmpty() && !value.isEmpty()) {
                values.put(key, value);
                values.put(key.toLowerCase(Locale.ROOT), value);
            }
        }
        return values;
    }

    private static String firstNonEmpty(Map<String, String> args, String... keys) {
        if (args == null || args.isEmpty() || keys == null) {
            return "";
        }
        for (String key : keys) {
            String direct = normalizeText(args.get(key), "");
            if (!direct.isEmpty()) {
                return direct;
            }
            String lowered = normalizeText(args.get(key == null ? "" : key.toLowerCase(Locale.ROOT)), "");
            if (!lowered.isEmpty()) {
                return lowered;
            }
        }
        return "";
    }

    private static boolean parseBoolean(String value, boolean fallback) {
        String normalized = normalizeText(value, "").toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return fallback;
        }
        if ("1".equals(normalized) || "true".equals(normalized) || "yes".equals(normalized) || "on".equals(normalized)) {
            return true;
        }
        if ("0".equals(normalized) || "false".equals(normalized) || "no".equals(normalized) || "off".equals(normalized)) {
            return false;
        }
        return fallback;
    }

    private static int parsePort(String value, int fallback) {
        String normalized = normalizeText(value, "");
        if (normalized.isEmpty()) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(normalized);
            return parsed >= 1 && parsed <= 65535 ? parsed : fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static String normalizeText(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
