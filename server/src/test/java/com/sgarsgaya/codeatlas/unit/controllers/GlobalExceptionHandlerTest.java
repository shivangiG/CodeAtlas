package com.sgarsgaya.codeatlas.unit.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.sgarsgaya.codeatlas.controllers.GlobalExceptionHandler;
import com.sgarsgaya.codeatlas.exception.ActiveSnapshotDeletionException;
import com.sgarsgaya.codeatlas.exception.DuplicateSnapshotException;
import com.sgarsgaya.codeatlas.exception.SnapshotNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFound_returns404_withNotFoundCode() {
        var response = handler.handleNotFound(new SnapshotNotFoundException("S1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().getMessage()).contains("S1");
    }

    @Test
    void handleActiveDelete_returns409_withActiveSnapshotCode() {
        var response = handler.handleActiveDelete(new ActiveSnapshotDeletionException("S1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("ACTIVE_SNAPSHOT");
        assertThat(response.getBody().getMessage()).contains("S1");
    }

    @Test
    void handleDuplicate_returns409_withDuplicateSnapshotCode() {
        var response = handler.handleDuplicate(new DuplicateSnapshotException("S1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_SNAPSHOT");
        assertThat(response.getBody().getMessage()).contains("S1");
    }
}
