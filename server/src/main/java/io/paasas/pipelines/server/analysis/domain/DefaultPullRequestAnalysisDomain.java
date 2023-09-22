package io.paasas.pipelines.server.analysis.domain;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.paasas.pipelines.deployment.domain.model.DeploymentManifest;
import io.paasas.pipelines.deployment.domain.model.deployment.DeploymentLabel;
import io.paasas.pipelines.deployment.domain.model.firebase.FirebaseAppDefinition;
import io.paasas.pipelines.server.analysis.domain.model.CloudRunAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.CloudRunDeployment;
import io.paasas.pipelines.server.analysis.domain.model.FirebaseAppAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.FirebaseAppDeployment;
import io.paasas.pipelines.server.analysis.domain.model.PullRequestAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.RefreshPullRequestAnalysisRequest;
import io.paasas.pipelines.server.analysis.domain.model.TerraformAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.TerraformDeployment;
import io.paasas.pipelines.server.analysis.domain.model.TestReport;
import io.paasas.pipelines.server.analysis.domain.port.api.PullRequestAnalysisDomain;
import io.paasas.pipelines.server.analysis.domain.port.backend.PullRequestAnalysisRepository;
import io.paasas.pipelines.server.github.domain.model.commit.CommitState;
import io.paasas.pipelines.server.github.domain.model.commit.CreateCommitStatus;
import io.paasas.pipelines.server.github.domain.model.pull.CreatePullRequestComment;
import io.paasas.pipelines.server.github.domain.port.backend.CommitStatusRepository;
import io.paasas.pipelines.server.github.domain.port.backend.PullRequestRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultPullRequestAnalysisDomain implements PullRequestAnalysisDomain {
	private static final String NO_DEPLOYMENT_WARNING = ":warning: **This artifact was never deployed and revision could not be inferred**";

	private static final String REVIEW_TEMPLATE = """
			# Pull Request Analysis

			## Artifacts

			{{IMPACTED_ARTIFACTS}}""";

	private static final String REVISION_TAG_WARNING = ":warning: **This artifact is not configured with a tag**";

	private static final String CLOUD_RUN_TEMPLATE = """
			### Cloud Run services

			{{CLOUD_RUN_SERVICES}}""";

	private static final String CLOUD_RUN_SERVICE_TEMPLATE = """
			#### {{SERVICE_NAME}}

			##### Revision

			Image: **{{IMAGE}}**
			Tag: **{{TAG}}**

			##### Tests

			{{TESTS}}

			##### Past deployments

			{{DEPLOYMENTS}}""";

	private static final String FIREBASE_TEMPLATE = """
			### Firebase App

			#### Revision

			Commit: {{COMMIT}}
			{{REVISION}}Repository: [{{REPOSITORY}}](https://github.com/{{REPOSITORY}})

			#### Tests

			{{TESTS}}

			#### Past deployments

			{{DEPLOYMENTS}}""";

	private static final String TERRAFORM_TEMPLATE = """
			### Terraform packages

			{{TERRAFORM_PACKAGES}}""";

	private static final String TERRAFORM_ENTRY_TEMPLATE = """
			#### {{NAME}}

			##### Revision

			{{REVISION_WARNINGS}}{{COMMIT_DETAILS}}{{REVISION}}Repository: [{{REPOSITORY}}](https://github.com/{{REPOSITORY}})

			##### Past deployments

			{{DEPLOYMENTS}}
			""";

	private static final String TERRAFORM_COMMIT_DETAILS = """
			Commit: {{COMMIT}}
			""";

	private static final ObjectMapper YAML_MAPPER = new ObjectMapper(YAMLFactory.builder().build());

	CommitStatusRepository commitStatusRepository;
	PullRequestRepository pullRequestRepository;
	PullRequestAnalysisRepository repository;

	@Override
	public PullRequestAnalysis refresh(RefreshPullRequestAnalysisRequest request) {
		var deploymentManfiest = readDeploymentManifest(request);
		var pullRequestAnalysis = repository.refresh(deploymentManfiest, request);

		publishAnalysisToGithub(deploymentManfiest, pullRequestAnalysis);

		return pullRequestAnalysis;
	}

	DeploymentManifest readDeploymentManifest(RefreshPullRequestAnalysisRequest request) {
		try {
			return YAML_MAPPER.readValue(Base64.getDecoder().decode(request.getManifestBase64().getBytes()),
					DeploymentManifest.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private boolean hasNoDeployment(FirebaseAppAnalysis firebaseAppAnalysis) {
		return firebaseAppAnalysis.getDeployments() == null || firebaseAppAnalysis.getDeployments().isEmpty();
	}

	private boolean isTagged(FirebaseAppDefinition firebaseApp) {
		return !isBlank(firebaseApp.getGit().getTag());
	}

	void publishAnalysisToGithub(DeploymentManifest deploymentManifest, PullRequestAnalysis pullRequestAnalysis) {
		pullRequestRepository.createPullRequestComment(
				pullRequestAnalysis.getPullRequestNumber(),
				pullRequestAnalysis.getRepository(),
				CreatePullRequestComment.builder()
						.body(generatePullRequestReviewBody(deploymentManifest, pullRequestAnalysis))
						.build());

		if (deploymentManifest.getLabels() != null
				&& (deploymentManifest.getLabels().contains(DeploymentLabel.ACCP)
						|| deploymentManifest.getLabels().contains(DeploymentLabel.PROD))) {

			var undeployedArtifacts = Stream.of(
					pullRequestAnalysis.getCloudRun().stream()
							.filter(cloudRunAnalysis -> cloudRunAnalysis.getDeployments() == null
									|| cloudRunAnalysis.getDeployments().isEmpty())
							.map(CloudRunAnalysis::getServiceName),
					Optional.ofNullable(pullRequestAnalysis.getFirebase())
							.filter(firebaseAppAnalysis -> isTagged(
									deploymentManifest.getFirebaseApp()))
							.filter(this::hasNoDeployment)
							.map(firebaseAppAnlysis -> "firebase-app")
							.stream(),
					pullRequestAnalysis.getTerraform().stream()
							.filter(terraformAnalysis -> terraformAnalysis.getDeployments() == null ||
									terraformAnalysis.getDeployments().isEmpty())
							.map(terraformAnalysis -> "terraform-"
									+ terraformAnalysis.getPackageName()))
					.flatMap(stream -> stream)
					.toList();

			var untestedArtifacts = Stream.of(
					pullRequestAnalysis.getCloudRun().stream()
							.filter(cloudRunAnalysis -> cloudRunAnalysis.getTestReports() == null
									|| cloudRunAnalysis.getTestReports().isEmpty())
							.map(CloudRunAnalysis::getServiceName),

					Optional.ofNullable(pullRequestAnalysis.getFirebase())

							// Report untested app if no deployment exists with a test report
							.filter(firebaseAppAnalysis -> firebaseAppAnalysis.getDeployments() == null
									|| firebaseAppAnalysis.getDeployments().stream()
											.noneMatch(deployment -> !deployment.getTestReports().isEmpty()))
							.map(firebaseAppAnlysis -> "firebase-app")
							.stream())
					.flatMap(stream -> stream)
					.toList();

			var untaggedArtifacts = Stream.of(
					Optional.ofNullable(deploymentManifest.getFirebaseApp())
							.filter(firebaseApp -> isBlank(firebaseApp.getGit().getTag()))
							.map(firebaseApp -> "firebase-app")
							.stream(),
					deploymentManifest.getTerraform().stream()
							.filter(terraform -> isBlank(terraform.getGit().getTag()))
							.map(terraform -> "terraform-" + terraform.getName()),
					Optional.ofNullable(deploymentManifest.getComposer())
							.stream()
							.flatMap(List::stream)
							.filter(composerConfig -> composerConfig.getDags() != null)
							.filter(composerConfig -> isBlank(
									composerConfig.getDags().getGit().getTag()))
							.map(composerConfig -> composerConfig.getName() + "-dags"))
					.flatMap(stream -> stream)
					.toList();

			commitStatusRepository.createCommitStatus(
					pullRequestAnalysis.getRepository(),
					pullRequestAnalysis.getCommit(),
					CreateCommitStatus.builder()
							.context("compliance/deployments")
							.description(undeployedArtifacts.isEmpty()
									? "All artifacts have been deployed."
									: "Missing deployments for: "
											+ undeployedArtifacts.stream().collect(Collectors.joining(", ")))
							.state(undeployedArtifacts.isEmpty() ? CommitState.SUCCESS : CommitState.ERROR)
							.build());

			commitStatusRepository.createCommitStatus(
					pullRequestAnalysis.getRepository(),
					pullRequestAnalysis.getCommit(),
					CreateCommitStatus.builder()
							.context("compliance/testing")
							.description(untestedArtifacts.isEmpty()
									? "All artifacts have been tested."
									: "Untested artifacts: "
											+ untestedArtifacts.stream().collect(Collectors.joining(", ")))
							.state(untestedArtifacts.isEmpty() ? CommitState.SUCCESS : CommitState.ERROR)
							.build());

			commitStatusRepository.createCommitStatus(
					pullRequestAnalysis.getRepository(),
					pullRequestAnalysis.getCommit(),
					CreateCommitStatus.builder()
							.context("compliance/tagging")
							.description(untaggedArtifacts.isEmpty()
									? "All artifacts are tagged."
									: "Untagged artifacts: "
											+ untaggedArtifacts.stream().collect(Collectors.joining(", ")))
							.state(untaggedArtifacts.isEmpty() ? CommitState.SUCCESS : CommitState.ERROR)
							.build());
		}
	}

	static String generatePullRequestReviewBody(
			DeploymentManifest deploymentManifest,
			PullRequestAnalysis pullRequestAnalysis) {
		return REVIEW_TEMPLATE
				.replace(
						"{{IMPACTED_ARTIFACTS}}",
						Stream
								.of(
										cloudRunServices(pullRequestAnalysis),
										firebaseApp(pullRequestAnalysis),
										terraform(deploymentManifest, pullRequestAnalysis))
								.filter(Optional::isPresent)
								.map(Optional::get)
								.collect(Collectors.joining("\n\n")));
	}

	static Optional<String> cloudRunServices(PullRequestAnalysis pullRequestAnalysis) {
		if (pullRequestAnalysis.getCloudRun() == null || pullRequestAnalysis.getCloudRun().isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(CLOUD_RUN_TEMPLATE
				.replace("{{CLOUD_RUN_SERVICES}}",
						Optional.ofNullable(pullRequestAnalysis.getCloudRun())
								.map(DefaultPullRequestAnalysisDomain::cloudRunServices)
								.orElse("")));
	}

	static String cloudRunServices(List<CloudRunAnalysis> cloudRun) {
		var cloudRunServices = cloudRun.stream()
				.flatMap(DefaultPullRequestAnalysisDomain::cloudRunServices)
				.collect(Collectors.joining("\n"));

		return !cloudRunServices.isBlank() ? cloudRunServices : "No cloud run deployment found";
	}

	static Stream<String> cloudRunServices(CloudRunAnalysis cloudRunAnalysis) {
		if (cloudRunAnalysis.getDeployments() == null || cloudRunAnalysis.getDeployments().isEmpty()) {
			return Stream.empty();
		}

		var latestDeployment = cloudRunAnalysis.getDeployments().stream()
				.sorted((deployment1, deployment2) -> deployment1.getDeploymentInfo().getTimestamp()
						.compareTo(deployment2.getDeploymentInfo().getTimestamp()))
				.reduce((deployment1, deployment2) -> deployment2)
				.orElseThrow(() -> new IllegalStateException(
						String.format(
								"expected at least one deployment for cloud run service %s",
								cloudRunAnalysis.getServiceName())));

		return Stream.of(
				CLOUD_RUN_SERVICE_TEMPLATE
						.replace("{{SERVICE_NAME}}", cloudRunAnalysis.getServiceName())
						.replace("{{IMAGE}}", latestDeployment.getImage())
						.replace("{{TAG}}", latestDeployment.getTag())
						.replace("{{TESTS}}", cloudRunTests(cloudRunAnalysis))
						.replace("{{DEPLOYMENTS}}", cloudRunDeployments(cloudRunAnalysis)));
	}

	static String cloudRunDeployments(CloudRunAnalysis cloudRunAnalysis) {
		if (cloudRunAnalysis.getDeployments() == null || cloudRunAnalysis.getDeployments().isEmpty()) {
			return "*No deployment recorded*";
		}

		return cloudRunAnalysis.getDeployments().stream()
				.map(DefaultPullRequestAnalysisDomain::cloudRunDeployment)
				.collect(Collectors.joining("\n\n"));
	}

	static String cloudRunTests(CloudRunAnalysis cloudRunAnalysis) {
		if (cloudRunAnalysis.getTestReports() == null || cloudRunAnalysis.getTestReports().isEmpty()) {
			return "*No test recorded*";
		}

		return cloudRunAnalysis.getTestReports().stream()
				.map(DefaultPullRequestAnalysisDomain::testReport)
				.collect(Collectors.joining("\n\n"));
	}

	static String cloudRunDeployment(CloudRunDeployment deployment) {
		return String.format(
				"* [%s - %s](%s)",
				deployment.getDeploymentInfo().getTimestamp(),
				deployment.getDeploymentInfo().getProjectId(),
				deployment.getDeploymentInfo().getUrl());
	}

	static String testReport(TestReport testReport) {
		return String.format(
				"* **%s - %s** - [Build %s](%s) - [Tests report](%s)",
				testReport.getTimestamp(),
				testReport.getProjectId(),
				testReport.getBuildName(),
				testReport.getBuildUrl(),
				testReport.getReportUrl());
	}

	static Optional<String> firebaseApp(PullRequestAnalysis pullRequestAnalysis) {
		if (pullRequestAnalysis.getFirebase() == null
				|| pullRequestAnalysis.getFirebase().getDeployments() == null
				|| pullRequestAnalysis.getFirebase().getDeployments().isEmpty()) {
			return Optional.empty();
		}

		var gitRevision = pullRequestAnalysis.getFirebase().getDeployments().stream()
				.sorted((deployment1, deployment2) -> deployment1.getDeploymentInfo().getTimestamp()
						.compareTo(deployment2.getDeploymentInfo().getTimestamp()))
				.reduce((deployment1, deployment2) -> deployment2)
				.map(FirebaseAppDeployment::getGitRevision)
				.orElseThrow(() -> new IllegalStateException(
						"expected at least one deployment for firebase app"));

		var hasTag = isNotBlank(gitRevision.getTag());
		var hasRevision = hasTag || isNotBlank(gitRevision.getBranch());

		return Optional.of(FIREBASE_TEMPLATE
				.replace(
						"{{COMMIT}}",
						String.format(
								"[%s](https://github.com/%s/commit/%s)",
								gitRevision.getCommit(),
								gitRevision.getRepository(),
								gitRevision.getCommit()))
				.replace("{{REPOSITORY}}", gitRevision.getRepository())
				.replace(
						"{{REVISION}}",
						hasRevision
								? String.format(
										"%s: [%s](https://github.com/%s/tree/%s%s)\n",
										hasTag ? "Tag" : "Branch",
										hasTag ? gitRevision.getTag()
												: gitRevision.getBranch(),
										gitRevision.getRepository(),
										hasTag ? gitRevision.getTag()
												: gitRevision.getBranch(),
										Optional.ofNullable(gitRevision.getPath())
												.map(path -> "/" + path).orElse(""))

								: "")
				.replace("{{TESTS}}", firebaseAppTest(pullRequestAnalysis.getFirebase()))
				.replace(
						"{{DEPLOYMENTS}}",
						Optional
								.ofNullable(pullRequestAnalysis.getFirebase().getDeployments())
								.map(deployments -> deployments.stream()
										.map(DefaultPullRequestAnalysisDomain::firebaseAppDeployment)
										.collect(Collectors.joining("\n")))
								.orElse("*No deployment recorded*")));
	}

	static String firebaseAppDeployment(FirebaseAppDeployment deployment) {
		return String.format(
				"* [%s - %s](%s)",
				deployment.getDeploymentInfo().getTimestamp(),
				deployment.getDeploymentInfo().getProjectId(),
				deployment.getDeploymentInfo().getUrl());
	}

	static String firebaseAppTest(FirebaseAppAnalysis firebaseAppAnalysis) {
		if (firebaseAppAnalysis.getDeployments() == null || firebaseAppAnalysis.getDeployments().isEmpty()) {
			return "*No test recorded*";
		}

		return firebaseAppAnalysis.getDeployments().stream()
				.filter(cloudRunDeployment -> cloudRunDeployment.getTestReports() != null)
				.flatMap(cloudRunDeployment -> cloudRunDeployment.getTestReports().stream())
				.map(DefaultPullRequestAnalysisDomain::testReport)
				.collect(Collectors.joining("\n\n"));
	}

	static boolean isNotBlank(String value) {
		return value != null && !value.isBlank();
	}

	static Optional<String> terraform(DeploymentManifest deploymentManifest, PullRequestAnalysis pullRequestAnalysis) {
		if (pullRequestAnalysis.getTerraform() == null || pullRequestAnalysis.getTerraform().isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(TERRAFORM_TEMPLATE
				.replace("{{TERRAFORM_PACKAGES}}",
						Optional.ofNullable(pullRequestAnalysis.getTerraform())
								.map(terraform -> DefaultPullRequestAnalysisDomain.terraform(
										deploymentManifest,
										terraform))
								.orElse("")));
	}

	static String terraform(DeploymentManifest deploymentManifest, List<TerraformAnalysis> terraform) {
		return terraform.stream()
				.flatMap(terraformAnalysis -> DefaultPullRequestAnalysisDomain.terraform(deploymentManifest,
						terraformAnalysis))
				.collect(Collectors.joining("\n"));
	}

	static Stream<String> terraform(DeploymentManifest deploymentManifest, TerraformAnalysis terraformAnalysis) {
		log.info("Computing latest git revision from {}", terraformAnalysis.getDeployments());

		var terraformWatcher = deploymentManifest.getTerraform().stream()
				.filter(watcher -> watcher.getName().equals(terraformAnalysis.getPackageName()))
				.findFirst().orElseThrow();

		var gitRevision = terraformAnalysis.getDeployments().stream()
				.sorted((deployment1, deployment2) -> deployment1.getDeploymentInfo().getTimestamp()
						.compareTo(deployment2.getDeploymentInfo().getTimestamp()))
				.reduce((deployment1, deployment2) -> deployment2)
				.map(TerraformDeployment::getGitRevision)
				.orElse(null);

		var hasTag = isNotBlank(terraformWatcher.getGit().getTag());
		var hasRevision = hasTag || isNotBlank(terraformWatcher.getGit().getBranch());

		var commitDetails = gitRevision != null
				? TERRAFORM_COMMIT_DETAILS
						.replace(
								"{{COMMIT}}",
								String.format(
										"[%s](https://github.com/%s/commit/%s)",
										gitRevision.getCommit(),
										terraformWatcher.getGit().getUri()
												.replace("git@github.com:", "")
												.replace(".git", ""),
										gitRevision.getCommit()))
				: "";

		var revisionWarnings = Stream
				.<Stream<String>>of(
						!hasTag ? Stream.of(REVISION_TAG_WARNING) : Stream.empty(),
						terraformAnalysis.getDeployments().isEmpty() ? Stream.of(NO_DEPLOYMENT_WARNING)
								: Stream.empty())
				.flatMap(stream -> stream)
				.collect(Collectors.joining("\n\n"));

		var githubRepository = terraformWatcher.getGit().getUri().replace("git@github.com:", "").replace(".git", "");

		return Stream.of(
				TERRAFORM_ENTRY_TEMPLATE
						.replace("{{REVISION_WARNINGS}}", !revisionWarnings.isBlank() ? revisionWarnings + "\n\n" : "")
						.replace("{{COMMIT_DETAILS}}", commitDetails)
						.replace("{{NAME}}", terraformAnalysis.getPackageName())
						.replace("{{REPOSITORY}}", githubRepository)
						.replace(
								"{{REVISION}}",
								hasRevision
										? String.format(
												"%s: [%s](https://github.com/%s/tree/%s%s)\n",
												hasTag ? "Tag" : "Branch",
												hasTag
														? terraformWatcher.getGit().getTag()
														: terraformWatcher.getGit().getBranch(),
												githubRepository,
												hasTag
														? terraformWatcher.getGit().getTag()
														: terraformWatcher.getGit().getBranch(),
												Optional.ofNullable(terraformWatcher.getGit().getPath())
														.map(path -> "/" + path).orElse(""))

										: "")
						.replace(
								"{{DEPLOYMENTS}}",
								Optional
										.ofNullable(terraformAnalysis.getDeployments())
										.filter(deployments -> !deployments.isEmpty())
										.map(deployments -> deployments.stream()
												.map(DefaultPullRequestAnalysisDomain::terraformDeployment)
												.collect(Collectors.joining("\n")))
										.orElse("* *No deployment recorded*")));
	}

	static String terraformDeployments(TerraformAnalysis terraformAnalysis) {
		if (terraformAnalysis.getDeployments() == null || terraformAnalysis.getDeployments().isEmpty()) {
			return "*No deployment recorded*";
		}

		return terraformAnalysis.getDeployments().stream()
				.map(DefaultPullRequestAnalysisDomain::terraformDeployment)
				.collect(Collectors.joining("\n\n"));
	}

	static String terraformDeployment(TerraformDeployment deployment) {
		return String.format(
				"* [%s - %s](%s)",
				deployment.getDeploymentInfo().getTimestamp(),
				deployment.getDeploymentInfo().getProjectId(),
				deployment.getDeploymentInfo().getUrl());
	}

}
