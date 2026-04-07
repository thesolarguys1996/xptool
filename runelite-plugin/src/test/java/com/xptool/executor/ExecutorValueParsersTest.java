package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

class ExecutorValueParsersTest {
    @Test
    void asIntReturnsFallbackForMissingOrInvalidValues() {
        JsonObject payload = new JsonObject();
        payload.addProperty("valid", 42);
        payload.addProperty("invalid", "abc");

        assertEquals(42, ExecutorValueParsers.asInt(payload.get("valid"), -1));
        assertEquals(-1, ExecutorValueParsers.asInt(payload.get("invalid"), -1));
        assertEquals(-1, ExecutorValueParsers.asInt(payload.get("missing"), -1));
    }

    @Test
    void detailsBuildsExpectedFields() {
        JsonObject details = ExecutorValueParsers.details(
            "count", 2,
            "ok", true,
            "name", "sample",
            "empty", null
        );

        assertEquals(2, details.get("count").getAsInt());
        assertTrue(details.get("ok").getAsBoolean());
        assertEquals("sample", details.get("name").getAsString());
        assertEquals("", details.get("empty").getAsString());
    }
}
