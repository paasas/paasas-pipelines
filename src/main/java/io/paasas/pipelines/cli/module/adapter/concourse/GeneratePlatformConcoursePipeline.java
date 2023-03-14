package io.paasas.pipelines.cli.module.adapter.concourse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import io.paasas.pipelines.ConcourseConfiguration;
import io.paasas.pipelines.cli.domain.exception.IllegalCommandArgumentsException;
import io.paasas.pipelines.cli.domain.io.Output;
import io.paasas.pipelines.platform.domain.model.TargetConfig;
import io.paasas.pipelines.platform.module.adapter.concourse.PlatformConcoursePipeline;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GeneratePlatformConcoursePipeline extends PipelineSupplierCommand {
	PlatformConcoursePipeline platformConcoursePipeline;

	public GeneratePlatformConcoursePipeline(
			Output errorOutput,
			ConcourseConfiguration configuration,
			PlatformConcoursePipeline platformConcoursePipeline) {
		super(errorOutput, configuration);

		this.platformConcoursePipeline = platformConcoursePipeline;
	}

	public void process(String... args) {
		if (args.length != 2) {
			throw new IllegalCommandArgumentsException("expected two arguments for command");
		}

		var directory = args[0];
		var pipelineFile = args[1];

		assertIsDirectory(directory);
		assertIsWritableFile(pipelineFile);

		var targets = scanTargets(directory);

		writeFile(pipelineFile, platformConcoursePipeline.pipeline(targets));
	}

	private List<TargetConfig> scanTargets(String directory) {
		return listYamlFiles(directory)
				.stream()
				.sorted()
				.map(this::toTargetConfig)
				.toList();
	}

	List<String> listYamlFiles(String directory) {
		var absolutePath = Path.of(directory).toAbsolutePath().toString();

		try (Stream<Path> walk = Files.walk(Paths.get(directory))) {
			return walk
					.filter(p -> !Files.isDirectory(p))
					.map(p -> p.toAbsolutePath().toString())
					.filter(f -> f.toLowerCase().endsWith(".yaml") || f.toLowerCase().endsWith(".yml"))
					.map(file -> file.substring(absolutePath.length() + 1))
					.toList();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private TargetConfig toTargetConfig(String file) {
		return TargetConfig.builder()
				.deploymentManifestPath(configuration.getDeploymentPathPrefix() + file)
				.name(file.replace("/", "-").replace(".yaml", "").replace(".yml", ""))
				.platformManifestPath(configuration.getPlatformPathPrefix() + file)
				.terraformExtensionsDirectory(String.format(
						"%s-tf",
						configuration.getPlatformPathPrefix() + file.replace(".yaml", "").replace(".yml", "")))
				.build();
	}

	@Override
	public void printUsage(String error) {
		errorOutput.println(error + "\n");
		errorOutput.println(
				"""
						Usage:
						  pipelines-concourse generate-platform-pipeline <inputDirectory> <outputPipeline>

						Command arguments:
						  inputDirectory				Source directory containing the platform manifests
						  outputPipeline				Output file container the concuorse pipeline definition""");
	}
}
