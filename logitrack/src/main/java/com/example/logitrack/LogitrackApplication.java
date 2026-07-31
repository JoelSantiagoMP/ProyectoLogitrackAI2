package com.example.logitrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication
public class LogitrackApplication {

	public static void main(String[] args) {
		SpringApplication.run(LogitrackApplication.class, args);

	}

}
