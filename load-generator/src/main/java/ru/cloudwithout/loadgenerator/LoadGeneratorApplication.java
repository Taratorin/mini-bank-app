package ru.cloudwithout.loadgenerator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.cloudwithout.loadgenerator.config.LoadGeneratorProperties;

@SpringBootApplication
@EnableConfigurationProperties(LoadGeneratorProperties.class)
@EnableScheduling
public class LoadGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoadGeneratorApplication.class, args);
    }
}