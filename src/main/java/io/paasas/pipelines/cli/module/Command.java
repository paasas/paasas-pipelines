package io.paasas.pipelines.cli.module;

import java.util.Arrays;

public enum Command {
	GENERATE_DEPLOYMENT_PIPELINE(
			"generate-deployment-pipeline",
			"""

					"""),
	GENERATE_PLATFORM_PIPELINE(
			"generate-platform-pipeline",
			"""

					"""),

	UPDATE_GOOGLE_DEPLOYMENT(
			"update-google-deployment",
			"""

					""");

	final String command;
	final String usage;

	Command(String command, String usage) {
		this.command = command;
		this.usage = usage;
	}

	public String getCommand() {
		return command;
	}

	public String getUsage() {
		return usage;
	}

	public static Command fromCommand(String command) {
		return Arrays.stream(Command.values())
				.filter(cmd -> cmd.getCommand().equals(command))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException());
	}
}
