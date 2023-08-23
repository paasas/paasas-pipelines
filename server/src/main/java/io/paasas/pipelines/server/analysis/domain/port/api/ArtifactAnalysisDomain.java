package io.paasas.pipelines.server.analysis.domain.port.api;

import java.util.List;
import java.util.Optional;

import io.paasas.pipelines.deployment.domain.model.DeploymentManifest;
import io.paasas.pipelines.server.analysis.domain.model.CloudRunAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.FirebaseAppAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.TerraformAnalysis;

public interface ArtifactAnalysisDomain {
	List<CloudRunAnalysis> cloudRunAnalysis(DeploymentManifest deploymentManifest);

	Optional<FirebaseAppAnalysis> firebaseAppAnalysis(DeploymentManifest deploymentManifest);

	List<TerraformAnalysis> terraformAnalysis(DeploymentManifest deploymentManifest);
}
