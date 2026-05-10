package com.sgarsgaya.codeatlas.core.config;

import java.util.ArrayList;
import java.util.List;

public class CiConfig {

    private List<String> blockOn = new ArrayList<>();
    private boolean rejectOverlay = true;

    public List<String> getBlockOn() {
        return blockOn;
    }

    public void setBlockOn(List<String> blockOn) {
        this.blockOn = blockOn;
    }

    public boolean isRejectOverlay() {
        return rejectOverlay;
    }

    public void setRejectOverlay(boolean rejectOverlay) {
        this.rejectOverlay = rejectOverlay;
    }
}
