package com.xptool.executor;

final class SkillTimingProfile {
    final long retryWindowMs;
    final long outcomeWaitWindowMs;
    final long targetReclickCooldownMs;

    SkillTimingProfile(long retryWindowMs, long outcomeWaitWindowMs, long targetReclickCooldownMs) {
        this.retryWindowMs = Math.max(0L, retryWindowMs);
        this.outcomeWaitWindowMs = Math.max(0L, outcomeWaitWindowMs);
        this.targetReclickCooldownMs = Math.max(0L, targetReclickCooldownMs);
    }
}
