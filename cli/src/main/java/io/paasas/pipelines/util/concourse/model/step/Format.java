package io.paasas.pipelines.util.concourse.model.step;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Format {
	@JsonProperty("json")
	JSON,

	@JsonProperty("raw")
	RAW,

	@JsonProperty("trim")
	TRIM,

	@JsonProperty("yaml")
	YAML,

	@JsonProperty("yml")
	YML
}
