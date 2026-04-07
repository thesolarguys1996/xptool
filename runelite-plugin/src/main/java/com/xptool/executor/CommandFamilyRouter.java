package com.xptool.executor;

import com.google.gson.JsonObject;
import com.xptool.motion.MotionProfile;
import com.xptool.sessions.BankSession;
import com.xptool.sessions.DropSession;
import com.xptool.sessions.InteractionSession;
import java.util.function.Supplier;

final class CommandFamilyRouter {
    private CommandFamilyRouter() {
    }

    static CommandExecutor.CommandDecision route(
        String commandType,
        JsonObject commandPayload,
        MotionProfile motionProfile,
        BankSession bankSession,
        DropSession dropSession,
        InteractionSession interactionSession,
        Supplier<CommandExecutor.CommandDecision> unsupported
    ) {
        if (bankSession.supports(commandType)) {
            return bankSession.execute(commandType, commandPayload, motionProfile);
        }
        if (dropSession.supports(commandType)) {
            return dropSession.execute(commandType, commandPayload, motionProfile);
        }
        if (interactionSession.supports(commandType)) {
            return interactionSession.execute(commandType, commandPayload, motionProfile);
        }
        return unsupported.get();
    }
}
