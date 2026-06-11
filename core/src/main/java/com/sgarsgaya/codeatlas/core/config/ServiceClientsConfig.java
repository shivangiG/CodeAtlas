package com.sgarsgaya.codeatlas.core.config;

import java.util.ArrayList;
import java.util.List;

public class ServiceClientsConfig {

    private List<String> dependencyPatterns = new ArrayList<>(List.of(
            "*_client",
            "*_sdk",
            "*_openapi_client"
    ));

    public List<String> getDependencyPatterns() {
        return dependencyPatterns;
    }

    public void setDependencyPatterns(List<String> dependencyPatterns) {
        this.dependencyPatterns = dependencyPatterns;
    }
}
