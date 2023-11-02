package io.paasas.pipelines.server.analysis.domain;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
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
import io.paasas.pipelines.server.analysis.domain.model.RegisterTerraformPlan;
import io.paasas.pipelines.server.analysis.domain.model.TerraformAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.TerraformDeployment;
import io.paasas.pipelines.server.analysis.domain.model.TerraformExecution;
import io.paasas.pipelines.server.analysis.domain.model.TerraformExecutionState;
import io.paasas.pipelines.server.analysis.domain.model.TerraformPlanExecution;
import io.paasas.pipelines.server.analysis.domain.model.TestReport;
import io.paasas.pipelines.server.analysis.domain.port.api.PullRequestAnalysisDomain;
import io.paasas.pipelines.server.analysis.domain.port.backend.PullRequestAnalysisRepository;
import io.paasas.pipelines.server.github.domain.model.commit.CommitState;
import io.paasas.pipelines.server.github.domain.model.commit.CreateCommitStatus;
import io.paasas.pipelines.server.github.domain.model.pull.UpdateIssueCommentRequest;
import io.paasas.pipelines.server.github.domain.port.backend.CommitStatusRepository;
import io.paasas.pipelines.server.github.domain.port.backend.IssueCommentRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultPullRequestAnalysisDomain implements PullRequestAnalysisDomain {
	private static String TERRAFORM_PLAN_MESSAGE_FAILED = ":red_circle: **Terraform Plan execution failed**";
	private static String TERRAFORM_PLAN_MESSAGE_PENDING = ":white_circle: **Terraform Plan execution is pending**";
	private static String TERRAFORM_PLAN_MESSAGE_RUNNING = ":yellow_circle: **Terraform Plan execution is in progress**";
	private static String TERRAFORM_PLAN_MESSAGE_SUCCESS = ":green_circle: **Terraform Plan execution completed**";

	private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private static final String NO_DEPLOYMENT_WARNING = ":warning: **This artifact was never deployed**\n\n";

	private static final String REVIEW_TEMPLATE = """
			# Pull Request Analysis

			## Artifacts

			{{IMPACTED_ARTIFACTS}}""";

	private static final String REVISION_TAG_WARNING = ":warning: **This artifact is not configured with a tag**\n\n";

	private static final String CLOUD_RUN_TEMPLATE = """
			### Cloud Run services

			{{CLOUD_RUN_SERVICES}}""";

	private static final String CLOUD_RUN_SERVICE_TEMPLATE = """
			#### {{SERVICE_NAME}}

			{{WARNINGS}}##### Revision

			Image: **{{IMAGE}}**
			Tag: **{{TAG}}**

			##### Tests

			{{TESTS}}

			##### Past deployments

			{{DEPLOYMENTS}}
			""";

	private static final String FIREBASE_TEMPLATE = """
			### Firebase App

			{{WARNINGS}}#### Revision

			{{COMMIT}}{{REVISION}}Repository: [{{REPOSITORY}}](https://github.com/{{REPOSITORY}})

			#### Tests

			{{TESTS}}

			#### Past deployments

			{{DEPLOYMENTS}}""";

	private static final String TERRAFORM_TEMPLATE = """
			### Terraform packages

			{{TERRAFORM_PACKAGES}}""";

	private static final String TERRAFORM_ENTRY_TEMPLATE = """
			#### {{NAME}}

			{{REVISION_MESSAGES}}##### Revision

			{{COMMIT_DETAILS}}{{REVISION}}Repository: [{{REPOSITORY}}](https://github.com/{{REPOSITORY}})

			##### Past deployments

			{{DEPLOYMENTS}}
			""";

	private static final String COMMIT_DETAILS = """
			Commit: {{COMMIT}}
			""";

	private static final String UNTESTED_ARTIFACT = ":warning: **This artifact is untested**\n\n";

	private static final ObjectMapper YAML_MAPPER = new ObjectMapper(YAMLFactory.builder().build());

	CommitStatusRepository commitStatusRepository;
	IssueCommentRepository issueCommentRepository;
	PullRequestAnalysisRepository repository;

	@Override
	public PullRequestAnalysis refresh(RefreshPullRequestAnalysisRequest request) {
		var deploymentManifest = readDeploymentManifest(request);

		return publishAnalysisToGithub(
				deploymentManifest,
				repository.refresh(deploymentManifest, request));
	}

	@Override
	public PullRequestAnalysis registerTerraformPlan(RegisterTerraformPlan request) {
		var pullRequestAnalysis = repository
				.findExistingPullRequestAnalysis(
						request.getPullRequestNumber(),
						request.getGitRevision().getRepository(),
						request.getJobInfo().getProjectId())
				.orElseThrow(() -> new IllegalArgumentException(String.format(
						"pull request %s/%d for project %s has not been analyzed before",
						request.getGitRevision().getRepository(),
						request.getPullRequestNumber(),
						request.getJobInfo().getProjectId())));

		var deploymentManifest = readDeploymentManifest(pullRequestAnalysis.getManifest());

		return publishAnalysisToGithub(
				deploymentManifest,
				repository.registerTerraformPlan(
						deploymentManifest,
						pullRequestAnalysis,
						request));
	}

	DeploymentManifest readDeploymentManifest(RefreshPullRequestAnalysisRequest request) {
		return readDeploymentManifest(new String(Base64.getDecoder().decode(request.getManifestBase64().getBytes())));
	}

	DeploymentManifest readDeploymentManifest(String manifest) {
		try {
			return YAML_MAPPER.readValue(manifest, DeploymentManifest.class);
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

	PullRequestAnalysis createIssueComment(
			DeploymentManifest deploymentManifest,
			PullRequestAnalysis pullRequestAnalysis) {
		var commentId = issueCommentRepository
				.createIssueComment(
						pullRequestAnalysis.getPullRequestNumber(),
						pullRequestAnalysis.getRepository(),
						UpdateIssueCommentRequest.builder()
								.body(generatePullRequestReviewBody(deploymentManifest, pullRequestAnalysis))
								.build())
				.getId();

		repository.updateCommentId(
				commentId,
				pullRequestAnalysis.getPullRequestNumber(),
				pullRequestAnalysis.getRepository(),
				pullRequestAnalysis.getProjectId());

		return pullRequestAnalysis.toBuilder()
				.commentId(commentId)
				.build();
	}

	PullRequestAnalysis updateIssueComment(
			DeploymentManifest deploymentManifest,
			PullRequestAnalysis pullRequestAnalysis) {
		issueCommentRepository.updateIssueComment(
				pullRequestAnalysis.getCommentId(),
				pullRequestAnalysis.getRepository(),
				UpdateIssueCommentRequest.builder()
						.body(generatePullRequestReviewBody(deploymentManifest, pullRequestAnalysis))
						.build());

		return pullRequestAnalysis;
	}

	boolean requiresTerraformPlanPendingCommitStatus(PullRequestAnalysis pullRequestAnalysis) {
		if (pullRequestAnalysis.getTerraform().isEmpty()) {
			return false;
		}

		return pullRequestAnalysis.getTerraform().stream()
				.filter(terraformAnalysis -> terraformAnalysis.getPlanExecution() == null)
				.findAny()
				.isPresent();
	}

	void updateTerraformPlanCommitStatus(PullRequestAnalysis pullRequestAnalysis) {
		if (pullRequestAnalysis.getTerraform().isEmpty()) {
			return;
		}

		var hasMissingExecution = pullRequestAnalysis.getTerraform().stream()
				.filter(terraformAnalysis -> terraformAnalysis.getPlanExecution() == null)
				.findAny()
				.isPresent();

		if (!hasMissingExecution) {
			return;
		}

		commitStatusRepository.createCommitStatus(
				pullRequestAnalysis.getRepository(),
				pullRequestAnalysis.getCommit(),
				CreateCommitStatus.builder()
						.context("compliance/tf-plan")
						.description("A terraform plan execution is pending")
						.state(CommitState.PENDING)
						.build());
	}

	PullRequestAnalysis publishAnalysisToGithub(
			DeploymentManifest deploymentManifest,
			PullRequestAnalysis pullRequestAnalysis) {
		var updatedPullRequestAnalysis = pullRequestAnalysis.getCommentId() == null
				? createIssueComment(deploymentManifest, pullRequestAnalysis)
				: updateIssueComment(deploymentManifest, pullRequestAnalysis);

		updateTerraformPlanCommitStatus(pullRequestAnalysis);

		if (deploymentManifest.getLabels() == null
				|| (!deploymentManifest.getLabels().contains(DeploymentLabel.ACCP)
						&& !deploymentManifest.getLabels().contains(DeploymentLabel.PROD))) {
			return updatedPullRequestAnalysis;
		}

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

		return updatedPullRequestAnalysis;
	}

	static String generatePullRequestReviewBody(
			DeploymentManifest deploymentManifest,
			PullRequestAnalysis pullRequestAnalysis) {
		return REVIEW_TEMPLATE
				.replace(
						"{{IMPACTED_ARTIFACTS}}",
						Stream
								.of(
										cloudRunServices(deploymentManifest, pullRequestAnalysis),
										firebaseApp(deploymentManifest, pullRequestAnalysis),
										terraform(deploymentManifest, pullRequestAnalysis))
								.filter(Optional::isPresent)
								.map(Optional::get)
								.collect(Collectors.joining("\n")));
	}

	static Optional<String> cloudRunServices(
			DeploymentManifest deploymentManifest,
			PullRequestAnalysis pullRequestAnalysis) {
		if (pullRequestAnalysis.getCloudRun() == null || pullRequestAnalysis.getCloudRun().isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(CLOUD_RUN_TEMPLATE
				.replace("{{CLOUD_RUN_SERVICES}}",
						Optional.ofNullable(pullRequestAnalysis.getCloudRun())
								.map(cloudRun -> cloudRunServices(deploymentManifest, cloudRun))
								.orElse("")));
	}

	static String cloudRunServices(DeploymentManifest deploymentManifest, List<CloudRunAnalysis> cloudRun) {
		var cloudRunServices = cloudRun.stream()
				.flatMap(cloudRunAnalysis -> cloudRunServices(deploymentManifest, cloudRunAnalysis))
				.collect(Collectors.joining("\n"));

		return !cloudRunServices.isBlank() ? cloudRunServices : "No cloud run deployment found";
	}

	static Stream<String> cloudRunServices(DeploymentManifest deploymentManifest, CloudRunAnalysis cloudRunAnalysis) {
		var app = deploymentManifest.getApps().stream()
				.filter(manifestApp -> cloudRunAnalysis.getServiceName().equals(manifestApp.getName())).findFirst()
				.orElseThrow();

		var warnings = Stream.<Stream<String>>of(
				cloudRunAnalysis.getTestReports() == null || cloudRunAnalysis.getTestReports().isEmpty()
						? Stream.of(UNTESTED_ARTIFACT)
						: Stream.empty(),
				cloudRunAnalysis.getDeployments() == null || cloudRunAnalysis.getDeployments().isEmpty()
						? Stream.of(NO_DEPLOYMENT_WARNING)
						: Stream.empty())
				.flatMap(stream -> stream)
				.collect(Collectors.joining());

		return Stream.of(
				CLOUD_RUN_SERVICE_TEMPLATE
						.replace("{{WARNINGS}}", warnings)
						.replace("{{SERVICE_NAME}}", cloudRunAnalysis.getServiceName())
						.replace("{{IMAGE}}", app.getImage())
						.replace("{{TAG}}", app.getTag())
						.replace("{{TESTS}}", cloudRunTests(cloudRunAnalysis))
						.replace("{{DEPLOYMENTS}}", cloudRunDeployments(cloudRunAnalysis)));
	}

	static String cloudRunDeployments(CloudRunAnalysis cloudRunAnalysis) {
		if (cloudRunAnalysis.getDeployments() == null || cloudRunAnalysis.getDeployments().isEmpty()) {
			return "* *No deployment recorded*";
		}

		return cloudRunAnalysis.getDeployments().stream()
				.map(DefaultPullRequestAnalysisDomain::cloudRunDeployment)
				.collect(Collectors.joining("\n"));
	}

	static String cloudRunTests(CloudRunAnalysis cloudRunAnalysis) {
		if (cloudRunAnalysis.getTestReports() == null || cloudRunAnalysis.getTestReports().isEmpty()) {
			return "* *No test recorded*";
		}

		return cloudRunAnalysis.getTestReports().stream()
				.map(DefaultPullRequestAnalysisDomain::testReport)
				.collect(Collectors.joining("\n"));
	}

	static String cloudRunDeployment(CloudRunDeployment deployment) {
		return String.format(
				"* [%s - %s](%s)",
				deployment.getDeploymentInfo().getTimestamp().format(DATETIME_FORMAT),
				deployment.getDeploymentInfo().getProjectId(),
				deployment.getDeploymentInfo().getUrl());
	}

	static String testReport(TestReport testReport) {
		var testReportUrl = testReport.getReportUrl() != null && !testReport.getReportUrl().isBlank()
				? String.format(" - [Tests report](%s)", testReport.getReportUrl())
				: "";

		return String.format(
				"* %s **%s - %s** - [Build %s](%s)%s",
				testReport.isSuccessful() ? ":green_circle:" : ":red_circle:",
				testReport.getTimestamp().format(DATETIME_FORMAT),
				testReport.getProjectId(),
				testReport.getBuildName(),
				testReport.getBuildUrl(),
				testReportUrl);
	}

	static Optional<String> firebaseApp(
			DeploymentManifest deploymentManifest,
			PullRequestAnalysis pullRequestAnalysis) {
		if (pullRequestAnalysis.getFirebase() == null) {
			return Optional.empty();
		}

		var gitRevision = Optional.ofNullable(pullRequestAnalysis.getFirebase().getDeployments())
				.orElseGet(() -> List.of())
				.stream()
				.sorted((deployment1, deployment2) -> deployment1.getDeploymentInfo().getTimestamp()
						.compareTo(deployment2.getDeploymentInfo().getTimestamp()))
				.reduce((deployment1, deployment2) -> deployment2)
				.map(FirebaseAppDeployment::getGitRevision)
				.orElse(null);

		var hasTag = isNotBlank(deploymentManifest.getFirebaseApp().getGit().getTag());
		var hasRevision = hasTag || isNotBlank(deploymentManifest.getFirebaseApp().getGit().getBranch());

		var githubRepository = deploymentManifest.getFirebaseApp().getGit().getUri()
				.replace("git@github.com:", "")
				.replace(".git", "");

		var commitDetails = gitRevision != null
				? COMMIT_DETAILS
						.replace(
								"{{COMMIT}}",
								String.format(
										"[%s](https://github.com/%s/commit/%s)",
										gitRevision.getCommit(),
										githubRepository,
										gitRevision.getCommit()))
				: "";

		var warnings = Stream
				.<Stream<String>>of(
						pullRequestAnalysis.getFirebase().getDeployments() == null
								|| pullRequestAnalysis.getFirebase().getDeployments().isEmpty()
								|| pullRequestAnalysis.getFirebase().getDeployments().stream()
										.filter(deployment -> deployment.getTestReports() == null
												|| deployment.getTestReports().isEmpty())
										.findAny()
										.isPresent()
												? Stream.of(UNTESTED_ARTIFACT)
												: Stream.empty(),
						pullRequestAnalysis.getFirebase().getDeployments() == null
								|| pullRequestAnalysis.getFirebase().getDeployments().isEmpty()
										? Stream.of(NO_DEPLOYMENT_WARNING)
										: Stream.empty(),
						!hasTag ? Stream.of(REVISION_TAG_WARNING) : Stream.empty())
				.flatMap(stream -> stream)
				.collect(Collectors.joining());

		var revisionUrl = hasTag
				? String.format(
						"https://github.com/%s/releases/tag/%s",
						githubRepository,
						deploymentManifest.getFirebaseApp().getGit().getTag())
				: String.format(
						"https://github.com/%s/tree/%s%s",
						githubRepository,
						deploymentManifest.getFirebaseApp().getGit().getBranch(),
						Optional.ofNullable(deploymentManifest.getFirebaseApp().getGit().getPath())
								.map(path -> "/" + path).orElse(""));

		return Optional.of(FIREBASE_TEMPLATE
				.replace("{{WARNINGS}}", warnings)
				.replace("{{COMMIT}}", commitDetails)
				.replace("{{REPOSITORY}}", githubRepository)
				.replace(
						"{{REVISION}}",
						hasRevision
								? String.format(
										"%s: [%s](%s)\n",
										hasTag ? "Tag" : "Branch",
										hasTag
												? deploymentManifest.getFirebaseApp().getGit().getTag()
												: deploymentManifest.getFirebaseApp().getGit().getBranch(),
										revisionUrl)

								: "")
				.replace("{{TESTS}}", firebaseAppTest(pullRequestAnalysis.getFirebase()))
				.replace(
						"{{DEPLOYMENTS}}",
						Optional
								.ofNullable(pullRequestAnalysis.getFirebase().getDeployments())
								.filter(deployments -> !deployments.isEmpty())
								.map(deployments -> deployments.stream()
										.map(DefaultPullRequestAnalysisDomain::firebaseAppDeployment)
										.collect(Collectors.joining("\n")))
								.orElse("* *No deployment recorded*")))
				.map(firebase -> firebase + "\n");
	}

	static String firebaseAppDeployment(FirebaseAppDeployment deployment) {
		return String.format(
				"* [%s - %s](%s)",
				deployment.getDeploymentInfo().getTimestamp().format(DATETIME_FORMAT),
				deployment.getDeploymentInfo().getProjectId(),
				deployment.getDeploymentInfo().getUrl());
	}

	static String firebaseAppTest(FirebaseAppAnalysis firebaseAppAnalysis) {
		if (firebaseAppAnalysis.getDeployments() == null || firebaseAppAnalysis.getDeployments().isEmpty()) {
			return "* *No test recorded*";
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

	static String terraformPlanExecutionMessage(TerraformAnalysis terraformAnalysis) {
		var state = Optional.ofNullable(terraformAnalysis.getPlanExecution())
				.map(TerraformPlanExecution::getExecution)
				.map(TerraformExecution::getState)
				.orElse(TerraformExecutionState.PENDING);

		var jobLink = Optional.ofNullable(terraformAnalysis.getPlanExecution())
				.map(TerraformPlanExecution::getExecution)
				.map(TerraformExecution::getJobUrl)
				.map(url -> String.format(" - [View job](%s)", url))
				.orElse("");

		return switch (state) {
		case FAILED -> TERRAFORM_PLAN_MESSAGE_FAILED;
		case PENDING -> TERRAFORM_PLAN_MESSAGE_PENDING;
		case RUNNING -> TERRAFORM_PLAN_MESSAGE_RUNNING;
		case SUCCESS -> TERRAFORM_PLAN_MESSAGE_SUCCESS;
		} + jobLink + "\n\n";
	}

	static Stream<String> terraform(DeploymentManifest deploymentManifest, TerraformAnalysis terraformAnalysis) {
		log.debug("Computing latest git revision from {}", terraformAnalysis.getDeployments());

		var terraformWatcher = deploymentManifest.getTerraform().stream()
				.filter(watcher -> watcher.getName().equals(terraformAnalysis.getPackageName()))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException(String.format(
						"could not find a terraform watcher for terraform package %s",
						terraformAnalysis.getPackageName())));

		var gitRevision = terraformAnalysis.getDeployments().stream()
				.sorted((deployment1, deployment2) -> deployment1.getDeploymentInfo().getTimestamp()
						.compareTo(deployment2.getDeploymentInfo().getTimestamp()))
				.reduce((deployment1, deployment2) -> deployment2)
				.map(TerraformDeployment::getGitRevision)
				.orElse(null);

		var hasTag = isNotBlank(terraformWatcher.getGit().getTag());
		var hasRevision = hasTag || isNotBlank(terraformWatcher.getGit().getBranch());

		var commitDetails = gitRevision != null
				? COMMIT_DETAILS
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

		var revisionMessages = Stream
				.of(
						Stream.of(terraformPlanExecutionMessage(terraformAnalysis)),
						!hasTag ? Stream.of(REVISION_TAG_WARNING) : Stream.<String>empty(),
						terraformAnalysis.getDeployments().isEmpty()
								? Stream.of(NO_DEPLOYMENT_WARNING)
								: Stream.<String>empty())
				.flatMap(stream -> stream)
				.collect(Collectors.joining());

		var githubRepository = terraformWatcher.getGit().getUri().replace("git@github.com:", "").replace(".git", "");

		var revisionUrl = hasTag
				? String.format(
						"https://github.com/%s/releases/tag/%s",
						githubRepository,
						terraformWatcher.getGit().getTag())
				: String.format(
						"https://github.com/%s/tree/%s%s",
						githubRepository,
						terraformWatcher.getGit().getBranch(),
						Optional.ofNullable(terraformWatcher.getGit().getPath())
								.map(path -> "/" + path).orElse(""));

		return Stream.of(
				TERRAFORM_ENTRY_TEMPLATE
						.replace("{{REVISION_MESSAGES}}", !revisionMessages.isBlank() ? revisionMessages : "")
						.replace("{{COMMIT_DETAILS}}", commitDetails)
						.replace("{{NAME}}", terraformAnalysis.getPackageName())
						.replace("{{REPOSITORY}}", githubRepository)
						.replace(
								"{{REVISION}}",
								hasRevision
										? String.format(
												"%s: [%s](%s)\n",
												hasTag ? "Tag" : "Branch",
												hasTag
														? terraformWatcher.getGit().getTag()
														: terraformWatcher.getGit().getBranch(),
												revisionUrl)

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
				deployment.getDeploymentInfo().getTimestamp().format(DATETIME_FORMAT),
				deployment.getDeploymentInfo().getProjectId(),
				deployment.getDeploymentInfo().getUrl());
	}

}
