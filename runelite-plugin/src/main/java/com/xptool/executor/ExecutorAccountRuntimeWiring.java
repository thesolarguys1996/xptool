package com.xptool.executor;

import com.google.gson.JsonObject;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.runelite.api.GameState;
import net.runelite.api.widgets.Widget;

final class ExecutorAccountRuntimeWiring {
    @FunctionalInterface
    interface KeywordWidgetFinder {
        Optional<Widget> find(String... keywords);
    }

    private ExecutorAccountRuntimeWiring() {
    }

    static ExecutorAccountRuntimeBundle createBundle(ExecutorAccountRuntimeInputs.Inputs inputs) {
        return createBundle(
            inputs.gameStateSupplier,
            inputs.findVisibleWidgetByKeywords,
            inputs.isGameStateNamed,
            inputs.isPrimaryLoginSubmitPromptVisible,
            inputs.isSecondaryLoginSubmitPromptVisible,
            inputs.ensureTypingFocus,
            inputs.pressLoginKeyChord,
            inputs.details,
            inputs.submitLogin,
            inputs.openWorldSelect,
            inputs.attemptLogout,
            inputs.isGameReadyForRuntime,
            inputs.requestStopAllRuntime,
            inputs.requestLogoutForBreakStart,
            inputs.startLoginRuntime,
            inputs.emitTypingEvent,
            inputs.emitLoginEvent,
            inputs.emitLogoutEvent,
            inputs.emitResumeEvent,
            inputs.emitBreakEvent
        );
    }

    static ExecutorAccountRuntimeBundle createBundle(
        Supplier<GameState> gameStateSupplier,
        KeywordWidgetFinder findVisibleWidgetByKeywords,
        Predicate<String> isGameStateNamed,
        BooleanSupplier isPrimaryLoginSubmitPromptVisible,
        BooleanSupplier isSecondaryLoginSubmitPromptVisible,
        BooleanSupplier ensureTypingFocus,
        ExecutorAccountRuntimeInputs.KeyPressDispatcher pressLoginKeyChord,
        Function<Object[], JsonObject> details,
        BooleanSupplier submitLogin,
        BooleanSupplier openWorldSelect,
        Supplier<LogoutInteractionController.AttemptStatus> attemptLogout,
        BooleanSupplier isGameReadyForRuntime,
        BooleanSupplier requestStopAllRuntime,
        BooleanSupplier requestLogoutForBreakStart,
        BooleanSupplier startLoginRuntime,
        BiConsumer<String, JsonObject> emitTypingEvent,
        BiConsumer<String, JsonObject> emitLoginEvent,
        BiConsumer<String, JsonObject> emitLogoutEvent,
        BiConsumer<String, JsonObject> emitResumeEvent,
        BiConsumer<String, JsonObject> emitBreakEvent
    ) {
        BooleanSupplier isLoggedIn = () -> gameStateSupplier.get() == GameState.LOGGED_IN;

        LoginScreenStateResolver loginScreenStateResolver = new LoginScreenStateResolver(
            new LoginScreenStateResolver.Host() {
                @Override
                public boolean isLoggedIn() {
                    return isLoggedIn.getAsBoolean();
                }

                @Override
                public boolean isLoginFormVisible() {
                    GameState gameState = gameStateSupplier.get();
                    if (gameState == GameState.LOGIN_SCREEN || gameState == GameState.LOGGING_IN) {
                        return true;
                    }
                    return findVisibleWidgetByKeywords.find(
                        "existing user",
                        "new user",
                        "log in",
                        "play now"
                    ).isPresent();
                }

                @Override
                public boolean isWorldSelectVisible() {
                    if (isGameStateNamed.test("WORLD")) {
                        return true;
                    }
                    return findVisibleWidgetByKeywords.find(
                        "select a world",
                        "switch world",
                        "members world",
                        "free world"
                    ).isPresent();
                }

                @Override
                public boolean isAuthenticatorPromptVisible() {
                    return false;
                }

                @Override
                public boolean isDisconnectedDialogVisible() {
                    if (isGameStateNamed.test("CONNECTION") || isGameStateNamed.test("LOST")) {
                        return true;
                    }
                    return findVisibleWidgetByKeywords.find(
                        "connection lost",
                        "unable to connect",
                        "try again",
                        "server offline"
                    ).isPresent();
                }

                @Override
                public boolean isLoginErrorVisible() {
                    return findVisibleWidgetByKeywords.find(
                        "invalid credentials",
                        "incorrect",
                        "too many login attempts",
                        "error",
                        "try again"
                    ).isPresent();
                }
            }
        );

        HumanTypingEngine humanTypingEngine = new HumanTypingEngine(
            new HumanTypingEngine.Host() {
                @Override
                public long nowMs() {
                    return System.currentTimeMillis();
                }

                @Override
                public boolean ensureTypingFocus() {
                    return ensureTypingFocus.getAsBoolean();
                }

                @Override
                public boolean pressKey(int keyCode, boolean holdShift, int holdMs) {
                    return pressLoginKeyChord.dispatch(keyCode, holdShift, holdMs);
                }

                @Override
                public JsonObject details(Object... kvPairs) {
                    return details.apply(kvPairs);
                }

                @Override
                public void emitTypingEvent(String reason, JsonObject detailsJson) {
                    emitTypingEvent.accept(reason, detailsJson);
                }
            }
        );

        LoginRuntime loginRuntime = new LoginRuntime(
            new LoginRuntime.Host() {
                @Override
                public boolean isLoggedIn() {
                    return isLoggedIn.getAsBoolean();
                }

                @Override
                public boolean isPrimarySubmitPromptVisible() {
                    return isPrimaryLoginSubmitPromptVisible.getAsBoolean();
                }

                @Override
                public boolean isSecondarySubmitPromptVisible() {
                    return isSecondaryLoginSubmitPromptVisible.getAsBoolean();
                }

                @Override
                public boolean focusUsernameField() {
                    return true;
                }

                @Override
                public boolean focusPasswordField() {
                    return true;
                }

                @Override
                public boolean submitLogin() {
                    return submitLogin.getAsBoolean();
                }

                @Override
                public boolean openWorldSelect() {
                    return openWorldSelect.getAsBoolean();
                }

                @Override
                public JsonObject details(Object... kvPairs) {
                    return details.apply(kvPairs);
                }

                @Override
                public void emitLoginEvent(String reason, JsonObject detailsJson) {
                    emitLoginEvent.accept(reason, detailsJson);
                }
            },
            loginScreenStateResolver,
            humanTypingEngine
        );

        LogoutRuntime logoutRuntime = new LogoutRuntime(
            new LogoutRuntime.Host() {
                @Override
                public boolean isLoggedIn() {
                    return isLoggedIn.getAsBoolean();
                }

                @Override
                public LogoutInteractionController.AttemptStatus attemptLogout() {
                    return attemptLogout.get();
                }

                @Override
                public JsonObject details(Object... kvPairs) {
                    return details.apply(kvPairs);
                }

                @Override
                public void emitLogoutEvent(String reason, JsonObject detailsJson) {
                    emitLogoutEvent.accept(reason, detailsJson);
                }
            }
        );

        ResumePlanner resumePlanner = new ResumePlanner(
            new ResumePlanner.Host() {
                @Override
                public boolean isGameReadyForRuntime() {
                    return isGameReadyForRuntime.getAsBoolean();
                }

                @Override
                public JsonObject details(Object... kvPairs) {
                    return details.apply(kvPairs);
                }

                @Override
                public void emitResumeEvent(String reason, JsonObject detailsJson) {
                    emitResumeEvent.accept(reason, detailsJson);
                }
            }
        );

        BreakRuntime breakRuntime = new BreakRuntime(
            new BreakRuntime.Host() {
                @Override
                public boolean isLoggedIn() {
                    return isLoggedIn.getAsBoolean();
                }

                @Override
                public boolean requestStopAllRuntime() {
                    return requestStopAllRuntime.getAsBoolean();
                }

                @Override
                public boolean requestLogout() {
                    return requestLogoutForBreakStart.getAsBoolean();
                }

                @Override
                public boolean requestLoginStart() {
                    if (loginRuntime.isSuccessful()) {
                        return true;
                    }
                    return startLoginRuntime.getAsBoolean();
                }

                @Override
                public JsonObject details(Object... kvPairs) {
                    return details.apply(kvPairs);
                }

                @Override
                public void emitBreakEvent(String reason, JsonObject detailsJson) {
                    emitBreakEvent.accept(reason, detailsJson);
                }
            },
            loginRuntime,
            resumePlanner
        );

        return new ExecutorAccountRuntimeBundle(
            humanTypingEngine,
            loginRuntime,
            logoutRuntime,
            resumePlanner,
            breakRuntime
        );
    }
}
