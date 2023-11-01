package io.paasas.pipelines.server.analysis.domain.port.api;

import io.paasas.pipelines.server.analysis.domain.model.PullRequestAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.RefreshPullRequestAnalysisRequest;
import io.paasas.pipelines.server.analysis.domain.model.RegisterTerraformPlan;

public interface PullRequestAnalysisDomain {
	PullRequestAnalysis refresh(RefreshPullRequestAnalysisRequest request);

	PullRequestAnalysis registerTerraformPlan(RegisterTerraformPlan request);
}
