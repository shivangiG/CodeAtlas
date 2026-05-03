package com.sgarsgaya.codeatlas.controllers;

import java.time.OffsetDateTime;

import com.sgarsgaya.codeatlas.api.HealthApi;
import com.sgarsgaya.codeatlas.constants.AppConstants;
import com.sgarsgaya.codeatlas.dto.HealthResponseDto;
import com.sgarsgaya.codeatlas.model.GetHealth200Response;
import com.sgarsgaya.codeatlas.service.AtlasHealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AppConstants.API_BASE)
public class HealthController implements HealthApi {

    private final AtlasHealthService atlasHealthService;

    public HealthController(AtlasHealthService atlasHealthService) {
        this.atlasHealthService = atlasHealthService;
    }

    @Override
    public ResponseEntity<GetHealth200Response> getHealth() {
        HealthResponseDto dto = atlasHealthService.health();
        GetHealth200Response response = new GetHealth200Response()
                .status(dto.status())
                .application(dto.application())
                .timestamp(OffsetDateTime.parse(dto.timestamp()));
        return ResponseEntity.ok(response);
    }
}
