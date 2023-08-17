package io.paasas.pipelines.util.concourse.model.step;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Version {
	@JsonProperty("every")
	EVERY,

	@JsonProperty("latest")
	LATEST
}
