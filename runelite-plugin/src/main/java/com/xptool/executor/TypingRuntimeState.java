package com.xptool.executor;

enum TypingRuntimeState {
    IDLE,
    PRE_FOCUS_SETTLE,
    TYPE_STREAM,
    TYPO_CORRECTION,
    POST_FIELD_HESITATE,
    DONE,
    CANCELLED
}
