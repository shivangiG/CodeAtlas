package com.sgarsgaya.codeatlas.integration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import com.sgarsgaya.codeatlas.CodeAtlasApplication;
import com.sgarsgaya.codeatlas.client.CodeAtlasClient;
import com.sgarsgaya.codeatlas.client.CodeAtlasClientConfig;

/**
 * Base class for all integration tests in this module.
 *
 * <p>Starts the full Spring Boot application on a random port, then wires a
 * {@link CodeAtlasClient} pointed at that port. Every subclass test makes
 * real HTTP requests — no MockMvc, no in-process calls.
 *
 * <p>{@code @DirtiesContext} resets the H2 database between test classes so
 * tests do not share state.
 */
@SpringBootTest(
        classes = CodeAtlasApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class IntegrationTestBase {

    @LocalServerPort
    private int port;

    protected CodeAtlasClient client;

    @BeforeEach
    void setUpClient() {
        client = new CodeAtlasClient(CodeAtlasClientConfig.local(port));
    }
}
