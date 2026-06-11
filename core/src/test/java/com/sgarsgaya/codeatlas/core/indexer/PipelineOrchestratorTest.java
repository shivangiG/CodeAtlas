package com.sgarsgaya.codeatlas.core.indexer;

import com.sgarsgaya.codeatlas.core.config.RepoAtlasConfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PipelineOrchestratorTest {

    @Test
    void publishes_snapshot_pointer_on_minimal_java_repo(@TempDir Path tmp) throws Exception {
        Files.writeString(tmp.resolve("build.gradle"), "plugins { id 'java' }\n");

        Path java =
                Files.createDirectories(tmp.resolve("src/main/java/demo")).resolve("Hello.java");
        Files.writeString(
                java,
                """
                        package demo;
                        public final class Hello {}
                        """);

        BuildResult result = new PipelineOrchestrator(tmp, new RepoAtlasConfig()).buildFull();

        assertThat(result.success()).isTrue();
        assertThat(result.nodeCount()).isGreaterThan(0);
        assertThat(result.snapshotFileName()).startsWith("graph_").endsWith(".sqlite");

        Path pointer = tmp.resolve(".repoatlas/graph_latest.pointer");
        assertThat(Files.exists(pointer)).isTrue();

        Path snapshot =
                tmp.resolve(".repoatlas/snapshots").resolve(result.snapshotFileName());
        assertThat(Files.exists(snapshot)).isTrue();
    }
}
