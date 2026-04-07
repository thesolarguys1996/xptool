package com.xptool.activities.fishing;

import com.google.gson.JsonObject;
import com.xptool.core.runtime.FatigueSnapshot;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import java.util.Set;
import net.runelite.api.Player;

public final class FishingExecutionContext {
    public final JsonObject payload;
    public final ClickMotionSettings motion;
    public final long now;
    public final FatigueSnapshot fatigue;
    public final Player local;
    public final Set<Integer> preferredNpcIds;
    public final long lastDispatchAtMs;
    public final long sinceLastDispatchMs;
    public final int lastDispatchNpcIndex;
    public final int noAnimationRetryStreak;
    public final boolean levelUpFastRefishActive;

    public FishingExecutionContext(
        JsonObject payload,
        ClickMotionSettings motion,
        long now,
        FatigueSnapshot fatigue,
        Player local,
        Set<Integer> preferredNpcIds,
        long lastDispatchAtMs,
        long sinceLastDispatchMs,
        int lastDispatchNpcIndex,
        int noAnimationRetryStreak,
        boolean levelUpFastRefishActive
    ) {
        this.payload = payload;
        this.motion = motion;
        this.now = now;
        this.fatigue = fatigue;
        this.local = local;
        this.preferredNpcIds = preferredNpcIds;
        this.lastDispatchAtMs = lastDispatchAtMs;
        this.sinceLastDispatchMs = sinceLastDispatchMs;
        this.lastDispatchNpcIndex = lastDispatchNpcIndex;
        this.noAnimationRetryStreak = noAnimationRetryStreak;
        this.levelUpFastRefishActive = levelUpFastRefishActive;
    }
}
