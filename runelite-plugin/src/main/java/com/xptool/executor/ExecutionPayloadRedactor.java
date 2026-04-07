package com.xptool.executor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Locale;
import java.util.Map;

final class ExecutionPayloadRedactor {
    private static final String REDACTED_VALUE = "***REDACTED***";
    private static final String[] SENSITIVE_KEY_TOKENS = new String[] {
        "password",
        "passwd",
        "secret",
        "token",
        "apikey",
        "authorization",
        "authtoken",
        "accesstoken",
        "refreshtoken",
        "username",
        "email",
        "accountname",
        "displayname",
        "playername"
    };

    JsonObject redact(JsonObject payload) {
        if (payload == null) {
            return null;
        }
        JsonElement redacted = redactElement(payload, null);
        if (redacted == null || !redacted.isJsonObject()) {
            return new JsonObject();
        }
        return redacted.getAsJsonObject();
    }

    private JsonElement redactElement(JsonElement value, String key) {
        if (value == null || value.isJsonNull()) {
            return value;
        }
        if (isSensitiveKey(key)) {
            return new JsonPrimitive(REDACTED_VALUE);
        }
        if (value.isJsonObject()) {
            JsonObject out = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
                out.add(entry.getKey(), redactElement(entry.getValue(), entry.getKey()));
            }
            return out;
        }
        if (value.isJsonArray()) {
            JsonArray out = new JsonArray();
            for (JsonElement child : value.getAsJsonArray()) {
                // Arrays do not provide key names, so child values inherit the current key context.
                out.add(redactElement(child, key));
            }
            return out;
        }
        return value.deepCopy();
    }

    private static boolean isSensitiveKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        String normalized = normalizeKey(key);
        if (normalized.isEmpty()) {
            return false;
        }
        for (String token : SENSITIVE_KEY_TOKENS) {
            if (normalized.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeKey(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char ch = lower.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                out.append(ch);
            }
        }
        return out.toString();
    }
}
