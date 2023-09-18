package io.paasas.pipelines.server.analysis.domain.model;

import io.paasas.pipelines.deployment.domain.model.deployment.JobInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@AllArgsConstructor
@Builder(toBuilder = true)
public class RegisterCloudRunTestReport {
	JobInfo jobInfo;
	String image;
	String reportUrl;
	String tag;
	GitRevision testGitRevision;
}
