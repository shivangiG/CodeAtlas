package com.sgarsgaya.codeatlas.core.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadDefaults_returnsFullyPopulatedConfig() {
        RepoAtlasConfig config = ConfigLoader.loadDefaults();

        assertThat(config.getSnapshotRetentionCount()).isEqualTo(3);
        assertThat(config.getAmbiguity().getCiThresholdPercent()).isEqualTo(8);
        assertThat(config.getGenerated().getPaths()).containsExactly(
                "**/generated/**", "**/build/**", "**/target/**");
        assertThat(config.getServiceClients().getDependencyPatterns()).containsExactly(
                "*_client", "*_sdk", "*_openapi_client");
        assertThat(config.getCi().getBlockOn()).isEmpty();
        assertThat(config.getCi().isRejectOverlay()).isTrue();
        assertThat(config.getMcp().getDirtyOverlay().getDefaultMode()).isEqualTo("off");
        assertThat(config.getMcp().getDirtyOverlay().isAllowExperimentalOptIn()).isFalse();
        assertThat(config.getMcp().getDirtyOverlay().getForbiddenUses()).isEmpty();
        assertThat(config.getLayers().getLayers()).isEmpty();
    }

    @Test
    void load_missingFile_returnsDefaults() {
        RepoAtlasConfig config = ConfigLoader.load(tempDir);

        assertThat(config.getSnapshotRetentionCount()).isEqualTo(3);
        assertThat(config.getAmbiguity().getCiThresholdPercent()).isEqualTo(8);
    }

    @Test
    void load_fullYaml_parsesAllFields() throws IOException {
        writeConfigYaml("""
                snapshotRetentionCount: 10
                layers:
                  layers:
                    api:
                      packages:
                        - com.example.api
                      canCall:
                        - service
                    service:
                      packages:
                        - com.example.service
                      canCall:
                        - repo
                ambiguity:
                  ciThresholdPercent: 15
                generated:
                  paths:
                    - "**/gen/**"
                serviceClients:
                  dependencyPatterns:
                    - "*_grpc"
                ci:
                  blockOn:
                    - layerViolation
                    - ambiguity
                  rejectOverlay: false
                mcp:
                  dirtyOverlay:
                    defaultMode: warn
                    allowExperimentalOptIn: true
                    forbiddenUses:
                      - production
                """);

        RepoAtlasConfig config = ConfigLoader.load(tempDir);

        assertThat(config.getSnapshotRetentionCount()).isEqualTo(10);

        assertThat(config.getLayers().getLayers()).containsKeys("api", "service");
        assertThat(config.getLayers().getLayers().get("api").getPackages())
                .containsExactly("com.example.api");
        assertThat(config.getLayers().getLayers().get("api").getCanCall())
                .containsExactly("service");

        assertThat(config.getAmbiguity().getCiThresholdPercent()).isEqualTo(15);
        assertThat(config.getGenerated().getPaths()).containsExactly("**/gen/**");
        assertThat(config.getServiceClients().getDependencyPatterns()).containsExactly("*_grpc");
        assertThat(config.getCi().getBlockOn()).containsExactly("layerViolation", "ambiguity");
        assertThat(config.getCi().isRejectOverlay()).isFalse();
        assertThat(config.getMcp().getDirtyOverlay().getDefaultMode()).isEqualTo("warn");
        assertThat(config.getMcp().getDirtyOverlay().isAllowExperimentalOptIn()).isTrue();
        assertThat(config.getMcp().getDirtyOverlay().getForbiddenUses())
                .containsExactly("production");
    }

    @Test
    void load_partialYaml_mergesWithDefaults() throws IOException {
        writeConfigYaml("""
                snapshotRetentionCount: 7
                ambiguity:
                  ciThresholdPercent: 20
                """);

        RepoAtlasConfig config = ConfigLoader.load(tempDir);

        assertThat(config.getSnapshotRetentionCount()).isEqualTo(7);
        assertThat(config.getAmbiguity().getCiThresholdPercent()).isEqualTo(20);

        assertThat(config.getGenerated().getPaths()).containsExactly(
                "**/generated/**", "**/build/**", "**/target/**");
        assertThat(config.getCi().isRejectOverlay()).isTrue();
        assertThat(config.getMcp().getDirtyOverlay().getDefaultMode()).isEqualTo("off");
    }

    @Test
    void load_emptyYaml_returnsDefaults() throws IOException {
        writeConfigYaml("");

        RepoAtlasConfig config = ConfigLoader.load(tempDir);

        assertThat(config.getSnapshotRetentionCount()).isEqualTo(3);
    }

    @Test
    void bootstrap_createsFileWhenAbsent() throws IOException {
        ConfigBootstrap.bootstrap(tempDir);

        Path configPath = tempDir.resolve(".repoatlas").resolve("config.yaml");
        assertThat(configPath).exists();

        String content = Files.readString(configPath);
        assertThat(content).contains("snapshotRetentionCount: 3");
        assertThat(content).contains("ciThresholdPercent: 8");
        assertThat(content).contains("rejectOverlay: true");
    }

    @Test
    void bootstrap_doesNotOverwriteExistingFile() throws IOException {
        Path configDir = tempDir.resolve(".repoatlas");
        Files.createDirectories(configDir);
        Path configPath = configDir.resolve("config.yaml");
        Files.writeString(configPath, "snapshotRetentionCount: 99\n");

        ConfigBootstrap.bootstrap(tempDir);

        String content = Files.readString(configPath);
        assertThat(content).isEqualTo("snapshotRetentionCount: 99\n");
    }

    @Test
    void bootstrap_thenLoad_roundTrips() throws IOException {
        ConfigBootstrap.bootstrap(tempDir);

        RepoAtlasConfig config = ConfigLoader.load(tempDir);

        assertThat(config.getSnapshotRetentionCount()).isEqualTo(3);
        assertThat(config.getAmbiguity().getCiThresholdPercent()).isEqualTo(8);
        assertThat(config.getCi().isRejectOverlay()).isTrue();
        assertThat(config.getMcp().getDirtyOverlay().getDefaultMode()).isEqualTo("off");
    }

    private void writeConfigYaml(String content) throws IOException {
        Path configDir = tempDir.resolve(".repoatlas");
        Files.createDirectories(configDir);
        Files.writeString(configDir.resolve("config.yaml"), content);
    }
}
