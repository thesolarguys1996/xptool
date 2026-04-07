package com.xptool.executor;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;

final class ExecutionTelemetryFileSink {
    private static final Gson GSON = new Gson();
    private static final String DEFAULT_TELEMETRY_FILENAME = "xptool-telemetry.ndjson";
    private static final long DEFAULT_ROLLUP_INTERVAL_MS = 7000L;
    private static final long MIN_ROLLUP_INTERVAL_MS = 2000L;
    private static final long MAX_ROLLUP_INTERVAL_MS = 60000L;
    private static final long IO_ERROR_LOG_THROTTLE_MS = 30000L;
    private static final String REASON_ROLLUP = "telemetry_rollup";
    private static final String EVENT_TYPE_ROLLUP = "TELEMETRY_ROLLUP";

    private final Logger log;
    private final boolean enabled;
    private final Path outputPath;
    private final long rollupIntervalMs;
    private final String sessionId;
    private final TelemetryRollupAccumulator rollup = new TelemetryRollupAccumulator();
    private long eventSeq = 0L;
    private long lastRollupAtMs = 0L;
    private long lastIoErrorAtMs = 0L;

    ExecutionTelemetryFileSink(Logger log) {
        this.log = log;
        this.enabled = ExecutionPrivacySettings.isTelemetryFileEnabled();
        this.outputPath = resolveTelemetryPath();
        this.rollupIntervalMs = resolveRollupIntervalMs();
        this.sessionId = buildSessionId();
    }

    synchronized void publish(JsonObject payload) {
        if (!enabled || payload == null) {
            return;
        }
        long now = System.currentTimeMillis();
        JsonObject enriched = payload.deepCopy();
        enrichEnvelope(enriched, now);
        writeEventLine(enriched);
        rollup.record(enriched, now);
        maybeEmitRollup(now);
    }

    synchronized void emitPendingRollup() {
        if (!enabled) {
            return;
        }
        long now = System.currentTimeMillis();
        JsonObject rollupEvent = rollup.snapshotAndReset(now);
        if (rollupEvent == null) {
            return;
        }
        enrichEnvelope(rollupEvent, now);
        writeEventLine(rollupEvent);
        lastRollupAtMs = now;
    }

    private void maybeEmitRollup(long now) {
        if ((now - lastRollupAtMs) < rollupIntervalMs) {
            return;
        }
        JsonObject rollupEvent = rollup.snapshotAndReset(now);
        if (rollupEvent == null) {
            lastRollupAtMs = now;
            return;
        }
        enrichEnvelope(rollupEvent, now);
        writeEventLine(rollupEvent);
        lastRollupAtMs = now;
    }

    private void enrichEnvelope(JsonObject payload, long now) {
        payload.addProperty("sessionId", sessionId);
        payload.addProperty("eventSeq", ++eventSeq);
        payload.addProperty("emittedAtMs", now);
    }

    private void writeEventLine(JsonObject payload) {
        if (payload == null) {
            return;
        }
        try {
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(
                outputPath,
                GSON.toJson(payload) + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (IOException ex) {
            maybeLogIoError(ex);
        }
    }

    private void maybeLogIoError(Exception ex) {
        long now = System.currentTimeMillis();
        if ((now - lastIoErrorAtMs) < IO_ERROR_LOG_THROTTLE_MS) {
            return;
        }
        lastIoErrorAtMs = now;
        if (log != null) {
            log.warn("xptool.telemetry_file_write_failed path={} reason={}", String.valueOf(outputPath), ex.toString());
        }
    }

    private static Path resolveTelemetryPath() {
        String configured = safeString(System.getProperty("xptool.telemetryFilePath", "")).trim();
        if (!configured.isEmpty()) {
            return Paths.get(configured);
        }
        String userHome = safeString(System.getProperty("user.home", "")).trim();
        if (userHome.isEmpty()) {
            return Paths.get(DEFAULT_TELEMETRY_FILENAME);
        }
        return Paths.get(userHome, ".runelite", "logs", DEFAULT_TELEMETRY_FILENAME);
    }

    private static long resolveRollupIntervalMs() {
        long raw = parseLongProperty("xptool.telemetryRollupIntervalMs", DEFAULT_ROLLUP_INTERVAL_MS);
        return Math.max(MIN_ROLLUP_INTERVAL_MS, Math.min(MAX_ROLLUP_INTERVAL_MS, raw));
    }

    private static long parseLongProperty(String propertyName, long fallback) {
        String raw = safeString(System.getProperty(propertyName, "")).trim();
        if (raw.isEmpty()) {
            return fallback;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String buildSessionId() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        if (suffix.length() > 12) {
            suffix = suffix.substring(0, 12);
        }
        return Long.toString(System.currentTimeMillis()) + "-" + suffix.toLowerCase(Locale.ROOT);
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }

    private static final class TelemetryRollupAccumulator {
        private long windowStartMs = 0L;
        private long events = 0L;
        private long deferredEvents = 0L;
        private long clickTelemetryEvents = 0L;
        private long clickLatencySamples = 0L;
        private long clickLatencySumMs = 0L;
        private long dropClicks = 0L;
        private long dropExactRepeatClicks = 0L;
        private long dropRepeatBlocked = 0L;
        private long idleCursorMoves = 0L;
        private long idleCameraMoves = 0L;
        private long idleRepeatAvoided = 0L;
        private long idlePatternDetected = 0L;

        void record(JsonObject payload, long nowMs) {
            if (payload == null) {
                return;
            }
            if (windowStartMs <= 0L) {
                windowStartMs = nowMs;
            }
            events++;
            String eventType = asString(payload, "eventType");
            if ("DEFERRED".equals(eventType)) {
                deferredEvents++;
            }

            String reason = asString(payload, "reason");
            JsonObject details = asObject(payload, "details");
            if ("interaction_click_telemetry".equals(reason)) {
                clickTelemetryEvents++;
                long clickAtMs = asLong(details, "clickAtMs", -1L);
                if (clickAtMs > 0L && nowMs >= clickAtMs) {
                    clickLatencySamples++;
                    clickLatencySumMs += (nowMs - clickAtMs);
                }
                if ("drop_sweep".equals(asString(details, "motorOwner"))) {
                    dropClicks++;
                    if (asBoolean(details, "repeatedExactPixelFromPrevious")) {
                        dropExactRepeatClicks++;
                    }
                }
                return;
            }
            if ("drop_repeat_blocked".equals(reason)) {
                dropRepeatBlocked += Math.max(1L, asLong(details, "dropRepeatBlockedCount", 1L));
                return;
            }
            if ("idle_hover_move".equals(reason)
                || "idle_drift_move".equals(reason)
                || "idle_hand_park_move".equals(reason)
                || "idle_fishing_offscreen_park_move".equals(reason)) {
                idleCursorMoves++;
                return;
            }
            if ("idle_camera_micro_adjust".equals(reason)) {
                idleCameraMoves++;
                return;
            }
            if (IdleActionPatternDetector.REASON_REPEAT_AVOIDED.equals(reason)) {
                idleRepeatAvoided++;
                return;
            }
            if (IdleActionPatternDetector.REASON_PATTERN_DETECTED.equals(reason)) {
                idlePatternDetected++;
            }
        }

        JsonObject snapshotAndReset(long nowMs) {
            if (events <= 0L) {
                return null;
            }
            JsonObject details = new JsonObject();
            details.addProperty("windowStartMs", windowStartMs);
            details.addProperty("windowEndMs", nowMs);
            details.addProperty("windowMs", Math.max(0L, nowMs - windowStartMs));
            details.addProperty("events", events);
            details.addProperty("deferredEvents", deferredEvents);
            details.addProperty("clickTelemetryEvents", clickTelemetryEvents);
            details.addProperty("avgClickTelemetryLatencyMs", averageClickLatencyMs());
            details.addProperty("dropClicks", dropClicks);
            details.addProperty("dropExactRepeatClicks", dropExactRepeatClicks);
            details.addProperty("dropRepeatBlocked", dropRepeatBlocked);
            details.addProperty("idleCursorMoves", idleCursorMoves);
            details.addProperty("idleCameraMoves", idleCameraMoves);
            details.addProperty("idleRepeatAvoided", idleRepeatAvoided);
            details.addProperty("idlePatternDetected", idlePatternDetected);

            JsonObject rollupEvent = new JsonObject();
            rollupEvent.addProperty("status", "executed");
            rollupEvent.addProperty("reason", REASON_ROLLUP);
            rollupEvent.addProperty("eventType", EVENT_TYPE_ROLLUP);
            rollupEvent.add("details", details);

            reset(nowMs);
            return rollupEvent;
        }

        private long averageClickLatencyMs() {
            if (clickLatencySamples <= 0L) {
                return -1L;
            }
            return Math.round((double) clickLatencySumMs / (double) clickLatencySamples);
        }

        private void reset(long nowMs) {
            windowStartMs = nowMs;
            events = 0L;
            deferredEvents = 0L;
            clickTelemetryEvents = 0L;
            clickLatencySamples = 0L;
            clickLatencySumMs = 0L;
            dropClicks = 0L;
            dropExactRepeatClicks = 0L;
            dropRepeatBlocked = 0L;
            idleCursorMoves = 0L;
            idleCameraMoves = 0L;
            idleRepeatAvoided = 0L;
            idlePatternDetected = 0L;
        }

        private static String asString(JsonObject object, String key) {
            if (object == null || key == null || key.isEmpty()) {
                return "";
            }
            JsonElement value = object.get(key);
            if (value == null || value.isJsonNull()) {
                return "";
            }
            try {
                return value.getAsString();
            } catch (Exception ignored) {
                return "";
            }
        }

        private static long asLong(JsonObject object, String key, long fallback) {
            if (object == null || key == null || key.isEmpty()) {
                return fallback;
            }
            JsonElement value = object.get(key);
            if (value == null || value.isJsonNull()) {
                return fallback;
            }
            try {
                return value.getAsLong();
            } catch (Exception ignored) {
                return fallback;
            }
        }

        private static boolean asBoolean(JsonObject object, String key) {
            if (object == null || key == null || key.isEmpty()) {
                return false;
            }
            JsonElement value = object.get(key);
            if (value == null || value.isJsonNull()) {
                return false;
            }
            try {
                return value.getAsBoolean();
            } catch (Exception ignored) {
                return false;
            }
        }

        private static JsonObject asObject(JsonObject object, String key) {
            if (object == null || key == null || key.isEmpty()) {
                return null;
            }
            JsonElement value = object.get(key);
            if (value == null || !value.isJsonObject()) {
                return null;
            }
            return value.getAsJsonObject();
        }
    }
}
