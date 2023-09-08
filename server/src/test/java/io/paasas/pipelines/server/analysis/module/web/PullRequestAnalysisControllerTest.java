package io.paasas.pipelines.server.analysis.module.web;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.paasas.pipelines.deployment.domain.model.app.App;
import io.paasas.pipelines.deployment.domain.model.deployment.JobInfo;
import io.paasas.pipelines.deployment.domain.model.deployment.RegisterCloudRunDeployment;
import io.paasas.pipelines.server.analysis.domain.model.GitRevision;
import io.paasas.pipelines.server.analysis.domain.model.RefreshPullRequestAnalysisRequest;
import io.paasas.pipelines.server.analysis.domain.model.RegisterFirebaseAppDeployment;
import io.paasas.pipelines.server.analysis.domain.model.RegisterTerraformDeployment;
import io.paasas.pipelines.server.github.domain.port.backend.PullRequestRepository;

public class PullRequestAnalysisControllerTest extends AnalysisWebTest {
	@Autowired
	PullRequestRepository pullRequestRepository;

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

		client.post().uri("/api/ci/pull-request-analysis")
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
				.is2xxSuccessful();

		pullRequestRepository.listPullRequestsReviewComments(2, "paasas/paasas-pipelines");
	}
}