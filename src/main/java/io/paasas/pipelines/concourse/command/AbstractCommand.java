package io.paasas.pipelines.concourse.command;

import io.paasas.pipelines.concourse.command.exception.IllegalCommandArgumentsException;

public abstract class AbstractCommand {

	public void execute(String... args) {
		try {
			process(args);
		} catch (IllegalCommandArgumentsException e) {
			printUsage(e.getMessage());

			throw e;
		}
	}

	public abstract void printUsage(String error);

	protected abstract void process(String... args);
}
