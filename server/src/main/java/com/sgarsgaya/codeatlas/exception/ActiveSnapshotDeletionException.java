package com.sgarsgaya.codeatlas.exception;

public class ActiveSnapshotDeletionException extends RuntimeException {

    public ActiveSnapshotDeletionException(String snapshotName) {
        super("Cannot delete the active snapshot: " + snapshotName);
    }
}
