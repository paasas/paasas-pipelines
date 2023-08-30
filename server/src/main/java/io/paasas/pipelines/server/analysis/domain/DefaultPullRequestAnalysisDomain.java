package io.paasas.pipelines.server.analysis.domain;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.paasas.pipelines.deployment.domain.model.DeploymentManifest;
import io.paasas.pipelines.server.analysis.domain.model.CloudRunAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.CloudRunDeployment;
import io.paasas.pipelines.server.analysis.domain.model.FirebaseAppDeployment;
import io.paasas.pipelines.server.analysis.domain.model.PullRequestAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.RefreshPullRequestAnalysisRequest;
import io.paasas.pipelines.server.analysis.domain.model.TerraformAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.TerraformDeployment;
import io.paasas.pipelines.server.analysis.domain.port.api.ArtifactAnalysisDomain;
import io.paasas.pipelines.server.analysis.domain.port.api.PullRequestAnalysisDomain;
import io.paasas.pipelines.server.analysis.domain.port.backend.PullRequestAnalysisRepository;
import io.paasas.pipelines.server.github.domain.port.backend.PullRequestRepository;
import io.paasas.pipelines.server.github.module.adapter.model.pull.CreatePullRequestComment;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultPullRequestAnalysisDomain implements PullRequestAnalysisDomain {
	private static final String REVIEW_TEMPLATE = """
			# Pull Request Analysis

			## Artifacts

			{{IMPACTED_ARTIFACTS}}""";

	private static final String CLOUD_RUN_TEMPLATE = """
			### Cloud Run services

			{{CLOUD_RUN_SERVICES}}""";

	private static final String CLOUD_RUN_SERVICE_TEMPLATE = """
			#### {{SERVICE_NAME}}

			##### Revision

			Image: **{{IMAGE}}**
			Tag: **{{TAG}}**

			##### Past deployments

			{{DEPLOYMENTS}}""";

	private static final String FIREBASE_TEMPLATE = """
			### Firebase App

			#### Revision

			Commit: {{COMMIT}}
			Commit Author: [{{COMMIT_AUTHOR}}](https://github.com/{{COMMIT_AUTHOR}})
			{{REVISION}}Repository: [{{REPOSITORY}}](https://github.com/{{REPOSITORY}})

			#### Past deployments

			{{DEPLOYMENTS}}""";

	private static final String TERRAFORM_TEMPLATE = """
			### Terraform packages

			{{TERRAFORM_PACKAGES}}""";

	private static final String TERRAFORM_ENTRY_TEMPLATE = """
			#### {{NAME}}

			##### Revision

			Commit: {{COMMIT}}
			Commit Author: [{{COMMIT_AUTHOR}}](https://github.com/{{COMMIT_AUTHOR}})
			{{REVISION}}Repository: [{{REPOSITORY}}](https://github.com/{{REPOSITORY}})

			##### Past deployments

			{{DEPLOYMENTS}}""";

	private static final ObjectMapper YAML_MAPPER = new ObjectMapper(YAMLFactory.builder().build());

	ArtifactAnalysisDomain artifactAnalysisDomain;
	PullRequestRepository pullRequestRepository;
	PullRequestAnalysisRepository repository;

	@Override
	public void refresh(RefreshPullRequestAnalysisRequest request) {
		var deploymentManifest = readDeploymentManifest(request);

		var pullRequestAnalysis = PullRequestAnalysis.builder()
				.commit(request.getCommit())
				.commitAuthor(request.getCommitAuthor())
				.cloudRun(artifactAnalysisDomain.cloudRunAnalysis(deploymentManifest))
				.firebase(artifactAnalysisDomain.firebaseAppAnalysis(deploymentManifest).orElse(null))
				.manifest(request.getManifest())
				.projectId(request.getProject())
				.pullRequestNumber(request.getPullRequestNumber())
				.repository(request.getRepository())
				.repositoryOwner(request.getRepositoryOwner())
				.requester(request.getRequester())
				.terraform(artifactAnalysisDomain.terraformAnalysis(deploymentManifest))
				.build();

		log.info("Updating {}", pullRequestAnalysis);

		repository.save(pullRequestAnalysis);

		publishAnalysisToGithub(pullRequestAnalysis);
	}

	DeploymentManifest readDeploymentManifest(RefreshPullRequestAnalysisRequest request) {
		try {
			return YAML_MAPPER.readValue(request.getManifest(), DeploymentManifest.class);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	void publishAnalysisToGithub(PullRequestAnalysis pullRequestAnalysis) {
		pullRequestRepository.createPullRequestComment(
				pullRequestAnalysis.getPullRequestNumber(),
				pullRequestAnalysis.getRepositoryOwner(),
				pullRequestAnalysis.getRepository(),
				CreatePullRequestComment.builder()
						.body(generatePullRequestReviewBody(pullRequestAnalysis))
						.build());
	}

	static String generatePullRequestReviewBody(PullRequestAnalysis pullRequestAnalysis) {
		return REVIEW_TEMPLATE
				.replace(
						"{{IMPACTED_ARTIFACTS}}",
						Stream
								.of(
										cloudRunServices(pullRequestAnalysis),
										firebaseApp(pullRequestAnalysis),
										terraform(pullRequestAnalysis))
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
		return cloudRun.stream()
				.map(DefaultPullRequestAnalysisDomain::cloudRunServices)
				.collect(Collectors.joining("\n"));
	}

	static String cloudRunServices(CloudRunAnalysis cloudRunAnalysis) {
		var latestDeployment = cloudRunAnalysis.getDeployments().stream()
				.sorted((deployment1, deployment2) -> deployment1.getDeploymentInfo().getTimestamp()
						.compareTo(deployment2.getDeploymentInfo().getTimestamp()))
				.reduce((deployment1, deployment2) -> deployment2)
				.orElseThrow(() -> new IllegalStateException(
						String.format(
								"expected at least one deployment for cloud run service %s",
								cloudRunAnalysis.getServiceName())));

		return CLOUD_RUN_SERVICE_TEMPLATE
				.replace("{{SERVICE_NAME}}", cloudRunAnalysis.getServiceName())
				.replace("{{IMAGE}}", latestDeployment.getImage())
				.replace("{{TAG}}", latestDeployment.getTag())
				.replace("{{DEPLOYMENTS}}", cloudRunDeployments(cloudRunAnalysis));
	}

	static String cloudRunDeployments(CloudRunAnalysis cloudRunAnalysis) {
		if (cloudRunAnalysis.getDeployments() == null || cloudRunAnalysis.getDeployments().isEmpty()) {
			return "*No deployment recorded*";
		}

		return cloudRunAnalysis.getDeployments().stream()
				.map(DefaultPullRequestAnalysisDomain::cloudRunDeployment)
				.collect(Collectors.joining("\n\n"));
	}

	static String cloudRunDeployment(CloudRunDeployment deployment) {
		return String.format(
				"* [%s - %s](%s)",
				deployment.getDeploymentInfo().getTimestamp(),
				deployment.getDeploymentInfo().getProjectId(),
				deployment.getDeploymentInfo().getUrl());
	}

	static Optional<String> firebaseApp(PullRequestAnalysis pullRequestAnalysis) {
		if (pullRequestAnalysis.getFirebase() == null) {
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
				.replace("{{COMMIT_AUTHOR}}", gitRevision.getCommitAuthor())
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

	static boolean isNotBlank(String value) {
		return value != null && !value.isBlank();
	}

	static Optional<String> terraform(PullRequestAnalysis pullRequestAnalysis) {
		if (pullRequestAnalysis.getTerraform() == null || pullRequestAnalysis.getTerraform().isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(TERRAFORM_TEMPLATE
				.replace("{{TERRAFORM_PACKAGES}}",
						Optional.ofNullable(pullRequestAnalysis.getTerraform())
								.map(DefaultPullRequestAnalysisDomain::terraform)
								.orElse("")));
	}

	static String terraform(List<TerraformAnalysis> terraform) {
		return terraform.stream()
				.map(DefaultPullRequestAnalysisDomain::terraform)
				.collect(Collectors.joining("\n"));
	}

	static String terraform(TerraformAnalysis terraformAnalysis) {
		log.info("Computing latest git revision from {}", terraformAnalysis.getDeployments());

		var gitRevision = terraformAnalysis.getDeployments().stream()
				.sorted((deployment1, deployment2) -> deployment1.getDeploymentInfo().getTimestamp()
						.compareTo(deployment2.getDeploymentInfo().getTimestamp()))
				.reduce((deployment1, deployment2) -> deployment2)
				.map(TerraformDeployment::getGitRevision)
				.orElseThrow(() -> new IllegalStateException(
						String.format(
								"expected at least one deployment for terraform package %s",
								terraformAnalysis.getPackageName())));

		var hasTag = isNotBlank(gitRevision.getTag());
		var hasRevision = hasTag || isNotBlank(gitRevision.getBranch());

		return TERRAFORM_ENTRY_TEMPLATE
				.replace(
						"{{COMMIT}}",
						String.format(
								"[%s](https://github.com/%s/commit/%s)",
								gitRevision.getCommit(),
								gitRevision.getRepository(),
								gitRevision.getCommit()))
				.replace("{{COMMIT_AUTHOR}}", gitRevision.getCommitAuthor())
				.replace("{{NAME}}", terraformAnalysis.getPackageName())
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
				.replace(
						"{{DEPLOYMENTS}}",
						Optional
								.ofNullable(terraformAnalysis.getDeployments())
								.map(deployments -> deployments.stream()
										.map(DefaultPullRequestAnalysisDomain::terraformDeployment)
										.collect(Collectors.joining("\n")))
								.orElse("*No deployment recorded*"));
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
