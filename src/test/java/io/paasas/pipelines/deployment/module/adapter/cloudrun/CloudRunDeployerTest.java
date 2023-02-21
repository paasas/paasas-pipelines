package io.paasas.pipelines.deployment.module.adapter.cloudrun;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.paasas.pipelines.PaasasPipelinesApplication;
import io.paasas.pipelines.deployment.domain.model.App;
import io.paasas.pipelines.deployment.domain.model.DeploymentManifest;

@EnabledIfEnvironmentVariable(named = "RUN_IT", matches = "true")
@SpringBootTest(classes = PaasasPipelinesApplication.class)
public class CloudRunDeployerTest {
	@Autowired
	CloudRunDeployer cloudRunDeployer;

	@Test
	public void canDeploy() {
		Assertions.assertNotNull(cloudRunDeployer);

		var deploymentManifest = DeploymentManifest.builder()
				.apps(List.of(
						App.builder()
								.name("hello")
								.image("us-docker.pkg.dev/cloudrun/container/hello")
								.port(8080)
								.build()))
				.project("control-plane-377914")
				.region("northamerica-northeast1")
				.build();

		cloudRunDeployer.deploy(deploymentManifest);

		deploymentManifest = deploymentManifest.toBuilder().apps(List.of()).build();

		cloudRunDeployer.deploy(deploymentManifest);
	}

}
