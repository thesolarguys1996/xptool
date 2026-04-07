package com.xptool.executor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

final class ExecutorValueParsers {
    private ExecutorValueParsers() {
    }

    static String safeString(String value) {
        return value == null ? "" : value;
    }

    static String safePath(String path) {
        if (path == null) {
            return "";
        }
        String out = path.trim();
        if (out.isEmpty()) {
            return out;
        }
        // Normalize common Java properties escaping patterns for Windows file paths.
        out = out.replace("\\:", ":");
        out = out.replace("\\\\", "\\");
        return out;
    }

    static String asString(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "";
        }
        try {
            return element.getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    static int asInt(JsonElement element, int fallback) {
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            return element.getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    static boolean asBoolean(JsonElement element, boolean fallback) {
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            return element.getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    static JsonObject details(Object... kvPairs) {
        JsonObject out = new JsonObject();
        if (kvPairs == null) {
            return out;
        }
        for (int i = 0; i + 1 < kvPairs.length; i += 2) {
            String key = String.valueOf(kvPairs[i]);
            Object value = kvPairs[i + 1];
            if (value == null) {
                out.addProperty(key, "");
            } else if (value instanceof Number) {
                out.addProperty(key, (Number) value);
            } else if (value instanceof Boolean) {
                out.addProperty(key, (Boolean) value);
            } else {
                out.addProperty(key, String.valueOf(value));
            }
        }
        return out;
    }
}
