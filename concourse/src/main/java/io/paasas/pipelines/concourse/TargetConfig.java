package io.paasas.pipelines.concourse;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class TargetConfig {
	String name;
	String platformManifestPath;
}
