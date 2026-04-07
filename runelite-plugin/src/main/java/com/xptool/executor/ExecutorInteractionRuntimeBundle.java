package com.xptool.executor;

import com.xptool.systems.TargetSelectionEngine;

final class ExecutorInteractionRuntimeBundle {
    final TargetSelectionEngine targetSelectionEngine;
    final TargetPointVariationEngine targetPointVariationEngine;
    final InventorySlotInteractionController inventorySlotInteractionController;
    final InventorySlotPointPlanner inventorySlotPointPlanner;
    final FocusMenuInteractionController focusMenuInteractionController;
    final InteractionClickEngine interactionClickEngine;
    final BankMenuInteractionController bankMenuInteractionController;
    final IdleCursorTargetPlanner idleCursorTargetPlanner;
    final IdleOffscreenMoveEngine idleOffscreenMoveEngine;
    final CameraMotionService cameraMotionService;

    ExecutorInteractionRuntimeBundle(
        TargetSelectionEngine targetSelectionEngine,
        TargetPointVariationEngine targetPointVariationEngine,
        InventorySlotInteractionController inventorySlotInteractionController,
        InventorySlotPointPlanner inventorySlotPointPlanner,
        FocusMenuInteractionController focusMenuInteractionController,
        InteractionClickEngine interactionClickEngine,
        BankMenuInteractionController bankMenuInteractionController,
        IdleCursorTargetPlanner idleCursorTargetPlanner,
        IdleOffscreenMoveEngine idleOffscreenMoveEngine,
        CameraMotionService cameraMotionService
    ) {
        this.targetSelectionEngine = targetSelectionEngine;
        this.targetPointVariationEngine = targetPointVariationEngine;
        this.inventorySlotInteractionController = inventorySlotInteractionController;
        this.inventorySlotPointPlanner = inventorySlotPointPlanner;
        this.focusMenuInteractionController = focusMenuInteractionController;
        this.interactionClickEngine = interactionClickEngine;
        this.bankMenuInteractionController = bankMenuInteractionController;
        this.idleCursorTargetPlanner = idleCursorTargetPlanner;
        this.idleOffscreenMoveEngine = idleOffscreenMoveEngine;
        this.cameraMotionService = cameraMotionService;
    }
}
