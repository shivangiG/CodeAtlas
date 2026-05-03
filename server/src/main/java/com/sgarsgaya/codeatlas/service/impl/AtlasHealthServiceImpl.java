package com.sgarsgaya.codeatlas.service.impl;

import com.sgarsgaya.codeatlas.config.AtlasProperties;
import com.sgarsgaya.codeatlas.dto.HealthResponseDto;
import com.sgarsgaya.codeatlas.service.AtlasHealthService;
import com.sgarsgaya.codeatlas.util.TimeProvider;
import org.springframework.stereotype.Service;

@Service
public class AtlasHealthServiceImpl implements AtlasHealthService {
    private final AtlasProperties atlasProperties;
    private final TimeProvider timeProvider;

    public AtlasHealthServiceImpl(AtlasProperties atlasProperties, TimeProvider timeProvider) {
        this.atlasProperties = atlasProperties;
        this.timeProvider = timeProvider;
    }

    @Override
    public HealthResponseDto health() {
        return new HealthResponseDto("UP", atlasProperties.getAppName(), timeProvider.now().toString());
    }
}
