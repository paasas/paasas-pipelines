package io.paasas.pipelines.server.analysis.domain.model;

import java.util.Map;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class TerraformDeployment {
	DeploymentInfo deploymentInfo;
	GitRevision gitRevision;
	Map<String, String> params;
}
