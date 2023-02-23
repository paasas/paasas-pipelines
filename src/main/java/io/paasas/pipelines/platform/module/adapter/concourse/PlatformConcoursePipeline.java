package io.paasas.pipelines.platform.module.adapter.concourse;

import java.util.List;
import java.util.stream.Collectors;

import io.paasas.pipelines.platform.domain.model.TargetConfig;
import io.paasas.pipelines.platform.module.ConcourseConfiguration;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlatformConcoursePipeline {
	ConcourseConfiguration configuration;

	private static String RESOURCES = """
			- name: {TARGET}-platform-pr
			  type: pull-request
			  source:
			    access_token: ((github.accessToken))
			    repository: {GITHUB_REPOSITORY}
			    paths:
			    - {PLATFORM_MANIFEST_PATH}
			    - {TERRAFORM_EXTENSIONS_DIRECTORY}

			- name: {TARGET}-platform-src
			  type: git
			  source:
			    uri: {PLATFORM_SRC_URI}
			    private_key: ((git.ssh-private-key))
			    branch: {PLATFORM_SRC_BRANCH}
			    paths:
			    - {PLATFORM_MANIFEST_PATH}
			    - {TERRAFORM_EXTENSIONS_DIRECTORY}

			- name: {TARGET}-deployment-src
			  type: git
			  source:
			    uri: {DEPLOYMENT_SRC_URI}
			    private_key: ((git.ssh-private-key))
			    branch: {DEPLOYMENT_SRC_BRANCH}
			    paths:
			    - {DEPLOYMENT_MANIFEST_PATH}""";

	private static String JOBS = """
			- name: {TARGET}-terraform-apply
			  plan:
			  - in_parallel:
			    - get: {TARGET}-platform-src
			      trigger: true
			    - get: ci-src
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-apply
			    file: ci-src/.concourse/tasks/terraform/terraform-apply.yaml
			    input_mapping:
			      src: {TARGET}-platform-src
			    params:
			      PLATFORM_MANIFEST_PATH: {PLATFORM_MANIFEST_PATH}
			      TARGET: {TARGET}
			      TERRAFORM_BACKEND_GCS_BUCKET: {TERRAFORM_BACKEND_GCS_BUCKET}
			      TERRAFORM_EXTENSIONS_DIRECTORY: {TERRAFORM_EXTENSIONS_DIRECTORY}
			  {TEAMS_ON_SUCCESS}
			  {TEAMS_ON_FAILURE}

			- name: {TARGET}-terraform-destroy
			  plan:
			  - in_parallel:
			    - get: {TARGET}-platform-src
			    - get: ci-src
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-destroy
			    file: ci-src/.concourse/tasks/terraform/terraform-destroy.yaml
			    input_mapping:
			      src: {TARGET}-platform-src
			    params:
			      PLATFORM_MANIFEST_PATH: {PLATFORM_MANIFEST_PATH}
			      TARGET: {TARGET}
			      TERRAFORM_BACKEND_GCS_BUCKET: {TERRAFORM_BACKEND_GCS_BUCKET}
			      TERRAFORM_EXTENSIONS_DIRECTORY: {TERRAFORM_EXTENSIONS_DIRECTORY}
			  {TEAMS_ON_SUCCESS}
			  {TEAMS_ON_FAILURE}

			- name: {TARGET}-terraform-plan
			  plan:
			  - in_parallel:
			    - get: {TARGET}-platform-pr
			      trigger: true
			    - get: ci-src
			  - in_parallel:
			    - put: {TARGET}-platform-pr
			      params:
			        path: {TARGET}-platform-pr
			        status: pending
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-plan
			    file: ci-src/.concourse/tasks/terraform/terraform-plan.yaml
			    input_mapping:
			      src: {TARGET}-platform-pr
			    params:
			      PLATFORM_MANIFEST_PATH: {PLATFORM_MANIFEST_PATH}
			      TARGET: {TARGET}
			      TERRAFORM_BACKEND_GCS_BUCKET: {TERRAFORM_BACKEND_GCS_BUCKET}
			      TERRAFORM_EXTENSIONS_DIRECTORY: {TERRAFORM_EXTENSIONS_DIRECTORY}
			  - put: {TARGET}-platform-pr
			    params:
			      comment_file: terraform-out/terraform.md
			      path: {TARGET}-platform-pr
			      status: success
			  {TEAMS_ON_SUCCESS}
			  on_failure:
			    do:
			    - put: {TARGET}-platform-pr
			      params:
			        path: {TARGET}-infra-pr
			        status: failure
			    {TEAMS_JOB_FAILED_ENTRY}

			- name: {TARGET}-deployment-update
			  plan:
			  - in_parallel:
			    - get: {TARGET}-deployment-src
			      trigger: true
			    - get: ci-src
			  - task: cloudrun-deploy
			    file: ci-src/.concourse/tasks/cloudrun/cloudrun-deploy.yaml
			    input_mapping:
			      src: {TARGET}-deployment-src
			    params:
			      MANIFEST_PATH: {DEPLOYMENT_MANIFEST_PATH}
			  {TEAMS_ON_SUCCESS}
			  {TEAMS_ON_FAILURE}""";

	private static final String TEAMS_RESOURCE = """
			- name: teams
			  type: teams-notification
			  source:
			    url: ((teams.webhookUrl))""";

	private static final String TEAMS_RESOURCE_TYPE = """
			- name: teams-notification
			  type: docker-image
			  source:
			    repository: navicore/teams-notification-resource
			    tag: latest""";

	private static final String TEAMS_VARIABLES = """
			teams_job_failed: &teams_job_failed
			  put: teams
			  params:
			    text: |
			      Job ((concourse-url))/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME failed
			    actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME

			teams_job_success: &teams_job_success
			  put: teams
			  params:
			    text: |
			      Job ((concourse-url))/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME completed successfully
			    actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME""";

	private static final String TEMPLATE = """
			{VARIABLES}

			resource_types:
			- name: pull-request
			  type: docker-image
			  source:
			    repository: teliaoss/github-pr-resource
			{EXTRA_RESOURCE_TYPES}

			resources:
			- name: ci-src
			  type: git
			  source:
			    uri: {CI_SRC_URI}
			    private_key: ((git.ssh-private-key))
			    branch: {TERAFORM_SRC_BRANCH}
			    paths:
			    - .concourse
			- name: terraform-lts-src
			  type: git
			  source:
			    uri: {TERAFORM_SRC_URI}
			    private_key: ((git.ssh-private-key))
			    branch: main
			    paths:
			    - terraform/infra/lts
			- name: terraform-next-src
			  type: git
			  source:
			    uri: {TERAFORM_SRC_URI}
			    private_key: ((git.ssh-private-key))
			    branch: {TERAFORM_SRC_BRANCH}
			    paths:
			    - terraform/infra/next
			{TARGET_RESOURCES}
			{TEAMS_RESOURCE}
			{SLACK_RESOURCE}

			jobs:
			{JOBS}

			groups:
			{GROUPS}""";

	private static final String YAML_VARIABLES = """
			{TEAMS_VARIABLES}

			{SLACK_VARIABLES}""";

	private static String group(TargetConfig targetConfig) {
		return """
				- name: {TARGET}
				  jobs:
				  - {TARGET}-terraform-apply
				  - {TARGET}-terraform-destroy
				  - {TARGET}-terraform-plan
				  - {TARGET}-deployment-update"""
				.replace("{TARGET}", targetConfig.getName());
	}

	public String pipeline(
			List<TargetConfig> targetConfigs) {
		if (targetConfigs == null || targetConfigs.size() == 0) {
			throw new IllegalArgumentException("exepcted at least one target config");
		}

		return TEMPLATE
				.replace(
						"{VARIABLES}",
						YAML_VARIABLES
								.replace("{TEAMS_VARIABLES}", isTeamsConfigured() ? TEAMS_VARIABLES : "")
								.replace("{SLACK_VARIABLES}", ""))
				.replace("{EXTRA_RESOURCE_TYPES}", isTeamsConfigured() ? TEAMS_RESOURCE_TYPE : "")
				.replace("{CI_SRC_URI}", configuration.getCiSrcUri())
				.replace("{TERAFORM_SRC_URI}", configuration.getTerraformSrcUri())
				.replace("{TERAFORM_SRC_BRANCH}", configuration.getTerraformSrcBranch())
				.replace(
						"{TARGET_RESOURCES}",
						targetConfigs.stream()
								.map(targetConfig -> targetResources(
										targetConfig,
										configuration.getDeploymentSrcBranch(),
										configuration.getDeploymentSrcUri(),
										configuration.getGithubRepository(),
										configuration.getPlatformPathPrefix(),
										configuration.getPlatformSrcBranch(),
										configuration.getPlatformSrcUri()))
								.collect(Collectors.joining("\n")))
				.replace("{TEAMS_RESOURCE}", isTeamsConfigured() ? TEAMS_RESOURCE : "")
				.replace("{SLACK_RESOURCE}", "")
				.replace(
						"{JOBS}",
						targetConfigs.stream()
								.map(this::jobs)
								.collect(Collectors.joining("\n")))
				.replace(
						"{GROUPS}",
						targetConfigs.stream()
								.map(PlatformConcoursePipeline::group)
								.collect(Collectors.joining("\n")));
	}

	private String targetResources(
			TargetConfig targetConfig,
			String deploymentSrcBranch,
			String deploymentSrcUri,
			String githubRepository,
			String platformPathPrefix,
			String platformSrcBranch,
			String platformSrcUri) {
		return RESOURCES
				.replace("{DEPLOYMENT_MANIFEST_PATH}", targetConfig.getDeploymentManifestPath())
				.replace("{GITHUB_REPOSITORY}", githubRepository)
				.replace("{PLATFORM_SRC_BRANCH}", platformSrcBranch)
				.replace("{PLATFORM_SRC_URI}", platformSrcUri)
				.replace("{DEPLOYMENT_SRC_BRANCH}", deploymentSrcBranch)
				.replace("{DEPLOYMENT_SRC_URI}", deploymentSrcUri)
				.replace("{TARGET}", targetConfig.getName())
				.replace("{PLATFORM_MANIFEST_PATH}", targetConfig.getPlatformManifestPath())
				.replace("{TERRAFORM_EXTENSIONS_DIRECTORY}", targetConfig.getTerraformExtensionsDirectory())
				.replace("{TEAMS_RESOURCE}", isTeamsConfigured() ? TEAMS_RESOURCE : "null");
	}

	private String jobs(TargetConfig targetConfig) {
		return JOBS
				.replace("{DEPLOYMENT_MANIFEST_PATH}", targetConfig.getDeploymentManifestPath())
				.replace("{TARGET}", targetConfig.getName())
				.replace("{PLATFORM_MANIFEST_PATH}", targetConfig.getPlatformManifestPath())
				.replace("{TERRAFORM_BACKEND_GCS_BUCKET}", configuration.getTerraformBackendGcsBucket())
				.replace("{TERRAFORM_EXTENSIONS_DIRECTORY}", targetConfig.getTerraformExtensionsDirectory())
				.replace("{TEAMS_ON_SUCCESS}", isTeamsConfigured() ? "on_success: *teams_job_success" : "")
				.replace("{TEAMS_ON_FAILURE}", isTeamsConfigured() ? "on_failure: *teams_job_failed" : "")
				.replace("{TEAMS_JOB_FAILED_ENTRY}", isTeamsConfigured() ? "- <<: *teams_job_failed" : "");
	}

	private boolean isTeamsConfigured() {
		return configuration.getTeamsWebhookUrl() != null && !configuration.getTeamsWebhookUrl().isBlank();
	}
}
