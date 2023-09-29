package io.paasas.pipelines.server.analysis.module.adapter.database;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.paasas.pipelines.deployment.domain.model.DeploymentManifest;
import io.paasas.pipelines.deployment.domain.model.TerraformWatcher;
import io.paasas.pipelines.deployment.domain.model.app.App;
import io.paasas.pipelines.deployment.domain.model.firebase.FirebaseAppDefinition;
import io.paasas.pipelines.server.analysis.domain.model.AnalysisStatus;
import io.paasas.pipelines.server.analysis.domain.model.CloudRunAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.FindDeploymentRequest;
import io.paasas.pipelines.server.analysis.domain.model.FirebaseAppAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.PullRequestAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.PullRequestAnalysisJobInfo;
import io.paasas.pipelines.server.analysis.domain.model.RefreshPullRequestAnalysisRequest;
import io.paasas.pipelines.server.analysis.domain.model.TerraformAnalysis;
import io.paasas.pipelines.server.analysis.domain.port.backend.CloudRunDeploymentRepository;
import io.paasas.pipelines.server.analysis.domain.port.backend.CloudRunTestReportRepository;
import io.paasas.pipelines.server.analysis.domain.port.backend.FirebaseAppDeploymentRepository;
import io.paasas.pipelines.server.analysis.domain.port.backend.PullRequestAnalysisRepository;
import io.paasas.pipelines.server.analysis.domain.port.backend.TerraformDeploymentRepository;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.PullRequestAnalysisEntity;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.TerraformPlanExecutionEntity;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DatabasePullRequestAnalysisRepository implements PullRequestAnalysisRepository {
	PullRequestAnalysisJpaRepository repository;
	CloudRunDeploymentRepository cloudRunDeploymentRepository;
	CloudRunTestReportRepository cloudRunTestReportRepository;
	FirebaseAppDeploymentRepository firebaseAppDeploymentRepository;
	TerraformDeploymentRepository terraformDeploymentRepository;
	TerraformPlanExecutionJpaRepository terraformPlanExecutionRepository;

	CloudRunAnalysis appAnalysis(App app) {
		return CloudRunAnalysis.builder()
				.deployments(cloudRunDeploymentRepository.findByImageAndTag(app.getImage(), app.getTag()))
				.serviceName(app.getName())
				.testReports(cloudRunTestReportRepository.findByImageAndTag(app.getImage(), app.getTag()))
				.build();
	}

	List<CloudRunAnalysis> cloudRunAnalysis(DeploymentManifest deploymentManifest) {
		return Optional.ofNullable(deploymentManifest.getApps())
				.orElse(List.of())
				.stream()
				.map(this::appAnalysis)
				.toList();
	}

	Optional<FirebaseAppAnalysis> firebaseAppAnalysis(DeploymentManifest deploymentManifest) {
		return Optional.ofNullable(deploymentManifest.getFirebaseApp())
				.map(this::firebaseAppAnalysis);
	}

	FirebaseAppAnalysis firebaseAppAnalysis(FirebaseAppDefinition firebaseAppDefinition) {
		if (firebaseAppDefinition.getGit().getTag() == null || firebaseAppDefinition.getGit().getTag().isBlank()) {
			return FirebaseAppAnalysis.builder()
					.deployments(List.of())
					.status(AnalysisStatus.TAG_REQUIRED)
					.build();
		}

		return FirebaseAppAnalysis.builder()
				.deployments(firebaseAppDeploymentRepository.find(new FindDeploymentRequest(
						firebaseAppDefinition.getGit().getUri(),
						firebaseAppDefinition.getGit().getPath(),
						firebaseAppDefinition.getGit().getTag())))
				.status(AnalysisStatus.REVISION_RESOLVED)
				.build();
	}

	@Override
	@Transactional
	public PullRequestAnalysis refresh(
			DeploymentManifest deploymentManifest,
			RefreshPullRequestAnalysisRequest request) {
		var terraformAnalyses = terraformAnalysis(deploymentManifest);

		var pullRequestAnalysis = PullRequestAnalysis.builder()
				.commit(request.getCommit())
				.commitAuthor(request.getCommitAuthor())
				.cloudRun(cloudRunAnalysis(deploymentManifest))
				.firebase(firebaseAppAnalysis(deploymentManifest).orElse(null))
				.jobInfo(PullRequestAnalysisJobInfo.builder()
						.build(request.getJobInfo().getBuild())
						.job(request.getJobInfo().getJob())
						.pipeline(request.getJobInfo().getPipeline())
						.team(request.getJobInfo().getTeam())
						.timestamp(LocalDateTime.now())
						.url(request.getJobInfo().getUrl())
						.build())
				.manifest(new String(Base64.getDecoder().decode(request.getManifestBase64().getBytes())))
				.projectId(request.getJobInfo().getProjectId())
				.pullRequestNumber(request.getPullRequestNumber())
				.repository(request.getRepository())
				.terraform(terraformAnalyses)
				.build();

		log.info("Updating {}", pullRequestAnalysis);

		refreshTerraformPlanExecutions(
				terraformAnalyses,
				repository.save(PullRequestAnalysisEntity.from(pullRequestAnalysis)));

		return pullRequestAnalysis;
	}

	private void refreshTerraformPlanExecutions(
			List<TerraformAnalysis> terraformAnalyses,
			PullRequestAnalysisEntity pullRequestAnalysis) {
		// invalidates existing executions.
		terraformPlanExecutionRepository
				.deleteAll(terraformPlanExecutionRepository
						.findByKeyPullRequestAnalysis(pullRequestAnalysis)
						.stream()
						.filter(execution -> !execution.getCommitId().equals(pullRequestAnalysis.getCommit()))
						.toList());

		// Create new executions
		terraformPlanExecutionRepository.saveAll(
				terraformAnalyses
						.stream()
						.map(terraformAnalysis -> TerraformPlanExecutionEntity
								.create(terraformAnalysis.getPackageName(), pullRequestAnalysis))
						.toList());
	}

	List<TerraformAnalysis> terraformAnalysis(DeploymentManifest deploymentManifest) {
		return Optional.ofNullable(deploymentManifest.getTerraform())
				.orElse(List.of())
				.stream()
				.map(this::terraformAnalysis)
				.toList();
	}

	TerraformAnalysis terraformAnalysis(TerraformWatcher terraformWatcher) {
		if (terraformWatcher.getGit().getTag() == null || terraformWatcher.getGit().getTag().isBlank()) {
			return TerraformAnalysis.builder()
					.deployments(List.of())
					.packageName(terraformWatcher.getName())
					.status(AnalysisStatus.TAG_REQUIRED)
					.build();
		}

		return TerraformAnalysis.builder()
				.deployments(terraformDeploymentRepository.find(new FindDeploymentRequest(
						terraformWatcher.getGit().getUri(),
						terraformWatcher.getGit().getPath(),
						terraformWatcher.getGit().getTag())))
				.packageName(terraformWatcher.getName())
				.status(AnalysisStatus.REVISION_RESOLVED)
				.build();
	}

}
