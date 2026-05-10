package com.sgarsgaya.codeatlas.core.snapshot;

import java.util.List;

public record ValidationResult(boolean valid, List<String> errors) {

    public static ValidationResult ok() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult failure(List<String> errors) {
        return new ValidationResult(false, List.copyOf(errors));
    }
}
