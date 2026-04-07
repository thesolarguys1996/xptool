package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

class ExecutionPayloadRedactorTest {
    private final ExecutionPayloadRedactor redactor = new ExecutionPayloadRedactor();

    @Test
    void redactsSensitiveKeysRecursively() {
        JsonObject payload = new JsonObject();
        payload.addProperty("status", "executed");
        payload.addProperty("password", "top-secret");
        payload.addProperty("api_key", "abc123");
        payload.addProperty("username", "player-one");

        JsonObject details = new JsonObject();
        details.addProperty("clickType", "WOODCUT");
        details.addProperty("authToken", "token-value");
        details.addProperty("sessionSecret", "session-secret-value");
        details.addProperty("playerName", "display-name");
        details.addProperty("emailAddress", "player@example.com");

        JsonArray commands = new JsonArray();
        JsonObject command = new JsonObject();
        command.addProperty("commandType", "LOGIN_CREDENTIALS");
        command.addProperty("passWordHint", "sensitive");
        commands.add(command);
        details.add("commands", commands);

        payload.add("details", details);

        JsonObject redacted = redactor.redact(payload);

        assertEquals("***REDACTED***", redacted.get("password").getAsString());
        assertEquals("***REDACTED***", redacted.get("api_key").getAsString());
        assertEquals("***REDACTED***", redacted.get("username").getAsString());
        assertEquals("WOODCUT", redacted.getAsJsonObject("details").get("clickType").getAsString());
        assertEquals("***REDACTED***", redacted.getAsJsonObject("details").get("authToken").getAsString());
        assertEquals("***REDACTED***", redacted.getAsJsonObject("details").get("sessionSecret").getAsString());
        assertEquals("***REDACTED***", redacted.getAsJsonObject("details").get("playerName").getAsString());
        assertEquals("***REDACTED***", redacted.getAsJsonObject("details").get("emailAddress").getAsString());
        assertEquals(
            "***REDACTED***",
            redacted.getAsJsonObject("details")
                .getAsJsonArray("commands")
                .get(0)
                .getAsJsonObject()
                .get("passWordHint")
                .getAsString()
        );
    }

    @Test
    void doesNotMutateOriginalPayload() {
        JsonObject payload = new JsonObject();
        payload.addProperty("password", "do-not-change");

        JsonObject redacted = redactor.redact(payload);

        assertNotSame(payload, redacted);
        assertEquals("do-not-change", payload.get("password").getAsString());
        assertEquals("***REDACTED***", redacted.get("password").getAsString());
    }
}
