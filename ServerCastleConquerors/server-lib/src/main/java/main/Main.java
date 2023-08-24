package main;

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
	
	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}
}
