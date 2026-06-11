package com.sgarsgaya.codeatlas.core.freshness;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FileFingerprint {

    private static final String ALGORITHM = "SHA-256";
    private static final int BUFFER_SIZE = 8192;
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private FileFingerprint() {}

    public static String compute(Path file) throws IOException {
        MessageDigest digest = newDigest();
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream in = Files.newInputStream(file)) {
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        return toHex(digest.digest());
    }

    public static Map<String, String> computeAll(List<Path> files) throws IOException {
        Map<String, String> result = new LinkedHashMap<>();
        for (Path file : files) {
            result.put(file.toString(), compute(file));
        }
        return result;
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] hex = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hex[i * 2] = HEX_DIGITS[v >>> 4];
            hex[i * 2 + 1] = HEX_DIGITS[v & 0x0F];
        }
        return new String(hex);
    }
}
