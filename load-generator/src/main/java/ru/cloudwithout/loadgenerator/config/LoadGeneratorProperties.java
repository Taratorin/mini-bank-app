package ru.cloudwithout.loadgenerator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "bank.load")
public class LoadGeneratorProperties {

    private boolean enabled = true;
    private String gatewayBaseUrl = "http://localhost:8081";
    private double requestsPerSecond = 300;
    private int concurrency = 128;
    private int connectTimeoutMs = 2000;
    private int responseTimeoutSec = 15;
    private List<String> logins = List.of(
            "serg", "alex", "test",
            "user01", "user02", "user03", "user04", "user05",
            "user06", "user07", "user08", "user09", "user10"
    );
}