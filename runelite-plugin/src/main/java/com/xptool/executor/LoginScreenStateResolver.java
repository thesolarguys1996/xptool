package com.xptool.executor;

final class LoginScreenStateResolver {
    interface Host {
        boolean isLoggedIn();
        boolean isLoginFormVisible();
        boolean isWorldSelectVisible();
        boolean isAuthenticatorPromptVisible();
        boolean isDisconnectedDialogVisible();
        boolean isLoginErrorVisible();
    }

    private final Host host;

    LoginScreenStateResolver(Host host) {
        this.host = host;
    }

    LoginScreenState detect() {
        if (host == null) {
            return LoginScreenState.UNKNOWN;
        }
        if (host.isLoggedIn()) {
            return LoginScreenState.LOGGED_IN;
        }
        if (host.isWorldSelectVisible()) {
            return LoginScreenState.WORLD_SELECT;
        }
        if (host.isAuthenticatorPromptVisible()) {
            return LoginScreenState.AUTHENTICATOR;
        }
        if (host.isDisconnectedDialogVisible()) {
            return LoginScreenState.DISCONNECTED;
        }
        if (host.isLoginErrorVisible()) {
            return LoginScreenState.ERROR;
        }
        if (host.isLoginFormVisible()) {
            return LoginScreenState.LOGIN_FORM;
        }
        return LoginScreenState.UNKNOWN;
    }
}
