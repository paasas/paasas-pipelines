package io.paasas.pipelines.concourse.command.exception;

public class IllegalCommandArgumentsException extends IllegalArgumentException {
	public IllegalCommandArgumentsException(String message) {
		super(message);
	}
}
