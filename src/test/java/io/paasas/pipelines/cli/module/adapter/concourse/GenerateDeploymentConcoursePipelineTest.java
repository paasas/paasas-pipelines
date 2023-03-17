package io.paasas.pipelines.cli.module.adapter.concourse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import io.paasas.pipelines.cli.domain.exception.IllegalCommandArgumentsException;
import io.paasas.pipelines.deployment.module.adapter.concourse.DeploymentConcoursePipeline;

@TestInstance(Lifecycle.PER_CLASS)
public class GenerateDeploymentConcoursePipelineTest extends ConcoursePipelineTest {
	@Test
	public void assertInvalidArgumentCount() throws IOException {
		assertInvalidCommandUsage(
				"expected two arguments for command",
				"invalid-dir");
	}

	private GenerateDeploymentConcoursePipeline command() {
		var configuration = configuration();

		var deploymentPipeline = new DeploymentConcoursePipeline(configuration, gcpConfiguration());

		return new GenerateDeploymentConcoursePipeline(ERROR_OUTPUT, configuration, deploymentPipeline);
	}

	void assertInvalidCommandUsage(String errorMessage, String... args) {
		Assertions.assertThrows(
				IllegalCommandArgumentsException.class,
				() -> command().execute(args));

		assertCommandUsage(errorMessage);
	}

	@Test
	public void assertInvalidManifestFileDirectory() throws IOException {
		assertInvalidCommandUsage(
				"file invalid-manifest-file does not exist",
				"invalid-target",
				"invalid-manifest-file",
				OUTPUT_FILE);
	}

	@Test
	public void assertInvalidOutputFile() throws IOException {
		assertInvalidCommandUsage(
				"cannot write to /asdasdasd/invalid-output-file",
				"invalid-target",
				DIRECTORY.toString(),
				"/asdasdasd/invalid-output-file");
	}

	@Test
	public void assertValidOutputArguments() throws IOException {
		var manifestPath = Path.of(DIRECTORY.toString(), "/project1/backend/dev.yaml").toString();
		command().execute(
				"project1-backend-dev",
				manifestPath,
				OUTPUT_FILE);

		Assertions.assertEquals("", ERROR_OUTPUT.getOutput());

		Assertions.assertEquals(
				ExpectedDeploymentsPipeline.PIPELINE
				.replace("{{manifest-path}}", manifestPath),
				new String(Files.readAllBytes(Path.of(OUTPUT_FILE))));
	}

	private void assertCommandUsage(String errorMessage) {
		Assertions.assertEquals(
				errorMessage + baseUsage(),
				ERROR_OUTPUT.getOutput());
	}

	private String baseUsage() {
		return """


				Usage:
				  pipelines-concourse generate-deployment-pipeline <deploymentManifest> <outputPipeline>

				Command arguments:
				  deploymentManifest			File with deployment definition
				  outputPipeline				Output file container the concuorse pipeline definition
				""";
	}

	@Override
	String filesToCopyAntPattern() {
		return "classpath:deployment-manifests/teams/**/*.yaml";
	}

	@Override
	String prefixToStripDuringCopy() {
		return "deployment-manifests/teams";
	}
}
