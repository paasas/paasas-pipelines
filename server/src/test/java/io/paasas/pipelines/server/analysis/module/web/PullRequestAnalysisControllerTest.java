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
import io.paasas.pipelines.server.analysis.module.adapter.database.PullRequestAnalysisJpaRepository;
import io.paasas.pipelines.server.github.domain.port.backend.PullRequestRepository;

public class PullRequestAnalysisControllerTest extends AnalysisWebTest {
	@Autowired
	PullRequestRepository pullRequestRepository;

	@Autowired
	PullRequestAnalysisJpaRepository analysisRepository;

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
						.manifestBase64(new String(Base64.getEncoder().encode("""
								project: my-project
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
								- name: my-package
								  git:
								    uri: git@github.com:paasas/terraform-repository.git
								    tag: 1.1.0
								    path: terraform-path
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
				pullRequestAnalysis.getCloudRun().get(0).getDeployments().get(0).getTestReports().size());

		Assertions.assertEquals(
				1,
				pullRequestAnalysis.getFirebase().getDeployments().get(0).getTestReports().size());

		pullRequestRepository.listPullRequestsReviewComments(2, "paasas/paasas-pipelines");
	}
}
