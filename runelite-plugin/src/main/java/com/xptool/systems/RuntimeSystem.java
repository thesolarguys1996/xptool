package com.xptool.systems;

import com.xptool.models.Snapshot;

public interface RuntimeSystem {
    String name();

    boolean shouldRun(Snapshot snapshot);

    boolean run(Snapshot snapshot);
}

