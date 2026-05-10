package com.sgarsgaya.codeatlas.core.openapi;

import com.sgarsgaya.codeatlas.core.model.Confidence;

public record OperationMapping(
        String operationId,
        String httpMethod,
        String path,
        String controllerMethodNodeId,
        String openApiOperationNodeId,
        Confidence confidence,
        String reason) {}
