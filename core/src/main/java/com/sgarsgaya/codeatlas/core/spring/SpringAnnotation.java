package com.sgarsgaya.codeatlas.core.spring;

import java.util.Locale;
import java.util.Optional;

public enum SpringAnnotation {
    REST_CONTROLLER("org.springframework.web.bind.annotation.RestController", "RestController"),
    CONTROLLER("org.springframework.stereotype.Controller", "Controller"),
    SERVICE("org.springframework.stereotype.Service", "Service"),
    COMPONENT("org.springframework.stereotype.Component", "Component"),
    REPOSITORY_ANN("org.springframework.stereotype.Repository", "Repository"),
    CONFIGURATION("org.springframework.context.annotation.Configuration", "Configuration"),
    BEAN("org.springframework.context.annotation.Bean", "Bean"),
    ENTITY("jakarta.persistence.Entity", "Entity"),
    AUTowired("org.springframework.beans.factory.annotation.Autowired", "Autowired"),
    TRANSACTIONAL("org.springframework.transaction.annotation.Transactional", "Transactional"),
    ASYNC("org.springframework.scheduling.annotation.Async", "Async"),
    SCHEDULED("org.springframework.scheduling.annotation.Scheduled", "Scheduled"),
    GET_MAPPING("org.springframework.web.bind.annotation.GetMapping", "GetMapping"),
    POST_MAPPING("org.springframework.web.bind.annotation.PostMapping", "PostMapping"),
    PUT_MAPPING("org.springframework.web.bind.annotation.PutMapping", "PutMapping"),
    DELETE_MAPPING("org.springframework.web.bind.annotation.DeleteMapping", "DeleteMapping"),
    PATCH_MAPPING("org.springframework.web.bind.annotation.PatchMapping", "PatchMapping"),
    REQUEST_MAPPING("org.springframework.web.bind.annotation.RequestMapping", "RequestMapping");

    private final String qualified;
    private final String simple;

    SpringAnnotation(String qualified, String simple) {
        this.qualified = qualified;
        this.simple = simple;
    }

    public String qualifiedName() {
        return qualified;
    }

    public String simpleName() {
        return simple;
    }

    public static Optional<SpringAnnotation> fromQualifiedName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        for (SpringAnnotation v : values()) {
            if (v.qualified.equals(name)) {
                return Optional.of(v);
            }
        }
        return Optional.empty();
    }

    public static Optional<SpringAnnotation> fromSimpleName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String n = name.trim();
        for (SpringAnnotation v : values()) {
            if (v.simple.equalsIgnoreCase(n)
                    || v.simple.toLowerCase(Locale.ROOT).equals(n.toLowerCase(Locale.ROOT))) {
                return Optional.of(v);
            }
        }
        return Optional.empty();
    }
}
