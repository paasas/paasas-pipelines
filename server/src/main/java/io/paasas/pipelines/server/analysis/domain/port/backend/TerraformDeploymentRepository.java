package io.paasas.pipelines.server.analysis.domain.port.backend;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import io.paasas.pipelines.server.analysis.domain.model.FindDeploymentRequest;
import io.paasas.pipelines.server.analysis.domain.model.RegisterTerraformDeployment;
import io.paasas.pipelines.server.analysis.domain.model.RegisterTerraformPlan;
import io.paasas.pipelines.server.analysis.domain.model.RegisterTerraformPlanResult;
import io.paasas.pipelines.server.analysis.domain.model.TerraformDeployment;

public interface TerraformDeploymentRepository {
	List<TerraformDeployment> find(FindDeploymentRequest findRequest);

	void registerDeployment(RegisterTerraformDeployment registerTerraformDeployment);

	RegisterTerraformPlanResult registerPlan(RegisterTerraformPlan request);

	Page<TerraformDeployment> findByPackageNameAndProjectId(String packageName, String projectId, Pageable pageable);
}
