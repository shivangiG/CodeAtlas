package com.sgarsgaya.codeatlas.unit.controllers;

import com.sgarsgaya.codeatlas.controllers.HealthController;
import com.sgarsgaya.codeatlas.dto.HealthResponseDto;
import com.sgarsgaya.codeatlas.service.AtlasHealthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.sgarsgaya.codeatlas.model.GetHealth200Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private AtlasHealthService atlasHealthService;

    @InjectMocks
    private HealthController healthController;

    @Test
    void getHealth_returnsServicePayload() {
        when(atlasHealthService.health())
                .thenReturn(new HealthResponseDto("UP", "codeatlas", "2026-05-03T00:00:00Z"));

        ResponseEntity<GetHealth200Response> response = healthController.getHealth();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("UP");
        assertThat(response.getBody().getApplication()).isEqualTo("codeatlas");
    }
}
