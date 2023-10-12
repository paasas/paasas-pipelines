package io.paasas.pipelines.server.analysis.module.adapter.database;

import java.util.List;

import org.springframework.stereotype.Repository;

import io.paasas.pipelines.server.analysis.domain.model.TerraformPlanExecution;
import io.paasas.pipelines.server.analysis.domain.port.backend.TerraformPlanExecutionRepository;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.TerraformPlanExecutionEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DatabaseTerraformPlanExecutionRepository implements TerraformPlanExecutionRepository {
	TerraformPlanExecutionJpaRepository repository;

	@Override
	public List<TerraformPlanExecution> findByPullrequest(int pullRequestNumber, String repository) {
		return this.repository
				.findByKeyPullRequestAnalysisKeyNumberAndKeyPullRequestAnalysisKeyRepository(
						pullRequestNumber,
						repository)
				.stream()
				.map(TerraformPlanExecutionEntity::to)
				.toList();
	}

}
