package com.xptool.systems;

import com.xptool.models.Snapshot;
import com.xptool.motor.BaseMotorEngine;
import java.awt.event.KeyEvent;

public final class AutoLoginSystem implements RuntimeSystem {
    private final BaseMotorEngine motorEngine;

    public AutoLoginSystem(BaseMotorEngine motorEngine) {
        this.motorEngine = motorEngine;
    }

    @Override
    public String name() {
        return "AutoLoginSystem";
    }

    @Override
    public boolean shouldRun(Snapshot snapshot) {
        return snapshot != null && !snapshot.loggedIn;
    }

    @Override
    public boolean run(Snapshot snapshot) {
        if (!shouldRun(snapshot)) {
            return false;
        }
        return motorEngine.pressKey(KeyEvent.VK_ENTER);
    }
}

