package io.paasas.pipelines.server.analysis.module.adapter.database;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import io.paasas.pipelines.server.analysis.domain.model.FindDeploymentRequest;
import io.paasas.pipelines.server.analysis.domain.model.RegisterTerraformDeployment;
import io.paasas.pipelines.server.analysis.domain.model.RegisterTerraformPlan;
import io.paasas.pipelines.server.analysis.domain.model.RegisterTerraformPlanResult;
import io.paasas.pipelines.server.analysis.domain.model.TerraformDeployment;
import io.paasas.pipelines.server.analysis.domain.model.TerraformExecution;
import io.paasas.pipelines.server.analysis.domain.port.backend.TerraformDeploymentRepository;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.PullRequestAnalysisKey;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.TerraformDeploymentEntity;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.TerraformExecutionEntity;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.TerraformExecutionKey;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.TerraformPlanExecutionEntity;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.TerraformPlanStatusEntity;
import io.paasas.pipelines.server.github.domain.model.commit.CommitState;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DatabaseTerraformDeploymentRepository implements TerraformDeploymentRepository {
	PullRequestAnalysisJpaRepository pullRequestAnalysisRepository;
	TerraformDeploymentJpaRepository repository;
	TerraformPlanExecutionJpaRepository terraformPlanExecutionRepository;
	TerraformPlanStatusJpaRepository terraformPlanStatusRepository;

	CommitState computeCommitState(List<TerraformPlanExecutionEntity> executions) {
		return TerraformExecution.computeCommitState(executions.stream()
				.map(TerraformPlanExecutionEntity::getExecution)
				.map(TerraformExecutionEntity::getState)
				.toList());
	}

	@Override
	public List<TerraformDeployment> find(FindDeploymentRequest findRequest) {
		return repository.find(findRequest.gitPath(), findRequest.gitUri(), findRequest.gitTag())
				.stream()
				.map(TerraformDeploymentEntity::to)
				.toList();
	}

	@Override
	public Page<TerraformDeployment> findByPackageNameAndProjectId(
			String packageName,
			String projectId,
			Pageable pageable) {
		return repository.findByPackageNameAndDeploymentInfoProjectId(packageName, projectId, pageable)
				.map(TerraformDeploymentEntity::to);
	}

	private CommitState refreshTerraformPlanStatusCheck(TerraformPlanExecutionEntity execution) {
		/**
		 * This code could be called concurrently by mutiple terraform plan executions
		 * and contains a race condition. Each refresh attempt could potentially not
		 * confirm the other plan is completed given that both are updated at the same
		 * time.
		 * 
		 * The primary key of the status entity will guarantee consistency in the
		 * calculation of the status.
		 * 
		 * The status entity will always trigger an INSERT command and will throw a
		 * constraint exception if it deletes a previous status and another refresh
		 * occured at the same time. The constraint exception is catched and the status
		 * is recomputed until no race condition occurs.
		 */
		var attempt = 0;

		while (true) {
			try {
				var executions = terraformPlanExecutionRepository.findByKeyPullRequestAnalysis(
						execution.getKey().getPullRequestAnalysis());

				var terraformPlanStatus = terraformPlanStatusRepository.findById(execution.getKey())
						.stream()
						.peek(terraformPlanStatusRepository::delete)
						.findAny()
						.orElse(TerraformPlanStatusEntity.builder()
								.key(execution.getKey())
								.build());

				terraformPlanStatus.setCommitState(computeCommitState(executions));

				return terraformPlanStatusRepository
						.save(terraformPlanStatus)
						.getCommitState();
			} catch (DataIntegrityViolationException e) {
				log.error("failed to persist terraform plan check status", e);

				if (++attempt >= 5) {
					throw new RuntimeException("failed to persist terraform plan check status after 5 attempts", e);
				} else {
					sleep();
				}
			}
		}
	}

	@Override
	public void registerDeployment(RegisterTerraformDeployment registerTerraformDeployment) {
		repository.save(TerraformDeploymentEntity.from(registerTerraformDeployment));
	}

	@Override
	@Transactional
	public RegisterTerraformPlanResult registerPlan(RegisterTerraformPlan request) {
		var optionalExecution = terraformPlanExecutionRepository.findByKey(
				TerraformExecutionKey.builder()
						.packageName(request.getPackageName())
						.pullRequestAnalysis(pullRequestAnalysisRepository
								.findById(PullRequestAnalysisKey.builder()
										.number(request.getPullRequestNumber())
										.repository(request.getGitRevision().getRepository())
										.projectId(request.getJobInfo().getProjectId())
										.build())
								.orElseThrow(() -> new IllegalArgumentException(
										String.format(
												"could not find pull request %d for %s",
												request.getPullRequestNumber(),
												request.getGitRevision().getRepository()))))
						.build());

		if (optionalExecution.isEmpty()) {
			var message = String.format(
					"No terraform plan execution for pull request %d for %s with commit %s",
					request.getPullRequestNumber(),
					request.getGitRevision().getRepository(),
					request.getGitRevision().getCommit());

			log.info(message);

			return RegisterTerraformPlanResult.builder()
					.checkStatus(CommitState.SUCCESS)
					.message(message)
					.build();
		}

		var execution = optionalExecution.get();

		execution = terraformPlanExecutionRepository.save(
				execution.toBuilder()
						.execution(execution.getExecution().toBuilder()
								.jobUrl(request.getJobInfo().getUrl())
								.state(request.getState())
								.updateTimestamp(LocalDateTime.now())
								.build())
						.build());

		return RegisterTerraformPlanResult.builder()
				.checkStatus(refreshTerraformPlanStatusCheck(execution))
				.success(true)
				.build();
	}

	private void sleep() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
