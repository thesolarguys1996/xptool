package com.xptool.activities.fishing;

import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;

final class FishingHoldDebugTelemetry {
    private static final class ReasonStats {
        long count = 0L;
        long totalWaitMs = 0L;
        long maxWaitMs = 0L;
    }

    private final Map<String, ReasonStats> reasonStatsByKey = new HashMap<>();
    private long totalHoldCount = 0L;
    private long totalHoldWaitMs = 0L;
    private long maxSingleHoldWaitMs = 0L;
    private long consecutiveHoldCount = 0L;
    private long holdEpisodeStartedAtMs = 0L;
    private long lastHoldAtMs = 0L;

    JsonObject decorateHold(JsonObject details, String reason, long nowMs, long waitMs) {
        JsonObject out = details == null ? new JsonObject() : details;
        String key = safeReason(reason);
        long normalizedNowMs = Math.max(0L, nowMs);
        long normalizedWaitMs = Math.max(0L, waitMs);

        if (consecutiveHoldCount <= 0L) {
            holdEpisodeStartedAtMs = normalizedNowMs;
        }
        consecutiveHoldCount++;
        lastHoldAtMs = normalizedNowMs;
        totalHoldCount++;
        totalHoldWaitMs += normalizedWaitMs;
        maxSingleHoldWaitMs = Math.max(maxSingleHoldWaitMs, normalizedWaitMs);

        ReasonStats reasonStats = reasonStatsByKey.computeIfAbsent(key, unused -> new ReasonStats());
        reasonStats.count++;
        reasonStats.totalWaitMs += normalizedWaitMs;
        reasonStats.maxWaitMs = Math.max(reasonStats.maxWaitMs, normalizedWaitMs);

        TopReason top = computeTopReason();
        out.addProperty("holdDebugActive", true);
        out.addProperty("holdDebugReasonKey", key);
        out.addProperty("holdDebugWaitMs", normalizedWaitMs);
        out.addProperty("holdDebugReasonCount", reasonStats.count);
        out.addProperty("holdDebugReasonTotalWaitMs", reasonStats.totalWaitMs);
        out.addProperty("holdDebugReasonMaxWaitMs", reasonStats.maxWaitMs);
        out.addProperty("holdDebugConsecutiveCount", consecutiveHoldCount);
        out.addProperty(
            "holdDebugEpisodeElapsedMs",
            holdEpisodeStartedAtMs <= 0L ? 0L : Math.max(0L, normalizedNowMs - holdEpisodeStartedAtMs)
        );
        out.addProperty("holdDebugLastHoldAtMs", lastHoldAtMs);
        out.addProperty("holdDebugTotalCount", totalHoldCount);
        out.addProperty("holdDebugTotalWaitMs", totalHoldWaitMs);
        out.addProperty("holdDebugMaxSingleHoldWaitMs", maxSingleHoldWaitMs);
        out.addProperty("holdDebugTopReasonKey", top.reasonKey);
        out.addProperty("holdDebugTopReasonTotalWaitMs", top.totalWaitMs);
        out.addProperty("holdDebugTopReasonCount", top.count);
        return out;
    }

    JsonObject decorateProgress(JsonObject details, String progressReason, long nowMs) {
        JsonObject out = details == null ? new JsonObject() : details;
        long normalizedNowMs = Math.max(0L, nowMs);
        TopReason top = computeTopReason();

        out.addProperty("holdDebugActive", false);
        out.addProperty("holdDebugProgressReason", safeReason(progressReason));
        out.addProperty("holdDebugTotalCount", totalHoldCount);
        out.addProperty("holdDebugTotalWaitMs", totalHoldWaitMs);
        out.addProperty("holdDebugMaxSingleHoldWaitMs", maxSingleHoldWaitMs);
        out.addProperty("holdDebugTopReasonKey", top.reasonKey);
        out.addProperty("holdDebugTopReasonTotalWaitMs", top.totalWaitMs);
        out.addProperty("holdDebugTopReasonCount", top.count);
        out.addProperty("holdDebugConsecutiveCount", consecutiveHoldCount);
        out.addProperty(
            "holdDebugEpisodeElapsedMs",
            holdEpisodeStartedAtMs <= 0L ? 0L : Math.max(0L, normalizedNowMs - holdEpisodeStartedAtMs)
        );
        out.addProperty("holdDebugLastHoldAtMs", lastHoldAtMs);
        out.addProperty("holdDebugRecoveredFromHold", consecutiveHoldCount > 0L);
        if (consecutiveHoldCount > 0L) {
            out.addProperty("holdDebugRecoveredConsecutiveCount", consecutiveHoldCount);
        }

        consecutiveHoldCount = 0L;
        holdEpisodeStartedAtMs = 0L;
        return out;
    }

    private TopReason computeTopReason() {
        String bestReason = "";
        long bestWaitMs = 0L;
        long bestCount = 0L;
        for (Map.Entry<String, ReasonStats> entry : reasonStatsByKey.entrySet()) {
            String reason = entry.getKey();
            ReasonStats stats = entry.getValue();
            if (stats == null) {
                continue;
            }
            if (stats.totalWaitMs > bestWaitMs) {
                bestReason = reason;
                bestWaitMs = stats.totalWaitMs;
                bestCount = stats.count;
            }
        }
        return new TopReason(bestReason, bestWaitMs, bestCount);
    }

    private static String safeReason(String value) {
        return value == null ? "" : value;
    }

    private static final class TopReason {
        final String reasonKey;
        final long totalWaitMs;
        final long count;

        TopReason(String reasonKey, long totalWaitMs, long count) {
            this.reasonKey = reasonKey == null ? "" : reasonKey;
            this.totalWaitMs = Math.max(0L, totalWaitMs);
            this.count = Math.max(0L, count);
        }
    }
}
