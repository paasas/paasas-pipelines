package io.paasas.pipelines.cli.module;

import io.paasas.pipelines.cli.domain.exception.IllegalCommandArgumentsException;

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
