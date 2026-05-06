package com.ims;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableMongoAuditing
@EnableAsync
@EnableScheduling
@EnableRetry
public class ImsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImsApplication.class, args);
    }
}
