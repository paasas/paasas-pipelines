package io.paasas.pipelines.deployment.domain.model.app;

import java.util.Map;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class Resources {
	Map<String, String> limits;
}
