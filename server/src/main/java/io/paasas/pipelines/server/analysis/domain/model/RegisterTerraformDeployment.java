package io.paasas.pipelines.server.analysis.domain.model;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@AllArgsConstructor
@Builder(toBuilder = true)
public class RegisterTerraformDeployment {
	JobInfo jobInfo;
	GitRevision gitRevision;
	String packageName;
	Map<String, String> params;
}
