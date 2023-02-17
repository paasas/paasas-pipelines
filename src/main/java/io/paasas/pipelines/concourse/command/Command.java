package io.paasas.pipelines.concourse.command;

import java.util.Arrays;

public enum Command {
	GENERATE_PIPELINE(
			"generate-pipeline",
			"""

					"""),

	GENERATE_DEPLOYMENT(
			"generate-deployment",
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
