package io.paasas.pipelines.cli.module.adapter.concourse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import io.paasas.pipelines.cli.domain.exception.IllegalCommandArgumentsException;
import io.paasas.pipelines.cli.module.adapter.memory.StringBufferOutput;
import io.paasas.pipelines.platform.module.ConcourseConfiguration;
import io.paasas.pipelines.platform.module.adapter.concourse.PlatformConcoursePipeline;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TestInstance(Lifecycle.PER_CLASS)
public class GeneratePlatformConcoursePipelineTest {
	private static final Path DIRECTORY = Path.of(
			System.getProperty("java.io.tmpdir"),
			UUID.randomUUID().toString());

	private static final String OUTPUT_FILE = Path
			.of(
					System.getProperty("java.io.tmpdir"),
					UUID.randomUUID().toString(),
					"pipeline.yaml")
			.toString();

	private static StringBufferOutput ERROR_OUTPUT;

	@AfterEach
	void cleanupTmpDir() {
		try {
			Files.walk(DIRECTORY)
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
		} catch (Exception e) {
			log.error("Failed to cleanup temporary directory", e);
		}
	}

	@BeforeEach
	public void createTmpDirs() {
		ERROR_OUTPUT = new StringBufferOutput();

		try {
			var outputFilePath = Path.of(OUTPUT_FILE);
			Files.createDirectories(outputFilePath.getParent());

			var resources = new PathMatchingResourcePatternResolver()
					.getResources("classpath:platform-manifests/teams/**/*.yaml");

			Assertions.assertEquals(4, resources.length);

			Arrays.stream(resources)
					.peek(resource -> copy(
							resource))
					.toList();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void assertTargetScanner() {
		var command = command();

		command.listYamlFiles(DIRECTORY.toAbsolutePath().toString());
	}

	@Test
	public void assertInvalidArgumentCount() throws IOException {
		assertInvalidCommandUsage(
				"expected two arguments for command",
				"invalid-dir");
	}

	private GeneratePlatformConcoursePipeline command() {
		var configuration = new ConcourseConfiguration(
				"https://github.com/paasas/paasas-pipelines",
				"daniellavoie/infra-as-code-demo",
				"teams/",
				"https://github.com/daniellavoie/deployment-as-code-demo",
				"main",
				"teams/",
				"v2",
				"https://github.com/daniellavoie/infra-as-code-demo",
				null,
				null,
				"https://toto-sti",
				"terraform-states",
				"main",
				"https://github.com/daniellavoie/infra-as-code-demo");

		var platformPipeline = new PlatformConcoursePipeline(configuration);

		return new GeneratePlatformConcoursePipeline(ERROR_OUTPUT, configuration, platformPipeline);
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

	@Test
	public void assertValidOutputArguments() throws IOException {
		command().execute(
				DIRECTORY.toString(),
				OUTPUT_FILE);

		Assertions.assertEquals("", ERROR_OUTPUT.getOutput());

		Assertions.assertEquals(
				ExpectedPipeline.PIPELINE,
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
				  pipelines-concourse generate-platform-pipeline <inputDirectory> <outputPipeline>

				Command arguments:
				  inputDirectory				Source directory containing the platform manifests
				  outputPipeline				Output file container the concuorse pipeline definition
				  """;
	}

	private static void copy(Resource resource) {
		try (InputStream inputStream = resource.getInputStream()) {
			var target = Path.of(DIRECTORY.toString(),
					resource.getFile().toString().split("platform-manifests/teams")[1]);

			Files.createDirectories(target.getParent());

			log.info("Copying {} to {}", resource.toString(), target.toString());

			Files.copy(
					inputStream,
					target,
					StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
