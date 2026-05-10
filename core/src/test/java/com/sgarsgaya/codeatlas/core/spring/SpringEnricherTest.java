package com.sgarsgaya.codeatlas.core.spring;

import com.sgarsgaya.codeatlas.core.indexer.JavaAstIndexer;
import com.sgarsgaya.codeatlas.core.indexer.RepositoryScanner;
import com.sgarsgaya.codeatlas.core.model.NodeKind;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SpringEnricherTest {

    @Test
    void enriches_rest_controller_mappings(@TempDir Path tmp) throws Exception {
        Path root = Files.createDirectories(tmp.resolve("src/main/java/app"));
        Files.writeString(
                root.resolve("Api.java"),
                """
                        package app;
                        import org.springframework.web.bind.annotation.*;
                        @RestController
                        @RequestMapping("/api")
                        public class Api {
                          @GetMapping("/hello")
                          public String hello(){ return ""; }
                        }
                        """);

        Files.writeString(tmp.resolve("build.gradle"), "plugins { id 'java' }\n");

        var scanResult = new RepositoryScanner().scan(tmp, new com.sgarsgaya.codeatlas.core.config.RepoAtlasConfig());

        var indexed = JavaAstIndexer.indexAll("S1", scanResult);

        SpringEnrichmentResult enrichment = new SpringEnricher("S1").enrich(indexed);

        assertThat(enrichment.endpoints()).isNotEmpty();
        assertThat(enrichment.additionalNodes().stream().anyMatch(n -> n.kind() == NodeKind.HTTP_ENDPOINT))
                .isTrue();
    }
}
