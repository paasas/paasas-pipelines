package io.paasas.pipelines.deployment.module.adapter.concourse;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import io.paasas.pipelines.ConcourseConfiguration;
import io.paasas.pipelines.GcpConfiguration;
import io.paasas.pipelines.deployment.domain.model.DeploymentManifest;
import io.paasas.pipelines.deployment.domain.model.TerraformWatcher;
import io.paasas.pipelines.platform.domain.model.TargetConfig;
import io.paasas.pipelines.util.concourse.CommonResourceTypes;
import io.paasas.pipelines.util.concourse.ConcoursePipeline;
import io.paasas.pipelines.util.concourse.model.Group;
import io.paasas.pipelines.util.concourse.model.Job;
import io.paasas.pipelines.util.concourse.model.Pipeline;
import io.paasas.pipelines.util.concourse.model.Resource;
import io.paasas.pipelines.util.concourse.model.ResourceType;
import io.paasas.pipelines.util.concourse.model.resource.GitSource;
import io.paasas.pipelines.util.concourse.model.step.Get;
import io.paasas.pipelines.util.concourse.model.step.InParallel;
import io.paasas.pipelines.util.concourse.model.step.Step;
import io.paasas.pipelines.util.concourse.model.step.Task;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeploymentConcoursePipeline extends ConcoursePipeline {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(
			new YAMLFactory()
					.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true)
					.configure(YAMLGenerator.Feature.SPLIT_LINES, false))
			.findAndRegisterModules()
			.setSerializationInclusion(Include.NON_NULL);

	private static final String CI_SRC_RESOURCE = "ci-src";

	GcpConfiguration gcpConfiguration;

	public DeploymentConcoursePipeline(ConcourseConfiguration configuration, GcpConfiguration gcpConfiguration) {
		super(configuration);

		this.gcpConfiguration = gcpConfiguration;
	}

	Stream<Resource<?>> commonResources() {
		return Stream.of(
				Resource.builder()
						.name(CI_SRC_RESOURCE)
						.type(CommonResourceTypes.GIT_RESOURCE_TYPE)
						.source(GitSource.builder()
								.uri("git@github.com:paasas/paasas-pipelines.git")
								.privateKey("((git.ssh-private-key))")
								.branch("main")
								.paths(List.of(".concourse"))
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
						targetConfig.getName() + "-deployment-update"))
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

	private List<Job> jobs(DeploymentManifest manifest, String target) {
		return manifest.getTerraform() != null
				? manifest.getTerraform().stream()
						.map(watcher -> terraformApplyJob(watcher, manifest, target)).toList()
				: List.of();
	}

	Job terraformApplyJob(TerraformWatcher watcher, DeploymentManifest manifest, String target) {
		var terraformParams = new TreeMap<>(Map.of(
				"GCP_PROJECT_ID", manifest.getProject(),
				"TARGET", target,
				"TERRAFORM_BACKEND_GCS_BUCKET", configuration.getTerraformBackendGcsBucket(),
				"TERRAFORM_DIRECTORY", watcher.getGit().getPath()));

		if (gcpConfiguration.getImpersonateServiceAccount() != null
				&& !gcpConfiguration.getImpersonateServiceAccount().isBlank()) {
			terraformParams.put("GOOGLE_IMPERSONATE_SERVICE_ACCOUNT", gcpConfiguration.getImpersonateServiceAccount());
		}

		var src = String.format("terraform-%s-src", watcher.getName());

		return Job.builder()
				.name(String.format("terraform-apply-%s", watcher.getName()))
				.plan(List.of(
						inParallel(List.of(
								get("ci-src"),
								getWithTrigger("manifest-src"),
								getWithTrigger(src))),
						Task.builder()
								.task("terraform-apply")
								.file("ci-src/.concourse/tasks/terraform-deployment/terraform-deployment-apply.yaml")
								.inputMapping(Map.of(
										"src", src))
								.params(terraformParams)
								.build()))
				.onSuccess(teamsSuccessNotification())
				.onFailure(teamsFailureNotification())
				.build();
	}

	public String pipeline(DeploymentManifest manifest, String target, String deploymentManifestPath) {
		assertNotBlank(
				gcpConfiguration.getImpersonateServiceAccount(),
				"gcp impersonate service account is not configured");

		assertNotBlank(
				manifest.getProject(),
				"gcp project is not configured");

		assertNotBlank(
				manifest.getRegion(),
				"gcp region is not configured");

		return writePipeline(
				Pipeline.builder()
						.resourceTypes(resourceTypes())
						.resources(Stream
								.concat(
										commonResources(),
										deploymentResources(manifest, deploymentManifestPath))
								.toList())
						.jobs(jobs(manifest, target))
						.build());
	}

	List<ResourceType> resourceTypes() {
		return List.of(CommonResourceTypes.TEAMS_NOTIFICATION);
	}

	Stream<Resource<?>> deploymentResources(DeploymentManifest manifest, String deploymentManifestPath) {
		return Stream.concat(
				Stream.of(manifestResource(deploymentManifestPath)),
				terraformResources(manifest));
	}

	Resource<?> manifestResource(String deploymentManifestPath) {
		return Resource.builder()
				.name("manifest-src")
				.type(CommonResourceTypes.GIT_RESOURCE_TYPE)
				.source(GitSource.builder()
						.branch(configuration.getDeploymentSrcBranch() != null
								&& !configuration.getDeploymentSrcBranch().isBlank()
										? configuration.getDeploymentSrcBranch()
										: null)
						.paths(List.of(deploymentManifestPath))
						.privateKey("((git.ssh-private-key))")
						.uri(configuration.getDeploymentSrcUri())
						.build())
				.build();
	}

	Resource<?> terraformResource(TerraformWatcher watcher, int index) {
		if (watcher.getName() == null || watcher.getName().isBlank()) {
			throw new IllegalArgumentException(String.format(
					"dataset is defined for big query watcher at index %i",
					index));
		}

		if (watcher.getGit() == null) {
			throw new IllegalArgumentException(String.format(
					"git configuration for terraform group %s is undefined",
					watcher.getName()));
		}

		assertArgument(watcher.getGit().getUri(), "git.uri", watcher);

		if ((watcher.getGit().getBranch() == null || watcher.getGit().getBranch().isBlank()) &&
				(watcher.getGit().getTag() == null || watcher.getGit().getTag().isBlank())) {
			throw new IllegalArgumentException(String.format(
					"branch or tag filter needs to be defined for big query dataset %s",
					watcher.getName()));
		}

		return Resource.builder()
				.name(String.format("terraform-%s-src", watcher.getName()))
				.type(CommonResourceTypes.GIT_RESOURCE_TYPE)
				.source(GitSource.builder()
						.branch(
								watcher.getGit().getTag() == null || watcher.getGit().getTag().isBlank()
										? watcher.getGit().getBranch()
										: null)
						.uri(watcher.getGit().getUri())
						.privateKey("((git.ssh-private-key))")
						.paths(watcher.getGit().getPath() != null
								? List.of(watcher.getGit().getPath())
								: null)
						.tagFilter(watcher.getGit().getTag())
						.build())
				.build();
	}

	Stream<Resource<?>> terraformResources(DeploymentManifest manifest) {
		return manifest.getTerraform() != null
				? IntStream.range(0, manifest.getTerraform().size())
						.boxed()
						.map(index -> terraformResource(manifest.getTerraform().get(index), index))
				: Stream.of();
	}

	private String writePipeline(Pipeline pipeline) {
		try {
			return OBJECT_MAPPER.writeValueAsString(pipeline);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	private void assertArgument(String value, String property, TerraformWatcher bigQuery) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(String.format(
					"value of property %s of terraform group `%s` is undefined",
					property,
					bigQuery.getName()));
		}
	}

	private void assertNotBlank(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(message);
		}
	}
}
