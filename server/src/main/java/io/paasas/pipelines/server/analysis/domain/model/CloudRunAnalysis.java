package io.paasas.pipelines.server.analysis.domain.model;

import java.util.List;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class CloudRunAnalysis {
	List<CloudRunDeployment> deployments;
	String serviceName;
	List<TestReport> testReports;
}
