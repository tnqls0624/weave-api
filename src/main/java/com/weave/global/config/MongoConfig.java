package com.weave.global.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@Slf4j
@Configuration
@EnableMongoAuditing
public class MongoConfig {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @PostConstruct
    public void logMongoUri() {
        // URI에서 비밀번호 마스킹 (보안)
        String maskedUri = mongoUri.replaceAll("://([^:]+):([^@]+)@", "://$1:****@");
        log.info("=".repeat(80));
        log.info("MongoDB Configuration");
        log.info("=".repeat(80));
        log.info("MongoDB URI (masked): {}", maskedUri);
        log.info("MongoDB URI (full): {}", mongoUri);
        log.info("=".repeat(80));
    }
}
