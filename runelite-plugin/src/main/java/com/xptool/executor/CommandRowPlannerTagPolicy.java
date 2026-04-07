package com.xptool.executor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Locale;

final class CommandRowPlannerTagPolicy {
    private CommandRowPlannerTagPolicy() {
    }

    static String resolvePlannerTag(CommandRow row) {
        if (row == null) {
            return "";
        }
        JsonObject payload = row.commandPayload;
        if (payload == null || !payload.has("plannerTag")) {
            return "";
        }
        JsonElement plannerTagElement = payload.get("plannerTag");
        if (plannerTagElement == null || !plannerTagElement.isJsonPrimitive()) {
            return "";
        }
        String rawValue = plannerTagElement.getAsString();
        return rawValue == null ? "" : rawValue.trim().toLowerCase(Locale.ROOT);
    }
}
