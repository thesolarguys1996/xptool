package com.xptool.executor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class CommandEnvelopeVerifier {
    private static final String PROPERTY_ENABLED = "xptool.commandEnvelopeValidationEnabled";
    private static final String PROPERTY_REQUIRE_ENVELOPE = "xptool.commandEnvelopeRequire";
    private static final String PROPERTY_VERIFY_SIGNATURE = "xptool.commandEnvelopeVerifySignature";
    private static final String PROPERTY_SIGNING_KEY = "xptool.commandEnvelopeSigningKey";
    private static final String PROPERTY_REPLAY_WINDOW_SECONDS = "xptool.commandEnvelopeReplayWindowSeconds";
    private static final String PROPERTY_CANONICAL_METHOD = "xptool.commandEnvelopeCanonicalMethod";
    private static final String PROPERTY_CANONICAL_PATH = "xptool.commandEnvelopeCanonicalPath";

    private static final String ENVELOPE_KEY = "commandEnvelope";
    private static final String DEFAULT_CANONICAL_METHOD = "COMMAND";
    private static final String DEFAULT_CANONICAL_PATH = "/v1/planner/decision";
    private static final long MIN_REPLAY_WINDOW_MS = 1000L;
    private static final long MAX_REPLAY_WINDOW_MS = 300000L;
    private static final int MAX_SEEN_NONCES = 8192;

    private final boolean enabled;
    private final boolean requireEnvelope;
    private final boolean verifySignature;
    private final String signingKey;
    private final long replayWindowMs;
    private final String canonicalMethod;
    private final String canonicalPath;
    private final LinkedHashMap<String, Long> seenNonces = new LinkedHashMap<>();

    private CommandEnvelopeVerifier(
        boolean enabled,
        boolean requireEnvelope,
        boolean verifySignature,
        String signingKey,
        long replayWindowMs,
        String canonicalMethod,
        String canonicalPath
    ) {
        this.enabled = enabled;
        this.requireEnvelope = requireEnvelope;
        this.verifySignature = verifySignature;
        this.signingKey = signingKey == null ? "" : signingKey;
        this.replayWindowMs = replayWindowMs;
        this.canonicalMethod = canonicalMethod == null || canonicalMethod.isBlank()
            ? DEFAULT_CANONICAL_METHOD
            : canonicalMethod.trim().toUpperCase(Locale.ROOT);
        this.canonicalPath = canonicalPath == null || canonicalPath.isBlank()
            ? DEFAULT_CANONICAL_PATH
            : canonicalPath.trim();
    }

    static CommandEnvelopeVerifier fromSystemProperties() {
        boolean enabled = readBoolean(PROPERTY_ENABLED, false);
        boolean requireEnvelope = readBoolean(PROPERTY_REQUIRE_ENVELOPE, false);
        boolean verifySignature = readBoolean(PROPERTY_VERIFY_SIGNATURE, false);
        String signingKey = safeString(System.getProperty(PROPERTY_SIGNING_KEY)).trim();
        long replayWindowMs = readReplayWindowMs(PROPERTY_REPLAY_WINDOW_SECONDS, 30.0d);
        String canonicalMethod = safeString(System.getProperty(PROPERTY_CANONICAL_METHOD));
        String canonicalPath = safeString(System.getProperty(PROPERTY_CANONICAL_PATH));
        return new CommandEnvelopeVerifier(
            enabled,
            requireEnvelope,
            verifySignature,
            signingKey,
            replayWindowMs,
            canonicalMethod,
            canonicalPath
        );
    }

    boolean isEnabled() {
        return enabled;
    }

    boolean isRequired() {
        return requireEnvelope;
    }

    boolean isSignatureVerificationEnabled() {
        return verifySignature;
    }

    boolean hasSigningKey() {
        return !signingKey.isBlank();
    }

    long replayWindowMs() {
        return replayWindowMs;
    }

    synchronized ValidationResult validate(CommandRow row) {
        if (!enabled) {
            return ValidationResult.accept();
        }
        JsonObject payload = row == null ? null : row.commandPayload;
        JsonObject envelope = extractEnvelope(payload);
        if (envelope == null) {
            if (requireEnvelope) {
                return ValidationResult.reject("command_envelope_missing");
            }
            return ValidationResult.accept();
        }

        JsonObject envelopePayload = asObject(envelope.get("payload"));
        if (envelopePayload == null) {
            return ValidationResult.reject("command_envelope_payload_missing");
        }
        String envelopeType = safeString(asString(envelope.get("commandType"))).trim().toUpperCase(Locale.ROOT);
        String commandType = safeString(row == null ? "" : row.commandType).trim().toUpperCase(Locale.ROOT);
        if (envelopeType.isBlank() || !envelopeType.equals(commandType)) {
            return ValidationResult.reject("command_envelope_command_type_mismatch");
        }

        String nonce = safeString(asString(envelope.get("nonce"))).trim();
        if (nonce.isBlank()) {
            return ValidationResult.reject("command_envelope_nonce_missing");
        }
        Long issuedAtUnixMs = asLong(envelope.get("issuedAtUnixMs"));
        if (issuedAtUnixMs == null) {
            return ValidationResult.reject("command_envelope_timestamp_invalid");
        }
        long now = System.currentTimeMillis();
        if (Math.abs(now - issuedAtUnixMs.longValue()) > replayWindowMs) {
            return ValidationResult.reject("command_envelope_timestamp_out_of_window");
        }
        pruneSeenNonces(now);
        if (seenNonces.containsKey(nonce)) {
            return ValidationResult.reject("command_envelope_replay_rejected");
        }
        seenNonces.put(nonce, issuedAtUnixMs.longValue());
        trimSeenNonces();

        JsonObject expectedPayload = payload == null ? new JsonObject() : payload.deepCopy();
        expectedPayload.remove(ENVELOPE_KEY);
        if (!canonicalJson(expectedPayload).equals(canonicalJson(envelopePayload))) {
            return ValidationResult.reject("command_envelope_payload_mismatch");
        }

        if (!verifySignature) {
            return ValidationResult.accept();
        }
        if (signingKey.isBlank()) {
            return ValidationResult.reject("command_envelope_signature_unconfigured");
        }
        String signature = safeString(asString(envelope.get("signatureBase64"))).trim();
        if (signature.isBlank()) {
            signature = safeString(asString(envelope.get("signature"))).trim();
        }
        if (signature.isBlank()) {
            return ValidationResult.reject("command_envelope_signature_missing");
        }
        String sessionId = safeString(asString(envelope.get("sessionId"))).trim();
        String expectedSignature = sign(
            canonicalMethod,
            canonicalPath,
            canonicalJsonBytes(envelopePayload),
            issuedAtUnixMs.longValue(),
            nonce,
            sessionId,
            signingKey
        );
        if (!constantTimeEquals(signature, expectedSignature)) {
            return ValidationResult.reject("command_envelope_signature_mismatch");
        }
        return ValidationResult.accept();
    }

    private static JsonObject extractEnvelope(JsonObject commandPayload) {
        if (commandPayload == null) {
            return null;
        }
        return asObject(commandPayload.get(ENVELOPE_KEY));
    }

    private static JsonObject asObject(JsonElement element) {
        if (element == null || element.isJsonNull() || !element.isJsonObject()) {
            return null;
        }
        return element.getAsJsonObject();
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

    private static Long asLong(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        try {
            return element.getAsLong();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean constantTimeEquals(String left, String right) {
        byte[] a = safeString(left).getBytes(StandardCharsets.UTF_8);
        byte[] b = safeString(right).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }

    private void pruneSeenNonces(long nowUnixMs) {
        long cutoff = nowUnixMs - replayWindowMs;
        Iterator<Map.Entry<String, Long>> it = seenNonces.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (entry.getValue() < cutoff) {
                it.remove();
            }
        }
    }

    private void trimSeenNonces() {
        while (seenNonces.size() > MAX_SEEN_NONCES) {
            Iterator<Map.Entry<String, Long>> it = seenNonces.entrySet().iterator();
            if (!it.hasNext()) {
                break;
            }
            it.next();
            it.remove();
        }
    }

    private static String sign(
        String method,
        String path,
        byte[] bodyBytes,
        long timestampUnixMs,
        String nonce,
        String sessionId,
        String signingKey
    ) {
        String canonical = String.join(
            "\n",
            safeString(method).trim().toUpperCase(Locale.ROOT),
            safeString(path).trim(),
            String.valueOf(timestampUnixMs),
            safeString(nonce).trim(),
            safeString(sessionId).trim(),
            sha256Hex(bodyBytes)
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

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data == null ? new byte[0] : data);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    private static byte[] canonicalJsonBytes(JsonElement element) {
        return canonicalJson(element).getBytes(StandardCharsets.UTF_8);
    }

    private static String canonicalJson(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "null";
        }
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            return primitive.toString();
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < array.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(canonicalJson(array.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        JsonObject object = element.getAsJsonObject();
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            keys.add(entry.getKey());
        }
        keys.sort(String::compareTo);
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            String key = keys.get(i);
            sb.append(new JsonPrimitive(key));
            sb.append(":");
            sb.append(canonicalJson(object.get(key)));
        }
        sb.append("}");
        return sb.toString();
    }

    private static boolean readBoolean(String propertyName, boolean defaultValue) {
        String raw = safeString(System.getProperty(propertyName)).trim();
        if (raw.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(raw);
    }

    private static long readReplayWindowMs(String propertyName, double defaultSeconds) {
        String raw = safeString(System.getProperty(propertyName)).trim();
        if (raw.isBlank()) {
            return secondsToWindowMs(defaultSeconds);
        }
        try {
            return secondsToWindowMs(Double.parseDouble(raw));
        } catch (NumberFormatException ex) {
            return secondsToWindowMs(defaultSeconds);
        }
    }

    private static long secondsToWindowMs(double seconds) {
        double bounded = Math.max(1.0d, Math.min(300.0d, seconds));
        long ms = (long) (bounded * 1000.0d);
        return Math.max(MIN_REPLAY_WINDOW_MS, Math.min(MAX_REPLAY_WINDOW_MS, ms));
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }

    static final class ValidationResult {
        private final boolean accepted;
        private final String reason;

        private ValidationResult(boolean accepted, String reason) {
            this.accepted = accepted;
            this.reason = reason == null ? "" : reason;
        }

        static ValidationResult accept() {
            return new ValidationResult(true, "command_envelope_accepted");
        }

        static ValidationResult reject(String reason) {
            return new ValidationResult(false, reason);
        }

        boolean isAccepted() {
            return accepted;
        }

        String reason() {
            return reason;
        }
    }
}
