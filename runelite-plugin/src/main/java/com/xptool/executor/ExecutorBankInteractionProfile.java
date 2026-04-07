package com.xptool.executor;

final class ExecutorBankInteractionProfile {
    static final long BANK_SEARCH_KEY_MIN_DELAY_MS = 240L;
    static final long BANK_SEARCH_KEY_MAX_DELAY_MS = 430L;
    static final long BANK_SEARCH_KEY_HOLD_MIN_DELAY_MS = 42L;
    static final long BANK_SEARCH_KEY_HOLD_MAX_DELAY_MS = 88L;
    static final long BANK_MENU_RIGHT_CLICK_PRE_MAX_MS = 2L;
    static final long BANK_MENU_RIGHT_CLICK_POST_MAX_MS = 3L;
    static final double BANK_MENU_RIGHT_CLICK_REUSE_TOLERANCE_PX = 4.5;
    static final long RANDOM_EVENT_MOTOR_READY_WAIT_MAX_MS = 220L;
    static final long BANK_MOTOR_READY_WAIT_MAX_MS = 40L;

    private ExecutorBankInteractionProfile() {
    }
}
