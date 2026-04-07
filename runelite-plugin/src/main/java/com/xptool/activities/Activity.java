package com.xptool.activities;

import com.xptool.models.Snapshot;

public interface Activity {
    String name();

    boolean run(Snapshot snapshot);
}
