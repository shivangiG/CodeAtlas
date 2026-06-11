package com.sgarsgaya.codeatlas.core.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Creates a default {@code .repoatlas/config.yaml} if one does not already exist.
 */
public final class ConfigBootstrap {

    private static final String CONFIG_DIR = ".repoatlas";
    private static final String CONFIG_FILE = "config.yaml";

    private ConfigBootstrap() {}

    public static void bootstrap(Path repoRoot) throws IOException {
        Path configDir = repoRoot.resolve(CONFIG_DIR);
        Path configPath = configDir.resolve(CONFIG_FILE);

        if (Files.exists(configPath)) {
            return;
        }

        Files.createDirectories(configDir);
        Files.writeString(configPath, defaultConfigYaml());
    }

    static String defaultConfigYaml() {
        return """
                # ──────────────────────────────────────────────
                # RepoAtlas configuration
                # ──────────────────────────────────────────────

                # Number of snapshots to keep before pruning old ones.
                snapshotRetentionCount: 3

                # Architectural layer definitions.
                # Each layer lists the packages it owns and which other layers it may call.
                layers:
                  layers: {}

                # Ambiguity detection thresholds.
                ambiguity:
                  ciThresholdPercent: 8

                # Glob patterns that identify generated / build output paths.
                generated:
                  paths:
                    - "**/generated/**"
                    - "**/build/**"
                    - "**/target/**"

                # Patterns to detect service-client dependencies.
                serviceClients:
                  dependencyPatterns:
                    - "*_client"
                    - "*_sdk"
                    - "*_openapi_client"

                # CI gate configuration.
                ci:
                  blockOn: []
                  rejectOverlay: true

                # MCP (Model Context Protocol) settings.
                mcp:
                  dirtyOverlay:
                    defaultMode: "off"
                    allowExperimentalOptIn: false
                    forbiddenUses: []
                """;
    }
}
