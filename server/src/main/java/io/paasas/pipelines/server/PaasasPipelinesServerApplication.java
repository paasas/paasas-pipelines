package io.paasas.pipelines.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@PropertySource("classpath:paasas-pipelines-server-default.properties")
public class PaasasPipelinesServerApplication {
	public static void main(String[] args) {
		SpringApplication.run(PaasasPipelinesServerApplication.class, args);
	}
}
