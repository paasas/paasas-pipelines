package io.paasas.pipelines.server.analysis.domain.model;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class TerraformAnalysis {
	List<TerraformDeployment> deployments;
	String packageName;
	Map<String, String> params;
	AnalysisStatus status;
}
