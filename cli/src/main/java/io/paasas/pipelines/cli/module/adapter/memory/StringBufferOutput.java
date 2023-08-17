package io.paasas.pipelines.cli.module.adapter.memory;

import io.paasas.pipelines.cli.domain.io.Output;

public class StringBufferOutput implements Output {
	private final StringBuffer buffer = new StringBuffer();

	@Override
	public void println(String line) {
		buffer.append(line + "\n");
	}

	public String getOutput() {
		return buffer.toString();
	}
}
