package io.paasas.pipelines.server.github.module.adapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.paasas.pipelines.server.github.domain.port.backend.PullRequestRepository;
import io.paasas.pipelines.server.github.module.adapter.model.pull.CreatePullRequestComment;

public class MockPullRequestRepository implements PullRequestRepository {
	public static final Map<String, String> REVIEW_BODIES = new HashMap<>();

	@Override
	public void createPullRequestComment(
			int pullNumber,
			String repository,
			CreatePullRequestComment request) {
		REVIEW_BODIES.put(String.format("%s/%d", repository, pullNumber), request.getBody());
	}

	@Override
	public List<Object> listPullRequestsReviewComments(int pullNumber, String repository) {
		return List.of(REVIEW_BODIES.get(String.format("%s/%d", repository, pullNumber)));
	}

}
