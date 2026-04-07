package com.xptool.executor;

import com.xptool.activities.fishing.FishingRuntime;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FishingRuntimeTest {
    @Test
    void preserveClearKeepsDispatchSignal() throws Exception {
        FishingRuntime runtime = new FishingRuntime();
        setLong(runtime, "retryWindowUntilMs", 10_000L);
        setLong(runtime, "outcomeWaitUntilMs", 9_000L);
        setInt(runtime, "lastAttemptNpcIndex", 77);
        setLong(runtime, "approachWaitUntilMs", 8_000L);
        setInt(runtime, "lastDispatchNpcIndex", 42);
        setLong(runtime, "lastDispatchAtMs", 7_000L);

        runtime.clearInteractionWindowsPreserveDispatchSignal();

        assertEquals(0L, runtime.retryWindowUntilMs());
        assertEquals(0L, runtime.outcomeWaitUntilMs());
        assertEquals(-1, runtime.lastAttemptNpcIndex());
        assertEquals(0L, runtime.approachWaitUntilMs());
        assertEquals(42, runtime.lastDispatchNpcIndex());
        assertEquals(7_000L, runtime.lastDispatchAtMs());
    }

    @Test
    void fullClearAlsoResetsDispatchSignal() throws Exception {
        FishingRuntime runtime = new FishingRuntime();
        setLong(runtime, "retryWindowUntilMs", 10_000L);
        setLong(runtime, "outcomeWaitUntilMs", 9_000L);
        setInt(runtime, "lastAttemptNpcIndex", 77);
        setLong(runtime, "approachWaitUntilMs", 8_000L);
        setInt(runtime, "lastDispatchNpcIndex", 42);
        setLong(runtime, "lastDispatchAtMs", 7_000L);

        runtime.clearInteractionWindows();

        assertEquals(0L, runtime.retryWindowUntilMs());
        assertEquals(0L, runtime.outcomeWaitUntilMs());
        assertEquals(-1, runtime.lastAttemptNpcIndex());
        assertEquals(0L, runtime.approachWaitUntilMs());
        assertEquals(-1, runtime.lastDispatchNpcIndex());
        assertEquals(0L, runtime.lastDispatchAtMs());
    }

    private static void setLong(Object target, String fieldName, long value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.setLong(target, value);
    }

    private static void setInt(Object target, String fieldName, int value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.setInt(target, value);
    }
}
