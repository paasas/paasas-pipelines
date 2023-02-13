package io.paasas.pipelines.concourse.output;

public class ConsoleErrorOutput implements Output {

	@Override
	public void println(String line) {
		System.err.println(line);
	}
}
