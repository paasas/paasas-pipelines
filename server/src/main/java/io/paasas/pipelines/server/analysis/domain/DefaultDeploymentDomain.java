package io.paasas.pipelines.server.analysis.domain;

import org.springframework.stereotype.Repository;

import io.paasas.pipelines.server.analysis.domain.model.RegisterCloudRunDeployment;
import io.paasas.pipelines.server.analysis.domain.model.RegisterFirebaseAppDeployment;
import io.paasas.pipelines.server.analysis.domain.model.RegisterTerraformDeployment;
import io.paasas.pipelines.server.analysis.domain.port.api.DeploymentDomain;
import io.paasas.pipelines.server.analysis.domain.port.backend.CloudRunDeploymentRepository;
import io.paasas.pipelines.server.analysis.domain.port.backend.FirebaseAppDeploymentRepository;
import io.paasas.pipelines.server.analysis.domain.port.backend.TerraformDeploymentRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultDeploymentDomain implements DeploymentDomain {
	CloudRunDeploymentRepository cloudRunDeploymentRepository;
	FirebaseAppDeploymentRepository firebaseAppDeploymentRepository;
	TerraformDeploymentRepository terraformDeploymentRepository;

	@Override
	public void registerCloudRunDeployment(RegisterCloudRunDeployment registerCloudRunDeployment) {
		cloudRunDeploymentRepository.registerDeployment(registerCloudRunDeployment);
	}

	@Override
	public void registerFirebaseAppDeployment(RegisterFirebaseAppDeployment registerFirebaseAppDeployment) {
		firebaseAppDeploymentRepository.registerDeployment(registerFirebaseAppDeployment);
	}

	@Override
	public void registerTerraformDeployment(RegisterTerraformDeployment registerTerraformDeployment) {
		terraformDeploymentRepository.registerDeployment(registerTerraformDeployment);
	}

}
