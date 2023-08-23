package io.paasas.pipelines.server.github.module.adapter;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import io.paasas.pipelines.server.github.domain.port.backend.PullRequestRepository;
import io.paasas.pipelines.server.github.module.adapter.model.pull.CreatePullRequestComment;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PullRequestWebClient implements PullRequestRepository {
	RestTemplate restTemplate;

	@Override
	public void createPullRequestComment(
			int pullNumber,
			String owner,
			String repository,
			CreatePullRequestComment request) {
		restTemplate.postForObject(
				"/repos/{owner}/{repository}/issues/{pullNumber}/comments",
				request,
				Void.class,
				owner,
				repository,
				pullNumber);
	}

	@Override
	public List<Object> listPullRequestsReviewComments(int pullNumber, String owner, String repository) {
		return restTemplate.exchange(
				"/repos/{owner}/{repository}/issues/{pullNumber}/comments",
				HttpMethod.GET,
				null,
				new ParameterizedTypeReference<List<Object>>() {

				},
				owner,
				repository,
				pullNumber)
				.getBody();
	}
}
