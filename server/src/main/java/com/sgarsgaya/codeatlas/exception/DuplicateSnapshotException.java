package com.sgarsgaya.codeatlas.exception;

public class DuplicateSnapshotException extends RuntimeException {

    public DuplicateSnapshotException(String snapshotName) {
        super("Snapshot already exists: " + snapshotName);
    }
}
