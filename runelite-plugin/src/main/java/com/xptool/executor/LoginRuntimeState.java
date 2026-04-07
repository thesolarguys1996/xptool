package com.xptool.executor;

enum LoginRuntimeState {
    IDLE,
    DETECT_SCREEN,
    FOCUS_USERNAME,
    TYPE_USERNAME,
    FOCUS_PASSWORD,
    TYPE_PASSWORD,
    SUBMIT,
    WAIT_RESULT,
    WORLD_SELECT,
    SUCCESS,
    FAILED_RETRY_COOLDOWN,
    FAILED_HARD_STOP
}
