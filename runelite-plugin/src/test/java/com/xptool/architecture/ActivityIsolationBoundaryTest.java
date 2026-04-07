package com.xptool.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ActivityIsolationBoundaryTest {
    private static final String ACTIVITY_IMPORT_PREFIX = "import com.xptool.activities.";

    @Test
    void activityModulesDoNotImportOtherActivityModules() throws IOException {
        Path root = Paths.get("src/main/java/com/xptool/activities");
        if (!Files.exists(root)) {
            return;
        }
        List<String> violations = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
                .forEach(path -> collectViolations(root, path, violations));
        }
        assertTrue(
            violations.isEmpty(),
            "Cross-activity import violations detected:\n" + String.join("\n", violations)
        );
    }

    private static void collectViolations(Path root, Path file, List<String> violations) {
        String ownerActivity = resolveOwnerActivity(root, file);
        if (ownerActivity.isBlank()) {
            return;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException ignored) {
            return;
        }
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (!trimmed.startsWith(ACTIVITY_IMPORT_PREFIX)) {
                continue;
            }
            String importedActivity = resolveImportedActivity(trimmed);
            if (importedActivity.isBlank() || importedActivity.equals(ownerActivity)) {
                continue;
            }
            violations.add(file + " imports activity '" + importedActivity + "' from '" + ownerActivity + "'");
        }
    }

    private static String resolveOwnerActivity(Path root, Path file) {
        Path relative = root.relativize(file);
        if (relative.getNameCount() <= 1) {
            return "";
        }
        return relative.getName(0).toString();
    }

    private static String resolveImportedActivity(String importLine) {
        String remainder = importLine.substring(ACTIVITY_IMPORT_PREFIX.length());
        int dot = remainder.indexOf('.');
        if (dot < 0) {
            return "";
        }
        return remainder.substring(0, dot).trim();
    }
}
