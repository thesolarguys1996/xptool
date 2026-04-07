package com.xptool.core.motor;

@FunctionalInterface
public interface MotorGateway<TPoint, TProfile, THandle> {
    THandle schedule(TPoint point, TProfile profile);
}
