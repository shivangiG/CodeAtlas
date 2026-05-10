package com.sgarsgaya.codeatlas.core.freshness;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public class GraphStatusFile {

    private static final Logger LOG = Logger.getLogger(GraphStatusFile.class.getName());
    private static final String REPOATLAS_DIR = ".repoatlas";
    private static final String STATUS_FILE = "graph_status.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private String activeSnapshotId;
    private String freshness;
    private boolean rebuildWorkerActive;
    private String lastBuildTime;
    private String lastBuildDuration;
    private int nodeCount;
    private int edgeCount;

    public String getActiveSnapshotId() { return activeSnapshotId; }
    public void setActiveSnapshotId(String activeSnapshotId) { this.activeSnapshotId = activeSnapshotId; }

    public String getFreshness() { return freshness; }
    public void setFreshness(String freshness) { this.freshness = freshness; }

    public boolean isRebuildWorkerActive() { return rebuildWorkerActive; }
    public void setRebuildWorkerActive(boolean rebuildWorkerActive) { this.rebuildWorkerActive = rebuildWorkerActive; }

    public String getLastBuildTime() { return lastBuildTime; }
    public void setLastBuildTime(String lastBuildTime) { this.lastBuildTime = lastBuildTime; }

    public String getLastBuildDuration() { return lastBuildDuration; }
    public void setLastBuildDuration(String lastBuildDuration) { this.lastBuildDuration = lastBuildDuration; }

    public int getNodeCount() { return nodeCount; }
    public void setNodeCount(int nodeCount) { this.nodeCount = nodeCount; }

    public int getEdgeCount() { return edgeCount; }
    public void setEdgeCount(int edgeCount) { this.edgeCount = edgeCount; }

    public static GraphStatusFile read(Path repoRoot) {
        Path statusPath = repoRoot.resolve(REPOATLAS_DIR).resolve(STATUS_FILE);
        if (!Files.exists(statusPath)) {
            GraphStatusFile defaults = new GraphStatusFile();
            defaults.setFreshness("no_graph");
            return defaults;
        }
        try {
            String json = Files.readString(statusPath);
            return GSON.fromJson(json, GraphStatusFile.class);
        } catch (IOException e) {
            LOG.warning("Failed to read graph status file: " + e.getMessage());
            GraphStatusFile defaults = new GraphStatusFile();
            defaults.setFreshness("no_graph");
            return defaults;
        }
    }

    public void write(Path repoRoot) throws IOException {
        Path dir = repoRoot.resolve(REPOATLAS_DIR);
        Files.createDirectories(dir);
        Path statusPath = dir.resolve(STATUS_FILE);
        Files.writeString(statusPath, GSON.toJson(this));
    }
}
