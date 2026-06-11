package com.sgarsgaya.codeatlas.core.spring;

public record HttpEndpointInfo(
        String nodeId, String httpMethod, String path, String controllerClass, String methodName, String methodNodeId) {}
