package com.sgarsgaya.codeatlas.core.openapi;

import com.sgarsgaya.codeatlas.core.spring.HttpEndpointInfo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiMapperTest {

    @Test
    void maps_operation_by_operation_id(@TempDir Path tmp) throws Exception {
        Path yaml = Files.createTempFile(tmp, "spec", ".yaml");
        Files.writeString(
                yaml,
                """
                        openapi: 3.0.3
                        info:
                          title: Demo
                          version: 1.0.0
                        paths:
                          /hello:
                            get:
                              operationId: hello
                              responses:
                                '200':
                                  description: ok
                        """);

        OpenApiMapper mapper = new OpenApiMapper("S1");
        HttpEndpointInfo endpoint =
                new HttpEndpointInfo("http:test", "GET", "/hello", "app.Api", "hello", "meth:stub");

        OpenApiMapResult result = mapper.map(List.of(yaml), List.of(endpoint));

        assertThat(result.nodes()).isNotEmpty();
        assertThat(result.mappings()).isNotEmpty();
    }
}
