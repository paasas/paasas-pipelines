package io.paasas.pipelines.server.analysis.domain.model;

import java.util.List;

import io.paasas.pipelines.deployment.domain.model.app.App;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class CloudRunDeployment {
	App app;
	DeploymentInfo deploymentInfo;
	String image;
	String tag;
	List<TestReport> testReports;
}
