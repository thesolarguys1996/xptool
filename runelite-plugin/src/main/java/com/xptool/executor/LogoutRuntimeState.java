package com.xptool.executor;

enum LogoutRuntimeState {
    IDLE,
    ATTEMPT,
    WAIT_RESULT,
    RETRY_COOLDOWN,
    SUCCESS,
    FAILED_HARD_STOP
}
