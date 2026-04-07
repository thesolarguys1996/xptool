package com.xptool.bridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

final class BridgeDispatchConfigService {
    private final BridgeDispatchSettings dispatchSettings;

    BridgeDispatchConfigService(BridgeDispatchSettings dispatchSettings) {
        this.dispatchSettings = dispatchSettings;
    }

    JsonObject snapshot() {
        JsonObject payload = new JsonObject();
        payload.addProperty("ok", true);
        payload.addProperty("bridgeRuntimeEnabled", dispatchSettings.isBridgeRuntimeEnabled());
        payload.addProperty("liveDispatchEnabled", dispatchSettings.isLiveDispatchEnabled());
        payload.addProperty("commandAllowlistCsv", dispatchSettings.liveDispatchAllowlistCsv());
        payload.add("commandAllowlist", allowlistArray());
        return payload;
    }

    JsonObject apply(JsonObject request) {
        JsonObject safeRequest = request == null ? new JsonObject() : request;
        boolean updated = false;

        if (safeRequest.has("bridgeRuntimeEnabled")) {
            dispatchSettings.setBridgeRuntimeEnabled(safeRequest.get("bridgeRuntimeEnabled").getAsBoolean());
            updated = true;
        }

        if (safeRequest.has("liveDispatchEnabled")) {
            dispatchSettings.setLiveDispatchEnabled(safeRequest.get("liveDispatchEnabled").getAsBoolean());
            updated = true;
        }

        if (safeRequest.has("commandAllowlist")) {
            JsonElement element = safeRequest.get("commandAllowlist");
            if (!element.isJsonArray()) {
                throw new IllegalArgumentException("invalid_command_allowlist");
            }
            JsonArray array = element.getAsJsonArray();
            List<String> values = new ArrayList<>();
            for (JsonElement item : array) {
                if (item != null && item.isJsonPrimitive()) {
                    String raw = item.getAsString();
                    if (raw != null && !raw.isBlank()) {
                        values.add(raw);
                    }
                }
            }
            dispatchSettings.setLiveDispatchAllowlistFromIterable(values);
            updated = true;
        } else if (safeRequest.has("commandAllowlistCsv")) {
            dispatchSettings.setLiveDispatchAllowlistCsv(safeRequest.get("commandAllowlistCsv").getAsString());
            updated = true;
        }

        if (!updated) {
            throw new IllegalArgumentException("missing_config_fields");
        }

        JsonObject payload = snapshot();
        payload.addProperty("updated", true);
        return payload;
    }

    private JsonArray allowlistArray() {
        JsonArray array = new JsonArray();
        for (String token : dispatchSettings.liveDispatchAllowlistSnapshot()) {
            array.add(token);
        }
        return array;
    }
}
