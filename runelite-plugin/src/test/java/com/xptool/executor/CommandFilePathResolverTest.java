package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CommandFilePathResolverTest {
    @Test
    void usesDefaultPathWhenOverrideIsMissing() {
        CommandFilePathResolver resolver = new CommandFilePathResolver(
            () -> null,
            ExecutorValueParsers::safePath,
            "runelite-plugin/tools/command-bus.ndjson"
        );

        assertEquals("runelite-plugin/tools/command-bus.ndjson", resolver.resolveCommandFilePath());
    }

    @Test
    void prefersSanitizedOverridePathWhenPresent() {
        CommandFilePathResolver resolver = new CommandFilePathResolver(
            () -> " C:\\\\repo\\\\command-bus.ndjson ",
            ExecutorValueParsers::safePath,
            "runelite-plugin/tools/command-bus.ndjson"
        );

        assertEquals("C:\\repo\\command-bus.ndjson", resolver.resolveCommandFilePath());
    }
}
