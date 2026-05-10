package com.sgarsgaya.codeatlas.core.indexer;

public record IndexDiagnostic(String filePath, String message, DiagnosticSeverity severity) {}
