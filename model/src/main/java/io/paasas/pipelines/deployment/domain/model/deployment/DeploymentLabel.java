package io.paasas.pipelines.deployment.domain.model.deployment;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum DeploymentLabel {
	@JsonProperty("accp")
	ACCP,

	@JsonProperty("prod")
	PROD

}
