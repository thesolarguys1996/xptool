package com.xptool.executor;

final class MotorController<P, M> {
    private long nextProgramId = 1L;
    private P activeProgram = null;
    private M pendingMove = null;

    long nextProgramId() {
        return nextProgramId++;
    }

    P activeProgram() {
        return activeProgram;
    }

    void setActiveProgram(P program) {
        activeProgram = program;
    }

    void clearActiveProgram() {
        activeProgram = null;
    }

    M pendingMove() {
        return pendingMove;
    }

    void setPendingMove(M move) {
        pendingMove = move;
    }

    void clearPendingMove() {
        pendingMove = null;
    }

    boolean hasPendingMove() {
        return pendingMove != null;
    }
}
