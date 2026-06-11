package com.sgarsgaya.codeatlas.core.config;

/**
 * Top-level configuration read from {@code .repoatlas/config.yaml}.
 * Mutable bean — SnakeYAML requires no-arg constructor + setters.
 */
public class RepoAtlasConfig {

    private LayersConfig layers = new LayersConfig();
    private AmbiguityConfig ambiguity = new AmbiguityConfig();
    private GeneratedConfig generated = new GeneratedConfig();
    private ServiceClientsConfig serviceClients = new ServiceClientsConfig();
    private CiConfig ci = new CiConfig();
    private McpConfig mcp = new McpConfig();
    private int snapshotRetentionCount = 3;

    public LayersConfig getLayers() {
        return layers;
    }

    public void setLayers(LayersConfig layers) {
        this.layers = layers;
    }

    public AmbiguityConfig getAmbiguity() {
        return ambiguity;
    }

    public void setAmbiguity(AmbiguityConfig ambiguity) {
        this.ambiguity = ambiguity;
    }

    public GeneratedConfig getGenerated() {
        return generated;
    }

    public void setGenerated(GeneratedConfig generated) {
        this.generated = generated;
    }

    public ServiceClientsConfig getServiceClients() {
        return serviceClients;
    }

    public void setServiceClients(ServiceClientsConfig serviceClients) {
        this.serviceClients = serviceClients;
    }

    public CiConfig getCi() {
        return ci;
    }

    public void setCi(CiConfig ci) {
        this.ci = ci;
    }

    public McpConfig getMcp() {
        return mcp;
    }

    public void setMcp(McpConfig mcp) {
        this.mcp = mcp;
    }

    public int getSnapshotRetentionCount() {
        return snapshotRetentionCount;
    }

    public void setSnapshotRetentionCount(int snapshotRetentionCount) {
        this.snapshotRetentionCount = snapshotRetentionCount;
    }
}
