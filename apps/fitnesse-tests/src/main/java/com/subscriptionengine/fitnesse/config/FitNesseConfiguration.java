package com.subscriptionengine.fitnesse.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.web.reactive.function.client.WebClient;

import com.subscriptionengine.fitnesse.server.FitNesseServer;

/**
 * FitNesse configuration
 * 
 * This configuration is only active when fitnesse.enabled=true
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fitnesse.enabled", havingValue = "true", matchIfMissing = true)
public class FitNesseConfiguration {
    
    private final FitNesseProperties properties;
    
    @Bean
    public FitNesseServer fitNesseServer() {
        return new FitNesseServer(
            properties.getPort(),
            properties.getRootPath(),
            properties.getRootPagePath()
        );
    }
    
    @Bean
    public WebClient apiWebClient() {
        return WebClient.builder()
            .baseUrl(properties.getApi().getBaseUrl())
            .build();
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void startFitNesse() {
        if (properties.isEnabled()) {
            try {
                FitNesseServer server = fitNesseServer();
                server.start();
                
                log.info("╔════════════════════════════════════════════════════════════╗");
                log.info("║         FitNesse Test Server Started Successfully          ║");
                log.info("╠════════════════════════════════════════════════════════════╣");
                log.info("║  FitNesse URL: http://localhost:{}                      ║", properties.getPort());
                log.info("║  API Base URL: {}                ║", properties.getApi().getBaseUrl());
                log.info("║  Root Path:    {}                              ║", properties.getRootPath());
                log.info("╚════════════════════════════════════════════════════════════╝");
                
            } catch (Exception e) {
                log.error("Failed to start FitNesse server", e);
                throw new RuntimeException("FitNesse startup failed", e);
            }
        }
    }
}
