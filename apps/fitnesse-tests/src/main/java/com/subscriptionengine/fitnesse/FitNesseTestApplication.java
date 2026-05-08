package com.subscriptionengine.fitnesse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * FitNesse Test Application
 * 
 * This is a standalone Spring Boot application that starts a FitNesse server
 * for functional testing of the Subscription Manager API.
 * 
 * Usage:
 *   ./gradlew :apps:fitnesse-tests:bootRun
 * 
 * Access FitNesse at: http://localhost:9090
 */
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
@ComponentScan(basePackages = {
    "com.subscriptionengine.fitnesse"
})
public class FitNesseTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(FitNesseTestApplication.class, args);
    }
}
