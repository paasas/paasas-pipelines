package io.paasas.pipelines.server.analysis.domain.port.api;

import io.paasas.pipelines.server.analysis.domain.model.RegisterCloudRunDeployment;
import io.paasas.pipelines.server.analysis.domain.model.RegisterFirebaseAppDeployment;
import io.paasas.pipelines.server.analysis.domain.model.RegisterTerraformDeployment;

public interface DeploymentDomain {
	void registerCloudRunDeployment(RegisterCloudRunDeployment registerCloudRunDeployment);

	void registerFirebaseAppDeployment(RegisterFirebaseAppDeployment registerFirebaseAppDeployment);

	void registerTerraformDeployment(RegisterTerraformDeployment registerTerraformDeployment);

}
