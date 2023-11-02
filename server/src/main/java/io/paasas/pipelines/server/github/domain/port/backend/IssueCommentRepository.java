package io.paasas.pipelines.server.github.domain.port.backend;

import java.util.List;

import io.paasas.pipelines.server.github.domain.model.issue.IssueComment;
import io.paasas.pipelines.server.github.domain.model.pull.UpdateIssueCommentRequest;

public interface IssueCommentRepository {
	IssueComment createIssueComment(
			int pullNumber,
			String repository,
			UpdateIssueCommentRequest request);

	List<IssueComment> listPullRequestsReviewComments(int pullNumber, String repository);

	IssueComment updateIssueComment(
			int commentId,
			String repository,
			UpdateIssueCommentRequest request);
}
