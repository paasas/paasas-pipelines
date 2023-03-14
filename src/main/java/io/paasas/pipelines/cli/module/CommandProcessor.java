package io.paasas.pipelines.cli.module;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import io.paasas.pipelines.cli.domain.exception.IllegalCommandException;
import io.paasas.pipelines.cli.domain.io.Output;
import io.paasas.pipelines.cli.module.adapter.concourse.GenerateDeploymentConcoursePipeline;
import io.paasas.pipelines.cli.module.adapter.concourse.GeneratePlatformConcoursePipeline;
import io.paasas.pipelines.cli.module.adapter.google.UpdateGoogleDeployment;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommandProcessor {
	Output output;
	Output errorOutput;

	GenerateDeploymentConcoursePipeline generateDeploymentConcoursePipeline;
	GeneratePlatformConcoursePipeline generatePlatformConcoursePipeline;
	UpdateGoogleDeployment updateGoogleDeployment;

	public int execute(String... args) {
		if (args.length == 0) {
			output.println(usage());

			return 0;
		}

		try {
			command(args[0]).execute(commandArgs(args));

			return 0;
		} catch (IllegalCommandException illegalCommandEx) {
			errorOutput.println(usage());

			return 1;
		} catch (IllegalArgumentException illegalArgumentEx) {

			return 1;
		} catch (Exception e) {
			var stringWriter = new StringWriter();
			var printWriter = new PrintWriter(stringWriter);
			e.printStackTrace(printWriter);

			errorOutput.println(stringWriter.toString());

			return 1;
		}

	}

	String[] commandArgs(String... args) {
		return Arrays.copyOfRange(args, 1, args.length);
	}

	private AbstractCommand command(String command) {
		return switch (Command.fromCommand(command)) {
		case GENERATE_DEPLOYMENT_PIPELINE -> generateDeploymentConcoursePipeline;
		case GENERATE_PLATFORM_PIPELINE -> generatePlatformConcoursePipeline;
		case UPDATE_GOOGLE_DEPLOYMENT -> updateGoogleDeployment;
		};
	}

	private String usage() {
		return """
				Usage:
				  paasas-pipelines <command>

				Available commands:
				  generate-deployment-pipeline
				  generate-platform-pipeline
				  update-google-deployment

				""";
	}
}
