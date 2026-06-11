package com.sgarsgaya.codeatlas.core.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class SymbolIdentity {

    private SymbolIdentity() {}

    public static String primary(String fqClass, String methodName, String paramTypes, String returnType) {
        return fqClass + "#" + methodName + "(" + paramTypes + "):" + returnType;
    }

    public static String fallback(String repoPath, String filePath, SourceRange range, String kindHint) {
        String input = repoPath + "|" + filePath + "|" + range + "|" + kindHint;
        return sha256Hex(input);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 not available", e);
        }
    }
}
