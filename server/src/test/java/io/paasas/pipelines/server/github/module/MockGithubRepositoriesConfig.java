package io.paasas.pipelines.server.github.module;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.paasas.pipelines.server.github.module.adapter.MockAccessTokenRepository;
import io.paasas.pipelines.server.github.module.adapter.MockCommitStatusRepository;
import io.paasas.pipelines.server.github.module.adapter.MockPullRequestRepository;

@Configuration
public class MockGithubRepositoriesConfig {
	@Configuration
	@ConditionalOnProperty(name = "pipelines.github.enabled", havingValue = "false", matchIfMissing = false)
	class RepositoriesConfig {
		@Bean
		public MockAccessTokenRepository accessTokenRepository() {
			return new MockAccessTokenRepository();
		}

		@Bean
		public MockCommitStatusRepository commitStatusRepository() {
			return new MockCommitStatusRepository();
		}

		@Bean
		public MockPullRequestRepository pullRequestRepository() {
			return new MockPullRequestRepository();
		}
	}
}
