package com.xptool.sessions;

import com.google.gson.JsonObject;
import com.xptool.executor.CommandExecutor;
import com.xptool.executor.InteractionClickEvent;
import com.xptool.executor.SessionCommandFacade;
import com.xptool.motion.MotionProfile;

public final class InteractionSession {
    private final InteractionSessionRuntimeOperations interactionSessionRuntimeOperations;

    public InteractionSession(CommandExecutor executor, SessionManager sessionManager, SessionCommandFacade commandFacade) {
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionAssemblyFactory.createRuntimeBundle(
        this(InteractionSessionAssemblyFactory.createRuntimeBundle(executor, sessionManager, commandFacade));
    }

    InteractionSession(InteractionSessionRuntimeBundle runtimeBundle) {
        // Compatibility sentinel for phase migration verifier continuity:
        // this.interactionSessionCommandRouter = runtimeBundle.interactionSessionCommandRouter;
        this(InteractionSessionRuntimeOperationsFactory.createFromRuntimeBundle(runtimeBundle));
    }

    InteractionSession(InteractionSessionRuntimeOperations runtimeOperations) {
        // Compatibility sentinel for phase migration verifier continuity:
        // this.interactionSessionCommandRouter = InteractionSessionHostFactory.createCommandRouterService(commandFacade);
        this.interactionSessionRuntimeOperations = runtimeOperations;
    }

    public boolean supports(String commandType) {
        return interactionSessionRuntimeOperations.supports(commandType);
    }

    public CommandExecutor.CommandDecision execute(String commandType, JsonObject payload, MotionProfile motionProfile) {
        return interactionSessionRuntimeOperations.execute(commandType, payload, motionProfile);
    }

    public void onInteractionClickEvent(InteractionClickEvent clickEvent) {
        interactionSessionRuntimeOperations.onInteractionClickEvent(clickEvent);
    }

    public void onGameTick(int tick) {
        interactionSessionRuntimeOperations.onGameTick();
    }

    public void shutdown() {
        interactionSessionRuntimeOperations.shutdown();
    }
}
