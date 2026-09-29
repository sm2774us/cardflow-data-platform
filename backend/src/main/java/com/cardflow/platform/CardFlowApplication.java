package com.cardflow.platform;

import com.cardflow.platform.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class CardFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(CardFlowApplication.class, args);
    }
}
