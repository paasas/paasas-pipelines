package io.paasas.pipelines.server.analysis.domain.model;

import io.paasas.pipelines.server.github.domain.model.commit.CommitState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@AllArgsConstructor
@Builder(toBuilder = true)
public class RegisterTerraformPlanResult {
	CommitState checkStatus;
	boolean success;
	String message;
}
