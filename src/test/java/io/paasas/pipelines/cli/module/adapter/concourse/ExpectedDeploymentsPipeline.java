package io.paasas.pipelines.cli.module.adapter.concourse;

public abstract class ExpectedDeploymentsPipeline {
	public static final String PIPELINE = """
			---
			resource_types:
			- name: teams-notification
			  type: docker-image
			  source:
			    repository: navicore/teams-notification-resource
			    tag: latest
			resources:
			- name: ci-src
			  type: git
			  source:
			    uri: git@github.com:paasas/paasas-pipelines.git
			    private_key: ((git.ssh-private-key))
			    branch: main
			    paths:
			    - .concourse
			- name: teams
			  type: teams-notification
			  source:
			    url: ((teams.webhookUrl))
			- name: bigquery-dataset-1-src
			  type: git
			  source:
			    uri: git@github.com/teleport-java-client/paas-moe-le-cloud.hit
			    private_key: ((git.ssh-private-key))
			    branch: paas-moe-le-cloud
			    paths:
			    - dataset-1
			    tag_filter: v0.10.0
			- name: bigquery-dataset-2-src
			  type: git
			  source:
			    uri: git@github.com/teleport-java-client/paas-moe-le-cloud.hit
			    private_key: ((git.ssh-private-key))
			    branch: paas-moe-le-cloud
			    paths:
			    - dataset-2
			    tag_filter: v0.10.0
			jobs:
			- name: update-bigquery-dataset-1
			  plan:
			  - in_parallel:
			    - get: ci-src
			    - get: bigquery-dataset-1-src
			      trigger: true
			  - task: terraform-apply
			    file: ci-src/.concourse/tasks/terraform-deployment/terraform-deployment-apply.yaml
			    params:
			      GCP_PROJECT_ID: control-plane-377914
			      IMPERSONATE_SERVICE_ACCOUNT: service-account@yo.com
			      TARGET: project1-backend-dev
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states
			      TERRAFORM_DIRECTORY: dataset-1
			    input_mapping:
			      src: bigquery-dataset-1-src
			  on_success:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job ((concourse-url))/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME completed successfully
			  on_failure:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job ((concourse-url))/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME failed
			- name: update-bigquery-dataset-2
			  plan:
			  - in_parallel:
			    - get: ci-src
			    - get: bigquery-dataset-2-src
			      trigger: true
			  - task: terraform-apply
			    file: ci-src/.concourse/tasks/terraform-deployment/terraform-deployment-apply.yaml
			    params:
			      GCP_PROJECT_ID: control-plane-377914
			      IMPERSONATE_SERVICE_ACCOUNT: service-account@yo.com
			      TARGET: project1-backend-dev
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states
			      TERRAFORM_DIRECTORY: dataset-2
			    input_mapping:
			      src: bigquery-dataset-2-src
			  on_success:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job ((concourse-url))/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME completed successfully
			  on_failure:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job ((concourse-url))/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME failed
			""";
}
