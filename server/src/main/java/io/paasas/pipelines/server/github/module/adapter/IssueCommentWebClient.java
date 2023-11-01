package io.paasas.pipelines.server.github.module.adapter;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import io.paasas.pipelines.server.github.domain.model.issue.IssueComment;
import io.paasas.pipelines.server.github.domain.model.pull.UpdateIssueCommentRequest;
import io.paasas.pipelines.server.github.domain.port.backend.IssueCommentRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IssueCommentWebClient implements IssueCommentRepository {
	RestTemplate restTemplate;

	@Override
	public IssueComment createIssueComment(
			int pullNumber,
			String repository,
			UpdateIssueCommentRequest request) {
		return restTemplate.postForObject(
				"/repos/" + repository + "/issues/{pullNumber}/comments",
				request,
				IssueComment.class,
				pullNumber);
	}

	@Override
	public List<IssueComment> listPullRequestsReviewComments(int pullNumber, String repository) {
		return restTemplate.exchange(
				"/repos/" + repository + "/issues/{pullNumber}/comments",
				HttpMethod.GET,
				null,
				new ParameterizedTypeReference<List<IssueComment>>() {

				},
				pullNumber)
				.getBody();
	}

	@Override
	public IssueComment updateIssueComment(
			int commentId,
			int pullRequestNumber,
			String repository,
			UpdateIssueCommentRequest request) {
		return restTemplate.postForObject(
				"/repos/" + repository + "/issues/{pullNumber}/comments/{commentId}",
				request,
				IssueComment.class,
				pullRequestNumber,
				commentId);
	}
}
