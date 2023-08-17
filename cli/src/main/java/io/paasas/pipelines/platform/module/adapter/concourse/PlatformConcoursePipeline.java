package io.paasas.pipelines.platform.module.adapter.concourse;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import io.paasas.pipelines.ConcourseConfiguration;
import io.paasas.pipelines.GcpConfiguration;
import io.paasas.pipelines.platform.domain.model.TargetConfig;
import io.paasas.pipelines.util.concourse.CommonResourceTypes;
import io.paasas.pipelines.util.concourse.ConcoursePipeline;
import io.paasas.pipelines.util.concourse.model.Group;
import io.paasas.pipelines.util.concourse.model.Job;
import io.paasas.pipelines.util.concourse.model.Pipeline;
import io.paasas.pipelines.util.concourse.model.Resource;
import io.paasas.pipelines.util.concourse.model.ResourceType;
import io.paasas.pipelines.util.concourse.model.ResourceTypeSource;
import io.paasas.pipelines.util.concourse.model.resource.GitSource;
import io.paasas.pipelines.util.concourse.model.resource.PullRequestSource;
import io.paasas.pipelines.util.concourse.model.step.Do;
import io.paasas.pipelines.util.concourse.model.step.Get;
import io.paasas.pipelines.util.concourse.model.step.InParallel;
import io.paasas.pipelines.util.concourse.model.step.Put;
import io.paasas.pipelines.util.concourse.model.step.SetPipeline;
import io.paasas.pipelines.util.concourse.model.step.Step;
import io.paasas.pipelines.util.concourse.model.step.Task;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlatformConcoursePipeline extends ConcoursePipeline {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(
			new YAMLFactory()
					.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true)
					.configure(YAMLGenerator.Feature.SPLIT_LINES, false))
			.findAndRegisterModules()
			.setSerializationInclusion(Include.NON_NULL);

	private static final String CI_SRC_RESOURCE = "ci-src";
	private static final String GIT_RESOURCE_TYPE = "git";
	private static final String PULL_REQUEST_RESOURCE_TYPE = "pull-request";
	private static final String TERRAFORM_LTS_SRC_RESOURCE = "terraform-lts-src";
	private static final String TERRAFORM_NEXT_SRC_RESOURCE = "terraform-next-src";

	GcpConfiguration gcpConfiguration;

	public PlatformConcoursePipeline(
			ConcourseConfiguration configuration,
			GcpConfiguration gcpConfiguration) {
		super(configuration);

		this.gcpConfiguration = gcpConfiguration;
	}

	List<Resource<?>> commonResources() {
		return List.of(
				Resource.builder()
						.name(CI_SRC_RESOURCE)
						.type("git")
						.source(GitSource.builder()
								.uri("git@github.com:paasas/paasas-pipelines.git")
								.privateKey("((git.ssh-private-key))")
								.branch("main")
								.paths(List.of(".concourse"))
								.build())
						.build(),
				Resource.builder()
						.name(TERRAFORM_LTS_SRC_RESOURCE)
						.type("git")
						.source(GitSource.builder()
								.uri(configuration.getTerraformSrcUri())
								.privateKey("((git.ssh-private-key))")
								.branch("main")
								.paths(List.of("terraform/infra/lts"))
								.build())
						.build(),
				Resource.builder()
						.name(TERRAFORM_NEXT_SRC_RESOURCE)
						.type("git")
						.source(GitSource.builder()
								.uri(configuration.getTerraformSrcUri())
								.privateKey("((git.ssh-private-key))")
								.branch("main")
								.paths(List.of("terraform/infra/next"))
								.build())
						.build(),
				Resource.builder()
						.name("teams")
						.type(CommonResourceTypes.TEAMS_NOTIFICATION_RESOURCE_TYPE)
						.source(Map.of("url", "((teams.webhookUrl))"))
						.build());
	}

	private Get get(String resource) {
		return Get.builder()
				.get(resource)
				.build();
	}

	private Get getWithTrigger(String resource) {
		return Get.builder()
				.get(resource)
				.trigger(true)
				.build();
	}

	Group group(TargetConfig targetConfig) {
		return Group.builder()
				.name(targetConfig.getName())
				.jobs(List.of(
						targetConfig.getName() + "-terraform-apply",
						targetConfig.getName() + "-terraform-destroy",
						targetConfig.getName() + "-terraform-plan",
						targetConfig.getName() + "-update-deployment-pipeline"))
				.build();
	}

	List<Group> groups(List<TargetConfig> targetConfigs) {
		return targetConfigs.stream().map(this::group).toList();
	}

	private InParallel inParallel(List<Step> steps) {
		return InParallel.builder()
				.inParallel(steps)
				.build();
	}

	private List<Job> jobs(List<TargetConfig> targetConfigs) {
		return targetConfigs.stream().flatMap(this::jobs).toList();
	}

	private Stream<Job> jobs(TargetConfig targetConfig) {
		var terraformPlanParams = new TreeMap<>(Map.of(
				"PLATFORM_MANIFEST_PATH", targetConfig.getPlatformManifestPath(),
				"TARGET", targetConfig.getName(),
				"TERRAFORM_BACKEND_DIRECTORY", configuration.getPlatformTerraformBackendPrefix(),
				"TERRAFORM_BACKEND_GCS_BUCKET", configuration.getTerraformBackendGcsBucket(),
				"TERRAFORM_BACKEND_PREFIX", configuration.getPlatformTerraformBackendPrefix(),
				"TERRAFORM_EXTENSIONS_DIRECTORY", targetConfig.getTerraformExtensionsDirectory()));

		var deploymentUpdateParams = new TreeMap<>(new TreeMap<>(Map.of(
				"MANIFEST_PATH", targetConfig.getDeploymentManifestPath(),
				"PIPELINES_CONCOURSE_DEPLOYMENTPATHPREFIX", configuration.getDeploymentPathPrefix(),
				"PIPELINES_CONCOURSE_DEPLOYMENTSRCBRANCH", configuration.getDeploymentSrcBranch(),
				"PIPELINES_CONCOURSE_DEPLOYMENTSRCURI", configuration.getDeploymentSrcUri(),
				"PIPELINES_CONCOURSE_DEPLOYMENTTERRAFORMBACKENDPREFIX",
				configuration.getDeploymentTerraformBackendPrefix(),
				"PIPELINES_CONCOURSE_GITHUBEMAIL", configuration.getGithubEmail(),
				"PIPELINES_CONCOURSE_GITHUBREPOSITORY", configuration.getGithubRepository(),
				"PIPELINES_CONCOURSE_GITHUBUSERNAME", configuration.getGithubUsername(),
				"PIPELINES_CONCOURSE_PLATFORMPATHPREFIX", configuration.getPlatformPathPrefix(),
				"PIPELINES_CONCOURSE_PLATFORMSRCBRANCH", configuration.getPlatformSrcBranch())));

		deploymentUpdateParams.putAll(new TreeMap<>(Map.of(
				"PIPELINES_CONCOURSE_PLATFORMSRCURI", configuration.getPlatformSrcUri(),
				"PIPELINES_CONCOURSE_PLATFORMTERRAFORMBACKENDPREFIX", configuration.getPlatformTerraformBackendPrefix(),
				"PIPELINES_CONCOURSE_TERRAFORMBACKENDGCSBUCKET", configuration.getTerraformBackendGcsBucket(),
				"PIPELINES_CONCOURSE_TERRAFORMSRCBRANCH", configuration.getTerraformSrcBranch(),
				"PIPELINES_CONCOURSE_TERRAFORMSRCURI", configuration.getTerraformSrcUri(),
				"TARGET", targetConfig.getName())));

		if (gcpConfiguration.getImpersonateServiceAccount() != null
				&& !gcpConfiguration.getImpersonateServiceAccount().isBlank()) {
			terraformPlanParams.put(
					"GOOGLE_IMPERSONATE_SERVICE_ACCOUNT",
					gcpConfiguration.getImpersonateServiceAccount());

			deploymentUpdateParams.put("PIPELINES_GCP_IMPERSONATESERVICEACCOUNT",
					gcpConfiguration.getImpersonateServiceAccount());
		}

		if (configuration.getDeploymentTerraformBackendBucketSuffix() != null
				&& !configuration.getDeploymentTerraformBackendBucketSuffix().isBlank()) {
			deploymentUpdateParams.put(
					"PIPELINES_CONCOURSE_DEPLOYMENTTERRAFORMBACKENDBUCKETSUFFIX",
					configuration.getDeploymentTerraformBackendBucketSuffix());
		}
		
		if (configuration.getGcrCredentialsJsonSecretName() != null
				&& !configuration.getGcrCredentialsJsonSecretName().isBlank()) {
			deploymentUpdateParams.put(
					"PIPELINES_CONCOURSE_GCRCREDENTIALSJSONSECRETNAME",
					configuration.getGcrCredentialsJsonSecretName());
		}

		return Stream.of(
				terraformJob("apply", targetConfig, true),
				terraformJob("destroy", targetConfig, false),
				Job.builder()
						.name(targetConfig.getName() + "-terraform-plan")
						.plan(List.of(
								InParallel.builder()
										.inParallel(List.of(
												getWithTrigger(targetConfig.getName() + "-platform-pr"),
												get("ci-src")))
										.build(),
								InParallel.builder()
										.inParallel(List.of(
												updatePr(targetConfig.getName() + "-platform-pr", "pending"),
												Get.builder().get(TERRAFORM_LTS_SRC_RESOURCE).build(),
												Get.builder().get(TERRAFORM_NEXT_SRC_RESOURCE).build()))
										.build(),
								Task.builder()
										.task("terraform-plan")
										.file("ci-src/.concourse/tasks/terraform-platform/terraform-platform-plan.yaml")
										.inputMapping(Map.of("src", targetConfig.getName() + "-platform-pr"))
										.params(terraformPlanParams)
										.build(),
								updatePr(
										targetConfig.getName() + "-platform-pr",
										"success",
										"terraform-out/terraform.md")))
						.onSuccess(teamsSuccessNotification())
						.onFailure(Do.builder()
								.steps(Stream
										.concat(
												Stream.of(updatePr(targetConfig.getName() + "-platform-pr", "failure")),
												Optional.ofNullable(teamsFailureNotification()).stream())
										.toList())
								.build())
						.build(),
				Job.builder()
						.name(targetConfig.getName() + "-update-deployment-pipeline")
						.plan(List.of(
								inParallel(List.of(
										getWithTrigger(targetConfig.getName() + "-deployment-src"),
										get(CI_SRC_RESOURCE))),
								Task.builder()
										.task("update-deployment-pipeline")
										.file("ci-src/.concourse/tasks/deployment/update-deployment-pipeline.yaml")
										.inputMapping(Map.of("src", targetConfig.getName() + "-deployment-src"))
										.params(deploymentUpdateParams)
										.build(),
								SetPipeline.builder()
										.setPipeline(String.format("%s-deployment", targetConfig.getName()))
										.file("pipelines/pipelines.yaml")
										.build()))
						.onSuccess(teamsSuccessNotification())
						.onFailure(teamsFailureNotification())
						.build());
	}

	public String pipeline(
			List<TargetConfig> targetConfigs) {
		if (targetConfigs == null || targetConfigs.size() == 0) {
			throw new IllegalArgumentException("exepcted at least one target config");
		}

		if (configuration.getPlatformTerraformBackendPrefix() == null
				|| configuration.getPlatformTerraformBackendPrefix().isBlank()) {
			throw new IllegalStateException("platform terraform backend prefix is not configured");
		}

		return writePipeline(
				Pipeline.builder()
						.resourceTypes(resourceTypes())
						.resources(Stream
								.concat(
										commonResources().stream(),
										targetResources(targetConfigs).stream())
								.toList())
						.jobs(jobs(targetConfigs))
						.groups(groups(targetConfigs))
						.build());
	}

	private List<ResourceType> resourceTypes() {
		return List.of(ResourceType.builder()
				.name(PULL_REQUEST_RESOURCE_TYPE)
				.type("docker-image")
				.source(ResourceTypeSource.builder()
						.repository("teliaoss/github-pr-resource")
						.build())
				.build(),
				CommonResourceTypes.TEAMS_NOTIFICATION);
	}

	private List<Resource<?>> targetResources(List<TargetConfig> targetConfigs) {
		return targetConfigs.stream().flatMap(this::targetResources).toList();
	}

	private Stream<Resource<?>> targetResources(TargetConfig targetConfig) {
		return Stream.of(
				Resource.builder()
						.name(targetConfig.getName() + "-platform-pr")
						.type(PULL_REQUEST_RESOURCE_TYPE)
						.source(PullRequestSource.builder()
								.accessToken("((github.userAccessToken))")
								.repository(configuration.getGithubRepository())
								.paths(List.of(
										targetConfig.getPlatformManifestPath(),
										targetConfig.getTerraformExtensionsDirectory()))
								.build())
						.build(),
				Resource.builder()
						.name(targetConfig.getName() + "-platform-src")
						.type(GIT_RESOURCE_TYPE)
						.source(GitSource.builder()
								.uri(configuration.getPlatformSrcUri())
								.privateKey("((git.ssh-private-key))")
								.branch(configuration.getPlatformSrcBranch())
								.paths(List.of(
										targetConfig.getPlatformManifestPath(),
										targetConfig.getTerraformExtensionsDirectory()))
								.build())
						.build(),
				Resource.builder()
						.name(targetConfig.getName() + "-deployment-src")
						.type(GIT_RESOURCE_TYPE)
						.source(GitSource.builder()
								.uri(configuration.getDeploymentSrcUri())
								.privateKey("((git.ssh-private-key))")
								.branch(configuration.getDeploymentSrcBranch())
								.paths(List.of(targetConfig.getDeploymentManifestPath()))
								.build())
						.build());
	}

	private Job terraformJob(String type, TargetConfig targetConfig, Boolean trigger) {
		var terraformParams = new TreeMap<>(Map.of(
				"PLATFORM_MANIFEST_PATH", targetConfig.getPlatformManifestPath(),
				"TARGET", targetConfig.getName(),
				"TERRAFORM_BACKEND_GCS_BUCKET",
				configuration.getTerraformBackendGcsBucket(),
				"TERRAFORM_EXTENSIONS_DIRECTORY",
				targetConfig.getTerraformExtensionsDirectory()));

		if (gcpConfiguration.getImpersonateServiceAccount() != null
				&& !gcpConfiguration.getImpersonateServiceAccount().isBlank()) {
			terraformParams.put("GOOGLE_IMPERSONATE_SERVICE_ACCOUNT",
					gcpConfiguration.getImpersonateServiceAccount());
		}

		return Job.builder()
				.name(targetConfig.getName() + "-terraform-" + type)
				.plan(List.of(
						inParallel(List.of(
								trigger
										? getWithTrigger(targetConfig.getName() + "-platform-src")
										: get(targetConfig.getName() + "-platform-src"),
								get(CI_SRC_RESOURCE),
								get(TERRAFORM_LTS_SRC_RESOURCE),
								get(TERRAFORM_NEXT_SRC_RESOURCE))),
						Task.builder()
								.task("terraform-" + type)
								.file("ci-src/.concourse/tasks/terraform-platform/terraform-platform-" + type + ".yaml")
								.inputMapping(Map.of(
										"src", targetConfig.getName() + "-platform-src"))
								.params(terraformParams)
								.build()))
				.onSuccess(teamsSuccessNotification())
				.onFailure(teamsFailureNotification())
				.build();
	}

	private Put updatePr(String resource, String status) {
		return Put.builder()
				.put(resource)
				.params(new TreeMap<>(Map.of(
						"path", resource,
						"status", status)))
				.build();
	}

	private Put updatePr(String resource, String status, String commentFile) {
		return Put.builder()
				.put(resource)
				.params(new TreeMap<>(Map.of(
						"comment_file", commentFile,
						"path", resource,
						"status", status)))
				.build();
	}

	private String writePipeline(Pipeline pipeline) {
		try {
			return OBJECT_MAPPER.writeValueAsString(pipeline);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
}
