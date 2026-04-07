package com.xptool.sessions;

import com.google.gson.JsonObject;
import com.xptool.executor.CommandExecutor;
import com.xptool.executor.InteractionClickEvent;
import com.xptool.motion.MotionProfile;

final class InteractionSessionRuntimeOperations {
    private final InteractionSessionCommandRouter interactionSessionCommandRouter;
    private final InteractionSessionClickEventService interactionSessionClickEventService;
    private final InteractionSessionOwnershipService interactionSessionOwnershipService;
    private final InteractionSessionShutdownService interactionSessionShutdownService;

    InteractionSessionRuntimeOperations(
        InteractionSessionCommandRouter interactionSessionCommandRouter,
        InteractionSessionClickEventService interactionSessionClickEventService,
        InteractionSessionOwnershipService interactionSessionOwnershipService,
        InteractionSessionShutdownService interactionSessionShutdownService
    ) {
        this.interactionSessionCommandRouter = interactionSessionCommandRouter;
        this.interactionSessionClickEventService = interactionSessionClickEventService;
        this.interactionSessionOwnershipService = interactionSessionOwnershipService;
        this.interactionSessionShutdownService = interactionSessionShutdownService;
    }

    boolean supports(String commandType) {
        return interactionSessionCommandRouter.supports(commandType);
    }

    CommandExecutor.CommandDecision execute(String commandType, JsonObject payload, MotionProfile motionProfile) {
        return interactionSessionCommandRouter.execute(commandType, payload, motionProfile);
    }

    void onInteractionClickEvent(InteractionClickEvent clickEvent) {
        interactionSessionClickEventService.onInteractionClickEvent(clickEvent);
    }

    void onGameTick() {
        interactionSessionOwnershipService.onGameTick();
    }

    void shutdown() {
        interactionSessionShutdownService.shutdown();
    }
}
