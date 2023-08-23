package io.paasas.pipelines.server.github.domain.port.backend;

import java.util.List;

import io.paasas.pipelines.server.github.module.adapter.model.pull.CreatePullRequestComment;

public interface PullRequestRepository {
	void createPullRequestComment(
			int pullNumber,
			String owner,
			String repository,
			CreatePullRequestComment request);

	List<Object> listPullRequestsReviewComments(int pullNumber, String owner, String repository);
}
