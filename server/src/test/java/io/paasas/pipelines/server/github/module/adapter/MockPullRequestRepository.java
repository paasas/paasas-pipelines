package io.paasas.pipelines.server.github.module.adapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import io.paasas.pipelines.server.github.domain.model.issue.IssueComment;
import io.paasas.pipelines.server.github.domain.model.pull.UpdateIssueCommentRequest;
import io.paasas.pipelines.server.github.domain.port.backend.IssueCommentRepository;

public class MockPullRequestRepository implements IssueCommentRepository {
	private static final AtomicInteger COUNTER = new AtomicInteger();

	public static final Map<String, IssueComment> REVIEW_BODIES = new HashMap<>();

	@Override
	public IssueComment createIssueComment(
			int pullRequestNumber,
			String repository,
			UpdateIssueCommentRequest request) {
		var commentId = COUNTER.incrementAndGet();

		var issueComment = IssueComment.builder()
				.body(request.getBody())
				.id(commentId)
				.build();

		REVIEW_BODIES.put(String.format("%s/%d", repository, commentId), issueComment);

		return issueComment;
	}

	@Override
	public List<IssueComment> listPullRequestsReviewComments(int pullRequestNumber, String repository) {
		var prefix = String.format("%s/%d", repository, pullRequestNumber);

		return REVIEW_BODIES.entrySet()
				.stream()
				.filter(entry -> entry.getKey().startsWith(prefix))
				.map(Entry::getValue)
				.toList();
	}

	@Override
	public IssueComment updateIssueComment(
			int commentId,
			String repository,
			UpdateIssueCommentRequest request) {
		assert commentId != 0;

		var issueComment = IssueComment.builder()
				.id(commentId)
				.body(request.getBody())
				.build();

		REVIEW_BODIES.put(String.format("%s/%d", repository, commentId), issueComment);

		return issueComment;
	}
}
