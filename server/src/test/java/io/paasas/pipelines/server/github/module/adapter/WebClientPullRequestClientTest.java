package io.paasas.pipelines.server.github.module.adapter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.paasas.pipelines.server.PaasasPipelinesServerApplication;
import io.paasas.pipelines.server.github.domain.port.backend.PullRequestRepository;
import io.paasas.pipelines.server.github.module.adapter.model.pull.CreatePullRequestComment;

@SpringBootTest(classes = PaasasPipelinesServerApplication.class, properties = "spring.profiles.active=test,secrets")
public class WebClientPullRequestClientTest {
	@Autowired
	PullRequestRepository pullRequestRepository;

	@Test
	public void assertListPullRequests() {
		pullRequestRepository.createPullRequestComment(2, "paasas/paasas-pipelines", CreatePullRequestComment.builder()
				.body("yo")
				.build());

		pullRequestRepository.listPullRequestsReviewComments(2, "paasas/paasas-pipelines");
	}
}