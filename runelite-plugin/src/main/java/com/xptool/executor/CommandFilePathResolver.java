package com.xptool.executor;

import java.util.function.Function;
import java.util.function.Supplier;

final class CommandFilePathResolver {
    private final Supplier<String> commandPathOverrideSupplier;
    private final Function<String, String> pathSanitizer;
    private final String defaultCommandFilePath;

    CommandFilePathResolver(
        Supplier<String> commandPathOverrideSupplier,
        Function<String, String> pathSanitizer,
        String defaultCommandFilePath
    ) {
        this.commandPathOverrideSupplier = commandPathOverrideSupplier;
        this.pathSanitizer = pathSanitizer;
        this.defaultCommandFilePath = defaultCommandFilePath;
    }

    String resolveCommandFilePath() {
        String configuredOverride = pathSanitizer.apply(commandPathOverrideSupplier.get());
        if (!configuredOverride.isEmpty()) {
            return configuredOverride;
        }
        return defaultCommandFilePath;
    }
}
