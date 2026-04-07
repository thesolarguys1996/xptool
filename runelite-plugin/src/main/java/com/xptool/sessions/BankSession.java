package com.xptool.sessions;

import com.google.gson.JsonObject;
import com.xptool.executor.CommandExecutor;
import com.xptool.executor.SessionCommandFacade;
import com.xptool.motion.MotionProfile;

public final class BankSession {
    private final SessionCommandFacade commandFacade;

    public BankSession(SessionCommandFacade commandFacade) {
        this.commandFacade = commandFacade;
    }

    public boolean supports(String commandType) {
        return "OPEN_BANK".equals(commandType)
            || "BANK_OPEN_SAFE".equals(commandType)
            || "ENTER_BANK_PIN".equals(commandType)
            || "SEARCH_BANK_ITEM".equals(commandType)
            || "DEPOSIT_ITEM".equals(commandType)
            || "DEPOSIT_ALL_EXCEPT".equals(commandType)
            || "BANK_DEPOSIT_ALL_EXCEPT_TOOL_SAFE".equals(commandType)
            || "WITHDRAW_ITEM".equals(commandType)
            || "BANK_WITHDRAW_LOGS_SAFE".equals(commandType)
            || "CLOSE_BANK".equals(commandType);
    }

    public CommandExecutor.CommandDecision execute(String commandType, JsonObject payload, MotionProfile motionProfile) {
        switch (commandType) {
            case "OPEN_BANK":
            case "BANK_OPEN_SAFE":
                return commandFacade.executeOpenBank(payload, motionProfile);
            case "ENTER_BANK_PIN":
                return commandFacade.executeEnterBankPin(payload);
            case "SEARCH_BANK_ITEM":
                return commandFacade.executeSearchBankItem(payload);
            case "DEPOSIT_ITEM":
                return commandFacade.executeDepositItem(payload, motionProfile);
            case "DEPOSIT_ALL_EXCEPT":
                return commandFacade.executeDepositAllExcept(payload, motionProfile);
            case "BANK_DEPOSIT_ALL_EXCEPT_TOOL_SAFE":
                return commandFacade.executeDepositAllExcept(
                    commandFacade.normalizeDepositAllExceptPayload(payload),
                    motionProfile
                );
            case "WITHDRAW_ITEM":
                return commandFacade.executeWithdrawItem(payload, motionProfile);
            case "BANK_WITHDRAW_LOGS_SAFE":
                return commandFacade.executeWithdrawItemSafeWithdrawAll(payload, motionProfile);
            case "CLOSE_BANK":
                return commandFacade.executeCloseBank(motionProfile);
            default:
                return commandFacade.rejectUnsupportedCommandType();
        }
    }
}
