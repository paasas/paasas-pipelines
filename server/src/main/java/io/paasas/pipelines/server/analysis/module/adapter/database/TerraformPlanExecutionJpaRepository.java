package io.paasas.pipelines.server.analysis.module.adapter.database;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.paasas.pipelines.server.analysis.module.adapter.database.entity.PullRequestAnalysisEntity;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.TerraformExecutionKey;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.TerraformPlanExecutionEntity;

public interface TerraformPlanExecutionJpaRepository
		extends JpaRepository<TerraformPlanExecutionEntity, TerraformExecutionKey> {

	List<TerraformPlanExecutionEntity> findByKeyPullRequestAnalysis(PullRequestAnalysisEntity pullRequestAnalysis);

	Optional<TerraformPlanExecutionEntity> findByKey(TerraformExecutionKey key);

}
