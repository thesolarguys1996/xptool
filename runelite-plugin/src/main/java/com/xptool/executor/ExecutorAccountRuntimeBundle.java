package com.xptool.executor;

final class ExecutorAccountRuntimeBundle {
    final HumanTypingEngine humanTypingEngine;
    final LoginRuntime loginRuntime;
    final LogoutRuntime logoutRuntime;
    final ResumePlanner resumePlanner;
    final BreakRuntime breakRuntime;

    ExecutorAccountRuntimeBundle(
        HumanTypingEngine humanTypingEngine,
        LoginRuntime loginRuntime,
        LogoutRuntime logoutRuntime,
        ResumePlanner resumePlanner,
        BreakRuntime breakRuntime
    ) {
        this.humanTypingEngine = humanTypingEngine;
        this.loginRuntime = loginRuntime;
        this.logoutRuntime = logoutRuntime;
        this.resumePlanner = resumePlanner;
        this.breakRuntime = breakRuntime;
    }
}
