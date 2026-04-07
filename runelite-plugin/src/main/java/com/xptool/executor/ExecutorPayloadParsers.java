package com.xptool.executor;

import com.google.gson.JsonElement;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

final class ExecutorPayloadParsers {
    private ExecutorPayloadParsers() {
    }

    static Set<Integer> parseExcludeItemIds(JsonElement element) {
        Set<Integer> out = new HashSet<>();
        if (element == null || element.isJsonNull()) {
            return out;
        }
        try {
            if (element.isJsonArray()) {
                for (JsonElement entry : element.getAsJsonArray()) {
                    int id = asInt(entry, -1);
                    if (id > 0) {
                        out.add(id);
                    }
                }
                return out;
            }
            int id = asInt(element, -1);
            if (id > 0) {
                out.add(id);
            }
        } catch (Exception ignored) {
            // Keep parser tolerant to malformed payloads.
        }
        return out;
    }

    static Set<Integer> parsePreferredNpcIds(JsonElement targetNpcIdElement, JsonElement targetNpcIdsElement) {
        Set<Integer> out = new LinkedHashSet<>();
        appendNpcIds(out, targetNpcIdsElement);
        appendNpcIds(out, targetNpcIdElement);
        return out;
    }

    private static void appendNpcIds(Set<Integer> out, JsonElement element) {
        if (out == null || element == null || element.isJsonNull()) {
            return;
        }
        try {
            if (element.isJsonArray()) {
                for (JsonElement entry : element.getAsJsonArray()) {
                    appendNpcIds(out, entry);
                }
                return;
            }

            int numeric = asInt(element, -1);
            if (numeric > 0) {
                out.add(numeric);
                return;
            }

            String raw = asString(element).trim();
            if (raw.isEmpty()) {
                return;
            }
            String normalized = raw
                .replace('[', ' ')
                .replace(']', ' ')
                .replace(';', ',');
            String[] parts = normalized.split("[,\\s]+");
            for (String part : parts) {
                if (part == null || part.isBlank()) {
                    continue;
                }
                try {
                    int id = Integer.parseInt(part.trim());
                    if (id > 0) {
                        out.add(id);
                    }
                } catch (NumberFormatException ignored) {
                    // Keep parser tolerant to malformed payloads.
                }
            }
        } catch (Exception ignored) {
            // Keep parser tolerant to malformed payloads.
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
}
