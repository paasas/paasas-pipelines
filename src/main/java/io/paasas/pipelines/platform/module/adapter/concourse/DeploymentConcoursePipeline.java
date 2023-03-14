package io.paasas.pipelines.platform.module.adapter.concourse;

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
import io.paasas.pipelines.deployment.domain.model.BigQueryWatcher;
import io.paasas.pipelines.deployment.domain.model.DeploymentManifest;
import io.paasas.pipelines.platform.domain.model.TargetConfig;
import io.paasas.pipelines.platform.module.adapter.concourse.model.Group;
import io.paasas.pipelines.platform.module.adapter.concourse.model.Job;
import io.paasas.pipelines.platform.module.adapter.concourse.model.Pipeline;
import io.paasas.pipelines.platform.module.adapter.concourse.model.Resource;
import io.paasas.pipelines.platform.module.adapter.concourse.model.ResourceType;
import io.paasas.pipelines.platform.module.adapter.concourse.model.resource.GitSource;
import io.paasas.pipelines.platform.module.adapter.concourse.model.step.Get;
import io.paasas.pipelines.platform.module.adapter.concourse.model.step.InParallel;
import io.paasas.pipelines.platform.module.adapter.concourse.model.step.Step;
import io.paasas.pipelines.platform.module.adapter.concourse.model.step.Task;
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
		return manifest.getBigQuery() != null
				? manifest.getBigQuery().stream()
						.map(watcher -> updateBigQueryDatasetJob(watcher, manifest, target)).toList()
				: null;
	}

	Job updateBigQueryDatasetJob(BigQueryWatcher watcher, DeploymentManifest manifest, String target) {
		var terraformParams = new TreeMap<>(Map.of(
				"IMPERSONATE_SERVICE_ACCOUNT", gcpConfiguration.getImpersonateServiceAccount(),
				"GCP_PROJECT_ID", manifest.getProject(),
				"TARGET", target,
				"TERRAFORM_BACKEND_GCS_BUCKET", configuration.getTerraformBackendGcsBucket(),
				"TERRAFORM_DIRECTORY", watcher.getGit().getPath()));

		var src = String.format("bigquery-%s-src", watcher.getDataset());

		return Job.builder()
				.name(String.format("update-bigquery-%s", watcher.getDataset()))
				.plan(List.of(
						inParallel(List.of(
								get("ci-src"),
								getWithTrigger(src))),
						Task.builder()
								.task("terraform-apply")
								.file("ci-src/tasks/terraform-apply/terraform-apply.yaml")
								.inputMapping(Map.of(
										"src", src))
								.params(terraformParams)
								.build()))
				.onSuccess(teamsSuccessNotification())
				.onFailure(teamsFailureNotification())
				.build();
	}

	public String pipeline(DeploymentManifest manifest, String target) {
		assertNotBlank(
				gcpConfiguration.getImpersonateServiceAccount(),
				"gcp impersonate service account is not configured");

		assertNotBlank(
				gcpConfiguration.getProjectId(),
				"gcp project is not configured");

		assertNotBlank(
				gcpConfiguration.getRegion(),
				"gcp region is not configured");

		return writePipeline(
				Pipeline.builder()
						.resourceTypes(resourceTypes())
						.resources(Stream
								.concat(
										commonResources(),
										deploymentResources(manifest))
								.toList())
						.jobs(jobs(manifest, target))
						.build());
	}

	List<ResourceType> resourceTypes() {
		return List.of(CommonResourceTypes.TEAMS_NOTIFICATION);
	}

	Stream<Resource<?>> deploymentResources(DeploymentManifest manifest) {
		return bigQueryResources(manifest);
	}

	Resource<?> bigQueryResource(BigQueryWatcher watcher, int index) {
		if (watcher.getDataset() == null || watcher.getDataset().isBlank()) {
			throw new IllegalArgumentException(String.format(
					"dataset is defined for big query watcher at index %i",
					index));
		}

		if (watcher.getGit() == null) {
			throw new IllegalArgumentException(String.format(
					"git configuration for big query dataset %s is undefined",
					watcher.getDataset()));
		}

		assertArgument(watcher.getGit().getUri(), "git.uri", watcher);

		if ((watcher.getGit().getBranch() == null || watcher.getGit().getBranch().isBlank()) &&
				(watcher.getGit().getTag() == null || watcher.getGit().getTag().isBlank())) {
			throw new IllegalArgumentException(String.format(
					"branch or tag filter needs to be defined for big query dataset %s",
					watcher.getDataset()));
		}

		return Resource.builder()
				.name(watcher.getDataset() + "-src")
				.type(CommonResourceTypes.GIT_RESOURCE_TYPE)
				.source(GitSource.builder()
						.branch(watcher.getGit().getBranch())
						.uri(watcher.getGit().getUri())
						.privateKey("((git.ssh-private-key))")
						.paths(watcher.getGit().getPath() != null
								? List.of(watcher.getGit().getPath())
								: null)
						.tagFilter(watcher.getGit().getTag())
						.build())
				.build();
	}

	Stream<Resource<?>> bigQueryResources(DeploymentManifest manifest) {
		return manifest.getBigQuery() != null
				? IntStream.range(0, manifest.getBigQuery().size())
						.boxed()
						.map(index -> bigQueryResource(manifest.getBigQuery().get(index), index))
				: Stream.of();
	}

	private String writePipeline(Pipeline pipeline) {
		try {
			return OBJECT_MAPPER.writeValueAsString(pipeline);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	private void assertArgument(String value, String property, BigQueryWatcher bigQuery) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(String.format(
					"value of property %s of big query watcher `%s` is undefined",
					property,
					bigQuery.getDataset()));
		}
	}

	private void assertNotBlank(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(message);
		}
	}
}
