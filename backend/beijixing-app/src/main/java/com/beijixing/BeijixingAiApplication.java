package com.beijixing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.beijixing")
@EnableCaching
@EnableScheduling
public class BeijixingAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BeijixingAiApplication.class, args);
    }
}