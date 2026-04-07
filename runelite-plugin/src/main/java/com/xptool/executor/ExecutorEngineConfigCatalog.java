package com.xptool.executor;

import com.xptool.systems.TargetSelectionEngine;

final class ExecutorEngineConfigCatalog {
    static final double PENDING_MOVE_ARRIVAL_TOLERANCE_PX = 2.0;
    static final double PENDING_MOVE_TARGET_MATCH_TOLERANCE_PX = 16.0;
    static final long CURSOR_MOTOR_STATE_STALE_MS = 2200L;
    static final LoginSubmitTargetPlanner.Config LOGIN_SUBMIT_PRIMARY_TARGET_PLANNER_CONFIG =
        new LoginSubmitTargetPlanner.Config(
            ExecutorLoginUiProfile.LOGIN_SUBMIT_PRIMARY_REGION_LEFT_RATIO,
            ExecutorLoginUiProfile.LOGIN_SUBMIT_PRIMARY_REGION_TOP_RATIO,
            ExecutorLoginUiProfile.LOGIN_SUBMIT_PRIMARY_REGION_WIDTH_RATIO,
            ExecutorLoginUiProfile.LOGIN_SUBMIT_PRIMARY_REGION_HEIGHT_RATIO,
            ExecutorLoginUiProfile.LOGIN_SUBMIT_REGION_INSET_PX,
            ExecutorLoginUiProfile.LOGIN_SUBMIT_REGION_MAX_ATTEMPTS,
            ExecutorLoginUiProfile.LOGIN_SUBMIT_REGION_HISTORY_SIZE,
            ExecutorLoginUiProfile.LOGIN_SUBMIT_REGION_REPEAT_EXCLUSION_PX
        );
    static final LoginSubmitTargetPlanner.Config LOGIN_SUBMIT_SECONDARY_TARGET_PLANNER_CONFIG =
        new LoginSubmitTargetPlanner.Config(
            ExecutorLoginUiProfile.LOGIN_SUBMIT_SECONDARY_REGION_LEFT_RATIO,
            ExecutorLoginUiProfile.LOGIN_SUBMIT_SECONDARY_REGION_TOP_RATIO,
            ExecutorLoginUiProfile.LOGIN_SUBMIT_SECONDARY_REGION_WIDTH_RATIO,
            ExecutorLoginUiProfile.LOGIN_SUBMIT_SECONDARY_REGION_HEIGHT_RATIO,
            ExecutorLoginUiProfile.LOGIN_SUBMIT_REGION_INSET_PX,
            ExecutorLoginUiProfile.LOGIN_SUBMIT_REGION_MAX_ATTEMPTS,
            ExecutorLoginUiProfile.LOGIN_SUBMIT_REGION_HISTORY_SIZE,
            ExecutorLoginUiProfile.LOGIN_SUBMIT_REGION_REPEAT_EXCLUSION_PX
        );
    static final TargetSelectionEngine.Config TARGET_SELECTION_CONFIG =
        new TargetSelectionEngine.Config(
            1.0,
            0.018,
            3,
            2,
            8,
            0.55,
            0.35,
            0.12,
            0.85,
            0.20
        );
    static final TargetPointVariationEngine.Config TARGET_POINT_VARIATION_CONFIG =
        new TargetPointVariationEngine.Config(
            true,
            96,
            512,
            12,
            28,
            2048,
            2.0
        );
    static final InventorySlotInteractionController.Config INVENTORY_SLOT_INTERACTION_CONFIG =
        new InventorySlotInteractionController.Config(
            0,
            48,
            20L,
            16L,
            4,
            6
        );
    static final InventorySlotPointPlanner.Config INVENTORY_SLOT_POINT_PLANNER_CONFIG =
        new InventorySlotPointPlanner.Config(1, 5, 24);
    static final FocusMenuInteractionController.Config FOCUS_MENU_INTERACTION_CONFIG =
        new FocusMenuInteractionController.Config(10);
    static final BankMenuInteractionController.Config BANK_MENU_INTERACTION_CONFIG =
        new BankMenuInteractionController.Config(
            ExecutorBankInteractionProfile.BANK_MENU_RIGHT_CLICK_PRE_MAX_MS,
            ExecutorBankInteractionProfile.BANK_MENU_RIGHT_CLICK_POST_MAX_MS,
            28,
            1,
            3,
            3,
            20,
            85,
            1,
            4,
            2,
            10,
            1,
            10,
            2,
            8,
            14,
            8,
            19,
            15,
            7
        );

    private ExecutorEngineConfigCatalog() {
    }

    static InteractionClickEngine.Config createInteractionClickEngineConfig(
        boolean visualCursorMotionEnabled,
        boolean humanizedTimingEnabled
    ) {
        return new InteractionClickEngine.Config(
            visualCursorMotionEnabled,
            humanizedTimingEnabled,
            7,
            ExecutorBankInteractionProfile.BANK_MOTOR_READY_WAIT_MAX_MS,
            ExecutorBankInteractionProfile.BANK_MENU_RIGHT_CLICK_REUSE_TOLERANCE_PX,
            3,
            4,
            4,
            6,
            1
        );
    }

    static IdleOffscreenMoveEngine.Config createIdleOffscreenMoveConfig(boolean humanizedTimingEnabled) {
        return new IdleOffscreenMoveEngine.Config(
            ExecutorMotorProfileCatalog.MOTOR_OWNER_IDLE,
            1.5,
            14,
            42,
            humanizedTimingEnabled
        );
    }

    static MotorCanvasMoveEngine.Config createMotorCanvasMoveEngineConfig(boolean humanizedTimingEnabled) {
        return new MotorCanvasMoveEngine.Config(
            1.5,
            10,
            26,
            4,
            5,
            8,
            humanizedTimingEnabled
        );
    }

    static MotorProgramMoveEngine.Config createMotorProgramMoveEngineConfig(boolean humanizedTimingEnabled) {
        return new MotorProgramMoveEngine.Config(
            PENDING_MOVE_TARGET_MATCH_TOLERANCE_PX,
            1.5,
            10,
            26,
            10,
            26,
            0.96,
            1.30,
            1.10,
            1.34,
            0.05,
            0.10,
            0.28,
            humanizedTimingEnabled,
            ExecutorMotorProfileCatalog.MOTOR_OWNER_IDLE,
            ExecutorMotorProfileCatalog.MOTOR_OWNER_BANK,
            0.10,
            2.0,
            14.0,
            4,
            44,
            28,
            3,
            0.09,
            1.0,
            8.0
        );
    }

    static MotorProgramClickEngine.Config createMotorProgramClickEngineConfig(boolean humanizedTimingEnabled) {
        return new MotorProgramClickEngine.Config(
            4,
            6,
            ExecutorMotorProfileCatalog.CLICK_TYPE_WOODCUT_WORLD,
            humanizedTimingEnabled
        );
    }
}
