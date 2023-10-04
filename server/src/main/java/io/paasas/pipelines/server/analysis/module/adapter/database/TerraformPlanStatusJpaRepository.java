package io.paasas.pipelines.server.analysis.module.adapter.database;

import org.springframework.data.jpa.repository.JpaRepository;

import io.paasas.pipelines.server.analysis.module.adapter.database.entity.TerraformExecutionKey;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.TerraformPlanStatusEntity;

public interface TerraformPlanStatusJpaRepository
		extends JpaRepository<TerraformPlanStatusEntity, TerraformExecutionKey> {

}
