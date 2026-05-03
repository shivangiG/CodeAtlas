package com.sgarsgaya.codeatlas.integration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HealthApiIntegrationTest extends IntegrationTestBase {

    @Test
    void getHealth_returns200_withStatusUp() {
        var response = client.getHealth();

        assertThat(response.getStatus()).isEqualTo("UP");
        assertThat(response.getApplication()).isNotBlank();
        assertThat(response.getTimestamp()).isNotNull();
    }
}
