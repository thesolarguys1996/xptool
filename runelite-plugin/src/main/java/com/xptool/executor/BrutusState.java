package com.xptool.executor;

final class BrutusState {
    private long lastDodgeAtMs = 0L;
    private int lastTelegraphTick = Integer.MIN_VALUE;
    private String lastTelegraphType = "";

    long lastDodgeAtMs() {
        return lastDodgeAtMs;
    }

    void setLastDodgeAtMs(long value) {
        this.lastDodgeAtMs = value;
    }

    int lastTelegraphTick() {
        return lastTelegraphTick;
    }

    void setLastTelegraphTick(int value) {
        this.lastTelegraphTick = value;
    }

    String lastTelegraphType() {
        return lastTelegraphType;
    }

    void setLastTelegraphType(String value) {
        this.lastTelegraphType = value == null ? "" : value;
    }
}
