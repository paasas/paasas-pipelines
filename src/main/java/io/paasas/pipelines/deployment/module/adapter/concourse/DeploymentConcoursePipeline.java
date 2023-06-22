package io.paasas.pipelines.deployment.module.adapter.concourse;

import java.nio.file.Path;
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
import io.paasas.pipelines.deployment.domain.model.GitWatcher;
import io.paasas.pipelines.deployment.domain.model.TerraformWatcher;
import io.paasas.pipelines.deployment.domain.model.app.App;
import io.paasas.pipelines.deployment.domain.model.app.RegistryType;
import io.paasas.pipelines.deployment.domain.model.composer.ComposerConfig;
import io.paasas.pipelines.platform.domain.model.TargetConfig;
import io.paasas.pipelines.util.concourse.CommonResourceTypes;
import io.paasas.pipelines.util.concourse.ConcoursePipeline;
import io.paasas.pipelines.util.concourse.model.Group;
import io.paasas.pipelines.util.concourse.model.Job;
import io.paasas.pipelines.util.concourse.model.Pipeline;
import io.paasas.pipelines.util.concourse.model.Resource;
import io.paasas.pipelines.util.concourse.model.ResourceType;
import io.paasas.pipelines.util.concourse.model.resource.GitSource;
import io.paasas.pipelines.util.concourse.model.resource.RegistryImageSource;
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

	Stream<Resource<?>> appResources(App app) {
		if (app.getRegistryType() == RegistryType.GCR
				&& (configuration.getGcrCredentialsJsonSecretName() != null &&
						configuration.getGcrCredentialsJsonSecretName().isBlank())) {
			throw new IllegalStateException("gcr credentials json is undefined");
		}

		var streamBuilder = Stream.<Resource<?>>builder().add(
				Resource.builder()
						.name(String.format("%s-image", app.getName()))
						.type(CommonResourceTypes.REGISTRY_IMAGE_RESOURCE_TYPE)
						.source(RegistryImageSource.builder()
								.repository(app.getImage())
								.tag(app.getTag() != null && !app.getTag().isBlank()
										? app.getTag()
										: null)
								.username(app.getRegistryType() == RegistryType.GCR ? "_json_key" : null)
								.password(
										app.getRegistryType() == RegistryType.GCR
												? String.format("((%s))",
														configuration.getGcrCredentialsJsonSecretName())
												: null)
								.build())
						.build());

		if (app.getTests() != null) {
			streamBuilder.add(testResource(app.getTests(), app.getName()));
		}

		return streamBuilder.build();
	}

	private void assertArgument(String value, String property, ComposerConfig composerConfig) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(String.format(
					"value of property %s of composer dags `%s` is undefined",
					property,
					composerConfig.getName()));
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

	Resource<?> composerDagsResource(ComposerConfig composerConfig, int index) {
		if (composerConfig.getDags() == null) {
			throw new IllegalArgumentException(String.format(
					"dags configuration for composer %s is undefined",
					composerConfig.getName()));
		}

		if (composerConfig.getDags().getGit() == null) {
			throw new IllegalArgumentException(String.format(
					"git configuration for composer dags %s is undefined",
					composerConfig.getName()));
		}

		assertArgument(composerConfig.getDags().getGit().getUri(), "git.uri", composerConfig);

		if ((composerConfig.getDags().getGit().getBranch() == null
				|| composerConfig.getDags().getGit().getBranch().isBlank()) &&
				(composerConfig.getDags().getGit().getTag() == null
						|| composerConfig.getDags().getGit().getTag().isBlank())) {
			throw new IllegalArgumentException(String.format(
					"branch or tag filter needs to be defined for composer dags %s",
					composerConfig.getName()));
		}

		return Resource.builder()
				.name(String.format("%s-dags-src", composerConfig.getName()))
				.type("git")
				.source(GitSource.builder()
						.uri(composerConfig.getDags().getGit().getUri())
						.privateKey("((git.ssh-private-key))")
						.branch(composerConfig.getDags().getGit().getBranch())
						.paths(composerConfig.getDags().getGit().getPath() != null
								? List.of(composerConfig.getDags().getGit().getPath())
								: null)
						.build())
				.build();
	}

	Stream<Job> composerJobs(
			ComposerConfig composerConfig,
			DeploymentManifest manifest,
			String deploymentManifestPath) {
		return Stream.<Job>builder()
				.add(updateComposerDagsJob(composerConfig, manifest))
				.add(updateComposerVariablesJob(composerConfig, manifest, deploymentManifestPath))
				.build();
	}

	Stream<Resource<?>> composerResources(
			ComposerConfig composerConfig,
			int index,
			String deploymentManifestPath) {
		if (composerConfig.getName() == null || composerConfig.getName().isBlank()) {
			throw new IllegalArgumentException(String.format(
					"name is undefined for cloud composer dags at index %i",
					index));
		}

		if (composerConfig.getLocation() == null || composerConfig.getLocation().isBlank()) {
			throw new IllegalArgumentException(String.format(
					"location for cloud composer %s is undefined",
					composerConfig.getName()));
		}

		return Stream.<Resource<?>>builder()
				.add(composerDagsResource(composerConfig, index))
				.add(composerVariablesResource(composerConfig, deploymentManifestPath))
				.build();
	}

	String composerVariablesPath(
			ComposerConfig composerConfig,
			String manifestPath) {
		var path = Path.of(manifestPath);
		var manifestDirectory = path.getParent().toString();
		var deploymentName = path.getFileName().toString().split("\\.")[0];

		return String.format(
				"%s/%s-composer-variables/%s.json",
				manifestDirectory,
				deploymentName,
				composerConfig.getName());
	}

	Resource<?> composerVariablesResource(
			ComposerConfig composerConfig,
			String deploymentManifestPath) {
		return Resource.builder()
				.name(String.format("%s-variables-src", composerConfig.getName()))
				.type("git")
				.source(GitSource.builder()
						.branch(configuration.getDeploymentSrcBranch() != null
								&& !configuration.getDeploymentSrcBranch().isBlank()
										? configuration.getDeploymentSrcBranch()
										: null)
						.paths(List.of(composerVariablesPath(composerConfig, deploymentManifestPath)))
						.privateKey("((git.ssh-private-key))")
						.uri(configuration.getDeploymentSrcUri())
						.build())
				.build();
	}

	Stream<Resource<?>> deploymentResources(DeploymentManifest manifest, String deploymentManifestPath) {
		var streamBuilder = Stream.<Resource<?>>builder()
				.add(manifestResource(deploymentManifestPath));

		if (manifest.getApps() != null) {
			manifest.getApps()
					.stream()
					.flatMap(this::appResources)
					.forEach(streamBuilder::add);
		}

		if (manifest.getTerraform() != null) {
			IntStream.range(0, manifest.getTerraform().size())
					.boxed()
					.map(index -> terraformResource(manifest.getTerraform().get(index), index))
					.forEach(streamBuilder::add);
		}

		if (manifest.getComposer() != null) {
			IntStream.range(0, manifest.getComposer().size())
					.boxed()
					.flatMap(index -> composerResources(
							manifest.getComposer().get(index),
							index,
							deploymentManifestPath))
					.forEach(streamBuilder::add);
		}

		if (manifest.getFirebaseApp() != null) {
			firebaseResources(manifest).forEach(streamBuilder::add);
		}

		return streamBuilder.build();
	}

	Stream<Job> firebaseJobs(
			DeploymentManifest manifest,
			String deploymentManifestPath) {
		var firebaseApp = manifest.getFirebaseApp();

		if (firebaseApp == null) {
			throw new IllegalArgumentException("firebase app is undefined");
		}
		if (firebaseApp.getNpm() == null) {
			throw new IllegalArgumentException("firebase app npm config is undefined");
		}

		var serviceAccount = String.format("terraform@%s.iam.gserviceaccount.com", manifest.getProject());

		var mappings = new TreeMap<>(Map.of("src", "firebase-src"));

		var streamBuilder = Stream.<Job>builder().add(Job.builder()
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
				.build());

		if (manifest.getFirebaseApp().getTests() != null) {
			streamBuilder.add(testJob(
					manifest.getFirebaseApp().getTests(),
					"deploy-firebase",
					"firebase-app",
					manifest,
					deploymentManifestPath));
		}

		return streamBuilder.build();
	}

	Stream<Resource<?>> firebaseResources(DeploymentManifest deploymentManifest) {
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

		var streamBuilder = Stream.<Resource<?>>builder().add(Resource.builder()
				.name("firebase-src")
				.type("git")
				.source(GitSource.builder()
						.uri(deploymentManifest.getFirebaseApp().getGit().getUri())
						.privateKey("((git.ssh-private-key))")
						.branch(deploymentManifest.getFirebaseApp().getGit().getBranch())
						.paths(deploymentManifest.getFirebaseApp().getGit().getPath() != null
								? List.of(deploymentManifest.getFirebaseApp().getGit().getPath())
								: null)
						.tagFilter(deploymentManifest.getFirebaseApp().getGit().getTag())
						.build())
				.build());

		if (deploymentManifest.getFirebaseApp().getTests() != null) {
			streamBuilder.add(testResource(deploymentManifest.getFirebaseApp().getTests(), "firebase-app"));
		}

		return streamBuilder.build();
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
		var streamBuilder = Stream.<Job>builder();

		updateCloudRunJobs(manifest, deploymentManifestPath)
				.forEach(streamBuilder::add);

		if (manifest.getTerraform() != null) {
			manifest.getTerraform().stream()
					.map(watcher -> terraformApplyJob(watcher, manifest, target, deploymentManifestPath))
					.forEach(streamBuilder::add);
		}

		if (manifest.getComposer() != null) {
			manifest.getComposer().stream()
					.flatMap(dags -> composerJobs(dags, manifest, deploymentManifestPath))
					.forEach(streamBuilder::add);
		}

		if (manifest.getFirebaseApp() != null) {
			firebaseJobs(manifest, deploymentManifestPath).forEach(streamBuilder::add);
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
						.resourceTypes(resourceTypes(manifest))
						.resources(resources(manifest, deploymentManifestPath))
						.jobs(jobs(manifest, target, deploymentManifestPath))
						.build());
	}

	List<Resource<?>> resources(DeploymentManifest manifest, String deploymentManifestPath) {
		return Stream
				.concat(
						commonResources(),
						deploymentResources(manifest, deploymentManifestPath))
				.toList();
	}

	List<ResourceType> resourceTypes(DeploymentManifest manifest) {
		return Stream.<ResourceType>builder()
				.add(CommonResourceTypes.TEAMS_NOTIFICATION)
				.build().toList();
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
					"dataset is undefined for big query watcher at index %i",
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

	Job testJob(
			GitWatcher tests,
			String passed,
			String appName,
			DeploymentManifest deploymentManifest,
			String deploymentManifestPath) {
		return Job.builder()
				.name("test-" + appName)
				.plan(List.of(
						inParallel(List.of(
								get("ci-src"),
								Get.builder()
										.get("manifest-src")
										.passed(List.of(passed))
										.trigger(true)
										.build(),
								getWithTrigger(appName + "-tests-src"))),
						Task.builder()
								.task("test-" + appName)
								.file("ci-src/.concourse/tasks/maven-test/maven-test.yaml")
								.inputMapping(new TreeMap<>(Map.of(
										"src", appName + "-tests-src")))
								.params(new TreeMap<>(Map.of(
										"MANIFEST_PATH", deploymentManifestPath,
										"MVN_REPOSITORY_USERNAME", configuration.getGithubUsername(),
										"MVN_REPOSITORY_PASSWORD", "((github.accessToken))",
										"PIPELINES_GCP_IMPERSONATESERVICEACCOUNT", String.format(
												"terraform@%s.iam.gserviceaccount.com",
												deploymentManifest.getProject()))))
								.build()))
				.build();
	}

	Resource<?> testResource(GitWatcher tests, String appName) {
		if ((tests.getBranch() == null || tests.getBranch().isBlank()) &&
				tests.getTag() == null || tests.getTag().isBlank()) {
			throw new IllegalArgumentException(
					"branch or tag is required for test configuration of app " + appName);
		}

		if (tests.getUri() == null || tests.getUri().isBlank()) {
			throw new IllegalArgumentException("uri is required for test configuration of app " + appName);
		}

		return Resource.builder()
				.name(appName + "-tests-src")
				.type(CommonResourceTypes.GIT_RESOURCE_TYPE)
				.source(GitSource.builder()
						.branch(tests.getBranch() != null
								&& !tests.getBranch().isBlank()
										? tests.getBranch()
										: null)
						.tagFilter(tests.getTag() != null && !tests.getTag().isBlank()
								? tests.getBranch()
								: null)
						.paths(tests.getPath() != null ? List.of(tests.getPath()) : null)
						.privateKey("((git.ssh-private-key))")
						.uri(tests.getUri())
						.build())
				.build();
	}

	Stream<Job> updateCloudRunJobs(DeploymentManifest manifest, String deploymentManifestPath) {
		var apps = manifest.getApps() != null ? manifest.getApps() : List.<App>of();

		var cloudRunJob = Job.builder()
				.name("update-cloud-run")
				.plan(List.of(
						inParallel(
								Stream.concat(
										Stream.<Step>of(
												get("ci-src"),
												getWithTrigger("manifest-src")),
										apps.stream()
												.map(app -> String.format("%s-image", app.getName()))
												.map(this::getWithTrigger))
										.toList()),
						Task.builder()
								.task("update-cloud-run")
								.file("ci-src/.concourse/tasks/cloudrun/cloudrun-deploy.yaml")
								.inputMapping(new TreeMap<>(Map.of(
										"src", "manifest-src")))
								.params(new TreeMap<>(Map.of(
										"MANIFEST_PATH", deploymentManifestPath,
										"PIPELINES_GCP_IMPERSONATESERVICEACCOUNT", String.format(
												"terraform@%s.iam.gserviceaccount.com",
												manifest.getProject()))))
								.build()))
				.onSuccess(teamsSuccessNotification())
				.onFailure(teamsFailureNotification())
				.build();

		var jobBuilder = Stream.<Job>builder().add(cloudRunJob);

		apps.stream()
				.filter(app -> app.getTests() != null)
				.map(app -> testJob(
						app.getTests(),
						"update-cloud-run",
						app.getName(),
						manifest,
						deploymentManifestPath))
				.forEach(jobBuilder::add);

		return jobBuilder.build();
	}

	Job updateComposerDagsJob(ComposerConfig composerConfig, DeploymentManifest manifest) {
		var dagsSrc = String.format("%s-dags-src", composerConfig.getName());

		var params = new TreeMap<>(Map.of(
				"COMPOSER_DAGS_BUCKET_NAME", composerConfig.getBucketName(),
				"COMPOSER_DAGS_BUCKET_PATH", composerConfig.getBucketPath(),
				"COMPOSER_DAGS_PATH", composerConfig
						.getDags().getGit().getPath(),
				"GOOGLE_IMPERSONATE_SERVICE_ACCOUNT", String.format(
						"terraform@%s.iam.gserviceaccount.com",
						manifest.getProject())));

		if (composerConfig.getDags() != null && composerConfig.getDags().getFlexTemplates() != null) {
			try {
				params.put(
						"COMPOSER_FLEX_TEMPLATES",
						OBJECT_MAPPER.writeValueAsString(composerConfig.getDags().getFlexTemplates()));
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		}

		return Job.builder()
				.name(String.format("update-composer-dags-%s", composerConfig.getName()))
				.plan(List.of(
						inParallel(List.of(
								get("ci-src"),
								getWithTrigger(dagsSrc))),
						Task.builder()
								.task("update-dags")
								.file("ci-src/.concourse/tasks/composer-update-dags/composer-update-dags.yaml")
								.inputMapping(new TreeMap<>(Map.of("dags-src", dagsSrc)))
								.params(params)
								.build()))
				.build();
	}

	Job updateComposerVariablesJob(
			ComposerConfig composerConfig,
			DeploymentManifest manifest,
			String deploymentManifestPath) {
		var variablesSrc = String.format("%s-variables-src", composerConfig.getName());

		return Job.builder()
				.name(String.format("update-composer-variables-%s", composerConfig.getName()))
				.plan(List.of(
						inParallel(List.of(
								get("ci-src"),
								getWithTrigger(variablesSrc))),
						Task.builder()
								.task("update-variables")
								.file("ci-src/.concourse/tasks/composer-update-variables/composer-update-variables.yaml")
								.inputMapping(new TreeMap<>(Map.of("composer-variables-src", variablesSrc)))
								.params(new TreeMap<>(Map.of(
										"COMPOSER_DAGS_BUCKET_NAME", composerConfig.getBucketName(),
										"COMPOSER_DAGS_BUCKET_PATH", composerConfig.getBucketPath(),
										"COMPOSER_ENVIRONMENT_NAME", composerConfig.getName(),
										"COMPOSER_LOCATION", composerConfig.getLocation(),
										"COMPOSER_PROJECT", manifest.getProject(),
										"COMPOSER_VARIABLES_PATH", composerVariablesPath(
												composerConfig,
												deploymentManifestPath),
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
