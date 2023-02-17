package io.paasas.pipelines.concourse.command;

import java.util.Arrays;

import io.paasas.pipelines.concourse.command.exception.IllegalCommandException;
import io.paasas.pipelines.concourse.output.Output;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommandProcessor {
	Output output;
	Output errorOutput;

	GeneratePlatformPipeline generatePlatformPipeline;

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
			errorOutput.println(e.getMessage());

			return 1;
		}

	}

	String[] commandArgs(String... args) {
		return Arrays.copyOfRange(args, 1, args.length);
	}

	private AbstractCommand command(String command) {
		return switch (Command.fromCommand(command)) {
		case GENERATE_PIPELINE -> generatePlatformPipeline;
		case GENERATE_DEPLOYMENT -> throw new UnsupportedOperationException(command + " is not implemented yet");
		};
	}

	private String usage() {
		return """
				Usage:
				  paasas-pipelines <command>

				Available commands:
				  generate-pipeline          Abort a build (aliases: ab)
				  generate-deployment        List the active users since a date or for the past 2 months (aliases: au)
				""";
	}
}
