package io.paasas.pipelines.cli.domain.ports.backend;

import io.paasas.pipelines.deployment.domain.model.DeploymentManifest;

public interface Deployer {
	void deploy(DeploymentManifest deploymentManifest);
}
