package io.paasas.pipelines.cli.module.adapter.concourse;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.paasas.pipelines.ConcourseConfiguration;
import io.paasas.pipelines.cli.domain.exception.IllegalCommandArgumentsException;
import io.paasas.pipelines.cli.domain.io.Output;
import io.paasas.pipelines.deployment.domain.model.DeploymentManifest;
import io.paasas.pipelines.platform.module.adapter.concourse.DeploymentConcoursePipeline;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GenerateDeploymentConcoursePipeline extends PipelineSupplierCommand {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();

	DeploymentConcoursePipeline deploymentConcoursePipeline;

	public GenerateDeploymentConcoursePipeline(
			Output errorOutput,
			ConcourseConfiguration configuration,
			DeploymentConcoursePipeline deploymentConcoursePipeline) {
		super(errorOutput, configuration);

		this.deploymentConcoursePipeline = deploymentConcoursePipeline;
	}

	@Override
	public void printUsage(String error) {
		errorOutput.println(error + "\n");
		errorOutput.println(
				"""
						Usage:
						  pipelines-concourse generate-deployment-pipeline <deploymentManifest> <outputPipeline>

						Command arguments:
						  deploymentManifest			File with deployment definition
						  outputPipeline				Output file container the concuorse pipeline definition""");
	}

	@Override
	protected void process(String... args) {
		if (args.length != 3) {
			throw new IllegalCommandArgumentsException("expected two arguments for command");
		}

		var target = args[0];
		var deploymentManifestFile = args[1];
		var pipelineFile = args[2];

		assertFileExists(deploymentManifestFile);
		assertIsWritableFile(pipelineFile);

		writeFile(pipelineFile, deploymentConcoursePipeline.pipeline(
				readDeploymentManifest(deploymentManifestFile),
				target));
	}

	private DeploymentManifest readDeploymentManifest(String deploymentManifestFile) {
		try (var inputStream = new FileInputStream(Path.of(deploymentManifestFile).toFile())) {
			return OBJECT_MAPPER.readValue(inputStream, DeploymentManifest.class);
		} catch (IOException e) {
			log.debug("error while reading deployment manifest", e);

			throw new IllegalCommandArgumentsException(String.format(
					"file %s is not a file platform manifest, cause: %s",
					deploymentManifestFile,
					e.getMessage()));
		}
	}

}
