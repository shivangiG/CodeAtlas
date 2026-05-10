package com.sgarsgaya.codeatlas.core.indexer;

import com.sgarsgaya.codeatlas.core.config.RepoAtlasConfig;
import com.sgarsgaya.codeatlas.core.model.EdgeKind;
import com.sgarsgaya.codeatlas.core.model.NodeKind;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JavaAstIndexerTest {

    @Test
    void extracts_types_and_best_effort_call_edge(@TempDir Path tmp) throws Exception {
        Path java = Files.createDirectories(tmp.resolve("src/main/java/demo")).resolve("X.java");
        Files.writeString(
                java,
                """
                        package demo;
                        final class Foo {
                          void caller() {
                            String.valueOf(1);
                          }
                        }
                        """);

        Path build = tmp.resolve("build.gradle");
        Files.writeString(build, "plugins { id 'java' }\n");

        ScanResult scan = new RepositoryScanner().scan(tmp, new RepoAtlasConfig());

        IndexedSymbols indexed = JavaAstIndexer.indexAll("S1", scan);

        assertThat(indexed.nodes().stream().anyMatch(n -> n.kind() == NodeKind.CLASS)).isTrue();
        assertThat(indexed.edgeCandidates().stream().map(EdgeCandidate::kind).anyMatch(k -> k == EdgeKind.DECLARES))
                .isTrue();
        assertThat(indexed.edgeCandidates().stream().map(EdgeCandidate::kind).anyMatch(k -> k == EdgeKind.CALLS))
                .isTrue();
    }
}
