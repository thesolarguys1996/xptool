package com.xptool.executor;

import net.runelite.api.TileObject;

final class MotorProgramLifecycleEngine {
    interface Host {
        boolean isTopMenuBankOnObject();

        boolean isTopMenuChopOnTree(TileObject targetObject);

        boolean isTopMenuMineOnRock(TileObject targetObject);

        boolean hasAttackEntryOnNpc();

        void reserveMotorCooldown(long ms);
    }

    private final Host host;

    MotorProgramLifecycleEngine(Host host) {
        this.host = host;
    }

    boolean validateMotorProgramMenu(MotorProgram program) {
        if (program == null || program.profile == null) {
            return false;
        }
        if (program.profile.menuValidationMode == MotorMenuValidationMode.NONE) {
            return true;
        }
        if (program.profile.menuValidationMode == MotorMenuValidationMode.BANK_TOP_OPTION) {
            return host.isTopMenuBankOnObject();
        }
        if (program.profile.menuValidationMode == MotorMenuValidationMode.CHOP_ON_TARGET) {
            return host.isTopMenuChopOnTree(program.profile.menuTargetObject);
        }
        if (program.profile.menuValidationMode == MotorMenuValidationMode.MINE_ON_TARGET) {
            return host.isTopMenuMineOnRock(program.profile.menuTargetObject);
        }
        if (program.profile.menuValidationMode == MotorMenuValidationMode.COMBAT_TOP_ATTACK_ON_NPC) {
            return host.hasAttackEntryOnNpc();
        }
        return false;
    }

    void completeMotorProgram(MotorProgram program, String reason) {
        if (program == null) {
            return;
        }
        host.reserveMotorCooldown(Math.max(1L, program.profile.postGestureCooldownMs));
        program.status = MotorGestureStatus.COMPLETE;
        program.resultReason = safeString(reason);
    }

    void cancelMotorProgram(MotorProgram program, String reason) {
        if (program == null) {
            return;
        }
        program.status = MotorGestureStatus.CANCELLED;
        program.resultReason = safeString(reason);
    }

    void failMotorProgram(MotorProgram program, String reason) {
        if (program == null) {
            return;
        }
        program.status = MotorGestureStatus.FAILED;
        program.resultReason = safeString(reason);
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}
