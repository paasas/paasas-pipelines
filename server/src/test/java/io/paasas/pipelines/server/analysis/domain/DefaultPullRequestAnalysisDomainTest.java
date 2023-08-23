package io.paasas.pipelines.server.analysis.domain;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.paasas.pipelines.server.analysis.domain.model.CloudRunAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.CloudRunDeployment;
import io.paasas.pipelines.server.analysis.domain.model.DeploymentInfo;
import io.paasas.pipelines.server.analysis.domain.model.FirebaseAppAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.FirebaseAppDeployment;
import io.paasas.pipelines.server.analysis.domain.model.GitRevision;
import io.paasas.pipelines.server.analysis.domain.model.PullRequestAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.TerraformAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.TerraformDeployment;

public class DefaultPullRequestAnalysisDomainTest {
	private static final LocalDateTime DEPLOYMENT_TIMESTAMP_1 = LocalDateTime.now();
	private static final LocalDateTime DEPLOYMENT_TIMESTAMP_2 = LocalDateTime.now().minusDays(1);
	private static final LocalDateTime DEPLOYMENT_TIMESTAMP_3 = LocalDateTime.now().minusDays(2);

	private static final String EXPECTED_RESULT = """
			# Pull Request Analysis

			## Artifacts

			### Cloud Run services

			#### test-app

			##### Revision

			Image: **my-image**
			Tag: **1.0.0**

			##### Past deployments

			* [{{DEPLOYMENT_TIMESTAMP_1}} - my-google-project](http://some-super-duper-job-url)

			### Firebase App

			#### Revision

			Commit: [0fef174e8481c540cd42dd5e74aea7db86eda6d5](https://github.com/my-test-org/my-test-repository/commit/0fef174e8481c540cd42dd5e74aea7db86eda6d5)
			Commit Author: [daniellavoie](https://github.com/daniellavoie)
			Tag: [2.0.0](https://github.com/my-test-org/my-test-repository/tree/2.0.0)
			Repository: [my-test-org/my-test-repository](https://github.com/my-test-org/my-test-repository)

			#### Past deployments

			* [{{DEPLOYMENT_TIMESTAMP_2}} - my-other-google-project](http://another-super-duper-job-url)

			### Terraform packages

			#### terraform-1

			##### Revision

			Commit: [ccd1a17014013076ef30b19f1f741e38a3b6374c](https://github.com/my-test-org/my-terraform-repository/commit/ccd1a17014013076ef30b19f1f741e38a3b6374c)
			Commit Author: [daniellavoie](https://github.com/daniellavoie)
			Tag: [3.0.0](https://github.com/my-test-org/my-terraform-repository/tree/3.0.0/terraform-subdirectory/test)
			Repository: [my-test-org/my-terraform-repository](https://github.com/my-test-org/my-terraform-repository)

			##### Past deployments

			* [{{DEPLOYMENT_TIMESTAMP_3}} - my-terraform-google-project](http://another-super-terraform-job-url)"""
			.replace("{{DEPLOYMENT_TIMESTAMP_1}}", DEPLOYMENT_TIMESTAMP_1.toString())
			.replace("{{DEPLOYMENT_TIMESTAMP_2}}", DEPLOYMENT_TIMESTAMP_2.toString())
			.replace("{{DEPLOYMENT_TIMESTAMP_3}}", DEPLOYMENT_TIMESTAMP_3.toString());

	@Test
	void assertTemplate() {
		var pullRequestReview = PullRequestAnalysis.builder()
				.cloudRun(List.of(CloudRunAnalysis.builder()
						.serviceName("test-app")
						.deployments(List.of(CloudRunDeployment.builder()
								.deploymentInfo(DeploymentInfo.builder()
										.url("http://some-super-duper-job-url")
										.projectId("my-google-project")
										.timestamp(DEPLOYMENT_TIMESTAMP_1)
										.build())
								.image("my-image")
								.tag("1.0.0")
								.build()))
						.build()))
				.firebase(FirebaseAppAnalysis.builder()
						.deployments(List.of(FirebaseAppDeployment.builder()
								.deploymentInfo(DeploymentInfo.builder()
										.projectId("my-other-google-project")
										.timestamp(DEPLOYMENT_TIMESTAMP_2)
										.url("http://another-super-duper-job-url")
										.build())
								.gitRevision(GitRevision.builder()
										.commit("0fef174e8481c540cd42dd5e74aea7db86eda6d5")
										.commitAuthor("daniellavoie")
										.repository("my-test-repository")
										.repositoryOwner("my-test-org")
										.tag("2.0.0")
										.build())
								.build()))
						.build())
				.terraform(List.of(TerraformAnalysis.builder()
						.packageName("terraform-1")
						.deployments(List.of(TerraformDeployment.builder()
								.deploymentInfo(DeploymentInfo.builder()
										.projectId("my-terraform-google-project")
										.timestamp(DEPLOYMENT_TIMESTAMP_3)
										.url("http://another-super-terraform-job-url")
										.build())
								.gitRevision(GitRevision.builder()
										.commit("ccd1a17014013076ef30b19f1f741e38a3b6374c")
										.commitAuthor("daniellavoie")
										.path("terraform-subdirectory/test")
										.repository("my-terraform-repository")
										.repositoryOwner("my-test-org")
										.tag("3.0.0")
										.build())
								.build()))
						.build()))
				.build();

		Assertions.assertEquals(
				EXPECTED_RESULT,
				DefaultPullRequestAnalysisDomain.generatePullRequestReviewBody(pullRequestReview));
	}
}
