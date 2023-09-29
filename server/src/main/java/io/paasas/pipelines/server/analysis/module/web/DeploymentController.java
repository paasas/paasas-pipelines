package io.paasas.pipelines.server.analysis.module.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.paasas.pipelines.deployment.domain.model.deployment.RegisterCloudRunDeployment;
import io.paasas.pipelines.server.analysis.domain.model.RegisterFirebaseAppDeployment;
import io.paasas.pipelines.server.analysis.domain.model.RegisterTerraformDeployment;
import io.paasas.pipelines.server.analysis.domain.model.RegisterTerraformPlan;
import io.paasas.pipelines.server.analysis.domain.port.api.DeploymentDomain;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@AllArgsConstructor
@RequestMapping("/api/ci/deployment")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeploymentController {
	DeploymentDomain deploymentDomain;

	@PostMapping("/cloud-run")
	public void registerCloudRunDeployment(
			@RequestBody RegisterCloudRunDeployment request) {
		deploymentDomain.registerCloudRunDeployment(request);
	}

	@PostMapping("/firebase")
	public void registerFirebaseAppDeployment(
			@RequestBody RegisterFirebaseAppDeployment request) {
		deploymentDomain.registerFirebaseAppDeployment(request);
	}

	@PostMapping("/terraform")
	public void registerTerraformDeployment(
			@RequestBody RegisterTerraformDeployment request) {
		deploymentDomain.registerTerraformDeployment(request);
	}

	@PostMapping("/terraform-plan")
	public void registerTerraformPlan(
			@RequestBody RegisterTerraformPlan request) {
		deploymentDomain.registerTerraformPlan(request);
	}
}
