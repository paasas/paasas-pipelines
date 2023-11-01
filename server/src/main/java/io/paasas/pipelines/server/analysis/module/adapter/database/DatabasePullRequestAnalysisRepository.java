package io.paasas.pipelines.server.analysis.module.adapter.database;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import io.paasas.pipelines.deployment.domain.model.DeploymentManifest;
import io.paasas.pipelines.deployment.domain.model.TerraformWatcher;
import io.paasas.pipelines.deployment.domain.model.app.App;
import io.paasas.pipelines.deployment.domain.model.deployment.JobInfo;
import io.paasas.pipelines.deployment.domain.model.firebase.FirebaseAppDefinition;
import io.paasas.pipelines.server.analysis.domain.model.AnalysisStatus;
import io.paasas.pipelines.server.analysis.domain.model.CloudRunAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.FindDeploymentRequest;
import io.paasas.pipelines.server.analysis.domain.model.FirebaseAppAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.PullRequestAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.PullRequestAnalysisJobInfo;
import io.paasas.pipelines.server.analysis.domain.model.RefreshPullRequestAnalysisRequest;
import io.paasas.pipelines.server.analysis.domain.model.RegisterTerraformPlan;
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

	@Override
	public Optional<PullRequestAnalysis> findExistingPullRequestAnalysis(
			int pullRequestNumber,
			String repository,
			String projectId) {
		return this.repository.findByKeyNumberAndKeyRepositoryAndKeyProjectId(
				pullRequestNumber,
				repository,
				projectId)
				.map(entity -> entity.to(null, null, null));
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

	boolean hasBeenDeployedPreviously(TerraformAnalysis terraformAnalysis, String projectId) {
		return terraformDeploymentRepository
				.findByPackageNameAndProjectId(terraformAnalysis.getPackageName(), projectId, PageRequest.of(0, 1))
				.hasContent();
	}

	PullRequestAnalysis generateAnalysis(
			Integer commentId,
			String commit,
			String commitAuthor,
			DeploymentManifest deploymentManifest,
			JobInfo jobInfo,
			String manifest,
			int pullRequestNumber,
			String repository) {
		var terraformAnalyses = terraformAnalysis(
				deploymentManifest,
				pullRequestNumber,
				repository,
				deploymentManifest.getProject());

		return PullRequestAnalysis.builder()
				.commentId(commentId)
				.commit(commit)
				.commitAuthor(commitAuthor)
				.cloudRun(cloudRunAnalysis(deploymentManifest))
				.firebase(firebaseAppAnalysis(deploymentManifest).orElse(null))
				.jobInfo(PullRequestAnalysisJobInfo.builder()
						.build(jobInfo.getBuild())
						.job(jobInfo.getJob())
						.pipeline(jobInfo.getPipeline())
						.team(jobInfo.getTeam())
						.timestamp(LocalDateTime.now())
						.url(jobInfo.getUrl())
						.build())
				.manifest(manifest)
				.projectId(jobInfo.getProjectId())
				.pullRequestNumber(pullRequestNumber)
				.repository(repository)
				.terraform(terraformAnalyses)
				.build();
	}

	@Override
	@Transactional
	public PullRequestAnalysis refresh(
			DeploymentManifest deploymentManifest,
			RefreshPullRequestAnalysisRequest request) {
		var pullRequestAnalysis = generateAnalysis(
				null,
				request.getCommit(),
				request.getCommitAuthor(),
				deploymentManifest,
				request.getJobInfo(),
				new String(Base64.getDecoder().decode(request.getManifestBase64().getBytes())),
				request.getPullRequestNumber(),
				request.getRepository());

		var updatedPullRequestAnalysis = refresh(pullRequestAnalysis);

		refreshTerraformPlanExecutions(
				pullRequestAnalysis.getTerraform(),
				updatedPullRequestAnalysis);

		return pullRequestAnalysis;
	}

	PullRequestAnalysisEntity refresh(PullRequestAnalysis pullRequestAnalysis) {
		log.info("Updating {}", pullRequestAnalysis);

		return repository.save(PullRequestAnalysisEntity.from(pullRequestAnalysis));
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

		/*
		 * Create new executions only if the terraform resource has already been
		 * deployed on the environment. If the terraform resource has never been
		 * deployed, no concourse jobs is configured to trigger terraform plan.
		 */
		terraformPlanExecutionRepository.saveAll(
				terraformAnalyses
						.stream()
						.filter(terraformAnalysis -> hasBeenDeployedPreviously(
								terraformAnalysis,
								pullRequestAnalysis.getKey().getProjectId()))
						.map(terraformAnalysis -> TerraformPlanExecutionEntity
								.create(terraformAnalysis.getPackageName(), pullRequestAnalysis))
						.toList());
	}

	@Override
	@Transactional
	public PullRequestAnalysis registerTerraformPlan(
			DeploymentManifest deploymentManifest,
			PullRequestAnalysis existingPullRequestAnalysis,
			RegisterTerraformPlan request) {
		var pullRequestAnalysis = generateAnalysis(
				existingPullRequestAnalysis.getCommentId(),
				existingPullRequestAnalysis.getCommit(),
				existingPullRequestAnalysis.getCommitAuthor(),
				deploymentManifest,
				request.getJobInfo(),
				existingPullRequestAnalysis.getManifest(),
				request.getPullRequestNumber(),
				request.getGitRevision().getRepository());

		refresh(pullRequestAnalysis);

		return pullRequestAnalysis;
	}

	List<TerraformAnalysis> terraformAnalysis(
			DeploymentManifest deploymentManifest,
			int pullRequestNumber,
			String repository,
			String projectId) {
		return Optional.ofNullable(deploymentManifest.getTerraform())
				.orElse(List.of())
				.stream()
				.map(terraformWatcher -> terraformAnalysis(terraformWatcher, pullRequestNumber, repository, projectId))
				.toList();
	}

	TerraformAnalysis terraformAnalysis(TerraformWatcher terraformWatcher, int pullRequestNumber, String repository,
			String projectId) {
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
				.planExecution(terraformPlanExecutionRepository
						.findByKeyPackageNameAndKeyPullRequestAnalysisKeyNumberAndKeyPullRequestAnalysisKeyRepositoryAndKeyPullRequestAnalysisKeyProjectId(
								terraformWatcher.getName(),
								pullRequestNumber,
								repository,
								projectId)
						.map(TerraformPlanExecutionEntity::to)
						.orElse(null))
				.status(AnalysisStatus.REVISION_RESOLVED)
				.build();
	}

	@Override
	@Transactional
	public void updateCommentId(int commentId, int pullRequestNumber, String repository, String projectId) {
		this.repository.updateCommentId(commentId, pullRequestNumber, repository, projectId);
	}

}
