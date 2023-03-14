package io.paasas.pipelines.cli.module.adapter.google;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.paasas.pipelines.cli.domain.exception.IllegalCommandArgumentsException;
import io.paasas.pipelines.cli.domain.io.Output;
import io.paasas.pipelines.cli.domain.ports.backend.Deployer;
import io.paasas.pipelines.cli.module.AbstractCommand;
import io.paasas.pipelines.deployment.domain.model.DeploymentManifest;
import io.paasas.pipelines.deployment.module.adapter.gcp.cloudbuild.CloudBuildDeployer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UpdateGoogleDeployment extends AbstractCommand {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();

	CloudBuildDeployer cloudBuildDeployer;
	Deployer cloudRunDeployer;
	Output errorOutput;

	@Override
	public void printUsage(String error) {
		errorOutput.println(error + "\n");
		errorOutput.println(
				"""
						Usage:
						  pipelines-concourse update-google-deployment <deploymentManifest>

						Command arguments:
						  deploymentManifest	Path to the deployment manifest file""");
	}

	@Override
	protected void process(String... args) {
		if (args.length != 1) {
			throw new IllegalCommandArgumentsException("expected one argument for command");
		}

		var file = args[0];

		assertExists(file);

		var deploymentManifest = loadDeploymentManifest(file);

		cloudBuildDeployer.synchronizeProjectBuilds(deploymentManifest);
		cloudRunDeployer.deploy(deploymentManifest);
	}

	private void assertExists(String file) {
		var path = Path.of(file);

		if (!Files.exists(path)) {
			throw new IllegalCommandArgumentsException("deployment manifest file " + file + " does not exist");
		}
	}

	private DeploymentManifest loadDeploymentManifest(String file) {
		try {
			return OBJECT_MAPPER.readValue(Path.of(file).toFile(), DeploymentManifest.class);
		} catch (IOException e) {
			throw new IllegalCommandArgumentsException(
					"unexpected deployment manifest content in " + file + ", error: " + e.getMessage());
		}
	}
}
