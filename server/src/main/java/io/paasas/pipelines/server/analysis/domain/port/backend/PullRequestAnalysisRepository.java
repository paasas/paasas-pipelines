package io.paasas.pipelines.server.analysis.domain.port.backend;

import io.paasas.pipelines.server.analysis.domain.model.PullRequestAnalysis;

public interface PullRequestAnalysisRepository {
	void save(PullRequestAnalysis pullRequestAnalysis);
}
