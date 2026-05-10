package com.sgarsgaya.codeatlas.core.indexer;

import com.sgarsgaya.codeatlas.core.config.RepoAtlasConfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryScannerTest {

    @Test
    void discovers_module_and_ignores_generated_tree(@TempDir Path tmp) throws Exception {
        Path mod = Files.createDirectories(tmp.resolve("svc"));
        Files.writeString(mod.resolve("build.gradle"), "plugins { id 'java' }\n");

        Path javaFile = Files.createDirectories(mod.resolve("src/main/java/com/demo"))
                .resolve("Demo.java");
        Files.writeString(
                javaFile,
                """
                        package com.demo;
                        public final class Demo { }
                        """);

        Files.createDirectories(mod.resolve("build/gen"));
        Files.writeString(mod.resolve("build/gen/Bad.java"), "package bad;\npublic final class Bad {}\n");

        Files.writeString(mod.resolve(".gitignore"), "build/\n");

        ScanResult result = new RepositoryScanner().scan(tmp, new RepoAtlasConfig());

        assertThat(result.modules()).hasSize(1);
        assertThat(result.modules().getFirst().name()).isEqualTo("svc");

        assertThat(result.sourceFiles()).anyMatch(sf -> sf.relativePath().endsWith(Path.of("Demo.java")));
        assertThat(result.fingerprints().keySet()).noneMatch(k -> k.contains("svc/build/gen/Bad.java"));
    }

    @Test
    void fallback_single_module_when_no_build_scripts(@TempDir Path tmp) throws Exception {
        Path javaFile =
                Files.createDirectories(tmp.resolve("src/main/java/com/example")).resolve("A.java");
        Files.writeString(javaFile, """
                package com.example;
                public final class A { }
                """);

        ScanResult scan = new RepositoryScanner().scan(tmp, new RepoAtlasConfig());

        assertThat(scan.modules()).hasSize(1);
        assertThat(scan.modules().getFirst().name()).isEqualTo("root");
        assertThat(scan.sourceFiles()).hasSize(1);
        assertThat(scan.fingerprints()).hasSize(1);
    }
}
