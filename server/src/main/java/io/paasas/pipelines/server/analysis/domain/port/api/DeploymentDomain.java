package io.paasas.pipelines.server.analysis.domain.port.api;

import io.paasas.pipelines.deployment.domain.model.deployment.RegisterCloudRunDeployment;
import io.paasas.pipelines.server.analysis.domain.model.RegisterFirebaseAppDeployment;
import io.paasas.pipelines.server.analysis.domain.model.RegisterTerraformDeployment;

public interface DeploymentDomain {
	void registerCloudRunDeployment(RegisterCloudRunDeployment request);

	void registerFirebaseAppDeployment(RegisterFirebaseAppDeployment request);

	void registerTerraformDeployment(RegisterTerraformDeployment request);

}
