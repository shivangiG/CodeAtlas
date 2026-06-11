package com.sgarsgaya.codeatlas.core.model;

public record SourceRange(int lineStart, int colStart, int lineEnd, int colEnd) {

    public static SourceRange parse(String s) {
        String[] parts = s.split(":");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Expected format lineStart:colStart:lineEnd:colEnd, got: " + s);
        }
        return new SourceRange(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3]));
    }

    @Override
    public String toString() {
        return lineStart + ":" + colStart + ":" + lineEnd + ":" + colEnd;
    }
}
