package com.xptool.executor;

public final class ExecutionPrivacySettings {
    private static final String COMMAND_INGEST_DEBUG_ENABLED_PROPERTY = "xptool.commandIngestDebugEnabled";
    private static final String CLICK_TELEMETRY_ENABLED_PROPERTY = "xptool.clickTelemetryEnabled";
    private static final String TELEMETRY_FILE_ENABLED_PROPERTY = "xptool.telemetryFileEnabled";
    private static final String SELECTION_LOGS_ENABLED_PROPERTY = "xptool.selectionLogsEnabled";

    private ExecutionPrivacySettings() {
    }

    public static boolean isCommandIngestDebugEnabled() {
        return readBoolean(COMMAND_INGEST_DEBUG_ENABLED_PROPERTY, false);
    }

    public static boolean isClickTelemetryEnabled() {
        return readBoolean(CLICK_TELEMETRY_ENABLED_PROPERTY, false);
    }

    public static boolean isTelemetryFileEnabled() {
        return readBoolean(TELEMETRY_FILE_ENABLED_PROPERTY, false);
    }

    public static boolean isSelectionLogsEnabled() {
        return readBoolean(SELECTION_LOGS_ENABLED_PROPERTY, false);
    }

    private static boolean readBoolean(String propertyName, boolean defaultValue) {
        String raw = System.getProperty(propertyName);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(raw.trim());
    }
}
