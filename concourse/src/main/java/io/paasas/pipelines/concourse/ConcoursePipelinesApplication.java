package io.paasas.pipelines.concourse;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ConcoursePipelinesApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(ConcoursePipelinesApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

	}
}
