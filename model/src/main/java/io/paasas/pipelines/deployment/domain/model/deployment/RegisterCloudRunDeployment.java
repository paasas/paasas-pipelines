package io.paasas.pipelines.deployment.domain.model.deployment;

import io.paasas.pipelines.deployment.domain.model.app.App;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@AllArgsConstructor
@Builder(toBuilder = true)
public class RegisterCloudRunDeployment {
	App app;
	JobInfo jobInfo;
	String image;
	String tag;
}
