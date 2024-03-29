package io.paasas.pipelines.deployment.module.adapter.concourse;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import io.paasas.pipelines.deployment.domain.model.TestGitWatcher;
import io.paasas.pipelines.deployment.domain.model.app.App;
import io.paasas.pipelines.deployment.domain.model.app.RegistryType;
import io.paasas.pipelines.deployment.domain.model.composer.ComposerConfig;
import io.paasas.pipelines.deployment.domain.model.composer.FlexTemplate;
import io.paasas.pipelines.deployment.domain.model.firebase.FirebaseAppDefinition;
import io.paasas.pipelines.platform.domain.model.TargetConfig;
import io.paasas.pipelines.util.concourse.CommonResourceTypes;
import io.paasas.pipelines.util.concourse.ConcoursePipeline;
import io.paasas.pipelines.util.concourse.model.Group;
import io.paasas.pipelines.util.concourse.model.Job;
import io.paasas.pipelines.util.concourse.model.Pipeline;
import io.paasas.pipelines.util.concourse.model.Resource;
import io.paasas.pipelines.util.concourse.model.ResourceType;
import io.paasas.pipelines.util.concourse.model.resource.CronSource;
import io.paasas.pipelines.util.concourse.model.resource.GitSource;
import io.paasas.pipelines.util.concourse.model.resource.PullRequestSource;
import io.paasas.pipelines.util.concourse.model.resource.RegistryImageSource;
import io.paasas.pipelines.util.concourse.model.step.Get;
import io.paasas.pipelines.util.concourse.model.step.InParallel;
import io.paasas.pipelines.util.concourse.model.step.Put;
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

	private static final String BUILD_METADATA = "build-metadata";
	private static final String CI_SRC_RESOURCE = "ci-src";
	private static final String PULL_REQUEST = "pr";

	GcpConfiguration gcpConfiguration;

	public DeploymentConcoursePipeline(ConcourseConfiguration configuration, GcpConfiguration gcpConfiguration) {
		super(configuration);

		this.gcpConfiguration = gcpConfiguration;
	}

	private boolean isNotBlank(String value) {
		return value != null && !value.isBlank();
	}

	private Job analyzePullRequestJob(DeploymentManifest deploymentManifest, String deploymentManifestPath) {
		return Job.builder()
				.name("analyze-pull-request")
				.plan(List.of(
						inParallel(List.of(
								get(BUILD_METADATA),
								getWithTrigger(PULL_REQUEST),
								get(CI_SRC_RESOURCE))),
						Task.builder()
								.task("analyze-pull-request")
								.file(CI_SRC_RESOURCE
										+ "/.concourse/tasks/analyze-pull-request/analyze-pull-request.yaml")
								.params(new TreeMap<>(Map.of(
										"GCP_PROJECT_ID", deploymentManifest.getProject(),
										"GITHUB_REPOSITORY", configuration.getGithubDeploymentRepository(),
										"MANIFEST_PATH", deploymentManifestPath,
										"PIPELINES_SERVER", configuration.getPipelinesServer(),
										"PIPELINES_SERVER_USERNAME", configuration.getPipelinesServerUsername())))
								.build()))
				.build();
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
			app.getTests().stream()
					.flatMap(tests -> testResources(tests, app.getName()))
					.forEach(streamBuilder::add);
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

	Stream<Resource<?>> commonResources(DeploymentManifest manifest, String deploymentManifestPath) {
		var builder = Stream.<Resource<?>>builder().add(
				Resource
						.builder()
						.name(BUILD_METADATA)
						.type(CommonResourceTypes.BUILD_METADATA_TYPE)
						.build())
				.add(
						Resource.builder()
								.name(PULL_REQUEST)
								.type(CommonResourceTypes.GITHUB_PULL_REQUEST_TYPE)
								.source(PullRequestSource.builder()
										.accessToken("((github.accessToken))")
										.repository(configuration.getGithubDeploymentRepository())
										.paths(List.of(deploymentManifestPath))
										.build())
								.build())
				.add(
						Resource.builder()
								.name(CI_SRC_RESOURCE)
								.type(CommonResourceTypes.GIT_RESOURCE_TYPE)
								.source(GitSource.builder()
										.uri(configuration.getCiSrcUri())
										.privateKey("((git.ssh-private-key))")
										.branch("main")
										.paths(List.of(".concourse"))
										.build())
								.build())
				.add(
						Resource.builder()
								.name("teams")
								.type(CommonResourceTypes.TEAMS_NOTIFICATION_RESOURCE_TYPE)
								.source(Map.of("url", "((teams.webhookUrl))"))
								.build());

		if (containsTests(manifest)) {
			builder.add(Resource.builder()
					.name("metadata")
					.type(CommonResourceTypes.METADATA_RESOURCE_TYPE)
					.build());
		}

		return builder.build();
	}

	Job composerBuildFlexTemplatesJob(
			FlexTemplate flexTemplate,
			ComposerConfig composerConfig,
			DeploymentManifest manifest) {
		var dagsSrc = String.format("%s-dags-src", composerConfig.getName());
		var imageName = flexTemplate.getName().replace(" ", "");

		return Job.builder()
				.name("build-flex-template-" + imageName)
				.plan(List.of(
						inParallel(List.of(
								get(CI_SRC_RESOURCE),
								getWithTrigger(imageName + "-image"),
								Get.builder()
										.get(dagsSrc)
										.passed(List
												.of(String.format("update-composer-dags-%s", composerConfig.getName())))
										.build())),
						Task.builder()
								.task("build-flex-template")
								.file(CI_SRC_RESOURCE
										+ "/.concourse/tasks/composer-update-flex-templates/composer-update-flex-templates.yaml")
								.inputMapping(new TreeMap<>(Map.of("dags-src", dagsSrc)))
								.params(new TreeMap<>(Map.of(
										"COMPOSER_FLEX_TEMPLATES_TARGET_BUCKET", composerConfig.getBucketName(),
										"COMPOSER_FLEX_TEMPLATES_TARGET_PATH", flexTemplate.getGcsPath(),
										"COMPOSER_FLEX_TEMPLATES_IMAGE", flexTemplate.getImage(),
										"COMPOSER_FLEX_TEMPLATES_VERSION", flexTemplate.getImageTag(),
										"COMPOSER_FLEX_TEMPLATES_METADATA", flexTemplate.getMetadataFile(),
										"GOOGLE_IMPERSONATE_SERVICE_ACCOUNT",
										String.format("terraform@%s.iam.gserviceaccount.com", manifest.getProject()))))
								.build()))
				.build();
	}

	Stream<Job> composerBuildFlexTemplatesJobs(ComposerConfig composerConfig, DeploymentManifest manifest) {
		if (composerConfig.getDags() == null || composerConfig.getDags().getFlexTemplates() == null) {
			return Stream.empty();
		}

		return composerConfig.getDags().getFlexTemplates()
				.stream()
				.map(flexTemplate -> composerBuildFlexTemplatesJob(flexTemplate, composerConfig, manifest));
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
						.tagFilter(composerConfig.getDags().getGit().getTag())
						.build())
				.build();
	}

	Resource<?> composerFlexTemplatesResource(FlexTemplate flexTemplate) {
		if (flexTemplate == null) {
			throw new IllegalArgumentException("undefined flex template");
		}

		if (flexTemplate.getName() == null || flexTemplate.getName().isBlank()) {
			throw new IllegalArgumentException("name for flex template is undefined");
		}

		if (flexTemplate.getGcsPath() == null || flexTemplate.getGcsPath().isBlank()) {
			throw new IllegalArgumentException(String.format(
					"gcs path for flex template %s is undefined",
					flexTemplate.getName()));
		}

		if (flexTemplate.getImage() == null || flexTemplate.getImage().isBlank()) {
			throw new IllegalArgumentException(String.format(
					"image for flex template %s is undefined",
					flexTemplate.getName()));
		}

		if (flexTemplate.getImageTag() == null || flexTemplate.getImageTag().isBlank()) {
			throw new IllegalArgumentException(String.format(
					"image tag for flex template %s is undefined",
					flexTemplate.getName()));
		}

		if (flexTemplate.getMetadataFile() == null || flexTemplate.getMetadataFile().isBlank()) {
			throw new IllegalArgumentException(String.format(
					"metadata file for flex template %s is undefined",
					flexTemplate.getName()));
		}

		return Resource.builder()
				.name(String.format("%s-image", flexTemplate.getName().replace(" ", "")))
				.type(CommonResourceTypes.REGISTRY_IMAGE_RESOURCE_TYPE)
				.source(RegistryImageSource.builder()
						.repository(flexTemplate.getImage())
						.tag(flexTemplate.getImageTag())
						.username(flexTemplate.getImageRegistryType() == RegistryType.GCR ? "_json_key" : null)
						.password(
								flexTemplate.getImageRegistryType() == RegistryType.GCR
										? String.format("((%s))",
												configuration.getGcrCredentialsJsonSecretName())
										: null)
						.build())
				.build();
	}

	Stream<Resource<?>> composerFlexTemplatesResources(ComposerConfig composerConfig) {
		if (composerConfig.getDags() == null || composerConfig.getDags().getFlexTemplates() == null) {
			return Stream.empty();
		}

		return composerConfig.getDags().getFlexTemplates().stream()
				.map(this::composerFlexTemplatesResource);
	}

	Stream<Job> composerJobs(
			ComposerConfig composerConfig,
			DeploymentManifest manifest,
			String deploymentManifestPath) {
		var builder = Stream.<Job>builder()
				.add(updateComposerVariablesJob(composerConfig, manifest, deploymentManifestPath))
				.add(updateComposerDagsJob(composerConfig, manifest));

		composerBuildFlexTemplatesJobs(composerConfig, manifest).forEach(builder::add);

		return builder.build();
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

		var builder = Stream.<Resource<?>>builder()
				.add(composerDagsResource(composerConfig, index))
				.add(composerVariablesResource(composerConfig, deploymentManifestPath));

		composerFlexTemplatesResources(composerConfig)
				.forEach(builder::add);

		return builder.build();
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

	boolean containsCron(DeploymentManifest manifest) {
		// Checks if the firebase app has any cron configs in its tests.
		return (manifest.getFirebaseApp() != null && manifest.getFirebaseApp().getTests() != null
				&& manifest.getFirebaseApp().getTests().stream()
						.filter(tests -> tests.getCron() != null && !tests.getCron().isBlank()).findAny().isPresent()

				||

				// Checks if any of the cloud run app has any cron configs in its tests.
				manifest.getApps() != null && manifest.getApps().stream()
						.filter(app -> app.getTests() != null)
						.flatMap(app -> app.getTests().stream())
						.filter(tests -> tests.getCron() != null && !tests.getCron().isBlank()).findAny().isPresent());
	}

	boolean containsTests(DeploymentManifest manifest) {
		return (manifest.getFirebaseApp() != null && manifest.getFirebaseApp().getTests() != null) ||
				(manifest.getApps() != null
						&& manifest.getApps().stream().filter(app -> app.getTests() != null).findAny().isPresent());
	}

	Map<String, String> testJobCommonParams(
			TestGitWatcher tests,
			String appName,
			DeploymentManifest deploymentManifest,
			String deploymentManifestPath) {
		var params = new TreeMap<>(Map.of(
				"APP_ID", appName,
				"GIT_PRIVATE_KEY", "((git.ssh-private-key))",
				"GIT_USER_EMAIL", configuration.getGithubEmail(),
				"GIT_USER_NAME", configuration.getGithubUsername(),
				"GOOGLE_IMPERSONATE_SERVICE_ACCOUNT", String.format(
						"terraform@%s.iam.gserviceaccount.com",
						deploymentManifest.getProject()),
				"GOOGLE_PROJECT_ID", deploymentManifest.getProject(),
				"MANIFEST_PATH", deploymentManifestPath,
				"MVN_REPOSITORY_USERNAME", configuration.getGithubUsername(),
				"MVN_REPOSITORY_PASSWORD", "((github.accessToken))",
				"PIPELINES_GCP_IMPERSONATESERVICEACCOUNT", String.format(
						"terraform@%s.iam.gserviceaccount.com",
						deploymentManifest.getProject())));

		params.putAll(Map.of("TEST_NAME", tests.getName()));

		if (configuration.getPipelinesServer() != null && !configuration.getPipelinesServer().isBlank()) {
			params.putAll(Map.of(
					"PIPELINES_SERVER", configuration.getPipelinesServer(),
					"PIPELINES_SERVER_USERNAME", configuration.getPipelinesServerUsername(),
					"TEST_GITHUB_REPOSITORY", tests.getUri()
							.replace("git@github.com:", "")
							.replace(".git", "")));
		}

		if (isNotBlank(tests.getExtraMavenOpts())) {
			params.put("MVN_EXTRA_OPTS", tests.getExtraMavenOpts());
		}

		return params;
	}

	Job cloudRunTestJob(
			TestGitWatcher tests,
			App app,
			String passed,
			DeploymentManifest deploymentManifest,
			String deploymentManifestPath) {

		var testSrc = String.format("%s-tests-%s-src", app.getName(), tests.getName());

		var hasCron = tests.getCron() != null && !tests.getCron().isBlank();

		var manifestResource = hasCron
				? Get.builder()
						.get("manifest-src")
						.passed(List.of(passed))
						.build()
				: Get.builder()
						.get("manifest-src")
						.passed(List.of(passed))
						.trigger(true)
						.build();

		return testJob(
				app.getName(),
				tests.getName(),
				CI_SRC_RESOURCE + "/.concourse/tasks/maven-test/maven-cloud-run-test.yaml",
				testJobCommonParams(
						tests,
						app.getName(),
						deploymentManifest,
						deploymentManifestPath),
				List.of(
						get(BUILD_METADATA),
						get(CI_SRC_RESOURCE),
						manifestResource,
						put("metadata"),
						hasCron ? get(testSrc) : getWithTrigger(testSrc),
						get(String.format("%s-test-%s-reports-src", app.getName(), tests.getName()))));
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

		var deployParamms = new TreeMap<>(Map.of(
				"GCP_PROJECT_ID", manifest.getProject(),
				"GOOGLE_IMPERSONATE_SERVICE_ACCOUNT", serviceAccount,
				"FIREBASE_APP_PATH", blankIfNull(firebaseApp.getGit().getPath()),
				"FIREBASE_CONFIG", blankIfNull(firebaseApp.getConfig()),
				"MANIFEST_PATH", deploymentManifestPath));

		if (configuration.getPipelinesServer() != null && !configuration.getPipelinesServer().isBlank()) {
			deployParamms.putAll(Map.of(
					"GITHUB_REPOSITORY", firebaseApp.getGit().getUri()
							.replace("git@github.com:", "")
							.replace(".git", ""),
					"PIPELINES_SERVER", configuration.getPipelinesServer(),
					"PIPELINES_SERVER_USERNAME", configuration.getPipelinesServerUsername()));
		}

		var streamBuilder = Stream.<Job>builder().add(Job.builder()
				.name("deploy-firebase")
				.plan(List.of(
						inParallel(List.of(
								get(BUILD_METADATA),
								get(CI_SRC_RESOURCE),
								get("manifest-src"),
								getWithTrigger("firebase-src"))),
						Task.builder()
								.task("npm-build")
								.file(CI_SRC_RESOURCE + "/.concourse/tasks/npm-build/npm-build.yaml")
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
								.file(CI_SRC_RESOURCE + "/.concourse/tasks/firebase-deploy/firebase-deploy.yaml")
								.inputMapping(mappings)
								.outputMapping(mappings)
								.params(deployParamms)
								.build()))
				.build());

		if (manifest.getFirebaseApp().getTests() != null) {
			manifest.getFirebaseApp().getTests().stream()
					.forEach(tests -> streamBuilder.add(firebaseTestJob(
							tests,
							manifest.getFirebaseApp(),
							"deploy-firebase",
							manifest,
							deploymentManifestPath)));
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
			deploymentManifest.getFirebaseApp().getTests()
					.stream()
					.flatMap(tests -> testResources(tests, "firebase-app"))
					.forEach(streamBuilder::add);
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
			DeploymentManifest deploymentManifest,
			String target,
			String deploymentManifestPath) {
		var streamBuilder = Stream.<Job>builder()
				.add(analyzePullRequestJob(deploymentManifest, deploymentManifestPath));

		updateCloudRunJobs(deploymentManifest, deploymentManifestPath)
				.forEach(streamBuilder::add);

		if (deploymentManifest.getTerraform() != null) {
			deploymentManifest.getTerraform().stream()
					.map(watcher -> terraformApplyJob(watcher, deploymentManifest, target, deploymentManifestPath))
					.forEach(streamBuilder::add);

			deploymentManifest.getTerraform().stream()
					.map(watcher -> terraformPlanJob(watcher, deploymentManifest, target, deploymentManifestPath))
					.forEach(streamBuilder::add);
		}

		if (deploymentManifest.getComposer() != null) {
			deploymentManifest.getComposer().stream()
					.flatMap(dags -> composerJobs(dags, deploymentManifest, deploymentManifestPath))
					.forEach(streamBuilder::add);
		}

		if (deploymentManifest.getFirebaseApp() != null) {
			firebaseJobs(deploymentManifest, deploymentManifestPath).forEach(streamBuilder::add);
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

	private Put put(String resource) {
		return Put.builder()
				.put(resource)
				.build();
	}

	List<Resource<?>> resources(DeploymentManifest manifest, String deploymentManifestPath) {
		return Stream
				.concat(
						commonResources(manifest, deploymentManifestPath),
						deploymentResources(manifest, deploymentManifestPath))
				.toList();
	}

	List<ResourceType> resourceTypes(DeploymentManifest manifest) {
		var builder = Stream.<ResourceType>builder()
				.add(CommonResourceTypes.TEAMS_NOTIFICATION);

		if (containsTests(manifest)) {
			builder.add(CommonResourceTypes.METADATA);
		}

		if (containsCron(manifest)) {
			builder.add(CommonResourceTypes.CRON);
		}

		return builder
				.add(CommonResourceTypes.BUILD_METADATA)
				.add(CommonResourceTypes.GITHUB_PULL_REQUEST)
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
				"GITHUB_REPOSITORY", configuration.getGithubDeploymentRepository(),
				"MANIFEST_PATH", deploymentManifestPath,
				"PIPELINES_SERVER", configuration.getPipelinesServer(),
				"PIPELINES_SERVER_USERNAME", configuration.getPipelinesServerUsername(),
				"TERRAFORM_PREFIX", target + "-" + watcher.getName(),
				"TERRAFORM_BACKEND_GCS_BUCKET", terraformBackendGcsBucket,
				"TERRAFORM_DIRECTORY", watcher.getGit().getPath(),
				"TERRAFORM_GROUP_NAME", watcher.getName(),
				"GOOGLE_IMPERSONATE_SERVICE_ACCOUNT", String.format(
						"terraform@%s.iam.gserviceaccount.com",
						manifest.getProject())));

		if (configuration.getPipelinesServer() != null && !configuration.getPipelinesServer().isBlank() &&
				configuration.getPipelinesServerUsername() != null
				&& !configuration.getPipelinesServerUsername().isBlank()) {
			terraformParams.putAll(Map.of(
					"GITHUB_REPOSITORY", watcher.getGit().getUri()
							.replace("git@github.com:", "")
							.replace(".git", ""),
					"PIPELINES_SERVER", configuration.getPipelinesServer(),
					"PIPELINES_SERVER_USERNAME", configuration.getPipelinesServerUsername()));
		}

		var src = String.format("terraform-%s-src", watcher.getName());

		return Job.builder()
				.name(String.format("terraform-apply-%s", watcher.getName()))
				.plan(List.of(
						inParallel(List.of(
								get(BUILD_METADATA),
								get(CI_SRC_RESOURCE),
								getWithTrigger("manifest-src"),
								getWithTrigger(src))),
						Task.builder()
								.task("terraform-apply")
								.file(CI_SRC_RESOURCE
										+ "/.concourse/tasks/terraform-deployment/terraform-deployment-apply.yaml")
								.inputMapping(Map.of(
										"src", src))
								.params(terraformParams)
								.build()))
				.onSuccess(teamsSuccessNotification())
				.onFailure(teamsFailureNotification())
				.build();
	}

	Job terraformPlanJob(
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
				"GIT_PRIVATE_KEY", "((git.ssh-private-key))",
				"GIT_USER_EMAIL", configuration.getGithubEmail(),
				"GIT_USER_NAME", configuration.getGithubUsername(),
				"GITHUB_REPOSITORY", configuration.getGithubDeploymentRepository(),
				"GOOGLE_IMPERSONATE_SERVICE_ACCOUNT", String.format(
						"terraform@%s.iam.gserviceaccount.com",
						manifest.getProject()),
				"MANIFEST_PATH", deploymentManifestPath,
				"PIPELINES_SERVER", configuration.getPipelinesServer(),
				"PIPELINES_SERVER_USERNAME", configuration.getPipelinesServerUsername()));

		terraformParams.putAll(Map.of(
				"TERRAFORM_BACKEND_GCS_BUCKET", terraformBackendGcsBucket,
				"TERRAFORM_GROUP_NAME", watcher.getName(),
				"TERRAFORM_PREFIX", target + "-" + watcher.getName()));

		if (configuration.getPipelinesServer() != null && !configuration.getPipelinesServer().isBlank() &&
				configuration.getPipelinesServerUsername() != null
				&& !configuration.getPipelinesServerUsername().isBlank()) {
			terraformParams.putAll(Map.of(
					"GITHUB_REPOSITORY", watcher.getGit().getUri()
							.replace("git@github.com:", "")
							.replace(".git", ""),
					"PIPELINES_SERVER", configuration.getPipelinesServer(),
					"PIPELINES_SERVER_USERNAME", configuration.getPipelinesServerUsername()));
		}

		return Job.builder()
				.name(String.format("terraform-plan-%s", watcher.getName()))
				.plan(List.of(
						inParallel(List.of(
								get(BUILD_METADATA),
								get(CI_SRC_RESOURCE),
								Get.builder()
										.get("pr")
										.passed(List.of("analyze-pull-request"))
										.trigger(true)
										.build())),
						Task.builder()
								.task("terraform-plan")
								.file(CI_SRC_RESOURCE
										+ "/.concourse/tasks/terraform-deployment/terraform-deployment-pr-plan.yaml")
								.inputMapping(Map.of("manifest-src", "pr"))
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

	Job firebaseTestJob(
			TestGitWatcher tests,
			FirebaseAppDefinition firebaseApp,
			String passed,
			DeploymentManifest deploymentManifest,
			String deploymentManifestPath) {
		var appName = "firebase-app";

		var params = testJobCommonParams(tests, appName, deploymentManifest, deploymentManifestPath);

		if (configuration.getPipelinesServer() != null && !configuration.getPipelinesServer().isBlank()) {
			params.put("GITHUB_REPOSITORY", firebaseApp.getGit().getUri()
					.replace("git@github.com:", "")
					.replace(".git", ""));
		}

		var testSrc = String.format("%s-tests-%s-src", appName, tests.getName());

		var hasCron = tests.getCron() != null && !tests.getCron().isBlank();

		var manifestResource = hasCron
				? Get.builder()
						.get("manifest-src")
						.passed(List.of(passed))
						.build()
				: Get.builder()
						.get("manifest-src")
						.passed(List.of(passed))
						.trigger(true)
						.build();

		var parralelResources = new ArrayList<>(List.of(
				get(BUILD_METADATA),
				get(CI_SRC_RESOURCE),
				Get.builder()
						.get("firebase-src")
						.passed(List.of(passed))
						.build(),
				manifestResource,
				put("metadata"),
				hasCron ? get(testSrc) : getWithTrigger(testSrc),
				get(String.format("%s-test-%s-reports-src", appName, tests.getName()))));

		if (hasCron) {
			parralelResources.add(getWithTrigger(String.format("%s-tests-%s-cron", appName, tests.getName())));
		}

		return testJob(
				appName,
				tests.getName(),
				CI_SRC_RESOURCE + "/.concourse/tasks/maven-test/maven-firebase-test.yaml",
				params,
				parralelResources);
	}

	Job testJob(
			String appName,
			String testsName,
			String taskFile,
			Map<String, String> params,
			List<Step> parallelSteps) {
		return Job.builder()
				.name(String.format("test-%s-%s", appName, testsName))
				.plan(List.of(
						inParallel(parallelSteps),
						Task.builder()
								.task(String.format("test-%s-%s", appName, testsName))
								.file(taskFile)
								.inputMapping(new TreeMap<>(Map.of(
										"src", String.format("%s-tests-%s-src", appName, testsName),
										"test-reports-src", String.format(
												"%s-test-%s-reports-src",
												appName,
												testsName))))
								.params(params)
								.build()))
				.build();
	}

	Stream<Resource<?>> testResources(TestGitWatcher tests, String appName) {
		if ((tests.getBranch() == null || tests.getBranch().isBlank()) &&
				(tests.getTag() == null || tests.getTag().isBlank())) {
			throw new IllegalArgumentException(
					"branch or tag is required for test configuration of app " + appName);
		}

		if (tests.getUri() == null || tests.getUri().isBlank()) {
			throw new IllegalArgumentException("uri is required for test configuration of app " + appName);
		}

		var streamBuilder = Stream.<Resource<?>>builder()
				.add(Resource.builder()
						.name(String.format("%s-tests-%s-src", appName, tests.getName()))
						.type(CommonResourceTypes.GIT_RESOURCE_TYPE)
						.source(GitSource.builder()
								.branch(tests.getBranch() != null
										&& !tests.getBranch().isBlank()
												? tests.getBranch()
												: null)
								.tagFilter(tests.getTag() != null && !tests.getTag().isBlank()
										? tests.getTag()
										: null)
								.paths(tests.getPath() != null ? List.of(tests.getPath()) : null)
								.privateKey("((git.ssh-private-key))")
								.uri(tests.getUri())
								.build())
						.build())
				.add(Resource.builder()
						.name(String.format("%s-test-%s-reports-src", appName, tests.getName()))
						.type(CommonResourceTypes.GIT_RESOURCE_TYPE)
						.source(GitSource.builder()
								.branch(configuration.getTestReportsBranch())
								.privateKey("((git.ssh-private-key))")
								.uri(tests.getUri())
								.build())
						.build());

		if (tests.getCron() != null && !tests.getCron().isBlank()) {
			streamBuilder.add(Resource.builder()
					.name(String.format("%s-tests-%s-cron", appName, tests.getName()))
					.type(CommonResourceTypes.CRON_RESOURCE_TYPE)
					.source(CronSource.builder()
							.expression(tests.getCron())
							.location("America/New_York")
							.build())
					.build());
		}

		return streamBuilder.build();
	}

	Stream<Job> updateCloudRunJobs(DeploymentManifest manifest, String deploymentManifestPath) {
		var apps = manifest.getApps() != null ? manifest.getApps() : List.<App>of();

		var cloudRunParams = new TreeMap<>(Map.of(
				"MANIFEST_PATH", deploymentManifestPath,
				"PIPELINES_CONCOURSE_GITHUBDEPLOYMENTREPOSITORY", configuration.getGithubDeploymentRepository(),
				"PIPELINES_CONCOURSE_GITHUBPLATFORMREPOSITORY", configuration.getGithubPlatformRepository(),
				"PIPELINES_GCP_IMPERSONATESERVICEACCOUNT", String.format(
						"terraform@%s.iam.gserviceaccount.com",
						manifest.getProject())));

		if (configuration.getPipelinesServer() != null && !configuration.getPipelinesServer().isBlank()) {
			cloudRunParams.putAll(Map.of(
					"PIPELINES_SERVER", configuration.getPipelinesServer(),
					"PIPELINES_SERVER_USERNAME", configuration.getPipelinesServerUsername()));
		}

		var cloudRunJob = Job.builder()
				.name("update-cloud-run")
				.plan(List.of(
						inParallel(
								Stream.concat(
										Stream.<Step>of(
												get(BUILD_METADATA),
												get(CI_SRC_RESOURCE),
												getWithTrigger("manifest-src")),
										apps.stream()
												.map(app -> String.format("%s-image", app.getName()))
												.map(this::getWithTrigger))
										.toList()),
						Task.builder()
								.task("update-cloud-run")
								.file(CI_SRC_RESOURCE + "/.concourse/tasks/cloudrun/cloudrun-deploy.yaml")
								.inputMapping(new TreeMap<>(Map.of(
										"src", "manifest-src")))
								.params(cloudRunParams)
								.build()))
				.onSuccess(teamsSuccessNotification())
				.onFailure(teamsFailureNotification())
				.build();

		var jobBuilder = Stream.<Job>builder().add(cloudRunJob);

		apps.stream()
				.filter(app -> app.getTests() != null)
				.forEach(app -> app.getTests().stream()
						.map(tests -> cloudRunTestJob(
								tests,
								app,
								"update-cloud-run",
								manifest,
								deploymentManifestPath))
						.forEach(jobBuilder::add));

		return jobBuilder.build();
	}

	Job updateComposerDagsJob(ComposerConfig composerConfig, DeploymentManifest manifest) {
		var dagsSrc = String.format("%s-dags-src", composerConfig.getName());
		var composerVariablesSrc = String.format("%s-variables-src", composerConfig.getName());
		var updateComposerVariableJob = String.format("update-composer-variables-%s", composerConfig.getName());

		var params = new TreeMap<>(Map.of(
				"COMPOSER_DAGS_BUCKET_NAME", composerConfig.getBucketName(),
				"COMPOSER_DAGS_BUCKET_PATH", composerConfig.getBucketPath(),
				"COMPOSER_DAGS_PATH", composerConfig
						.getDags().getGit().getPath(),
				"GOOGLE_IMPERSONATE_SERVICE_ACCOUNT", String.format(
						"terraform@%s.iam.gserviceaccount.com",
						manifest.getProject())));

		if (isNotBlank(composerConfig.getDags().getPreUpdateScript())) {
			params.put("PRE_UPDATE_SCRIPT", composerConfig.getDags().getPreUpdateScript());
		}

		var resources = new ArrayList<Step>(List.of(
				get(CI_SRC_RESOURCE),
				Get.builder()
						.get(dagsSrc)
						.passed(List.of(updateComposerVariableJob))
						.trigger(true)
						.build(),
				Get.builder()
						.get(composerVariablesSrc)
						.passed(List.of(updateComposerVariableJob))
						.trigger(true)
						.build()));

		if (isNotBlank(composerConfig.getDags().getDependsOn())) {
			resources.add(
					Get.builder()
							.get(String.format("terraform-%s-src", composerConfig.getDags().getDependsOn()))
							.passed(List.of(updateComposerVariableJob))
							.build());
		}

		return Job.builder()
				.name(String.format("update-composer-dags-%s", composerConfig.getName()))
				.plan(List.of(
						inParallel(resources),
						Task.builder()
								.task("update-dags")
								.file(CI_SRC_RESOURCE
										+ "/.concourse/tasks/composer-update-dags/composer-update-dags.yaml")
								.inputMapping(new TreeMap<>(Map.of("dags-src", dagsSrc)))
								.params(params)
								.build()))
				.build();
	}

	Job updateComposerVariablesJob(
			ComposerConfig composerConfig,
			DeploymentManifest manifest,
			String deploymentManifestPath) {
		var dagsSrc = String.format("%s-dags-src", composerConfig.getName());
		var variablesSrc = String.format("%s-variables-src", composerConfig.getName());

		var resources = new ArrayList<Step>(List.of(
				get(CI_SRC_RESOURCE),
				getWithTrigger(dagsSrc),
				getWithTrigger(variablesSrc)));

		if (isNotBlank(composerConfig.getDags().getDependsOn())) {
			var dependsOnResource = Optional.ofNullable(manifest.getTerraform()).stream().flatMap(List::stream)
					.map(TerraformWatcher::getName)
					.filter(name -> name.equals(composerConfig.getDags().getDependsOn()))
					.map(name -> "terraform-apply-" + name)
					.findFirst()
					.orElseThrow(() -> new IllegalArgumentException(
							String.format(
									"could not find a terraform package name %s within the deployment manifest",
									composerConfig.getDags().getDependsOn())));

			resources.add(Get.builder()
					.get(String.format("terraform-%s-src", composerConfig.getDags().getDependsOn()))
					.passed(List.of(dependsOnResource))
					.build());
		}

		return Job.builder()
				.name(String.format("update-composer-variables-%s", composerConfig.getName()))
				.plan(List.of(
						inParallel(resources),
						Task.builder()
								.task("update-variables")
								.file(CI_SRC_RESOURCE
										+ "/.concourse/tasks/composer-update-variables/composer-update-variables.yaml")
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
