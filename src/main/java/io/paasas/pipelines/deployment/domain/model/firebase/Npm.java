package io.paasas.pipelines.deployment.domain.model.firebase;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class Npm {
	String installArgs;
	String command;
	String env;
}
