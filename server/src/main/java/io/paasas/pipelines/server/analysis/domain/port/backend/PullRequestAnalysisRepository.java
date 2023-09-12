package io.paasas.pipelines.server.analysis.domain.port.backend;

import io.paasas.pipelines.deployment.domain.model.DeploymentManifest;
import io.paasas.pipelines.server.analysis.domain.model.PullRequestAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.RefreshPullRequestAnalysisRequest;

public interface PullRequestAnalysisRepository {
	PullRequestAnalysis refresh(DeploymentManifest deploymentManifest, RefreshPullRequestAnalysisRequest request);
}
