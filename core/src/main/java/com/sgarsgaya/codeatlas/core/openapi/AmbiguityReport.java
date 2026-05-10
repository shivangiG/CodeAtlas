package com.sgarsgaya.codeatlas.core.openapi;

import java.util.List;

public record AmbiguityReport(
        String operationId,
        String httpMethod,
        String path,
        List<String> candidateMethodNodeIds,
        String chosenMethodNodeId,
        String reason) {}
