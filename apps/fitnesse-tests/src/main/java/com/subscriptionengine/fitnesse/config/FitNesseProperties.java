package com.subscriptionengine.fitnesse.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * FitNesse configuration properties
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "fitnesse")
public class FitNesseProperties {
    
    private boolean enabled = true;
    private int port = 9090;
    private String rootPath = "FitNesseRoot";
    private String rootPagePath = "FrontPage";
    
    private ApiConfig api = new ApiConfig();
    private TestDataConfig testData = new TestDataConfig();
    
    @Data
    public static class ApiConfig {
        private String baseUrl = "http://localhost:8080/api";
        private int timeout = 30000;
    }
    
    @Data
    public static class TestDataConfig {
        private boolean cleanupAfterTest = true;
        private boolean useTestDatabase = false;
    }
}
