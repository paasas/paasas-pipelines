package io.paasas.pipelines.server.analysis.domain.port.backend;

import java.util.Optional;

import io.paasas.pipelines.deployment.domain.model.DeploymentManifest;
import io.paasas.pipelines.server.analysis.domain.model.PullRequestAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.RefreshPullRequestAnalysisRequest;
import io.paasas.pipelines.server.analysis.domain.model.RegisterTerraformPlan;

public interface PullRequestAnalysisRepository {
	Optional<PullRequestAnalysis> findExistingPullRequestAnalysis(int pullRequestNumber, String repository,
			String projectId);

	PullRequestAnalysis refresh(DeploymentManifest deploymentManifest, RefreshPullRequestAnalysisRequest request);

	PullRequestAnalysis registerTerraformPlan(
			DeploymentManifest deploymentManifest,
			PullRequestAnalysis existingPullRequestAnalysis, 
			RegisterTerraformPlan request);

	void updateCommentId(int commentId, int pullRequestNumber, String repository, String projectId);
}
