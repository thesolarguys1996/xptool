package com.xptool.executor;

enum ResumePlannerState {
    IDLE,
    WAIT_LOGIN_SUCCESS,
    WAIT_GAME_READY,
    COOLDOWN,
    READY,
    FAILED
}
