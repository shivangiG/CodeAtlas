package com.sgarsgaya.codeatlas.core.config;

import java.util.ArrayList;
import java.util.List;

public class LayerDef {

    private List<String> packages = new ArrayList<>();
    private List<String> canCall = new ArrayList<>();

    public List<String> getPackages() {
        return packages;
    }

    public void setPackages(List<String> packages) {
        this.packages = packages;
    }

    public List<String> getCanCall() {
        return canCall;
    }

    public void setCanCall(List<String> canCall) {
        this.canCall = canCall;
    }
}
