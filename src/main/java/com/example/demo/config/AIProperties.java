package com.example.demo.config;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai")
@Data
public class AIProperties {
    // Spring Boot יזהה אוטומטית את ai.balanced, ai.defensive וכו' ויכניס לפה!
    @NestedConfigurationProperty
    private AIStrategyProps balanced;
    @NestedConfigurationProperty
    private AIStrategyProps defensive;
    @NestedConfigurationProperty
    private AIStrategyProps offensive;
}