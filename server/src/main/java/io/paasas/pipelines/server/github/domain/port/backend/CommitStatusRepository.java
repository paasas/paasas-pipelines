package io.paasas.pipelines.server.github.domain.port.backend;

import io.paasas.pipelines.server.github.domain.model.commit.CommitStatus;
import io.paasas.pipelines.server.github.domain.model.commit.CreateCommitStatus;

public interface CommitStatusRepository {
	CommitStatus createCommitStatus(String repository, String sha, CreateCommitStatus request);
}
