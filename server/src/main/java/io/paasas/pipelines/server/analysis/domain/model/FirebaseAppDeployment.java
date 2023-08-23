package io.paasas.pipelines.server.analysis.domain.model;

import io.paasas.pipelines.deployment.domain.model.firebase.Npm;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class FirebaseAppDeployment {
	String config;
	DeploymentInfo deploymentInfo;
	GitRevision gitRevision;
	Npm npm;
}
