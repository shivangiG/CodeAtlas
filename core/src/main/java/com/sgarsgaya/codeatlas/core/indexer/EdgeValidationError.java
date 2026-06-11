package com.sgarsgaya.codeatlas.core.indexer;

public record EdgeValidationError(EdgeCandidate candidate, String field, String message) {}
