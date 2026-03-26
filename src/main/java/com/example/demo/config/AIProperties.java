package com.example.demo.config;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai")
@Data
public class AIProperties {
    // Spring Boot יזהה אוטומטית את ai.balanced, ai.defensive וכו' ויכניס לפה!
    private AIStrategyProps balanced;
    private AIStrategyProps defensive;
    private AIStrategyProps offensive;
}