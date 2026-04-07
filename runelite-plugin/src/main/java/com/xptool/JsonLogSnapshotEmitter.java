package com.xptool;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JsonLogSnapshotEmitter implements SnapshotEmitter {
    private static final Logger LOG = LoggerFactory.getLogger(JsonLogSnapshotEmitter.class);
    private static final Gson GSON = new Gson();

    @Override
    public void emit(GameStateSnapshot snapshot) {
        LOG.info("xptool.snapshot {}", GSON.toJson(snapshot));
    }
}
