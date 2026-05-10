package com.sgarsgaya.codeatlas.core.config;

import java.util.LinkedHashMap;
import java.util.Map;

public class LayersConfig {

    private Map<String, LayerDef> layers = new LinkedHashMap<>();

    public Map<String, LayerDef> getLayers() {
        return layers;
    }

    public void setLayers(Map<String, LayerDef> layers) {
        this.layers = layers;
    }
}
