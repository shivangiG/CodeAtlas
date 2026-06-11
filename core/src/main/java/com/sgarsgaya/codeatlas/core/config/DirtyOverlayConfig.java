package com.sgarsgaya.codeatlas.core.config;

import java.util.ArrayList;
import java.util.List;

public class DirtyOverlayConfig {

    private String defaultMode = "off";
    private boolean allowExperimentalOptIn = false;
    private List<String> forbiddenUses = new ArrayList<>();

    public String getDefaultMode() {
        return defaultMode;
    }

    public void setDefaultMode(String defaultMode) {
        this.defaultMode = defaultMode;
    }

    public boolean isAllowExperimentalOptIn() {
        return allowExperimentalOptIn;
    }

    public void setAllowExperimentalOptIn(boolean allowExperimentalOptIn) {
        this.allowExperimentalOptIn = allowExperimentalOptIn;
    }

    public List<String> getForbiddenUses() {
        return forbiddenUses;
    }

    public void setForbiddenUses(List<String> forbiddenUses) {
        this.forbiddenUses = forbiddenUses;
    }
}
