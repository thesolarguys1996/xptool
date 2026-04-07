package com.xptool.executor;

import com.xptool.motion.MotionProfile;

final class ExecutorMotorProfileCatalog {
    static final String MOTOR_OWNER_INTERACTION = "interaction";
    static final String MOTOR_OWNER_IDLE = "idle";
    static final String MOTOR_OWNER_BANK = "bank";
    static final String SESSION_DROP_SWEEP = "drop_sweep";

    static final long MOTOR_LEASE_INTERACTION_MS = 900L;
    static final long MOTOR_LEASE_IDLE_MS = 320L;
    static final long MOTOR_LEASE_BANK_MS = 260L;
    static final long MOTOR_LEASE_DROP_SWEEP_MS = 260L;
    static final long MOTOR_PROGRAM_MIN_LEASE_MS = 1800L;

    static final String CLICK_TYPE_NONE = "none";
    static final String CLICK_TYPE_WOODCUT_WORLD = "woodcut_world_interaction";
    static final String CLICK_TYPE_FISHING_WORLD = "fishing_world_interaction";
    static final String CLICK_TYPE_NON_WORLD = "non_world_interaction";
    private static final int WOODCUT_MOTOR_MAX_STEPS_PER_TICK = 3;
    private static final int MOTOR_PROGRAM_MAX_STEPS_PER_TICK = 3;
    private static final int DROP_MOTOR_MAX_STEPS_PER_TICK = 3;
    private static final int MOTOR_PROGRAM_HOVER_SETTLE_TICKS = 1;
    private static final int MOTOR_PROGRAM_MAX_MENU_VALIDATION_TICKS = 2;

    static final MoveAndClickMotorProfileSpec BANK_MOTOR_PROFILE_SPEC =
        new MoveAndClickMotorProfileSpec(
            MOTOR_OWNER_BANK,
            CLICK_TYPE_NON_WORLD,
            true,
            MotionProfile.GENERIC_INTERACT,
            MotorMenuValidationMode.NONE,
            MOTOR_PROGRAM_HOVER_SETTLE_TICKS,
            MOTOR_PROGRAM_MAX_MENU_VALIDATION_TICKS,
            MOTOR_PROGRAM_MAX_STEPS_PER_TICK
        );

    static final MoveAndClickMotorProfileSpec WOODCUT_MOTOR_PROFILE_SPEC =
        new MoveAndClickMotorProfileSpec(
            MOTOR_OWNER_INTERACTION,
            CLICK_TYPE_WOODCUT_WORLD,
            false,
            MotionProfile.WOODCUT,
            MotorMenuValidationMode.CHOP_ON_TARGET,
            MOTOR_PROGRAM_HOVER_SETTLE_TICKS,
            MOTOR_PROGRAM_MAX_MENU_VALIDATION_TICKS,
            WOODCUT_MOTOR_MAX_STEPS_PER_TICK
        );

    static final MoveAndClickMotorProfileSpec MINING_MOTOR_PROFILE_SPEC =
        new MoveAndClickMotorProfileSpec(
            MOTOR_OWNER_INTERACTION,
            CLICK_TYPE_WOODCUT_WORLD,
            false,
            MotionProfile.MINING,
            MotorMenuValidationMode.MINE_ON_TARGET,
            MOTOR_PROGRAM_HOVER_SETTLE_TICKS,
            MOTOR_PROGRAM_MAX_MENU_VALIDATION_TICKS,
            MOTOR_PROGRAM_MAX_STEPS_PER_TICK
        );

    static final MoveAndClickMotorProfileSpec FISHING_MOTOR_PROFILE_SPEC =
        new MoveAndClickMotorProfileSpec(
            MOTOR_OWNER_INTERACTION,
            CLICK_TYPE_FISHING_WORLD,
            false,
            MotionProfile.FISHING,
            MotorMenuValidationMode.NONE,
            MOTOR_PROGRAM_HOVER_SETTLE_TICKS,
            MOTOR_PROGRAM_MAX_MENU_VALIDATION_TICKS,
            MOTOR_PROGRAM_MAX_STEPS_PER_TICK
        );

    static final MoveAndClickMotorProfileSpec AGILITY_MOTOR_PROFILE_SPEC =
        new MoveAndClickMotorProfileSpec(
            MOTOR_OWNER_INTERACTION,
            CLICK_TYPE_WOODCUT_WORLD,
            false,
            MotionProfile.FISHING,
            MotorMenuValidationMode.NONE,
            MOTOR_PROGRAM_HOVER_SETTLE_TICKS,
            MOTOR_PROGRAM_MAX_MENU_VALIDATION_TICKS,
            MOTOR_PROGRAM_MAX_STEPS_PER_TICK
        );

    static final MoveAndClickMotorProfileSpec WALK_MOTOR_PROFILE_SPEC =
        new MoveAndClickMotorProfileSpec(
            MOTOR_OWNER_INTERACTION,
            CLICK_TYPE_WOODCUT_WORLD,
            false,
            MotionProfile.GENERIC_INTERACT,
            MotorMenuValidationMode.NONE,
            MOTOR_PROGRAM_HOVER_SETTLE_TICKS,
            MOTOR_PROGRAM_MAX_MENU_VALIDATION_TICKS,
            MOTOR_PROGRAM_MAX_STEPS_PER_TICK
        );

    static final MoveAndClickMotorProfileSpec COMBAT_MOTOR_PROFILE_SPEC =
        new MoveAndClickMotorProfileSpec(
            MOTOR_OWNER_INTERACTION,
            CLICK_TYPE_WOODCUT_WORLD,
            false,
            MotionProfile.COMBAT,
            MotorMenuValidationMode.COMBAT_TOP_ATTACK_ON_NPC,
            MOTOR_PROGRAM_HOVER_SETTLE_TICKS,
            MOTOR_PROGRAM_MAX_MENU_VALIDATION_TICKS,
            MOTOR_PROGRAM_MAX_STEPS_PER_TICK
        );

    static final MoveAndClickMotorProfileSpec COMBAT_DODGE_MOTOR_PROFILE_SPEC =
        new MoveAndClickMotorProfileSpec(
            MOTOR_OWNER_INTERACTION,
            CLICK_TYPE_WOODCUT_WORLD,
            false,
            MotionProfile.COMBAT,
            MotorMenuValidationMode.NONE,
            MOTOR_PROGRAM_HOVER_SETTLE_TICKS,
            MOTOR_PROGRAM_MAX_MENU_VALIDATION_TICKS,
            MOTOR_PROGRAM_MAX_STEPS_PER_TICK
        );

    static final MoveOnlyMotorProfileSpec DROP_MOTOR_PROFILE_SPEC =
        new MoveOnlyMotorProfileSpec(
            SESSION_DROP_SWEEP,
            CLICK_TYPE_NONE,
            MotionProfile.DROP,
            MotorMenuValidationMode.NONE,
            DROP_MOTOR_MAX_STEPS_PER_TICK,
            1L
        );

    private ExecutorMotorProfileCatalog() {
    }
}
