package com.xptool.activities.fishing;

import com.xptool.core.runtime.ActivityModule;
import com.xptool.systems.FishingTargetResolver;
import java.util.Objects;
import java.util.function.Function;

public final class FishingActivityModule implements ActivityModule<FishingCommandService> {
    private static final String ACTIVITY_ID = "fishing";

    private final FishingTargetResolver targetResolver;
    private final Function<FishingTargetResolver, FishingCommandService.Host> commandHostFactory;

    public FishingActivityModule(
        FishingTargetResolver.Host targetResolverHost,
        Function<FishingTargetResolver, FishingCommandService.Host> commandHostFactory
    ) {
        this.targetResolver = new FishingTargetResolver(Objects.requireNonNull(targetResolverHost, "targetResolverHost"));
        this.commandHostFactory = Objects.requireNonNull(commandHostFactory, "commandHostFactory");
    }

    @Override
    public String activityId() {
        return ACTIVITY_ID;
    }

    public FishingTargetResolver targetResolver() {
        return targetResolver;
    }

    @Override
    public FishingCommandService createService() {
        return new FishingCommandService(commandHostFactory.apply(targetResolver));
    }
}
