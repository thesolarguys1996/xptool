package com.xptool.executor;

import java.awt.Point;

interface MotorRuntimePort {
    PendingMouseMove pendingMouseMove();

    void clearPendingMouseMove();

    boolean isPendingMouseMoveOwnerValid(PendingMouseMove pendingMove);

    boolean isMotorActionReadyNow();

    void notePendingMoveAge(PendingMouseMove pendingMove);

    boolean pendingMoveHasExceededCommitTimeout(PendingMouseMove pendingMove);

    boolean pendingMoveTargetInvalidated(PendingMouseMove pendingMove);

    void notePendingMoveRemainingDistance(PendingMouseMove pendingMove);

    boolean tryConsumeMouseMutationBudget();

    Point currentPointerLocationOr(Point fallback);

    void notePendingMoveBlocked(PendingMouseMove pendingMove, String reason, int tick);

    void notePendingMoveAdvanced(PendingMouseMove pendingMove, int tick, Point after);

    void notePendingMoveCleared(PendingMouseMove pendingMove, String reason, int tick);

    void noteMouseMutation(Point after);

    void noteInteractionActivityNow();

    double pendingMoveArrivalTolerancePx();

    MotorProgram activeMotorProgram();

    String normalizedMotorOwnerName(String owner);

    boolean isSessionMotorOwner(String owner);

    boolean renewSessionMotor(String owner, long leaseMs);

    long motorProgramLeaseMsForOwner(String owner);

    void cancelMotorProgram(MotorProgram program, String reason);

    String pushMotorOwnerContext(String owner);

    String pushClickTypeContext(String clickType);

    void advanceMotorProgramMove(MotorProgram program);

    boolean validateMotorProgramMenu(MotorProgram program);

    void failMotorProgram(MotorProgram program, String reason);

    void runMotorProgramClick(MotorProgram program);

    void popClickTypeContext(String previous);

    void popMotorOwnerContext(String previous);
}
