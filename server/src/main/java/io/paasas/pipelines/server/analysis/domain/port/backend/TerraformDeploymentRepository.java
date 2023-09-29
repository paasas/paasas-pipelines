package io.paasas.pipelines.server.analysis.domain.port.backend;

import java.util.List;

import io.paasas.pipelines.server.analysis.domain.model.FindDeploymentRequest;
import io.paasas.pipelines.server.analysis.domain.model.RegisterTerraformDeployment;
import io.paasas.pipelines.server.analysis.domain.model.RegisterTerraformPlan;
import io.paasas.pipelines.server.analysis.domain.model.RegisterTerraformPlanResult;
import io.paasas.pipelines.server.analysis.domain.model.TerraformDeployment;

public interface TerraformDeploymentRepository {
	List<TerraformDeployment> find(FindDeploymentRequest findRequest);

	void registerDeployment(RegisterTerraformDeployment registerTerraformDeployment);

	RegisterTerraformPlanResult registerPlan(RegisterTerraformPlan request);
}
