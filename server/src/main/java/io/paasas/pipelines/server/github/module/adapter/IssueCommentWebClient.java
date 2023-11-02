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
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
			String repository,
			UpdateIssueCommentRequest request) {
		assert commentId != 0;
		assert repository != null && !repository.isBlank();

		log.debug("Updating issue comment {} from {} with {}", commentId, repository, request);

		return restTemplate.postForObject(
				"/repos/" + repository + "/issues/comments/{commentId}",
				request,
				IssueComment.class,
				commentId);
	}
}
