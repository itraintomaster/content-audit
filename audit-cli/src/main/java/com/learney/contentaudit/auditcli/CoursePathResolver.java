package com.learney.contentaudit.auditcli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class CoursePathResolver {
    private static final String ENV_KEY = "CONTENT_AUDIT_CONTENT_FOLDER";

    private CoursePathResolver() {}

    static String resolve(String explicitPath) {
        if (explicitPath != null && !explicitPath.isBlank()) return explicitPath;

        String envVal = System.getenv(ENV_KEY);
        if (envVal != null && !envVal.isBlank()) return envVal;

        Path dotEnv = Path.of(".env");
        if (Files.exists(dotEnv)) {
            try {
                for (String line : Files.readAllLines(dotEnv)) {
                    line = line.strip();
                    if (line.startsWith("#") || line.isEmpty()) continue;
                    if (line.startsWith(ENV_KEY + "=")) {
                        String val = line.substring(ENV_KEY.length() + 1).strip();
                        if ((val.startsWith("\"") && val.endsWith("\""))
                                || (val.startsWith("'") && val.endsWith("'"))) {
                            val = val.substring(1, val.length() - 1);
                        }
                        if (!val.isBlank()) return val;
                    }
                }
            } catch (IOException ignored) { }
        }
        return null;
    }
}
