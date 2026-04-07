package com.xptool.executor;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Optional;

final class CommandRowParser {
    private CommandRowParser() {
    }

    static Optional<ParsedCommandRow> parseCommandLine(String line, Gson gson) {
        try {
            JsonObject obj = gson.fromJson(line, JsonObject.class);
            if (obj == null) {
                return Optional.empty();
            }
            String type = asString(obj.get("type"));
            if (!"COMMAND".equals(type)) {
                return Optional.empty();
            }
            int tick = asInt(obj.get("tick"), -1);
            String source = asString(obj.get("source"));
            JsonObject payload = obj.getAsJsonObject("payload");
            if (payload == null) {
                return Optional.empty();
            }
            String commandId = asString(payload.get("commandId"));
            long createdAtUnixMillis = asLong(payload.get("createdAtUnixMillis"), 0L);
            String commandType = asString(payload.get("commandType"));
            JsonObject commandPayload = payload.getAsJsonObject("commandPayload");
            if (commandPayload == null) {
                commandPayload = new JsonObject();
            }
            String reason = asString(payload.get("reason"));
            return Optional.of(
                new ParsedCommandRow(tick, source, commandId, createdAtUnixMillis, commandType, commandPayload, reason)
            );
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static String asString(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "";
        }
        try {
            return element.getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static int asInt(JsonElement element, int fallback) {
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            return element.getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static long asLong(JsonElement element, long fallback) {
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            return element.getAsLong();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    static final class ParsedCommandRow {
        final int tick;
        final String source;
        final String commandId;
        final long createdAtUnixMillis;
        final String commandType;
        final JsonObject commandPayload;
        final String reason;

        ParsedCommandRow(
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
            this.commandType = commandType;
            this.commandPayload = commandPayload;
            this.reason = reason;
        }
    }
}
