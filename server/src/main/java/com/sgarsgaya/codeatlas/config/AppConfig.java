package com.sgarsgaya.codeatlas.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AtlasProperties.class)
public class AppConfig {
}
