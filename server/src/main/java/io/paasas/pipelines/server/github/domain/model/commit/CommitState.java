package io.paasas.pipelines.server.github.domain.model.commit;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum CommitState {
	@JsonProperty("error")
	ERROR,
	
	@JsonProperty("failure")
	FAILURE,
	
	@JsonProperty("pending")
	PENDING,
	
	@JsonProperty("success")
	SUCCESS
}
