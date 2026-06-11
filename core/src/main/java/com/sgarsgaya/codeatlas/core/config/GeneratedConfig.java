package com.sgarsgaya.codeatlas.core.config;

import java.util.ArrayList;
import java.util.List;

public class GeneratedConfig {

    private List<String> paths = new ArrayList<>(List.of(
            "**/generated/**",
            "**/build/**",
            "**/target/**"
    ));

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }
}
