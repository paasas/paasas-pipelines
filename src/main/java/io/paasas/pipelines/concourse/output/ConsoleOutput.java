package io.paasas.pipelines.concourse.output;

public class ConsoleOutput implements Output {

	@Override
	public void println(String line) {
		System.out.println(line);
	}

}
