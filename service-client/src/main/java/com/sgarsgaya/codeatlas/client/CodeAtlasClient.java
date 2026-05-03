package com.sgarsgaya.codeatlas.client;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sgarsgaya.codeatlas.model.ErrorResponse;
import com.sgarsgaya.codeatlas.model.GraphStatusResponse;
import com.sgarsgaya.codeatlas.model.HealthResponse;
import com.sgarsgaya.codeatlas.model.SnapshotRequest;
import com.sgarsgaya.codeatlas.model.SnapshotResponse;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Typed HTTP client for the CodeAtlas API.
 *
 * <p>Uses OkHttp for transport and Jackson for JSON serialisation.
 * All methods are synchronous and throw {@link ClientException} for non-2xx responses.
 * Callers may catch on {@code getStatusCode()} to distinguish 404 from 409 etc.
 *
 * <p>Thread-safe: share a single instance across tests.
 */
public class CodeAtlasClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String API_BASE = "/api/v1";

    private final OkHttpClient http;
    private final ObjectMapper json;
    private final String baseUrl;

    public CodeAtlasClient(CodeAtlasClientConfig config) {
        this.baseUrl = config.baseUrl();
        this.http = new OkHttpClient.Builder()
                .connectTimeout(config.connectTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(config.readTimeoutSeconds(), TimeUnit.SECONDS)
                .build();
        this.json = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // ─── Health ──────────────────────────────────────────────────────────────

    public HealthResponse getHealth() {
        return get("/health", HealthResponse.class);
    }

    // ─── Snapshots ───────────────────────────────────────────────────────────

    public SnapshotResponse createSnapshot(SnapshotRequest request) {
        return post("/snapshots", request, SnapshotResponse.class);
    }

    public List<SnapshotResponse> listSnapshots() {
        return get("/snapshots", new TypeReference<>() {});
    }

    public GraphStatusResponse getActiveSnapshot() {
        return get("/snapshots/active", GraphStatusResponse.class);
    }

    public void deleteSnapshot(String snapshotId) {
        delete("/snapshots/" + snapshotId);
    }

    // ─── HTTP primitives ─────────────────────────────────────────────────────

    private <T> T get(String path, Class<T> responseType) {
        Request request = new Request.Builder().url(url(path)).get().build();
        return execute(request, responseType);
    }

    private <T> T get(String path, TypeReference<T> typeReference) {
        Request request = new Request.Builder().url(url(path)).get().build();
        return execute(request, typeReference);
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        Request request = new Request.Builder()
                .url(url(path))
                .post(toRequestBody(body))
                .build();
        return execute(request, responseType);
    }

    private void delete(String path) {
        Request request = new Request.Builder().url(url(path)).delete().build();
        executeVoid(request);
    }

    // ─── Internal execution helpers ──────────────────────────────────────────

    private <T> T execute(Request request, Class<T> responseType) {
        try (Response response = http.newCall(request).execute()) {
            String body = requireBody(response);
            assertSuccess(response, body);
            return json.readValue(body, responseType);
        } catch (IOException e) {
            throw new RuntimeException("HTTP call failed: " + request.url(), e);
        }
    }

    private <T> T execute(Request request, TypeReference<T> typeReference) {
        try (Response response = http.newCall(request).execute()) {
            String body = requireBody(response);
            assertSuccess(response, body);
            return json.readValue(body, typeReference);
        } catch (IOException e) {
            throw new RuntimeException("HTTP call failed: " + request.url(), e);
        }
    }

    private void executeVoid(Request request) {
        try (Response response = http.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            assertSuccess(response, body);
        } catch (IOException e) {
            throw new RuntimeException("HTTP call failed: " + request.url(), e);
        }
    }

    private void assertSuccess(Response response, String rawBody) throws IOException {
        if (!response.isSuccessful()) {
            String code = "UNKNOWN";
            String message = rawBody;
            try {
                ErrorResponse error = json.readValue(rawBody, ErrorResponse.class);
                code = error.getCode() != null ? error.getCode() : code;
                message = error.getMessage() != null ? error.getMessage() : message;
            } catch (Exception ignored) {
                // body was not a valid ErrorResponse — use raw body as message
            }
            throw new ClientException(response.code(), code, message);
        }
    }

    private String requireBody(Response response) throws IOException {
        if (response.body() == null) {
            throw new RuntimeException("Response body was null for: " + response.request().url());
        }
        return response.body().string();
    }

    private RequestBody toRequestBody(Object object) {
        try {
            return RequestBody.create(json.writeValueAsBytes(object), JSON);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialise request body", e);
        }
    }

    private String url(String path) {
        return baseUrl + API_BASE + path;
    }
}
