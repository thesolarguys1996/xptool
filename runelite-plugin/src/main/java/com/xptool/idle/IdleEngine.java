package com.xptool.idle;

import com.xptool.models.Snapshot;
import com.xptool.motor.BaseMotorEngine;
import java.awt.event.KeyEvent;
import java.util.concurrent.ThreadLocalRandom;

public final class IdleEngine {
    private final BaseMotorEngine motorEngine;
    private long nextIdleAtMs = 0L;

    public IdleEngine(BaseMotorEngine motorEngine) {
        this.motorEngine = motorEngine;
    }

    public boolean run(Snapshot snapshot) {
        if (snapshot == null || !snapshot.loggedIn) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now < nextIdleAtMs) {
            return false;
        }
        boolean acted = executeIdleBehavior();
        long delayMs = ThreadLocalRandom.current().nextLong(500L, 1900L);
        nextIdleAtMs = now + delayMs;
        return acted;
    }

    private boolean executeIdleBehavior() {
        int width = motorEngine.canvasWidth();
        int height = motorEngine.canvasHeight();
        if (width <= 0 || height <= 0) {
            return false;
        }
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < 55) {
            int x = ThreadLocalRandom.current().nextInt((int) (width * 0.30), (int) (width * 0.74));
            int y = ThreadLocalRandom.current().nextInt((int) (height * 0.28), (int) (height * 0.78));
            return motorEngine.moveMouse(x, y);
        }
        if (roll < 82) {
            int x = ThreadLocalRandom.current().nextInt(Math.max(8, width - 230), Math.max(9, width - 20));
            int y = ThreadLocalRandom.current().nextInt(Math.max(8, height - 325), Math.max(9, height - 30));
            return motorEngine.moveMouse(x, y);
        }
        int[] keys = {KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN};
        int key = keys[ThreadLocalRandom.current().nextInt(keys.length)];
        return motorEngine.pressKey(key);
    }
}
