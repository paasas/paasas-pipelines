package io.paasas.pipelines.server.analysis.module.adapter.database;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import io.paasas.pipelines.server.analysis.domain.model.FindDeploymentRequest;
import io.paasas.pipelines.server.analysis.domain.model.RegisterTerraformDeployment;
import io.paasas.pipelines.server.analysis.domain.model.RegisterTerraformPlan;
import io.paasas.pipelines.server.analysis.domain.model.RegisterTerraformPlanResult;
import io.paasas.pipelines.server.analysis.domain.model.TerraformDeployment;
import io.paasas.pipelines.server.analysis.domain.port.backend.TerraformDeploymentRepository;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.PullRequestKey;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.TerraformDeploymentEntity;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.TerraformExecutionKey;
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

	@Override
	public List<TerraformDeployment> find(FindDeploymentRequest findRequest) {
		return repository.find(findRequest.gitPath(), findRequest.gitUri(), findRequest.gitTag())
				.stream()
				.map(TerraformDeploymentEntity::to)
				.toList();
	}

	@Override
	public void registerDeployment(RegisterTerraformDeployment registerTerraformDeployment) {
		repository.save(TerraformDeploymentEntity.from(registerTerraformDeployment));
	}

	@Override
	public RegisterTerraformPlanResult registerPlan(RegisterTerraformPlan request) {
		var optionalExecution = terraformPlanExecutionRepository.findByKey(
				TerraformExecutionKey.builder()
						.packageName(request.getPackageName())
						.pullRequestAnalysis(pullRequestAnalysisRepository
								.findById(PullRequestKey.builder()
										.number(request.getPullRequestNumber())
										.repository(request.getGitRevision().getRepository())
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
					.message(message)
					.build();
		}

		var execution = optionalExecution.get();

		terraformPlanExecutionRepository.save(
				execution.toBuilder()
						.execution(execution.getExecution().toBuilder()
								.state(request.getState())
								.updateTimestamp(LocalDateTime.now())
								.build())
						.build());

		return RegisterTerraformPlanResult.builder()
				.success(true)
				.build();
	}
}
