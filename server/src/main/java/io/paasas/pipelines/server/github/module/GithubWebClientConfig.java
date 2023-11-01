package io.paasas.pipelines.server.github.module;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.paasas.pipelines.server.github.domain.port.backend.AccessTokenRepository;
import io.paasas.pipelines.server.github.domain.port.backend.CommitStatusRepository;
import io.paasas.pipelines.server.github.domain.port.backend.IssueCommentRepository;
import io.paasas.pipelines.server.github.module.adapter.AccessTokenWebClient;
import io.paasas.pipelines.server.github.module.adapter.CommitStatusWebClient;
import io.paasas.pipelines.server.github.module.adapter.IssueCommentWebClient;

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
		public RestTemplate githubRestTemplate(
				AccessTokenRepository accessTokenRepository,
				GithubConfiguration githubConfiguration) {
			assertNotBlank(githubConfiguration.getAppId(), "app-id");
			assertNotBlank(githubConfiguration.getInstallationId(), "installation-id");
			assertNotBlank(githubConfiguration.getPrivateKeyBase64(), "private-key-base64");

			var messageConverter = new MappingJackson2HttpMessageConverter();
			messageConverter.setObjectMapper(new ObjectMapper()
					.findAndRegisterModules()
					.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
					.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false));

			return new RestTemplateBuilder()
					.rootUri(githubConfiguration.getBaseUrl())
					.defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
					.defaultHeader("X-GitHub-Api-Version", githubConfiguration.getApiVersion())
					.messageConverters(messageConverter)
					.interceptors(new GithubAuthenticationInterceptor(accessTokenRepository))
					.build();
		}

		@Bean
		public AccessTokenRepository accessTokenRepository(GithubConfiguration githubConfiguration) {
			return new AccessTokenWebClient(githubConfiguration);
		}

		@Bean
		public CommitStatusRepository commitStatusRepository(RestTemplate githubRestTemplate) {
			return new CommitStatusWebClient(githubRestTemplate);
		}

		@Bean
		public IssueCommentRepository issueCommentRepository(RestTemplate githubRestTemplate) {
			return new IssueCommentWebClient(githubRestTemplate);
		}
	}

	private void assertNotBlank(String value, String property) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("expected a value for property pipelines.github." + property);
		}

	}
}
