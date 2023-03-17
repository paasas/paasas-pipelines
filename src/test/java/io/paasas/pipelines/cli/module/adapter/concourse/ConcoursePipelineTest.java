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
import org.junit.jupiter.api.BeforeEach;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import io.paasas.pipelines.ConcourseConfiguration;
import io.paasas.pipelines.GcpConfiguration;
import io.paasas.pipelines.cli.module.adapter.memory.StringBufferOutput;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ConcoursePipelineTest {
	static final Path DIRECTORY = Path.of(
			System.getProperty("java.io.tmpdir"),
			UUID.randomUUID().toString());

	static final String OUTPUT_FILE = Path
			.of(
					System.getProperty("java.io.tmpdir"),
					UUID.randomUUID().toString(),
					"pipeline.yaml")
			.toString();

	static StringBufferOutput ERROR_OUTPUT;

	ConcourseConfiguration configuration() {
		return ConcourseConfiguration.builder()
				.ciSrcUri("git@github.com:paasas/paasas-pipelines.git")
				.deploymentPathPrefix("teams/")
				.deploymentSrcBranch("main")
				.deploymentSrcUri("git@github.com:daniellavoie/deployment-as-code-demo.git")
				.deploymentTerraformBackendPrefix("terraform/deployments/")
				.githubRepository("daniellavoie/infra-as-code-demo")
				.platformPathPrefix("teams/")
				.platformSrcBranch("v2")
				.platformSrcUri("git@github.com:daniellavoie/infra-as-code-demo.git")
				.platformTerraformBackendPrefix("terraform/platforms/")
				.slackChannel(null)
				.slackWebhookUrl(null)
				.teamsWebhookUrl("https://toto-sti")
				.terraformBackendGcsBucket("terraform-states")
				.terraformSrcBranch("main")
				.terraformSrcUri("git@github.com:daniellavoie/infra-as-code-demo")
				.build();
	}

	GcpConfiguration gcpConfiguration() {
		return new GcpConfiguration(
				null,
				"service-account@yo.com");
	}

	abstract String filesToCopyAntPattern();

	abstract String prefixToStripDuringCopy();

	@BeforeEach
	public void createTmpDirs() {
		ERROR_OUTPUT = new StringBufferOutput();

		try {
			var outputFilePath = Path.of(OUTPUT_FILE);
			Files.createDirectories(outputFilePath.getParent());

			var resources = new PathMatchingResourcePatternResolver()
					.getResources(filesToCopyAntPattern());

			Arrays.stream(resources)
					.peek(resource -> copy(
							resource,
							prefixToStripDuringCopy()))
					.toList();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

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

	private static void copy(Resource resource, String prefixToStrip) {
		try (InputStream inputStream = resource.getInputStream()) {
			var target = Path.of(DIRECTORY.toString(),
					resource.getFile().toString().split(prefixToStrip)[1]);

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
