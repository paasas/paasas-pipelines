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
import io.paasas.pipelines.deployment.domain.model.composer.Dags;
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

	private void assertArgument(String value, String property, Dags dags) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(String.format(
					"value of property %s of composer dags `%s` is undefined",
					property,
					dags.getName()));
		}
	}

	private void assertArgument(String value, String property, TerraformWatcher terraformGroup) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(String.format(
					"value of property %s of terraform group `%s` is undefined",
					property,
					terraformGroup.getName()));
		}
	}

	private void assertNotBlank(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(message);
		}
	}

	String blankIfNull(String value) {
		return value != null ? value : "";
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

	Resource<?> composerDagsResource(Dags dags, int index) {
		if (dags.getName() == null || dags.getName().isBlank()) {
			throw new IllegalArgumentException(String.format(
					"name is defined for cloud compoer dags at index %i",
					index));
		}

		if (dags.getGit() == null) {
			throw new IllegalArgumentException(String.format(
					"git configuration for composer dags %s is undefined",
					dags.getName()));
		}

		assertArgument(dags.getGit().getUri(), "git.uri", dags);

		if ((dags.getGit().getBranch() == null || dags.getGit().getBranch().isBlank()) &&
				(dags.getGit().getTag() == null || dags.getGit().getTag().isBlank())) {
			throw new IllegalArgumentException(String.format(
					"branch or tag filter needs to be defined for composer dags %s",
					dags.getName()));
		}

		return Resource.builder()
				.name(String.format("%s-dags-src", dags.getName()))
				.type("git")
				.source(GitSource.builder()
						.uri(dags.getGit().getUri())
						.privateKey("((git.ssh-private-key))")
						.branch(dags.getGit().getBranch())
						.paths(dags.getGit().getPath() != null ? List.of(dags.getGit().getPath()) : null)
						.build())
				.build();
	}

	Stream<Resource<?>> deploymentResources(DeploymentManifest manifest, String deploymentManifestPath) {
		var streamBuilder = Stream.<Resource<?>>builder()
				.add(manifestResource(deploymentManifestPath));

		if (manifest.getTerraform() != null) {
			IntStream.range(0, manifest.getTerraform().size())
					.boxed()
					.map(index -> terraformResource(manifest.getTerraform().get(index), index))
					.forEach(streamBuilder::add);
		}

		if (manifest.getComposerDags() != null) {
			IntStream.range(0, manifest.getComposerDags().size())
					.boxed()
					.map(index -> composerDagsResource(manifest.getComposerDags().get(index), index))
					.forEach(streamBuilder::add);
		}

		if (manifest.getFirebaseApp() != null) {
			streamBuilder.add(firebaseResource(manifest));
		}

		return streamBuilder.build();
	}

	Job firebaseJob(DeploymentManifest manifest) {
		var firebaseApp = manifest.getFirebaseApp();

		if (firebaseApp == null) {
			throw new IllegalArgumentException("firebase app is undefined");
		}
		if (firebaseApp.getNpm() == null) {
			throw new IllegalArgumentException("firebase app npm config is undefined");
		}

		var serviceAccount = String.format("terraform@%s.iam.gserviceaccount.com", manifest.getProject());

		var mappings = new TreeMap<>(Map.of("src", "firebase-src"));

		return Job.builder()
				.name("deploy-firebase")
				.plan(List.of(
						inParallel(List.of(
								get("ci-src"),
								getWithTrigger("firebase-src"))),
						Task.builder()
								.task("npm-build")
								.file("ci-src/.concourse/tasks/npm-build/npm-build.yaml")
								.inputMapping(mappings)
								.outputMapping(mappings)
								.params(new TreeMap<>(Map.of(
										"NPM_INSTALL_ARGS", blankIfNull(firebaseApp.getNpm().getInstallArgs()),
										"NPM_COMMAND", blankIfNull(firebaseApp.getNpm().getCommand()),
										"NPM_ENV", blankIfNull(firebaseApp.getNpm().getEnv()),
										"NPM_PATH", blankIfNull(firebaseApp.getGit().getPath()))))
								.build(),
						Task.builder()
								.task("firebase-deploy")
								.file("ci-src/.concourse/tasks/firebase-deploy/firebase-deploy.yaml")
								.inputMapping(mappings)
								.outputMapping(mappings)
								.params(new TreeMap<>(Map.of(
										"GCP_PROJECT_ID", manifest.getProject(),
										"GOOGLE_IMPERSONATE_SERVICE_ACCOUNT", serviceAccount,
										"FIREBASE_APP_PATH", blankIfNull(firebaseApp.getGit().getPath()),
										"FIREBASE_CONFIG", blankIfNull(firebaseApp.getConfig()))))
								.build()))
				.build();
	}

	Resource<?> firebaseResource(DeploymentManifest deploymentManifest) {
		if (deploymentManifest.getFirebaseApp() == null) {
			throw new IllegalArgumentException("firebase app is undefined");
		}

		if (deploymentManifest.getFirebaseApp().getGit() == null) {
			throw new IllegalArgumentException("git configuration for firebase app is undefined");
		}

		if (deploymentManifest.getFirebaseApp().getGit().getUri() == null ||
				deploymentManifest.getFirebaseApp().getGit().getUri().isBlank()) {
			throw new IllegalArgumentException("property git.uri of firebase app is undefined");
		}

		if ((deploymentManifest.getFirebaseApp().getGit().getBranch() == null ||
				deploymentManifest.getFirebaseApp().getGit().getBranch().isBlank()) &&
				(deploymentManifest.getFirebaseApp().getGit().getTag() == null ||
						deploymentManifest.getFirebaseApp().getGit().getTag().isBlank())) {
			throw new IllegalArgumentException("branch or tag filter needs to be defined for firebase app");
		}

		return Resource.builder()
				.name("firebase-src")
				.type("git")
				.source(GitSource.builder()
						.uri(deploymentManifest.getFirebaseApp().getGit().getUri())
						.privateKey("((git.ssh-private-key))")
						.branch(deploymentManifest.getFirebaseApp().getGit().getBranch())
						.paths(deploymentManifest.getFirebaseApp().getGit().getPath() != null
								? List.of(deploymentManifest.getFirebaseApp().getGit().getPath())
								: null)
						.build())
				.build();
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

	private List<Job> jobs(
			DeploymentManifest manifest,
			String target,
			String deploymentManifestPath) {
		var streamBuilder = Stream.<Job>builder()
				.add(updateCloudRunJob(manifest, deploymentManifestPath));

		if (manifest.getTerraform() != null) {
			manifest.getTerraform().stream()
					.map(watcher -> terraformApplyJob(watcher, manifest, target, deploymentManifestPath))
					.forEach(streamBuilder::add);
		}

		if (manifest.getComposerDags() != null) {
			manifest.getComposerDags().stream()
					.map(dags -> updateComposerDags(dags, manifest))
					.forEach(streamBuilder::add);
		}

		if (manifest.getFirebaseApp() != null) {
			streamBuilder.add(firebaseJob(manifest));
		}

		return streamBuilder.build().toList();
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

	public String pipeline(DeploymentManifest manifest, String target, String deploymentManifestPath) {
		assertNotBlank(
				configuration.getDeploymentTerraformBackendPrefix(),
				"deployment terraform backend prefix is not configured");

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
						.jobs(jobs(manifest, target, deploymentManifestPath))
						.build());
	}

	List<ResourceType> resourceTypes() {
		return List.of(CommonResourceTypes.TEAMS_NOTIFICATION);
	}

	Job terraformApplyJob(
			TerraformWatcher watcher,
			DeploymentManifest manifest,
			String target,
			String deploymentManifestPath) {
		var terraformBackendGcsBucket = configuration.getDeploymentTerraformBackendBucketSuffix() != null
				&& !configuration.getDeploymentTerraformBackendBucketSuffix().isBlank()
						? String.format(
								"%s.%s",
								manifest.getProject(),
								configuration.getDeploymentTerraformBackendBucketSuffix())
						: manifest.getProject();

		var terraformParams = new TreeMap<>(Map.of(
				"GCP_PROJECT_ID", manifest.getProject(),
				"MANIFEST_PATH", deploymentManifestPath,
				"TERRAFORM_PREFIX", target + "-" + watcher.getName(),
				"TERRAFORM_BACKEND_GCS_BUCKET", terraformBackendGcsBucket,
				"TERRAFORM_DIRECTORY", watcher.getGit().getPath(),
				"TERRAFORM_GROUP_NAME", watcher.getName(),
				"GOOGLE_IMPERSONATE_SERVICE_ACCOUNT", String.format(
						"terraform@%s.iam.gserviceaccount.com",
						manifest.getProject())));

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

	Job updateCloudRunJob(DeploymentManifest manifest, String deploymentManifestPath) {
		var taskParams = new TreeMap<>(Map.of(
				"MANIFEST_PATH", deploymentManifestPath,
				"PIPELINES_GCP_IMPERSONATESERVICEACCOUNT", String.format(
						"terraform@%s.iam.gserviceaccount.com",
						manifest.getProject())));

		return Job.builder()
				.name("update-cloud-run")
				.plan(List.of(
						inParallel(List.of(
								get("ci-src"),
								getWithTrigger("manifest-src"))),
						Task.builder()
								.task("update-cloud-run")
								.file("ci-src/.concourse/tasks/cloudrun/cloudrun-deploy.yaml")
								.inputMapping(new TreeMap<>(Map.of(
										"src", "manifest-src")))
								.params(taskParams)
								.build()))
				.onSuccess(teamsSuccessNotification())
				.onFailure(teamsFailureNotification())
				.build();
	}

	Job updateComposerDags(Dags dags, DeploymentManifest manifest) {
		var dagsSrc = String.format("%s-dags-src", dags.getName());
		return Job.builder()
				.name(String.format("update-composer-dags-%s", dags.getName()))
				.plan(List.of(
						inParallel(List.of(
								get("ci-src"),
								getWithTrigger(dagsSrc))),
						Task.builder()
								.task("update-dags")
								.file("ci-src/.concourse/tasks/composer-update-dags/composer-update-dags.yaml")
								.inputMapping(new TreeMap<>(Map.of("dags-src", dagsSrc)))
								.params(new TreeMap<>(Map.of(
										"COMPOSER_DAGS_BUCKET_NAME", dags.getBucketName(),
										"COMPOSER_DAGS_BUCKET_PATH", dags.getBucketPath(),
										"COMPOSER_DAGS_PATH", dags.getGit().getPath(),
										"GOOGLE_IMPERSONATE_SERVICE_ACCOUNT", String.format(
												"terraform@%s.iam.gserviceaccount.com",
												manifest.getProject()))))
								.build()))
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
