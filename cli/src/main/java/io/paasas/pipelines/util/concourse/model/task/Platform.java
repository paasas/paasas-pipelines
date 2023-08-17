package io.paasas.pipelines.util.concourse.model.task;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Platform {
	@JsonProperty("darwin")
	DARWIN,

	@JsonProperty("linux")
	LINUX,

	@JsonProperty("windows")
	WINDOWS
}
