package com.buglens;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BugLensBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BugLensBackendApplication.class, args);
	}

}
