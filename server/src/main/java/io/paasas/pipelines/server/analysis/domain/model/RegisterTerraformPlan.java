package io.paasas.pipelines.server.analysis.domain.model;

import java.util.Map;

import io.paasas.pipelines.deployment.domain.model.deployment.JobInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@AllArgsConstructor
@Builder(toBuilder = true)
public class RegisterTerraformPlan {
	JobInfo jobInfo;
	GitRevision gitRevision;
	String packageName;
	Map<String, String> params;
	int pullRequestNumber;
	TerraformExecutionState state;

}
