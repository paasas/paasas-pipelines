package io.paasas.pipelines.server.github.module;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.paasas.pipelines.server.github.domain.port.backend.CommitStatusRepository;
import io.paasas.pipelines.server.github.domain.port.backend.PullRequestRepository;
import io.paasas.pipelines.server.github.module.adapter.CommitStatusWebClient;
import io.paasas.pipelines.server.github.module.adapter.PullRequestWebClient;

@Configuration
public class GithubWebClientConfig {

	@Bean
	@ConfigurationProperties(prefix = "pipelines.github")
	public GithubConfiguration githubConfiguration() {
		return new GithubConfiguration();
	}

	@Configuration
	@ConditionalOnProperty(name = "pipelines.github.enabled", havingValue = "true", matchIfMissing = false)
	class RepositoryConfig {
		@Bean
		public RestTemplate githubRestTemplate(GithubConfiguration githubConfiguration) {
			assertNotBlank(githubConfiguration.getAppId(), "app-id");
			assertNotBlank(githubConfiguration.getInstallationId(), "installation-id");
			assertNotBlank(githubConfiguration.getPrivateKeyBase64(), "private-key-base64");

			var messageConverter = new MappingJackson2HttpMessageConverter();
			messageConverter.setObjectMapper(new ObjectMapper()
					.findAndRegisterModules()
					.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE));
			return new RestTemplateBuilder()
					.rootUri(githubConfiguration.getBaseUrl())
					.defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
					.defaultHeader("X-GitHub-Api-Version", githubConfiguration.getApiVersion())
					.messageConverters(messageConverter)
					.interceptors(new GithubAuthenticationInterceptor(githubConfiguration))
					.build();
		}

		@Bean
		public CommitStatusRepository commitStatusRepository(RestTemplate githubRestTemplate) {
			return new CommitStatusWebClient(githubRestTemplate);
		}

		@Bean
		public PullRequestRepository pullRequestRepository(RestTemplate githubRestTemplate) {
			return new PullRequestWebClient(githubRestTemplate);
		}
	}

	private void assertNotBlank(String value, String property) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("expected a value for property pipelines.github." + property);
		}

	}

	static void configureCodecs(ClientCodecConfigurer configurer) {
		var objectMapper = new ObjectMapper()
				.findAndRegisterModules()
				.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

		configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
		configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
	}
}
