package com.cardflow.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {
    @Bean
    WebMvcConfigurer cors(@Value("${CORS_ORIGINS:http://localhost:5173,http://localhost:8081}") String[] origins) {
        return new WebMvcConfigurer() {
            @Override public void addCorsMappings(CorsRegistry r) {
                r.addMapping("/api/**").allowedOrigins(origins).allowedMethods("GET", "POST", "PUT", "DELETE");
            }
        };
    }
}
