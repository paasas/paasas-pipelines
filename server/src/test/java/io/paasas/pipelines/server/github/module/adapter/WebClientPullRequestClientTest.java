package io.paasas.pipelines.server.github.module.adapter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.paasas.pipelines.server.PaasasPipelinesServerApplication;
import io.paasas.pipelines.server.github.domain.model.pull.UpdateIssueCommentRequest;
import io.paasas.pipelines.server.github.domain.port.backend.IssueCommentRepository;

@SpringBootTest(classes = PaasasPipelinesServerApplication.class, properties = "spring.profiles.active=test,secrets")
public class WebClientPullRequestClientTest {
	@Autowired
	IssueCommentRepository issueCommentRepository;

	@Test
	public void assertListPullRequests() {
		issueCommentRepository.createIssueComment(2, "paasas/paasas-pipelines", UpdateIssueCommentRequest.builder()
				.body("yo")
				.build());

		issueCommentRepository.listPullRequestsReviewComments(2, "paasas/paasas-pipelines");
	}
}
