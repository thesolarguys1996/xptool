package com.xptool.activities;

import com.xptool.models.Snapshot;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ActivityEngine {
    private final Map<String, Activity> activitiesByName = new HashMap<>();
    private String activeActivityName;

    public ActivityEngine(List<Activity> activities, String activeActivityName) {
        if (activities != null) {
            for (Activity activity : activities) {
                if (activity == null) {
                    continue;
                }
                activitiesByName.put(normalize(activity.name()), activity);
            }
        }
        this.activeActivityName = normalize(activeActivityName);
    }

    public boolean hasActiveActivity() {
        return activitiesByName.containsKey(activeActivityName);
    }

    public String activeActivityName() {
        return activeActivityName;
    }

    public void setActiveActivity(String activityName) {
        this.activeActivityName = normalize(activityName);
    }

    public boolean run(Snapshot snapshot) {
        Activity activity = activitiesByName.get(activeActivityName);
        return activity != null && activity.run(snapshot);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
