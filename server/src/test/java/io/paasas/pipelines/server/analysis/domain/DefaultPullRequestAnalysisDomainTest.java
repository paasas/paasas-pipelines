package io.paasas.pipelines.server.analysis.domain;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.paasas.pipelines.deployment.domain.model.DeploymentManifest;
import io.paasas.pipelines.deployment.domain.model.GitWatcher;
import io.paasas.pipelines.deployment.domain.model.TerraformWatcher;
import io.paasas.pipelines.deployment.domain.model.app.App;
import io.paasas.pipelines.deployment.domain.model.firebase.FirebaseAppDefinition;
import io.paasas.pipelines.server.analysis.domain.model.CloudRunAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.CloudRunDeployment;
import io.paasas.pipelines.server.analysis.domain.model.DeploymentInfo;
import io.paasas.pipelines.server.analysis.domain.model.FirebaseAppAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.FirebaseAppDeployment;
import io.paasas.pipelines.server.analysis.domain.model.GitRevision;
import io.paasas.pipelines.server.analysis.domain.model.PullRequestAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.TerraformAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.TerraformDeployment;
import io.paasas.pipelines.server.analysis.domain.model.TerraformExecution;
import io.paasas.pipelines.server.analysis.domain.model.TerraformExecutionState;
import io.paasas.pipelines.server.analysis.domain.model.TerraformPlanExecution;
import io.paasas.pipelines.server.analysis.domain.model.TestReport;

public class DefaultPullRequestAnalysisDomainTest {
	
	private static final LocalDateTime DEPLOYMENT_TIMESTAMP_1 = LocalDateTime.of(2023, 9, 24, 13, 42);
	private static final LocalDateTime DEPLOYMENT_TIMESTAMP_2 = LocalDateTime.of(2023, 9, 24, 13, 43);
	private static final LocalDateTime DEPLOYMENT_TIMESTAMP_3 = LocalDateTime.of(2023, 9, 24, 13, 44);
	private static final LocalDateTime TEST_TIMESTAMP_1 = LocalDateTime.of(2023, 9, 24, 13, 40);
	private static final LocalDateTime TEST_TIMESTAMP_2 = LocalDateTime.of(2023, 9, 24, 13, 41);

	@Test
	void assertStandardTemplate() {
		var expectedResult = """
				# Pull Request Analysis

				## Artifacts

				### Cloud Run services

				#### test-app

				##### Revision

				Image: **my-image**
				Tag: **1.0.0**

				##### Tests

				* :green_circle: **2023-09-24 13:40 - my-google-project** - [Build 1](http://some-super-duper-test-url-1) - [Tests report](http://some-super-duper-test-report-url-1)
				* :red_circle: **2023-09-24 13:41 - my-google-project** - [Build 2](http://some-super-duper-test-url-2) - [Tests report](http://some-super-duper-test-report-url-2)

				##### Past deployments

				* [2023-09-24 13:42 - my-google-project](http://some-super-duper-job-url)
				* [2023-09-24 13:43 - my-google-project](http://some-super-duper-job-url)

				#### untested-app

				:warning: **This artifact is untested**

				##### Revision

				Image: **my-image**
				Tag: **1.0.0**

				##### Tests

				* *No test recorded*

				##### Past deployments

				* [2023-09-24 13:42 - my-google-project](http://some-super-duper-job-url)

				#### not-deployed-app

				:warning: **This artifact is untested**

				:warning: **This artifact was never deployed**

				##### Revision

				Image: **my-image**
				Tag: **1.0.0**

				##### Tests

				* *No test recorded*

				##### Past deployments

				* *No deployment recorded*

				### Firebase App

				#### Revision

				Commit: [0fef174e8481c540cd42dd5e74aea7db86eda6d5](https://github.com/my-test-org/my-test-repository/commit/0fef174e8481c540cd42dd5e74aea7db86eda6d5)
				Tag: [2.0.0](https://github.com/my-test-org/my-test-repository/releases/tag/2.0.0)
				Repository: [my-test-org/my-test-repository](https://github.com/my-test-org/my-test-repository)

				#### Tests

				* :green_circle: **2023-09-24 13:41 - my-google-project** - [Build 2](http://some-super-duper-test-url-2) - [Tests report](http://some-super-duper-test-report-url-2)

				#### Past deployments

				* [2023-09-24 13:43 - my-other-google-project](http://another-super-duper-job-url)

				### Terraform packages

				#### terraform-1
				
				:gray_circle: **Terraform Plan execution is pending**

				##### Revision

				Commit: [ccd1a17014013076ef30b19f1f741e38a3b6374c](https://github.com/my-test-org/my-terraform-repository/commit/ccd1a17014013076ef30b19f1f741e38a3b6374c)
				Tag: [3.0.0](https://github.com/my-test-org/my-terraform-repository/releases/tag/3.0.0)
				Repository: [my-test-org/my-terraform-repository](https://github.com/my-test-org/my-terraform-repository)

				##### Past deployments

				* [2023-09-24 13:44 - my-terraform-google-project](http://another-super-terraform-job-url)

				#### terraform-2
				
				:yellow_circle: **Terraform Plan execution is in progress** - [View job](https://terraform-2-plan-job-url)

				:warning: **This artifact is not configured with a tag**

				##### Revision

				Commit: [ccd1a17014013076ef30b19f1f741e38a3b6374c](https://github.com/my-test-org/my-terraform-repository/commit/ccd1a17014013076ef30b19f1f741e38a3b6374c)
				Branch: [test-branch](https://github.com/my-test-org/my-terraform-repository/tree/test-branch/terraform-subdirectory/test)
				Repository: [my-test-org/my-terraform-repository](https://github.com/my-test-org/my-terraform-repository)

				##### Past deployments

				* [2023-09-24 13:44 - my-terraform-google-project](http://another-super-terraform-job-url)

				#### terraform-3
				
				:green_circle: **Terraform Plan execution completed** - [View job](https://terraform-3-plan-job-url)

				:warning: **This artifact is not configured with a tag**

				:warning: **This artifact was never deployed**

				##### Revision

				Branch: [test-branch](https://github.com/my-test-org/my-terraform-repository/tree/test-branch/terraform-subdirectory/test)
				Repository: [my-test-org/my-terraform-repository](https://github.com/my-test-org/my-terraform-repository)

				##### Past deployments

				* *No deployment recorded*

				#### terraform-4
				
				:red_circle: **Terraform Plan execution failed** - [View job](https://terraform-4-plan-job-url)

				:warning: **This artifact is not configured with a tag**

				:warning: **This artifact was never deployed**

				##### Revision

				Branch: [test-branch](https://github.com/my-test-org/my-terraform-repository/tree/test-branch/terraform-subdirectory/test)
				Repository: [my-test-org/my-terraform-repository](https://github.com/my-test-org/my-terraform-repository)

				##### Past deployments

				* *No deployment recorded*
				""";

		var pullRequestReview = PullRequestAnalysis.builder()
				.cloudRun(List.of(
						CloudRunAnalysis.builder()
								.deployments(List.of(
										CloudRunDeployment.builder()
												.deploymentInfo(DeploymentInfo.builder()
														.url("http://some-super-duper-job-url")
														.projectId("my-google-project")
														.timestamp(DEPLOYMENT_TIMESTAMP_1)
														.build())
												.image("my-image")
												.tag("1.0.0")
												.build(),
										CloudRunDeployment.builder()
												.deploymentInfo(DeploymentInfo.builder()
														.url("http://some-super-duper-job-url")
														.projectId("my-google-project")
														.timestamp(DEPLOYMENT_TIMESTAMP_2)
														.build())
												.image("my-image")
												.tag("1.0.0")
												.build()))
								.serviceName("test-app")
								.testReports(List.of(
										TestReport.builder()
												.buildName("1")
												.buildUrl("http://some-super-duper-test-url-1")
												.projectId("my-google-project")
												.reportUrl("http://some-super-duper-test-report-url-1")
												.successful(true)
												.timestamp(TEST_TIMESTAMP_1)
												.build(),
										TestReport.builder()
												.buildName("2")
												.buildUrl("http://some-super-duper-test-url-2")
												.projectId("my-google-project")
												.reportUrl("http://some-super-duper-test-report-url-2")
												.timestamp(TEST_TIMESTAMP_2)
												.build()))
								.build(),
						CloudRunAnalysis.builder()
								.deployments(List.of(CloudRunDeployment.builder()
										.deploymentInfo(DeploymentInfo.builder()
												.url("http://some-super-duper-job-url")
												.projectId("my-google-project")
												.timestamp(DEPLOYMENT_TIMESTAMP_1)
												.build())
										.image("my-image")
										.tag("1.0.0")
										.build()))
								.serviceName("untested-app")
								.testReports(List.of())
								.build(),
						CloudRunAnalysis.builder()
								.deployments(List.of())
								.serviceName("not-deployed-app")
								.testReports(List.of())
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
										.repository("my-test-org/my-test-repository")
										.tag("2.0.0")
										.build())
								.testReports(List.of(TestReport.builder()
										.buildName("2")
										.buildUrl("http://some-super-duper-test-url-2")
										.projectId("my-google-project")
										.reportUrl("http://some-super-duper-test-report-url-2")
										.successful(true)
										.timestamp(TEST_TIMESTAMP_2)
										.build()))
								.build()))
						.build())
				.terraform(List.of(
						TerraformAnalysis.builder()
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
												.repository("my-test-org/my-terraform-repository")
												.tag("3.0.0")
												.build())
										.build()))
								.build(),
						TerraformAnalysis.builder()
								.packageName("terraform-2")
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
												.repository("my-test-org/my-terraform-repository")
												.branch("test-branch")
												.build())
										.build()))
								.planExecution(TerraformPlanExecution.builder()
										.execution(TerraformExecution.builder()
												.jobUrl("https://terraform-2-plan-job-url")
												.packageName("terraform-2")
												.state(TerraformExecutionState.RUNNING)
												.build())
										.build())
								.build(),
						TerraformAnalysis.builder()
								.packageName("terraform-3")
								.deployments(List.of())
								.planExecution(TerraformPlanExecution.builder()
										.execution(TerraformExecution.builder()
												.jobUrl("https://terraform-3-plan-job-url")
												.packageName("terraform-3")
												.state(TerraformExecutionState.SUCCESS)
												.build())
										.build())
								.build(),
						TerraformAnalysis.builder()
								.packageName("terraform-4")
								.deployments(List.of())
								.planExecution(TerraformPlanExecution.builder()
										.execution(TerraformExecution.builder()
												.jobUrl("https://terraform-4-plan-job-url")
												.packageName("terraform-4")
												.state(TerraformExecutionState.FAILED)
												.build())
										.build())
								.build()))
				.build();

		Assertions.assertEquals(
				expectedResult,
				DefaultPullRequestAnalysisDomain.generatePullRequestReviewBody(
						DeploymentManifest.builder()
								.apps(List.of(
										App.builder()
												.name("test-app")
												.image("my-image")
												.tag("1.0.0")
												.build(),
										App.builder()
												.name("untested-app")
												.image("my-image")
												.tag("1.0.0").build(),
										App.builder()
												.name("not-deployed-app")
												.image("my-image")
												.tag("1.0.0").build()))
								.firebaseApp(FirebaseAppDefinition.builder()
										.git(GitWatcher.builder()
												.branch("test-branch")
												.uri("git@github.com:my-test-org/my-test-repository")
												.tag("2.0.0")
												.build())
										.build())
								.terraform(List.of(
										TerraformWatcher.builder()
												.name("terraform-1")
												.git(GitWatcher.builder()
														.path("terraform-subdirectory/test")
														.tag("3.0.0")
														.uri("git@github.com:my-test-org/my-terraform-repository.git")
														.build())
												.build(),
										TerraformWatcher.builder()
												.name("terraform-2")
												.git(GitWatcher.builder()
														.branch("test-branch")
														.path("terraform-subdirectory/test")
														.uri("git@github.com:my-test-org/my-terraform-repository.git")
														.build())
												.build(),
										TerraformWatcher.builder()
												.name("terraform-3")
												.git(GitWatcher.builder()
														.branch("test-branch")
														.path("terraform-subdirectory/test")
														.uri("git@github.com:my-test-org/my-terraform-repository.git")
														.build())
												.build(),
										TerraformWatcher.builder()
												.name("terraform-4")
												.git(GitWatcher.builder()
														.branch("test-branch")
														.path("terraform-subdirectory/test")
														.uri("git@github.com:my-test-org/my-terraform-repository.git")
														.build())
												.build()))
								.build(),
						pullRequestReview));
	}

	@Test
	void assertNotDeployedFirebaseApp() {
		var expectedResult = """
				# Pull Request Analysis

				## Artifacts

				### Firebase App

				:warning: **This artifact is untested**

				:warning: **This artifact was never deployed**

				:warning: **This artifact is not configured with a tag**

				#### Revision

				Branch: [test-branch](https://github.com/my-test-org/my-test-repository/tree/test-branch)
				Repository: [my-test-org/my-test-repository](https://github.com/my-test-org/my-test-repository)

				#### Tests

				* *No test recorded*

				#### Past deployments

				* *No deployment recorded*
				""";

		var pullRequestReview = PullRequestAnalysis.builder()
				.firebase(FirebaseAppAnalysis.builder()
						.deployments(List.of())
						.build())
				.build();
		Assertions.assertEquals(
				expectedResult,
				DefaultPullRequestAnalysisDomain.generatePullRequestReviewBody(
						DeploymentManifest.builder()
								.firebaseApp(FirebaseAppDefinition.builder()
										.git(GitWatcher.builder()
												.branch("test-branch")
												.uri("git@github.com:my-test-org/my-test-repository")
												.build())

										.build())
								.build(),
						pullRequestReview));
	}
}
