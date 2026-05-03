package com.sgarsgaya.codeatlas.client;

/**
 * Immutable configuration for {@link CodeAtlasClient}.
 *
 * @param baseUrl               Base URL of the running server, e.g. {@code http://localhost:8080}.
 *                              No trailing slash.
 * @param connectTimeoutSeconds TCP connect timeout in seconds.
 * @param readTimeoutSeconds    Read timeout in seconds (time to receive the full response body).
 */
public record CodeAtlasClientConfig(String baseUrl, int connectTimeoutSeconds, int readTimeoutSeconds) {

    /** Sensible defaults for a local integration-test environment. */
    public static CodeAtlasClientConfig local(int port) {
        return new CodeAtlasClientConfig("http://localhost:" + port, 5, 30);
    }
}
