package io.paasas.pipelines.cli.module.adapter.stdout;

import io.paasas.pipelines.cli.domain.io.Output;

public class ConsoleOutput implements Output {

	@Override
	public void println(String line) {
		System.out.println(line);
	}

}
