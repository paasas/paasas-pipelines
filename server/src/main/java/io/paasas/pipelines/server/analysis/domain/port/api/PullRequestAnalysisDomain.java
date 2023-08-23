package io.paasas.pipelines.server.analysis.domain.port.api;

import io.paasas.pipelines.server.analysis.domain.model.RefreshPullRequestAnalysisRequest;

public interface PullRequestAnalysisDomain {
	void refresh(RefreshPullRequestAnalysisRequest request);
}
