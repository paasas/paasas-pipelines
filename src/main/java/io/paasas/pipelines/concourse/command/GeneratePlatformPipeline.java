package io.paasas.pipelines.concourse.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import io.paasas.pipelines.concourse.PipelinesConcourseConfiguration;
import io.paasas.pipelines.concourse.PlatformPipeline;
import io.paasas.pipelines.concourse.TargetConfig;
import io.paasas.pipelines.concourse.command.exception.IllegalCommandArgumentsException;
import io.paasas.pipelines.concourse.output.Output;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GeneratePlatformPipeline extends AbstractCommand {
	Output errorOutput;
	PipelinesConcourseConfiguration configuration;
	PlatformPipeline platformPipeline;

	public void process(String... args) {
		if (args.length != 2) {
			throw new IllegalCommandArgumentsException("expected two arguments for command");
		}

		var directory = args[0];
		var pipelineFile = args[1];

		assertIsDirectory(directory);
		assertIsWritableFile(pipelineFile);

		var targets = scanTargets(directory);

		try {
			Files.writeString(Path.of(pipelineFile), platformPipeline.pipeline(targets));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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
				.name(file.replace("/", "-").replace(".yaml", "").replace(".yml", ""))
				.platformManifestPath(configuration.getPlatformPathPrefix() + file)
				.terraformExtensionsDirectory(String.format(
						"%s-tf",
						configuration.getPlatformPathPrefix() + file.replace(".yaml", "").replace(".yml", "")))
				.build();
	}

	private void assertIsDirectory(String directory) {
		if (!Files.isDirectory(Path.of(directory))) {
			throw new IllegalCommandArgumentsException(directory + " is not a directory");
		}
	}

	private void assertIsWritableFile(String file) {
		var path = Path.of(file);
		try {
			if (!Files.exists(path)) {
				Files.createFile(path);
			}

			if (!Files.isWritable(Path.of(file))) {
				throw new IllegalCommandArgumentsException("cannot write to " + file);
			}
		} catch (NoSuchFileException noSuchFileEx) {
			throw new IllegalCommandArgumentsException("cannot write to " + file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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
