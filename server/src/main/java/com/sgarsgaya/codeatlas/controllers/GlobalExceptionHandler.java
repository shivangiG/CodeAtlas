package com.sgarsgaya.codeatlas.controllers;

import java.time.OffsetDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.sgarsgaya.codeatlas.exception.ActiveSnapshotDeletionException;
import com.sgarsgaya.codeatlas.exception.DuplicateSnapshotException;
import com.sgarsgaya.codeatlas.exception.SnapshotNotFoundException;
import com.sgarsgaya.codeatlas.model.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SnapshotNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(SnapshotNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ActiveSnapshotDeletionException.class)
    public ResponseEntity<ErrorResponse> handleActiveDelete(ActiveSnapshotDeletionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error("ACTIVE_SNAPSHOT", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateSnapshotException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateSnapshotException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error("DUPLICATE_SNAPSHOT", ex.getMessage()));
    }

    private ErrorResponse error(String code, String message) {
        return new ErrorResponse()
                .code(code)
                .message(message)
                .timestamp(OffsetDateTime.now());
    }
}
