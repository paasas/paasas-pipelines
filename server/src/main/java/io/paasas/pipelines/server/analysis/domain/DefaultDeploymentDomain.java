package io.paasas.pipelines.server.analysis.domain;

import org.springframework.stereotype.Repository;

import io.paasas.pipelines.deployment.domain.model.deployment.RegisterCloudRunDeployment;
import io.paasas.pipelines.server.analysis.domain.model.RegisterFirebaseAppDeployment;
import io.paasas.pipelines.server.analysis.domain.model.RegisterTerraformDeployment;
import io.paasas.pipelines.server.analysis.domain.port.api.DeploymentDomain;
import io.paasas.pipelines.server.analysis.domain.port.backend.CloudRunDeploymentRepository;
import io.paasas.pipelines.server.analysis.domain.port.backend.FirebaseAppDeploymentRepository;
import io.paasas.pipelines.server.analysis.domain.port.backend.TerraformDeploymentRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultDeploymentDomain implements DeploymentDomain {
	CloudRunDeploymentRepository cloudRunDeploymentRepository;
	FirebaseAppDeploymentRepository firebaseAppDeploymentRepository;
	TerraformDeploymentRepository terraformDeploymentRepository;

	@Override
	public void registerCloudRunDeployment(RegisterCloudRunDeployment request) {
		log.info("Registering {}", request);

		cloudRunDeploymentRepository.registerDeployment(request);
	}

	@Override
	public void registerFirebaseAppDeployment(RegisterFirebaseAppDeployment request) {
		log.info("Registering {}", request);

		firebaseAppDeploymentRepository.registerDeployment(request);
	}

	@Override
	public void registerTerraformDeployment(RegisterTerraformDeployment request) {
		log.info("Registering {}", request);

		terraformDeploymentRepository.registerDeployment(request);
	}

}
