package io.paasas.pipelines.cli.module.adapter.concourse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import io.paasas.pipelines.cli.domain.exception.IllegalCommandArgumentsException;
import io.paasas.pipelines.platform.module.adapter.concourse.PlatformConcoursePipeline;

@TestInstance(Lifecycle.PER_CLASS)
public class GeneratePlatformConcoursePipelineTest extends ConcoursePipelineTest {
	private void assertCommandUsage(String errorMessage) {
		Assertions.assertEquals(
				errorMessage + baseUsage(),
				ERROR_OUTPUT.getOutput());
	}

	@Test
	public void assertInvalidArgumentCount() throws IOException {
		assertInvalidCommandUsage(
				"expected two arguments for command",
				"invalid-dir");
	}

	private void assertInvalidCommandUsage(String errorMessage, String... args) {
		Assertions.assertThrows(
				IllegalCommandArgumentsException.class,
				() -> command().execute(args));

		assertCommandUsage(errorMessage);
	}

	@Test
	public void assertInvalidInputDirectory() throws IOException {
		assertInvalidCommandUsage(
				"invalid-dir is not a directory",
				"invalid-dir",
				OUTPUT_FILE);
	}

	@Test
	public void assertInvalidOutputFile() throws IOException {
		assertInvalidCommandUsage(
				"cannot write to /asdasdasd/invalid-output-file",
				DIRECTORY.toString(),
				"/asdasdasd/invalid-output-file");
	}

	public void assertTargetScanner() {
		var command = command();

		command.listYamlFiles(DIRECTORY.toAbsolutePath().toString());
	}

	@Test
	public void assertValidOutputArguments() throws IOException {
		command().execute(
				DIRECTORY.toString(),
				OUTPUT_FILE);

		Assertions.assertEquals("", ERROR_OUTPUT.getOutput());

		Assertions.assertEquals(
				ExpectedPlatformsPipeline.PIPELINE,
				new String(Files.readAllBytes(Path.of(OUTPUT_FILE))));
	}

	private String baseUsage() {
		return """


				Usage:
				  pipelines-concourse generate-platform-pipeline <inputDirectory> <outputPipeline>

				Command arguments:
				  inputDirectory				Source directory containing the platform manifests
				  outputPipeline				Output file container the concuorse pipeline definition
				  """;
	}

	private GeneratePlatformConcoursePipeline command() {
		var configuration = configuration();

		var platformPipeline = new PlatformConcoursePipeline(configuration);

		return new GeneratePlatformConcoursePipeline(ERROR_OUTPUT, configuration, platformPipeline);
	}

	@Override
	String filesToCopyAntPattern() {
		return "classpath:platform-manifests/teams/**/*.yaml";
	}

	@Override
	String prefixToStripDuringCopy() {
		return "platform-manifests/teams";
	}

}
