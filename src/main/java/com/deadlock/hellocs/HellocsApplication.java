package com.deadlock.hellocs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class HellocsApplication {

	/**
	 * Application entry point that launches the Spring Boot application.
	 *
	 * @param args command-line arguments passed to the application; may be empty
	 */
	public static void main(String[] args) {
		SpringApplication.run(HellocsApplication.class, args);
	}

}