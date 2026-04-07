package com.xptool.bridge;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public interface BridgeDispatchSettings {
    boolean isBridgeRuntimeEnabled();

    void setBridgeRuntimeEnabled(boolean enabled);

    boolean isLiveDispatchEnabled();

    void setLiveDispatchEnabled(boolean enabled);

    String liveDispatchAllowlistCsv();

    void setLiveDispatchAllowlistCsv(String csv);

    void setLiveDispatchAllowlistFromIterable(Iterable<String> values);

    Iterable<String> liveDispatchAllowlistSnapshot();

    final class RuntimeState {
        private static final String PROPERTY_BRIDGE_ENABLED = "xptool.bridge.enabled";
        private static final String PROPERTY_BRIDGE_LIVE_DISPATCH = "xptool.bridge.liveDispatch";
        private static final String PROPERTY_BRIDGE_LIVE_DISPATCH_ALLOWLIST = "xptool.bridge.liveDispatchCommandAllowlist";

        private static final String DEFAULT_ALLOWLIST_CSV =
            "FISH_NEAREST_SPOT_SAFE,WOODCUT_CHOP_NEAREST_TREE_SAFE,SET_FISHING_IDLE_MODE_SAFE,WALK_TO_WORLDPOINT_SAFE,CAMERA_NUDGE_SAFE,DROP_START_SESSION,DROP_STOP_SESSION,DROP_ITEM_SAFE,STOP_ALL_RUNTIME,LOGOUT_SAFE,LOGIN_START_TEST";

        private static final AtomicBoolean BRIDGE_RUNTIME_ENABLED = new AtomicBoolean(
            parseBoolean(System.getProperty(PROPERTY_BRIDGE_ENABLED), false)
        );
        private static final AtomicBoolean LIVE_DISPATCH_ENABLED = new AtomicBoolean(
            parseBoolean(System.getProperty(PROPERTY_BRIDGE_LIVE_DISPATCH), false)
        );
        private static final AtomicReference<Set<String>> LIVE_DISPATCH_ALLOWLIST = new AtomicReference<>(
            parseAllowlistCsv(System.getProperty(PROPERTY_BRIDGE_LIVE_DISPATCH_ALLOWLIST, DEFAULT_ALLOWLIST_CSV))
        );

        private RuntimeState() {
        }

        public static boolean isBridgeRuntimeEnabled() {
            return BRIDGE_RUNTIME_ENABLED.get();
        }

        public static void setBridgeRuntimeEnabled(boolean enabled) {
            BRIDGE_RUNTIME_ENABLED.set(enabled);
            System.setProperty(PROPERTY_BRIDGE_ENABLED, Boolean.toString(enabled));
        }

        public static boolean isLiveDispatchEnabled() {
            return LIVE_DISPATCH_ENABLED.get();
        }

        public static void setLiveDispatchEnabled(boolean enabled) {
            LIVE_DISPATCH_ENABLED.set(enabled);
            System.setProperty(PROPERTY_BRIDGE_LIVE_DISPATCH, Boolean.toString(enabled));
        }

        public static Set<String> liveDispatchAllowlistSnapshot() {
            return LIVE_DISPATCH_ALLOWLIST.get();
        }

        public static String liveDispatchAllowlistCsv() {
            return String.join(",", liveDispatchAllowlistSnapshot());
        }

        public static void setLiveDispatchAllowlistCsv(String csv) {
            Set<String> allowlist = parseAllowlistCsv(csv);
            LIVE_DISPATCH_ALLOWLIST.set(allowlist);
            System.setProperty(PROPERTY_BRIDGE_LIVE_DISPATCH_ALLOWLIST, String.join(",", allowlist));
        }

        public static void setLiveDispatchAllowlistFromIterable(Iterable<String> values) {
            Set<String> allowlist = parseAllowlist(values);
            LIVE_DISPATCH_ALLOWLIST.set(allowlist);
            System.setProperty(PROPERTY_BRIDGE_LIVE_DISPATCH_ALLOWLIST, String.join(",", allowlist));
        }

        public static boolean isAllowedLiveDispatchCommand(String commandType) {
            String normalized = normalizeCommandType(commandType);
            if (normalized.isEmpty()) {
                return false;
            }
            return LIVE_DISPATCH_ALLOWLIST.get().contains(normalized);
        }

        public static Set<String> parseAllowlistCsv(String csv) {
            LinkedHashSet<String> parsed = new LinkedHashSet<>();
            String raw = csv == null ? "" : csv;
            for (String token : raw.split(",")) {
                String normalized = normalizeCommandType(token);
                if (!normalized.isEmpty()) {
                    parsed.add(normalized);
                }
            }
            if (parsed.isEmpty()) {
                for (String token : DEFAULT_ALLOWLIST_CSV.split(",")) {
                    String normalized = normalizeCommandType(token);
                    if (!normalized.isEmpty()) {
                        parsed.add(normalized);
                    }
                }
            }
            return Collections.unmodifiableSet(parsed);
        }

        public static String normalizeCommandType(String value) {
            if (value == null) {
                return "";
            }
            return value.trim().toUpperCase(Locale.ROOT);
        }

        private static Set<String> parseAllowlist(Iterable<String> values) {
            LinkedHashSet<String> parsed = new LinkedHashSet<>();
            if (values != null) {
                for (String token : values) {
                    String normalized = normalizeCommandType(token);
                    if (!normalized.isEmpty()) {
                        parsed.add(normalized);
                    }
                }
            }
            if (parsed.isEmpty()) {
                for (String token : DEFAULT_ALLOWLIST_CSV.split(",")) {
                    String normalized = normalizeCommandType(token);
                    if (!normalized.isEmpty()) {
                        parsed.add(normalized);
                    }
                }
            }
            return Collections.unmodifiableSet(parsed);
        }

        private static boolean parseBoolean(String value, boolean fallback) {
            if (value == null || value.isBlank()) {
                return fallback;
            }
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if ("1".equals(normalized) || "true".equals(normalized) || "yes".equals(normalized) || "on".equals(normalized)) {
                return true;
            }
            if ("0".equals(normalized) || "false".equals(normalized) || "no".equals(normalized) || "off".equals(normalized)) {
                return false;
            }
            return fallback;
        }
    }
}
