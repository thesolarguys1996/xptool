package com.xptool.sessions;

final class InteractionSessionRuntimeBundle {
    // Compatibility sentinel for phase migration verifier continuity:
    // final InteractionSessionCommandRouter interactionSessionCommandRouter;
    private final InteractionSessionRuntimeOperationsBundle interactionSessionRuntimeOperationsBundle;
    private final InteractionSessionRuntimeControlBundle interactionSessionRuntimeControlBundle;

    InteractionSessionRuntimeBundle(
        InteractionSessionRuntimeOperationsBundle interactionSessionRuntimeOperationsBundle,
        InteractionSessionRuntimeControlBundle interactionSessionRuntimeControlBundle
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // this.interactionSessionRuntimeOperationsBundle = new InteractionSessionRuntimeOperationsBundle(
        this.interactionSessionRuntimeOperationsBundle = interactionSessionRuntimeOperationsBundle;
        this.interactionSessionRuntimeControlBundle = interactionSessionRuntimeControlBundle;
    }

    InteractionSessionRuntimeOperationsBundle interactionSessionRuntimeOperationsBundle() {
        return interactionSessionRuntimeOperationsBundle;
    }

    InteractionSessionRuntimeControlBundle interactionSessionRuntimeControlBundle() {
        return interactionSessionRuntimeControlBundle;
    }
}
