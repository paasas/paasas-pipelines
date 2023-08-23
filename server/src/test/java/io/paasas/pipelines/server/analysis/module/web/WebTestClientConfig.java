package io.paasas.pipelines.server.analysis.module.web;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.reactive.server.WebTestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

import io.paasas.pipelines.server.security.module.CiSecurityConfiguration;

@Configuration
public class WebTestClientConfig {

	@TestConfiguration
	static class WebTestClientBuilderCustomizerConfig {
		@Bean
		public WebTestClientBuilderCustomizer webTestClientBuilderCustomizer(
				CiSecurityConfiguration ciSecurityConfiguration) {
			var user = ciSecurityConfiguration.getUsers().stream().findFirst().orElseThrow();

			return (builder) -> builder.defaultHeader(
					HttpHeaders.AUTHORIZATION,
					"Basic " + HttpHeaders.encodeBasicAuth(
							user.getUsername(),
							user.getPassword(),
							null));
		}
	}
}
