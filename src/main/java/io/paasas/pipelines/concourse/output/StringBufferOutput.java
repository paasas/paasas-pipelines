package io.paasas.pipelines.concourse.output;

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
