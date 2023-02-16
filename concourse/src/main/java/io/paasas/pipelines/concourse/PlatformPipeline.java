package io.paasas.pipelines.concourse;

import java.util.List;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlatformPipeline {
	PipelinesConcourseConfiguration configuration;

	private static String RESOURCES = """
			- name: {TARGET}-platform-pr
			  type: pull-request
			  source:
			    access_token: ((github.accessToken))
			    repository: {GITHUB_REPOSITORY}
			    paths:
			    - {PLATFORM_MANIFEST_PATH}

			- name: {TARGET}-platform-src
			  type: git
			  source:
			    uri: {PLATFORM_SRC_URI}
			    private_key: ((git.ssh-private-key))
			    branch: {PLATFORM_SRC_BRANCH}
			    paths:
			    - {PLATFORM_MANIFEST_PATH}""";

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
			    file: ci-src/.concourse/tasks/terraform-apply/terraform-apply.yaml
			    input_mapping:
			      src: {TARGET}-platform-src
			    params:
			      PLATFORM_MANIFEST_PATH: {PLATFORM_MANIFEST_PATH}
			      TARGET: {TARGET}
			      TERRAFORM_BACKEND_GCS_BUCKET: {TERRAFORM_BACKEND_GCS_BUCKET}
			      
			- name: {TARGET}-terraform-destroy
			  plan:
			  - in_parallel:
			    - get: {TARGET}-platform-src
			    - get: ci-src
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-apply
			    file: ci-src/.concourse/tasks/terraform-destroy/terraform-destroy.yaml
			    input_mapping:
			      src: {TARGET}-platform-src
			    params:
			      PLATFORM_MANIFEST_PATH: {PLATFORM_MANIFEST_PATH}
			      TARGET: {TARGET}
			      TERRAFORM_BACKEND_GCS_BUCKET: {TERRAFORM_BACKEND_GCS_BUCKET}

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
			    file: ci-src/.concourse/tasks/terraform-plan/terraform-plan.yaml
			    input_mapping:
			      src: {TARGET}-platform-pr
			    params:
			      PLATFORM_MANIFEST_PATH: {PLATFORM_MANIFEST_PATH}
			      TARGET: {TARGET}
			      TERRAFORM_BACKEND_GCS_BUCKET: {TERRAFORM_BACKEND_GCS_BUCKET}
			  - put: {TARGET}-platform-pr
			    params:
			      comment_file: terraform-out/plan.md
			      path: {TARGET}-platform-pr
			      status: success
			  on_failure:
			    do:
			    - put: {TARGET}-platform-pr
			      params:
			        path: {TARGET}-infra-pr
			        status: failure""";

	private static final String TEMPLATE = """
			resource_types:
			- name: pull-request
			  type: docker-image
			  source:
			    repository: teliaoss/github-pr-resource

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
			{RESOURCES}

			jobs:
			{JOBS}

			groups:
			{GROUPS}""";

	private static String group(TargetConfig targetConfig) {
		return """
				- name: {TARGET}
				  jobs:
				  - {TARGET}-terraform-plan
				  - {TARGET}-terraform-apply
				"""
				.replace("{TARGET}", targetConfig.getName());
	}

	public String pipeline(
			List<TargetConfig> targetConfigs) {
		if (targetConfigs == null || targetConfigs.size() == 0) {
			throw new IllegalArgumentException("exepcted at least one target config");
		}

		return TEMPLATE
				.replace("{CI_SRC_URI}", configuration.getCiSrcUri())
				.replace("{TERAFORM_SRC_URI}", configuration.getTerraformSrcUri())
				.replace("{TERAFORM_SRC_BRANCH}", configuration.getTerraformSrcBranch())
				.replace(
						"{RESOURCES}",
						targetConfigs.stream()
								.map(targetConfig -> resources(
										targetConfig,
										configuration.getGithubRepository(),
										configuration.getPlatformPathPrefix(),
										configuration.getPlatformSrcBranch(),
										configuration.getPlatformSrcUri()))
								.collect(Collectors.joining("\n")))
				.replace(
						"{JOBS}",
						targetConfigs.stream()
								.map(this::jobs)
								.collect(Collectors.joining("\n")))
				.replace(
						"{GROUPS}",
						targetConfigs.stream()
								.map(PlatformPipeline::group)
								.collect(Collectors.joining("\n")));
	}

	private static String resources(
			TargetConfig targetConfig,
			String githubRepository,
			String platformPathPrefix,
			String platformSrcBranch,
			String platformSrcUri) {
		return RESOURCES
				.replace("{GITHUB_REPOSITORY}", githubRepository)
				.replace("{PLATFORM_SRC_BRANCH}", platformSrcBranch)
				.replace("{PLATFORM_SRC_URI}", platformSrcUri)
				.replace("{TARGET}", targetConfig.getName())
				.replace("{PLATFORM_MANIFEST_PATH}", targetConfig.getPlatformManifestPath());
	}

	private String jobs(TargetConfig targetConfig) {
		return JOBS
				.replace("{TARGET}", targetConfig.getName())
				.replace("{PLATFORM_MANIFEST_PATH}", targetConfig.getPlatformManifestPath())
				.replace("{TERRAFORM_BACKEND_GCS_BUCKET}", configuration.getTerraformBackendGcsBucket());
	}
}
