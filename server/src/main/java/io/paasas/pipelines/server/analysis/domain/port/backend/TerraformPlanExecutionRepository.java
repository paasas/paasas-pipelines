package io.paasas.pipelines.server.analysis.domain.port.backend;

import java.util.List;

import io.paasas.pipelines.server.analysis.domain.model.TerraformPlanExecution;

public interface TerraformPlanExecutionRepository {
	List<TerraformPlanExecution> findByPullrequest(int pullRequestNumber, String repository);
}
