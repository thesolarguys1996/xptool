package com.xptool.executor;

import com.google.gson.JsonObject;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.runelite.api.GameState;

final class ExecutorAccountRuntimeInputs {
    @FunctionalInterface
    interface KeyPressDispatcher {
        boolean dispatch(int keyCode, boolean holdShift, int holdMs);
    }

    private ExecutorAccountRuntimeInputs() {
    }

    static final class Inputs {
        Supplier<GameState> gameStateSupplier;
        ExecutorAccountRuntimeWiring.KeywordWidgetFinder findVisibleWidgetByKeywords;
        Predicate<String> isGameStateNamed;
        BooleanSupplier isPrimaryLoginSubmitPromptVisible;
        BooleanSupplier isSecondaryLoginSubmitPromptVisible;
        BooleanSupplier ensureTypingFocus;
        KeyPressDispatcher pressLoginKeyChord;
        Function<Object[], JsonObject> details;
        BooleanSupplier submitLogin;
        BooleanSupplier openWorldSelect;
        Supplier<LogoutInteractionController.AttemptStatus> attemptLogout;
        BooleanSupplier isGameReadyForRuntime;
        BooleanSupplier requestStopAllRuntime;
        BooleanSupplier requestLogoutForBreakStart;
        BooleanSupplier startLoginRuntime;
        BiConsumer<String, JsonObject> emitTypingEvent;
        BiConsumer<String, JsonObject> emitLoginEvent;
        BiConsumer<String, JsonObject> emitLogoutEvent;
        BiConsumer<String, JsonObject> emitResumeEvent;
        BiConsumer<String, JsonObject> emitBreakEvent;
    }
}
