package io.paasas.pipelines.server.analysis.module.adapter.database;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import io.paasas.pipelines.server.analysis.module.adapter.database.entity.PullRequestAnalysisEntity;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.TerraformExecutionKey;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.TerraformPlanExecutionEntity;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

public interface TerraformPlanExecutionJpaRepository
		extends JpaRepository<TerraformPlanExecutionEntity, TerraformExecutionKey> {

	List<TerraformPlanExecutionEntity> findByKeyPullRequestAnalysis(PullRequestAnalysisEntity pullRequestAnalysis);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@QueryHints({ @QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000") })
	Optional<TerraformPlanExecutionEntity> findByKeyPackageNameAndKeyPullRequestAnalysisKeyNumberAndKeyPullRequestAnalysisKeyRepositoryAndKeyPullRequestAnalysisKeyProjectId(
			String packageName,
			int number,
			String repository,
			String projectId);

	List<TerraformPlanExecutionEntity> findByKeyPullRequestAnalysisKeyNumberAndKeyPullRequestAnalysisKeyRepository(
			int number,
			String repository);

	Optional<TerraformPlanExecutionEntity> findByKey(TerraformExecutionKey key);

}
