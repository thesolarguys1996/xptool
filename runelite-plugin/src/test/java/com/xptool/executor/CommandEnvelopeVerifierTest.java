package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CommandEnvelopeVerifierTest {
    private static final String PROP_ENABLED = "xptool.commandEnvelopeValidationEnabled";
    private static final String PROP_REQUIRE = "xptool.commandEnvelopeRequire";
    private static final String PROP_VERIFY_SIGNATURE = "xptool.commandEnvelopeVerifySignature";
    private static final String PROP_SIGNING_KEY = "xptool.commandEnvelopeSigningKey";
    private static final String PROP_REPLAY_WINDOW_SECONDS = "xptool.commandEnvelopeReplayWindowSeconds";

    @AfterEach
    void clearVerifierProperties() {
        System.clearProperty(PROP_ENABLED);
        System.clearProperty(PROP_REQUIRE);
        System.clearProperty(PROP_VERIFY_SIGNATURE);
        System.clearProperty(PROP_SIGNING_KEY);
        System.clearProperty(PROP_REPLAY_WINDOW_SECONDS);
    }

    @Test
    void disabledVerifierAcceptsMissingEnvelope() {
        System.setProperty(PROP_ENABLED, "false");
        CommandEnvelopeVerifier verifier = CommandEnvelopeVerifier.fromSystemProperties();
        CommandRow row = commandRow("LOGOUT_SAFE", new JsonObject());
        CommandEnvelopeVerifier.ValidationResult result = verifier.validate(row);
        assertTrue(result.isAccepted());
    }

    @Test
    void requiredEnvelopeRejectsWhenMissing() {
        System.setProperty(PROP_ENABLED, "true");
        System.setProperty(PROP_REQUIRE, "true");
        CommandEnvelopeVerifier verifier = CommandEnvelopeVerifier.fromSystemProperties();
        CommandRow row = commandRow("LOGOUT_SAFE", new JsonObject());
        CommandEnvelopeVerifier.ValidationResult result = verifier.validate(row);
        assertFalse(result.isAccepted());
    }

    @Test
    void rejectsReplayNonce() {
        System.setProperty(PROP_ENABLED, "true");
        System.setProperty(PROP_REQUIRE, "true");
        System.setProperty(PROP_REPLAY_WINDOW_SECONDS, "300");
        CommandEnvelopeVerifier verifier = CommandEnvelopeVerifier.fromSystemProperties();

        JsonObject commandPayload = new JsonObject();
        JsonObject envelopePayload = new JsonObject();
        commandPayload.add("commandEnvelope", envelope(
            "LOGOUT_SAFE",
            envelopePayload,
            "nonce-replay",
            System.currentTimeMillis(),
            "sess-1",
            ""
        ));
        CommandRow row = commandRow("LOGOUT_SAFE", commandPayload);

        CommandEnvelopeVerifier.ValidationResult first = verifier.validate(row);
        CommandEnvelopeVerifier.ValidationResult second = verifier.validate(row);
        assertTrue(first.isAccepted());
        assertFalse(second.isAccepted());
    }

    @Test
    void verifiesSignatureWhenEnabled() {
        System.setProperty(PROP_ENABLED, "true");
        System.setProperty(PROP_REQUIRE, "true");
        System.setProperty(PROP_VERIFY_SIGNATURE, "true");
        System.setProperty(PROP_SIGNING_KEY, "test-signing-secret");
        System.setProperty(PROP_REPLAY_WINDOW_SECONDS, "300");
        CommandEnvelopeVerifier verifier = CommandEnvelopeVerifier.fromSystemProperties();

        long issuedAt = System.currentTimeMillis();
        JsonObject envelopePayload = new JsonObject();
        String signature = sign(
            "COMMAND",
            "/v1/planner/decision",
            "{}".getBytes(StandardCharsets.UTF_8),
            issuedAt,
            "nonce-signed",
            "sess-2",
            "test-signing-secret"
        );
        JsonObject commandPayload = new JsonObject();
        commandPayload.add("commandEnvelope", envelope(
            "LOGOUT_SAFE",
            envelopePayload,
            "nonce-signed",
            issuedAt,
            "sess-2",
            signature
        ));
        CommandRow row = commandRow("LOGOUT_SAFE", commandPayload);

        CommandEnvelopeVerifier.ValidationResult result = verifier.validate(row);
        assertTrue(result.isAccepted());
    }

    @Test
    void rejectsBadSignatureWhenEnabled() {
        System.setProperty(PROP_ENABLED, "true");
        System.setProperty(PROP_REQUIRE, "true");
        System.setProperty(PROP_VERIFY_SIGNATURE, "true");
        System.setProperty(PROP_SIGNING_KEY, "test-signing-secret");
        System.setProperty(PROP_REPLAY_WINDOW_SECONDS, "300");
        CommandEnvelopeVerifier verifier = CommandEnvelopeVerifier.fromSystemProperties();

        long issuedAt = System.currentTimeMillis();
        JsonObject commandPayload = new JsonObject();
        commandPayload.add("commandEnvelope", envelope(
            "LOGOUT_SAFE",
            new JsonObject(),
            "nonce-bad-signature",
            issuedAt,
            "sess-3",
            "invalid-signature"
        ));
        CommandRow row = commandRow("LOGOUT_SAFE", commandPayload);

        CommandEnvelopeVerifier.ValidationResult result = verifier.validate(row);
        assertFalse(result.isAccepted());
    }

    private static CommandRow commandRow(String commandType, JsonObject commandPayload) {
        return new CommandRow(
            1,
            "xptool.test",
            UUID.randomUUID().toString(),
            System.currentTimeMillis(),
            commandType,
            commandPayload,
            "test"
        );
    }

    private static JsonObject envelope(
        String commandType,
        JsonObject payload,
        String nonce,
        long issuedAtUnixMs,
        String sessionId,
        String signatureBase64
    ) {
        JsonObject envelope = new JsonObject();
        envelope.addProperty("commandType", commandType);
        envelope.add("payload", payload == null ? new JsonObject() : payload);
        envelope.addProperty("nonce", nonce);
        envelope.addProperty("issuedAtUnixMs", issuedAtUnixMs);
        envelope.addProperty("sessionId", sessionId);
        envelope.addProperty("signatureBase64", signatureBase64);
        return envelope;
    }

    private static String sign(
        String method,
        String path,
        byte[] body,
        long timestampUnixMs,
        String nonce,
        String sessionId,
        String signingKey
    ) {
        String canonical = String.join(
            "\n",
            method,
            path,
            String.valueOf(timestampUnixMs),
            nonce,
            sessionId,
            sha256Hex(body)
        );
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception ex) {
            return "";
        }
    }

    private static String sha256Hex(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input == null ? new byte[0] : input);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }
}
