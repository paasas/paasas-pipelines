package io.paasas.pipelines.server.github.module.adapter;

import java.util.HashMap;
import java.util.Map;

import io.paasas.pipelines.server.github.domain.model.commit.CommitStatus;
import io.paasas.pipelines.server.github.domain.model.commit.CreateCommitStatus;
import io.paasas.pipelines.server.github.domain.port.backend.CommitStatusRepository;

public class MockCommitStatusRepository implements CommitStatusRepository {
	private static final Map<String, CreateCommitStatus> COMMIT_STATUSES = new HashMap<>();

	@Override
	public CommitStatus createCommitStatus(String repository, String sha, CreateCommitStatus request) {
		COMMIT_STATUSES.put(String.format("%s/%s", repository, sha), request);

		return CommitStatus.builder()
				.state(request.getState())
				.build();
	}

}
