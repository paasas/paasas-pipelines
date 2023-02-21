package io.paasas.pipelines.cli.domain.exception;

public class IllegalCommandArgumentsException extends IllegalArgumentException {
	public IllegalCommandArgumentsException(String message) {
		super(message);
	}
}
