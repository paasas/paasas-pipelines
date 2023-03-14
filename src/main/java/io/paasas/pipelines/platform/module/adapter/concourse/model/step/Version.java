package io.paasas.pipelines.platform.module.adapter.concourse.model.step;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Version {
	@JsonProperty("every")
	EVERY,

	@JsonProperty("latest")
	LATEST
}
