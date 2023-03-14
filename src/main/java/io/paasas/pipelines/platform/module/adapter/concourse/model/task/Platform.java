package io.paasas.pipelines.platform.module.adapter.concourse.model.task;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Platform {
	@JsonProperty("darwin")
	DARWIN,

	@JsonProperty("linux")
	LINUX,

	@JsonProperty("windows")
	WINDOWS
}
