package main;

import java.util.Collections;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan
@Configuration
@EnableScheduling
public class Main {
	
	private static final int DEFAULT_PORT = 18235;

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}
}
