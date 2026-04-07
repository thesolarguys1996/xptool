package com.xptool.executor.activity;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ActivityRuntimeRegistry {
    private final Map<String, ActivityRuntime> byKey;

    private ActivityRuntimeRegistry(Map<String, ActivityRuntime> byKey) {
        this.byKey = Collections.unmodifiableMap(new LinkedHashMap<>(byKey));
    }

    public static ActivityRuntimeRegistry of(ActivityRuntime... runtimes) {
        Map<String, ActivityRuntime> byKey = new LinkedHashMap<>();
        if (runtimes == null) {
            return new ActivityRuntimeRegistry(byKey);
        }
        for (ActivityRuntime runtime : runtimes) {
            if (runtime == null) {
                continue;
            }
            String key = normalizeKey(runtime.activityKey());
            if (key.isEmpty()) {
                throw new IllegalArgumentException("Activity runtime key is required");
            }
            ActivityRuntime existing = byKey.putIfAbsent(key, runtime);
            if (existing != null && existing != runtime) {
                throw new IllegalArgumentException("Duplicate activity runtime key: " + key);
            }
        }
        return new ActivityRuntimeRegistry(byKey);
    }

    public <T extends ActivityRuntime> T require(String key, Class<T> runtimeType) {
        String normalizedKey = normalizeKey(key);
        ActivityRuntime runtime = byKey.get(normalizedKey);
        if (runtime == null) {
            throw new IllegalStateException("Missing activity runtime for key: " + normalizedKey);
        }
        if (!runtimeType.isInstance(runtime)) {
            throw new IllegalStateException(
                "Activity runtime key '"
                    + normalizedKey
                    + "' is not assignable to "
                    + runtimeType.getSimpleName()
            );
        }
        return runtimeType.cast(runtime);
    }

    private static String normalizeKey(String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    }
}
