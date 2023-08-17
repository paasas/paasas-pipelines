package io.paasas.pipelines.cli.module.adapter.concourse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import io.paasas.pipelines.ConcourseConfiguration;
import io.paasas.pipelines.cli.domain.exception.IllegalCommandArgumentsException;
import io.paasas.pipelines.cli.domain.io.Output;
import io.paasas.pipelines.cli.module.AbstractCommand;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public abstract class PipelineSupplierCommand extends AbstractCommand {
	Output errorOutput;
	ConcourseConfiguration configuration;

	void assertFileExists(String file) {
		if (!Files.exists(Path.of(file))) {
			throw new IllegalCommandArgumentsException(String.format("file %s does not exist", file));
		}
	}

	void assertIsDirectory(String directory) {
		if (!Files.isDirectory(Path.of(directory))) {
			throw new IllegalCommandArgumentsException(directory + " is not a directory");
		}
	}

	void assertIsWritableFile(String file) {
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

	void writeFile(String pipelineFile, String pipeline) {
		try {
			Files.writeString(Path.of(pipelineFile), pipeline);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
