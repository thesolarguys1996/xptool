package com.xptool.executor;

import com.google.gson.JsonObject;
import java.util.Locale;

final class CommandRow {
    final int tick;
    final String source;
    final String commandId;
    final long createdAtUnixMillis;
    final String commandType;
    final JsonObject commandPayload;
    final String reason;

    CommandRow(
        int tick,
        String source,
        String commandId,
        long createdAtUnixMillis,
        String commandType,
        JsonObject commandPayload,
        String reason
    ) {
        this.tick = tick;
        this.source = source;
        this.commandId = commandId;
        this.createdAtUnixMillis = createdAtUnixMillis;
        this.commandType = normalizeType(commandType);
        this.commandPayload = commandPayload;
        this.reason = reason;
    }

    static String normalizeType(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
