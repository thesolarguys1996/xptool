package com.xptool.executor;

import com.google.gson.JsonObject;
import java.awt.event.KeyEvent;
import java.util.concurrent.ThreadLocalRandom;

final class HumanTypingEngine {
    interface Host {
        long nowMs();
        boolean ensureTypingFocus();
        boolean pressKey(int keyCode, boolean holdShift, int holdMs);
        JsonObject details(Object... kvPairs);
        void emitTypingEvent(String reason, JsonObject details);
    }

    private final Host host;
    private TypingRuntimeState state = TypingRuntimeState.IDLE;
    private String pendingText = "";
    private TypingProfile profile = TypingProfile.defaults();
    private int nextCharIndex = 0;
    private long nextActionAtMs = 0L;
    private boolean correctionBackspacePending = false;
    private char correctionTargetChar = '\0';
    private int currentTick = Integer.MIN_VALUE;

    HumanTypingEngine(Host host) {
        this.host = host;
    }

    void startTyping(String text, TypingProfile typingProfile) {
        this.pendingText = text == null ? "" : text;
        this.profile = typingProfile == null ? TypingProfile.defaults() : typingProfile;
        this.state = TypingRuntimeState.PRE_FOCUS_SETTLE;
        this.nextCharIndex = 0;
        this.nextActionAtMs = nowMs() + Math.max(20L, sampleDelayMs(64, 0.24));
        this.correctionBackspacePending = false;
        this.correctionTargetChar = '\0';
        emit("typing_start");
    }

    void cancel() {
        if (!isTyping()) {
            return;
        }
        state = TypingRuntimeState.CANCELLED;
        emit("typing_cancelled");
        clear();
    }

    void onGameTick(int tick) {
        currentTick = tick;
    }

    boolean onClientTick(int tick) {
        currentTick = tick;
        return onPulse();
    }

    boolean isTyping() {
        return state != TypingRuntimeState.IDLE;
    }

    TypingRuntimeState state() {
        return state;
    }

    String pendingText() {
        return pendingText;
    }

    TypingProfile profile() {
        return profile;
    }

    private void clear() {
        state = TypingRuntimeState.IDLE;
        pendingText = "";
        nextCharIndex = 0;
        nextActionAtMs = 0L;
        correctionBackspacePending = false;
        correctionTargetChar = '\0';
    }

    private boolean onPulse() {
        if (!isTyping()) {
            return false;
        }
        long now = nowMs();
        if (now < nextActionAtMs) {
            return false;
        }
        switch (state) {
            case PRE_FOCUS_SETTLE:
                if (!host.ensureTypingFocus()) {
                    nextActionAtMs = now + Math.max(40L, sampleDelayMs(profile.baseKeyDelayMs(), 0.20));
                    emit("typing_focus_retry");
                    return false;
                }
                state = TypingRuntimeState.TYPE_STREAM;
                nextActionAtMs = now + Math.max(20L, sampleDelayMs(profile.baseKeyDelayMs(), 0.22));
                emit("typing_focus_settle");
                return true;
            case TYPE_STREAM:
                if (nextCharIndex >= pendingText.length()) {
                    state = TypingRuntimeState.POST_FIELD_HESITATE;
                    nextActionAtMs = now + Math.max(30L, samplePostFieldDelayMs());
                    emit("typing_field_complete");
                    return true;
                }
                return typeNextCharacter(now);
            case TYPO_CORRECTION:
                return applyTypoCorrection(now);
            case POST_FIELD_HESITATE:
                state = TypingRuntimeState.DONE;
                nextActionAtMs = now;
                emit("typing_complete");
                clear();
                return true;
            case DONE:
            case CANCELLED:
                clear();
                return false;
            case IDLE:
            default:
                return false;
        }
    }

    private boolean typeNextCharacter(long now) {
        char targetChar = pendingText.charAt(nextCharIndex);
        boolean injectTypo = shouldInjectTypo(targetChar);
        char charToType = injectTypo ? sampleTypoCharacter(targetChar) : targetChar;
        KeyStroke stroke = resolveKeyStroke(charToType);
        if (stroke == null) {
            nextCharIndex++;
            nextActionAtMs = now + Math.max(20L, sampleKeyDelayMs(targetChar));
            emit("typing_char_skipped");
            return true;
        }
        int holdMs = sampleHoldMs(targetChar);
        if (!host.pressKey(stroke.keyCode, stroke.holdShift, holdMs)) {
            nextActionAtMs = now + Math.max(40L, sampleDelayMs(profile.baseKeyDelayMs(), 0.28));
            emit("typing_key_dispatch_failed");
            return false;
        }
        emit("typing_key_dispatch");
        if (injectTypo) {
            correctionBackspacePending = true;
            correctionTargetChar = targetChar;
            state = TypingRuntimeState.TYPO_CORRECTION;
            nextActionAtMs = now + Math.max(24L, sampleCorrectionDelayMs());
            emit("typing_typo_injected");
            return true;
        }
        nextCharIndex++;
        nextActionAtMs = now + Math.max(20L, sampleKeyDelayMs(targetChar));
        return true;
    }

    private boolean applyTypoCorrection(long now) {
        if (correctionBackspacePending) {
            int holdMs = sampleHoldMs('\b');
            if (!host.pressKey(KeyEvent.VK_BACK_SPACE, false, holdMs)) {
                nextActionAtMs = now + Math.max(30L, sampleCorrectionDelayMs());
                emit("typing_backspace_failed");
                return false;
            }
            correctionBackspacePending = false;
            nextActionAtMs = now + Math.max(24L, sampleCorrectionDelayMs());
            emit("typing_backspace_correction");
            return true;
        }
        KeyStroke correctionStroke = resolveKeyStroke(correctionTargetChar);
        if (correctionStroke == null) {
            nextCharIndex++;
            correctionTargetChar = '\0';
            state = TypingRuntimeState.TYPE_STREAM;
            nextActionAtMs = now + Math.max(20L, sampleKeyDelayMs('a'));
            return true;
        }
        if (!host.pressKey(correctionStroke.keyCode, correctionStroke.holdShift, sampleHoldMs(correctionTargetChar))) {
            nextActionAtMs = now + Math.max(30L, sampleCorrectionDelayMs());
            emit("typing_correction_key_failed");
            return false;
        }
        nextCharIndex++;
        correctionTargetChar = '\0';
        state = TypingRuntimeState.TYPE_STREAM;
        nextActionAtMs = now + Math.max(20L, sampleKeyDelayMs('a'));
        emit("typing_correction_key");
        return true;
    }

    private boolean shouldInjectTypo(char targetChar) {
        if (!Character.isLetterOrDigit(targetChar)) {
            return false;
        }
        if (nextCharIndex >= pendingText.length() - 1) {
            return false;
        }
        double chancePct = Math.max(0.0, profile.typoChancePct());
        if (chancePct <= 0.0) {
            return false;
        }
        return ThreadLocalRandom.current().nextDouble(100.0) < chancePct;
    }

    private char sampleTypoCharacter(char targetChar) {
        if (Character.isDigit(targetChar)) {
            return (char) ('0' + ThreadLocalRandom.current().nextInt(10));
        }
        if (Character.isUpperCase(targetChar)) {
            return (char) ('A' + ThreadLocalRandom.current().nextInt(26));
        }
        if (Character.isLowerCase(targetChar)) {
            return (char) ('a' + ThreadLocalRandom.current().nextInt(26));
        }
        return targetChar;
    }

    private int sampleKeyDelayMs(char ch) {
        double classScale = keyClassScale(ch);
        double jitterPct = sampleCenteredJitterPct(profile.keyJitterPct());
        double delay = profile.baseKeyDelayMs() * classScale * (1.0 + jitterPct);
        if (ThreadLocalRandom.current().nextInt(100) < profile.chunkPauseChancePct()) {
            double pauseScale = Math.max(0.0, profile.chunkPauseScalePct() / 100.0);
            delay += profile.baseKeyDelayMs() * pauseScale;
            emit("typing_chunk_pause");
        }
        return (int) Math.max(16L, Math.round(delay));
    }

    private int sampleCorrectionDelayMs() {
        double scale = Math.max(0.0, profile.correctionScalePct() / 100.0);
        long delay = Math.round(profile.baseKeyDelayMs() * Math.max(0.2, scale));
        return (int) Math.max(18L, delay);
    }

    private int samplePostFieldDelayMs() {
        if (ThreadLocalRandom.current().nextInt(100) < profile.submitHesitationChancePct()) {
            double scale = Math.max(0.0, profile.submitHesitationScalePct() / 100.0);
            long delay = Math.round(profile.baseKeyDelayMs() * Math.max(0.3, scale));
            return (int) Math.max(40L, delay);
        }
        return Math.max(30, sampleDelayMs(profile.baseKeyDelayMs(), 0.20));
    }

    private int sampleHoldMs(char ch) {
        double classScale = Character.isLetterOrDigit(ch) ? 0.34 : 0.42;
        long hold = Math.round(profile.baseKeyDelayMs() * classScale);
        hold += ThreadLocalRandom.current().nextInt(6, 22);
        return (int) Math.max(14L, hold);
    }

    private int sampleDelayMs(int baseMs, double jitterPct) {
        double jitter = sampleCenteredJitterPct(Math.max(0.0, jitterPct));
        return (int) Math.max(12L, Math.round(baseMs * (1.0 + jitter)));
    }

    private static double keyClassScale(char ch) {
        if (Character.isLetter(ch)) {
            return 1.0;
        }
        if (Character.isDigit(ch)) {
            return 0.94;
        }
        if (Character.isWhitespace(ch)) {
            return 0.86;
        }
        return 1.14;
    }

    private static double sampleCenteredJitterPct(double maxPct) {
        double bounded = Math.max(0.0, Math.min(0.95, maxPct));
        if (bounded <= 0.0) {
            return 0.0;
        }
        return ThreadLocalRandom.current().nextDouble(-bounded, bounded);
    }

    private static KeyStroke resolveKeyStroke(char ch) {
        if (ch >= 'a' && ch <= 'z') {
            return new KeyStroke(KeyEvent.VK_A + (ch - 'a'), false);
        }
        if (ch >= 'A' && ch <= 'Z') {
            return new KeyStroke(KeyEvent.VK_A + (ch - 'A'), true);
        }
        if (ch >= '0' && ch <= '9') {
            return new KeyStroke(KeyEvent.VK_0 + (ch - '0'), false);
        }
        switch (ch) {
            case ' ':
                return new KeyStroke(KeyEvent.VK_SPACE, false);
            case '-':
                return new KeyStroke(KeyEvent.VK_MINUS, false);
            case '_':
                return new KeyStroke(KeyEvent.VK_MINUS, true);
            case '=':
                return new KeyStroke(KeyEvent.VK_EQUALS, false);
            case '+':
                return new KeyStroke(KeyEvent.VK_EQUALS, true);
            case '[':
                return new KeyStroke(KeyEvent.VK_OPEN_BRACKET, false);
            case '{':
                return new KeyStroke(KeyEvent.VK_OPEN_BRACKET, true);
            case ']':
                return new KeyStroke(KeyEvent.VK_CLOSE_BRACKET, false);
            case '}':
                return new KeyStroke(KeyEvent.VK_CLOSE_BRACKET, true);
            case ';':
                return new KeyStroke(KeyEvent.VK_SEMICOLON, false);
            case ':':
                return new KeyStroke(KeyEvent.VK_SEMICOLON, true);
            case '\'':
                return new KeyStroke(KeyEvent.VK_QUOTE, false);
            case '"':
                return new KeyStroke(KeyEvent.VK_QUOTE, true);
            case ',':
                return new KeyStroke(KeyEvent.VK_COMMA, false);
            case '<':
                return new KeyStroke(KeyEvent.VK_COMMA, true);
            case '.':
                return new KeyStroke(KeyEvent.VK_PERIOD, false);
            case '>':
                return new KeyStroke(KeyEvent.VK_PERIOD, true);
            case '/':
                return new KeyStroke(KeyEvent.VK_SLASH, false);
            case '?':
                return new KeyStroke(KeyEvent.VK_SLASH, true);
            case '\\':
                return new KeyStroke(KeyEvent.VK_BACK_SLASH, false);
            case '|':
                return new KeyStroke(KeyEvent.VK_BACK_SLASH, true);
            case '`':
                return new KeyStroke(KeyEvent.VK_BACK_QUOTE, false);
            case '~':
                return new KeyStroke(KeyEvent.VK_BACK_QUOTE, true);
            case '!':
                return new KeyStroke(KeyEvent.VK_1, true);
            case '@':
                return new KeyStroke(KeyEvent.VK_2, true);
            case '#':
                return new KeyStroke(KeyEvent.VK_3, true);
            case '$':
                return new KeyStroke(KeyEvent.VK_4, true);
            case '%':
                return new KeyStroke(KeyEvent.VK_5, true);
            case '^':
                return new KeyStroke(KeyEvent.VK_6, true);
            case '&':
                return new KeyStroke(KeyEvent.VK_7, true);
            case '*':
                return new KeyStroke(KeyEvent.VK_8, true);
            case '(':
                return new KeyStroke(KeyEvent.VK_9, true);
            case ')':
                return new KeyStroke(KeyEvent.VK_0, true);
            default:
                return null;
        }
    }

    private long nowMs() {
        if (host == null) {
            return System.currentTimeMillis();
        }
        long now = host.nowMs();
        return now <= 0L ? System.currentTimeMillis() : now;
    }

    private void emit(String reason) {
        if (host == null || reason == null || reason.isBlank()) {
            return;
        }
        host.emitTypingEvent(
            reason,
            host.details(
                "tick", currentTick,
                "length", pendingText.length(),
                "state", state.name().toLowerCase(),
                "nextCharIndex", nextCharIndex
            )
        );
    }

    private static final class KeyStroke {
        private final int keyCode;
        private final boolean holdShift;

        private KeyStroke(int keyCode, boolean holdShift) {
            this.keyCode = keyCode;
            this.holdShift = holdShift;
        }
    }
}
