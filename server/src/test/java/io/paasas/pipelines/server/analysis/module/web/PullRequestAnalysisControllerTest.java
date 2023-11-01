package io.paasas.pipelines.server.analysis.module.web;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.paasas.pipelines.deployment.domain.model.app.App;
import io.paasas.pipelines.deployment.domain.model.deployment.JobInfo;
import io.paasas.pipelines.deployment.domain.model.deployment.RegisterCloudRunDeployment;
import io.paasas.pipelines.server.analysis.domain.model.GitRevision;
import io.paasas.pipelines.server.analysis.domain.model.PullRequestAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.RefreshPullRequestAnalysisRequest;
import io.paasas.pipelines.server.analysis.domain.model.RegisterCloudRunTestReport;
import io.paasas.pipelines.server.analysis.domain.model.RegisterFirebaseAppDeployment;
import io.paasas.pipelines.server.analysis.domain.model.RegisterFirebaseAppTestReport;
import io.paasas.pipelines.server.analysis.domain.model.RegisterTerraformDeployment;
import io.paasas.pipelines.server.analysis.domain.model.RegisterTerraformPlan;
import io.paasas.pipelines.server.analysis.domain.model.TerraformExecution;
import io.paasas.pipelines.server.analysis.domain.model.TerraformExecutionState;
import io.paasas.pipelines.server.analysis.domain.model.TerraformPlanExecution;
import io.paasas.pipelines.server.analysis.module.adapter.database.PullRequestAnalysisJpaRepository;
import io.paasas.pipelines.server.analysis.module.adapter.database.TerraformPlanExecutionJpaRepository;
import io.paasas.pipelines.server.analysis.module.adapter.database.TerraformPlanStatusJpaRepository;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.PullRequestAnalysisEntity;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.PullRequestAnalysisKey;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.TerraformPlanExecutionEntity;
import io.paasas.pipelines.server.github.domain.model.commit.CommitState;
import io.paasas.pipelines.server.github.domain.port.backend.IssueCommentRepository;

public class PullRequestAnalysisControllerTest extends AnalysisWebTest {
	@Autowired
	IssueCommentRepository issueCommentRepository;

	@Autowired
	PullRequestAnalysisJpaRepository analysisRepository;

	@Autowired
	TerraformPlanExecutionJpaRepository terraformPlanExecutionRepository;

	@Autowired
	TerraformPlanStatusJpaRepository terraformPlanStatusRepository;

	@Test
	public void assertRefresh() {
		client.post()
				.uri("/api/ci/deployment/cloud-run")
				.bodyValue(RegisterCloudRunDeployment.builder()
						.app(App.builder()
								.name("my-app")
								.build())
						.jobInfo(JobInfo.builder()
								.build("100")
								.job("my-job")
								.pipeline("my-pipeline")
								.projectId("my-test-project")
								.team("my-team")
								.url("https://my-build-url")
								.build())
						.image("my-image")
						.tag("1.0.0")
						.build())
				.exchange()
				.expectStatus()
				.is2xxSuccessful();

		client.post()
				.uri("/api/ci/deployment/firebase")
				.bodyValue(RegisterFirebaseAppDeployment.builder()
						.jobInfo(JobInfo.builder()
								.build("102")
								.job("my-firebase-job")
								.pipeline("my-pipeline")
								.projectId("my-test-project")
								.team("my-team")
								.url("https://my-firebase-build-url")
								.build())
						.gitRevision(GitRevision.builder()
								.commit("3ac0c54190f175f7843be9c26c343908182e6d2c")
								.commitAuthor("daniellavoie")
								.repository("paasas/firebase-repository")
								.tag("1.0.1")
								.build())
						.build())
				.exchange()
				.expectStatus()
				.is2xxSuccessful();

		client.post()
				.uri("/api/ci/deployment/terraform")
				.bodyValue(RegisterTerraformDeployment.builder()
						.jobInfo(JobInfo.builder()
								.build("101")
								.job("my-terraform-job")
								.pipeline("my-pipeline")
								.projectId("my-test-project")
								.team("my-team")
								.url("https://my-terraform-build-url")
								.build())
						.gitRevision(GitRevision.builder()
								.commit("321d0eced5225b08d49cdff5e86d69d328a2b192")
								.commitAuthor("daniellavoie")
								.path("terraform-path")
								.repository("paasas/terraform-repository")
								.tag("1.1.0")
								.build())
						.packageName("my-tf-package")
						.params(new HashMap<>(Map.of("my-var", "my-value")))
						.build())
				.exchange()
				.expectStatus()
				.is2xxSuccessful();

		client.post()
				.uri("/api/ci/test-report/cloud-run")
				.bodyValue(RegisterCloudRunTestReport.builder()
						.image("my-image")
						.jobInfo(JobInfo.builder()
								.build("100")
								.job("my-cloud-run-test-job")
								.pipeline("my-pipeline")
								.projectId("my-test-project")
								.team("my-team")
								.url("https://my-build-url")
								.build())
						.reportUrl("https://report-website/test-1")
						.tag("1.0.0")
						.testGitRevision(GitRevision.builder()
								.commit("03c8743b90efd2a70a88161da230ed93ffab5673")
								.commitAuthor("daniellavoie")
								.path("cloud-run-test-path")
								.repository("paasas/cloud-run-repository")
								.tag("10.3.0")
								.build())
						.build())
				.exchange()
				.expectStatus()
				.is2xxSuccessful();

		client.post()
				.uri("/api/ci/test-report/firebase")
				.bodyValue(RegisterFirebaseAppTestReport.builder()
						.gitRevision(GitRevision.builder()
								.commit("3ac0c54190f175f7843be9c26c343908182e6d2c")
								.commitAuthor("daniellavoie")
								.repository("paasas/firebase-repository")
								.tag("1.0.1")
								.build())
						.testGitRevision(GitRevision.builder()
								.commit("b59ea0ab3019458c437724ee86ae3abfb45bee01")
								.commitAuthor("daniellavoie")
								.path("firebase-test-path")
								.repository("paasas/firebase-repository")
								.tag("10.2.0")
								.build())
						.jobInfo(JobInfo.builder()
								.build("100")
								.job("my-firebase-test-job")
								.pipeline("my-pipeline")
								.projectId("my-test-project")
								.team("my-team")
								.url("https://my-build-url")
								.build())
						.reportUrl("https://report-website/test-2")
						.build())
				.exchange()
				.expectStatus()
				.is2xxSuccessful();

		var pullRequestAnalysis = client.post().uri("/api/ci/pull-request-analysis")
				.bodyValue(RefreshPullRequestAnalysisRequest.builder()
						.commit("73c4918ea6f537795701927a4940b95e479dd10e")
						.commitAuthor("daniellavoie")
						.jobInfo(JobInfo.builder()
								.build("10")
								.job("pr-analysis-job")
								.pipeline("my-pipeline")
								.projectId("my-test-project")
								.team("my-team")
								.url("https://my-build-url")
								.build())
						.manifestBase64(new String(Base64.getEncoder().encode("""
								project: my-test-project
								labels:
								- prod
								region: northamerica-northeast1
								apps:
								- name: my-app
								  image: my-image
								  tag: 1.0.0
								  registryType: GCR
								firebaseApp:
								  git:
								    uri: git@github.com:paasas/firebase-repository.git
								    tag: 1.0.1
								  npm:
								    installArgs: --legacy-peer-deps
								    command: env-cmd -f .env npm run build
								    env: |
								      ENV=development
								terraform:
								- name: my-tf-package
								  git:
								    uri: git@github.com:paasas/terraform-repository.git
								    tag: 1.1.0
								    path: terraform-path
								  githubRepository:
								  vars:
								    my-var: my-value""".getBytes())))
						.project("my-tf-package")
						.pullRequestNumber(2)
						.repository("paasas/paasas-pipelines")
						.build())
				.exchange()
				.expectStatus()
				.is2xxSuccessful()
				.expectBody(PullRequestAnalysis.class)
				.returnResult()
				.getResponseBody();

		Assertions.assertEquals(
				1,
				pullRequestAnalysis.getCloudRun().get(0).getTestReports().size());

		Assertions.assertNotNull(pullRequestAnalysis.getCommentId());

		Assertions.assertEquals(
				1,
				pullRequestAnalysis.getFirebase().getDeployments().get(0).getTestReports().size());

		var terraformPlanExecution = terraformPlanExecutionRepository
				.findByKeyPullRequestAnalysis(PullRequestAnalysisEntity.builder()
						.key(PullRequestAnalysisKey.builder()
								.number(2)
								.repository("paasas/paasas-pipelines")
								.projectId("my-test-project")
								.build())
						.build())
				.stream()
				.findFirst()
				.orElseThrow()
				.to();

		Assertions.assertNotNull(terraformPlanExecution.getExecution().getCreateTimestamp());
		Assertions.assertNotNull(terraformPlanExecution.getExecution().getUpdateTimestamp());

		var expectedPlanExecution = TerraformPlanExecution.builder()
				.commitId("73c4918ea6f537795701927a4940b95e479dd10e")
				.execution(TerraformExecution.builder()
						.createTimestamp(terraformPlanExecution.getExecution().getCreateTimestamp())
						.packageName("my-tf-package")
						.state(TerraformExecutionState.PENDING)
						.updateTimestamp(terraformPlanExecution.getExecution().getUpdateTimestamp())
						.build())
				.build();

		Assertions.assertEquals(
				expectedPlanExecution,
				terraformPlanExecution);

		var registerTerraformPlan = RegisterTerraformPlan.builder()
				.jobInfo(JobInfo.builder()
						.build("101")
						.job("my-terraform-plan-job")
						.pipeline("my-pipeline")
						.projectId("my-test-project")
						.team("my-team")
						.url("https://my-terraform-plan-url")
						.build())
				.gitRevision(GitRevision.builder()
						.branch("main")
						.commit("73c4918ea6f537795701927a4940b95e479dd10e")
						.commitAuthor("daniellavoie")
						.path("manifest-path")
						.repository("paasas/paasas-pipelines")
						.build())
				.packageName("my-tf-package")
				.params(new HashMap<>(Map.of("my-var", "my-value")))
				.pullRequestNumber(2)
				.state(TerraformExecutionState.RUNNING)
				.build();

		client.post()
				.uri("/api/ci/deployment/terraform-plan")
				.bodyValue(registerTerraformPlan)
				.exchange()
				.expectStatus()
				.is2xxSuccessful();

		var terraformPlanExecutionEntity = findTerraformPlanExecution();

		terraformPlanExecution = terraformPlanExecutionEntity.to();

		expectedPlanExecution = expectedPlanExecution.toBuilder()
				.execution(expectedPlanExecution.getExecution().toBuilder()
						.updateTimestamp(terraformPlanExecution.getExecution().getUpdateTimestamp())
						.state(TerraformExecutionState.RUNNING)
						.build())
				.build();

		Assertions.assertEquals(
				expectedPlanExecution,
				terraformPlanExecution);

		client.post()
				.uri("/api/ci/deployment/terraform-plan")
				.bodyValue(registerTerraformPlan.toBuilder().state(TerraformExecutionState.SUCCESS).build())
				.exchange()
				.expectStatus()
				.is2xxSuccessful();

		terraformPlanExecutionEntity = findTerraformPlanExecution();

		terraformPlanExecution = terraformPlanExecutionEntity.to();

		Assertions.assertNotEquals(
				expectedPlanExecution.getExecution().getUpdateTimestamp(),
				terraformPlanExecution.getExecution().getUpdateTimestamp());

		expectedPlanExecution = expectedPlanExecution.toBuilder()
				.execution(expectedPlanExecution.getExecution().toBuilder()
						.updateTimestamp(terraformPlanExecution.getExecution().getUpdateTimestamp())
						.state(TerraformExecutionState.SUCCESS)
						.build())
				.build();

		Assertions.assertEquals(
				expectedPlanExecution,
				terraformPlanExecution);

		var terraformPlanStatus = terraformPlanStatusRepository.findById(terraformPlanExecutionEntity.getKey()).get();

		Assertions.assertEquals(CommitState.SUCCESS, terraformPlanStatus.getCommitState());

		issueCommentRepository.listPullRequestsReviewComments(2, "paasas/paasas-pipelines");
	}

	TerraformPlanExecutionEntity findTerraformPlanExecution() {
		return terraformPlanExecutionRepository
				.findByKeyPullRequestAnalysis(PullRequestAnalysisEntity.builder()
						.key(PullRequestAnalysisKey.builder()
								.number(2)
								.repository("paasas/paasas-pipelines")
								.projectId("my-test-project")
								.build())
						.build())
				.stream()
				.findFirst()
				.orElseThrow();
	}
}
