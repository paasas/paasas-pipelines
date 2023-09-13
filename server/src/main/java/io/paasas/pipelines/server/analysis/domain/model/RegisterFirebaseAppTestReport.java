package io.paasas.pipelines.server.analysis.domain.model;

import io.paasas.pipelines.deployment.domain.model.deployment.JobInfo;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class RegisterFirebaseAppTestReport {
	JobInfo jobInfo;
	GitRevision gitRevision;
	String reportUrl;
	GitRevision testGitRevision;
}
