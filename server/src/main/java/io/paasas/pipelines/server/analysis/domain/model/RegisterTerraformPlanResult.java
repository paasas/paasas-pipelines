package io.paasas.pipelines.server.analysis.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@AllArgsConstructor
@Builder(toBuilder = true)
public class RegisterTerraformPlanResult {
	boolean success;
	String message;
}
