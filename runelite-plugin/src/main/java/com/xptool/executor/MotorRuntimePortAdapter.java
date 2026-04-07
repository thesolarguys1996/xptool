package com.xptool.executor;

import java.awt.Point;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

final class MotorRuntimePortAdapter {
    @FunctionalInterface
    interface RenewSessionMotor {
        boolean renew(String owner, long leaseMs);
    }

    @FunctionalInterface
    interface PendingMoveBlockOrClearObserver {
        void observe(PendingMouseMove pendingMove, String reason, int tick);
    }

    @FunctionalInterface
    interface PendingMoveAdvanceObserver {
        void observe(PendingMouseMove pendingMove, int tick, Point after);
    }

    private MotorRuntimePortAdapter() {
    }

    static MotorRuntimePort create(
        Supplier<PendingMouseMove> pendingMouseMove,
        Runnable clearPendingMouseMove,
        Predicate<PendingMouseMove> isPendingMouseMoveOwnerValid,
        BooleanSupplier isMotorActionReadyNow,
        Consumer<PendingMouseMove> notePendingMoveAge,
        Predicate<PendingMouseMove> pendingMoveHasExceededCommitTimeout,
        Predicate<PendingMouseMove> pendingMoveTargetInvalidated,
        Consumer<PendingMouseMove> notePendingMoveRemainingDistance,
        BooleanSupplier tryConsumeMouseMutationBudget,
        Function<Point, Point> currentPointerLocationOr,
        PendingMoveBlockOrClearObserver notePendingMoveBlocked,
        PendingMoveAdvanceObserver notePendingMoveAdvanced,
        PendingMoveBlockOrClearObserver notePendingMoveCleared,
        Consumer<Point> noteMouseMutation,
        Runnable noteInteractionActivityNow,
        DoubleSupplier pendingMoveArrivalTolerancePx,
        Supplier<MotorProgram> activeMotorProgram,
        Function<String, String> normalizedMotorOwnerName,
        Predicate<String> isSessionMotorOwner,
        RenewSessionMotor renewSessionMotor,
        ToLongFunction<String> motorProgramLeaseMsForOwner,
        BiConsumer<MotorProgram, String> cancelMotorProgram,
        Function<String, String> pushMotorOwnerContext,
        Function<String, String> pushClickTypeContext,
        Consumer<MotorProgram> advanceMotorProgramMove,
        Predicate<MotorProgram> validateMotorProgramMenu,
        BiConsumer<MotorProgram, String> failMotorProgram,
        Consumer<MotorProgram> runMotorProgramClick,
        Consumer<String> popClickTypeContext,
        Consumer<String> popMotorOwnerContext
    ) {
        return new MotorRuntimePort() {
            @Override
            public PendingMouseMove pendingMouseMove() {
                return pendingMouseMove.get();
            }

            @Override
            public void clearPendingMouseMove() {
                clearPendingMouseMove.run();
            }

            @Override
            public boolean isPendingMouseMoveOwnerValid(PendingMouseMove pendingMove) {
                return isPendingMouseMoveOwnerValid.test(pendingMove);
            }

            @Override
            public boolean isMotorActionReadyNow() {
                return isMotorActionReadyNow.getAsBoolean();
            }

            @Override
            public void notePendingMoveAge(PendingMouseMove pendingMove) {
                notePendingMoveAge.accept(pendingMove);
            }

            @Override
            public boolean pendingMoveHasExceededCommitTimeout(PendingMouseMove pendingMove) {
                return pendingMoveHasExceededCommitTimeout.test(pendingMove);
            }

            @Override
            public boolean pendingMoveTargetInvalidated(PendingMouseMove pendingMove) {
                return pendingMoveTargetInvalidated.test(pendingMove);
            }

            @Override
            public void notePendingMoveRemainingDistance(PendingMouseMove pendingMove) {
                notePendingMoveRemainingDistance.accept(pendingMove);
            }

            @Override
            public boolean tryConsumeMouseMutationBudget() {
                return tryConsumeMouseMutationBudget.getAsBoolean();
            }

            @Override
            public Point currentPointerLocationOr(Point fallback) {
                return currentPointerLocationOr.apply(fallback);
            }

            @Override
            public void notePendingMoveBlocked(PendingMouseMove pendingMove, String reason, int tick) {
                notePendingMoveBlocked.observe(pendingMove, reason, tick);
            }

            @Override
            public void notePendingMoveAdvanced(PendingMouseMove pendingMove, int tick, Point after) {
                notePendingMoveAdvanced.observe(pendingMove, tick, after);
            }

            @Override
            public void notePendingMoveCleared(PendingMouseMove pendingMove, String reason, int tick) {
                notePendingMoveCleared.observe(pendingMove, reason, tick);
            }

            @Override
            public void noteMouseMutation(Point after) {
                noteMouseMutation.accept(after);
            }

            @Override
            public void noteInteractionActivityNow() {
                noteInteractionActivityNow.run();
            }

            @Override
            public double pendingMoveArrivalTolerancePx() {
                return pendingMoveArrivalTolerancePx.getAsDouble();
            }

            @Override
            public MotorProgram activeMotorProgram() {
                return activeMotorProgram.get();
            }

            @Override
            public String normalizedMotorOwnerName(String owner) {
                return normalizedMotorOwnerName.apply(owner);
            }

            @Override
            public boolean isSessionMotorOwner(String owner) {
                return isSessionMotorOwner.test(owner);
            }

            @Override
            public boolean renewSessionMotor(String owner, long leaseMs) {
                return renewSessionMotor.renew(owner, leaseMs);
            }

            @Override
            public long motorProgramLeaseMsForOwner(String owner) {
                return motorProgramLeaseMsForOwner.applyAsLong(owner);
            }

            @Override
            public void cancelMotorProgram(MotorProgram program, String reason) {
                cancelMotorProgram.accept(program, reason);
            }

            @Override
            public String pushMotorOwnerContext(String owner) {
                return pushMotorOwnerContext.apply(owner);
            }

            @Override
            public String pushClickTypeContext(String clickType) {
                return pushClickTypeContext.apply(clickType);
            }

            @Override
            public void advanceMotorProgramMove(MotorProgram program) {
                advanceMotorProgramMove.accept(program);
            }

            @Override
            public boolean validateMotorProgramMenu(MotorProgram program) {
                return validateMotorProgramMenu.test(program);
            }

            @Override
            public void failMotorProgram(MotorProgram program, String reason) {
                failMotorProgram.accept(program, reason);
            }

            @Override
            public void runMotorProgramClick(MotorProgram program) {
                runMotorProgramClick.accept(program);
            }

            @Override
            public void popClickTypeContext(String previous) {
                popClickTypeContext.accept(previous);
            }

            @Override
            public void popMotorOwnerContext(String previous) {
                popMotorOwnerContext.accept(previous);
            }
        };
    }
}
