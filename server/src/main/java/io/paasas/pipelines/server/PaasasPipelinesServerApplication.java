package io.paasas.pipelines.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource("classpath:paasas-pipelines-server-default.properties")
public class PaasasPipelinesServerApplication {
	public static void main(String[] args) {
		SpringApplication.run(PaasasPipelinesServerApplication.class, args);
	}
}
