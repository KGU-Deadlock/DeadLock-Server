package com.deadlock.hellocs.streak;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = "com.deadlock.hellocs.streak")
@EnableMongoRepositories(basePackages = "com.deadlock.hellocs.streak.adapter.out.persistence")
public class StreakServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StreakServiceApplication.class, args);
    }
}
