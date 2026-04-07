package com.xptool.sessions;

import com.google.gson.JsonObject;
import com.xptool.executor.CommandExecutor;
import com.xptool.executor.DropCommandService;
import com.xptool.motion.MotionProfile;

public final class DropSession {
    private static final String START_DROP_SESSION = "DROP_START_SESSION";
    private static final String STOP_DROP_SESSION = "DROP_STOP_SESSION";
    private static final String DROP_ITEM_SAFE = "DROP_ITEM_SAFE";
    private static final String LEGACY_START_DROP_SESSION = "WOODCUT_START_DROP_SESSION";
    private static final String LEGACY_STOP_DROP_SESSION = "WOODCUT_STOP_DROP_SESSION";
    private static final String LEGACY_DROP_ITEM_SAFE = "WOODCUT_DROP_ITEM_SAFE";

    private final DropCommandService dropCommandService;

    public DropSession(DropCommandService dropCommandService) {
        this.dropCommandService = dropCommandService;
    }

    public boolean supports(String commandType) {
        return START_DROP_SESSION.equals(commandType)
            || STOP_DROP_SESSION.equals(commandType)
            || DROP_ITEM_SAFE.equals(commandType)
            || LEGACY_START_DROP_SESSION.equals(commandType)
            || LEGACY_STOP_DROP_SESSION.equals(commandType)
            || LEGACY_DROP_ITEM_SAFE.equals(commandType);
    }

    public CommandExecutor.CommandDecision execute(String commandType, JsonObject payload, MotionProfile motionProfile) {
        switch (commandType) {
            case START_DROP_SESSION:
            case LEGACY_START_DROP_SESSION:
                return dropCommandService.executeStartDropSession(payload);
            case STOP_DROP_SESSION:
            case LEGACY_STOP_DROP_SESSION:
                return dropCommandService.executeStopDropSession(payload);
            case DROP_ITEM_SAFE:
            case LEGACY_DROP_ITEM_SAFE:
                return dropCommandService.executeDropItem(payload);
            default:
                return dropCommandService.rejectUnsupportedCommandType();
        }
    }

    public void onGameTick(int tick) {
        dropCommandService.onGameTick(tick);
    }
}
