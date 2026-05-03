package com.sgarsgaya.codeatlas.client;

/**
 * Thrown when the server returns a non-2xx HTTP status.
 * Carries the raw status code and, when the response body was parseable,
 * the {@code code} and {@code message} fields from the server's {@code ErrorResponse}.
 */
public class ClientException extends RuntimeException {

    private final int statusCode;
    private final String errorCode;

    public ClientException(int statusCode, String errorCode, String message) {
        super("[HTTP " + statusCode + "] " + errorCode + ": " + message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public int getStatusCode() { return statusCode; }
    public String getErrorCode() { return errorCode; }
}
