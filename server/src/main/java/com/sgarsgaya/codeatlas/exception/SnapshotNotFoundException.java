package com.sgarsgaya.codeatlas.exception;

public class SnapshotNotFoundException extends RuntimeException {

    public SnapshotNotFoundException(String snapshotName) {
        super("Snapshot not found: " + snapshotName);
    }
}
