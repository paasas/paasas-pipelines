package io.paasas.pipelines.server.analysis.domain.model;

import io.paasas.pipelines.deployment.domain.model.deployment.JobInfo;
import io.paasas.pipelines.deployment.domain.model.firebase.Npm;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class RegisterFirebaseAppDeployment {
	String config;
	JobInfo jobInfo;
	GitRevision gitRevision;
	Npm npm;
}