package io.paasas.pipelines.concourse;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.paasas.pipelines.concourse.command.CommandProcessor;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@SpringBootApplication
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaasasPipelinesApplication implements CommandLineRunner {
	CommandProcessor commandProcessor;

	public static void main(String[] args) {
		SpringApplication.run(PaasasPipelinesApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		commandProcessor.execute(args);
	}
}
