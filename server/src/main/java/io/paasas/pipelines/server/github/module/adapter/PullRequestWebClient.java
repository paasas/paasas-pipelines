package io.paasas.pipelines.server.github.module.adapter;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import io.paasas.pipelines.server.github.domain.model.pull.CreatePullRequestComment;
import io.paasas.pipelines.server.github.domain.port.backend.PullRequestRepository;
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
			String repository,
			CreatePullRequestComment request) {
		restTemplate.postForObject(
				"/repos/" + repository + "/issues/{pullNumber}/comments",
				request,
				Void.class,
				pullNumber);
	}

	@Override
	public List<Object> listPullRequestsReviewComments(int pullNumber, String repository) {
		return restTemplate.exchange(
				"/repos/" + repository + "/issues/{pullNumber}/comments",
				HttpMethod.GET,
				null,
				new ParameterizedTypeReference<List<Object>>() {

				},
				pullNumber)
				.getBody();
	}
}
