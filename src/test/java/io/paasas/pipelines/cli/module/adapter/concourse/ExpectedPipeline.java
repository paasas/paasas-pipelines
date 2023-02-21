package io.paasas.pipelines.cli.module.adapter.concourse;

public abstract class ExpectedPipeline {
	public static final String PIPELINE = """
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
			    actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME



			resource_types:
			- name: pull-request
			  type: docker-image
			  source:
			    repository: teliaoss/github-pr-resource
			- name: teams-notification
			  type: docker-image
			  source:
			    repository: navicore/teams-notification-resource
			    tag: latest

			resources:
			- name: ci-src
			  type: git
			  source:
			    uri: https://github.com/paasas/paasas-pipelines
			    private_key: ((git.ssh-private-key))
			    branch: main
			    paths:
			    - .concourse
			- name: terraform-lts-src
			  type: git
			  source:
			    uri: https://github.com/daniellavoie/infra-as-code-demo
			    private_key: ((git.ssh-private-key))
			    branch: main
			    paths:
			    - terraform/infra/lts
			- name: terraform-next-src
			  type: git
			  source:
			    uri: https://github.com/daniellavoie/infra-as-code-demo
			    private_key: ((git.ssh-private-key))
			    branch: main
			    paths:
			    - terraform/infra/next
			- name: project1-backend-dev-platform-pr
			  type: pull-request
			  source:
			    access_token: ((github.accessToken))
			    repository: daniellavoie/infra-as-code-demo
			    paths:
			    - teams/project1/backend/dev.yaml
			    - teams/project1/backend/dev-tf

			- name: project1-backend-dev-platform-src
			  type: git
			  source:
			    uri: https://github.com/daniellavoie/infra-as-code-demo
			    private_key: ((git.ssh-private-key))
			    branch: v2
			    paths:
			    - teams/project1/backend/dev.yaml
			    - teams/project1/backend/dev-tf
			- name: project1-backend-prod-platform-pr
			  type: pull-request
			  source:
			    access_token: ((github.accessToken))
			    repository: daniellavoie/infra-as-code-demo
			    paths:
			    - teams/project1/backend/prod.yaml
			    - teams/project1/backend/prod-tf

			- name: project1-backend-prod-platform-src
			  type: git
			  source:
			    uri: https://github.com/daniellavoie/infra-as-code-demo
			    private_key: ((git.ssh-private-key))
			    branch: v2
			    paths:
			    - teams/project1/backend/prod.yaml
			    - teams/project1/backend/prod-tf
			- name: project1-frontend-dev-platform-pr
			  type: pull-request
			  source:
			    access_token: ((github.accessToken))
			    repository: daniellavoie/infra-as-code-demo
			    paths:
			    - teams/project1/frontend/dev.yaml
			    - teams/project1/frontend/dev-tf

			- name: project1-frontend-dev-platform-src
			  type: git
			  source:
			    uri: https://github.com/daniellavoie/infra-as-code-demo
			    private_key: ((git.ssh-private-key))
			    branch: v2
			    paths:
			    - teams/project1/frontend/dev.yaml
			    - teams/project1/frontend/dev-tf
			- name: project1-frontend-prod-platform-pr
			  type: pull-request
			  source:
			    access_token: ((github.accessToken))
			    repository: daniellavoie/infra-as-code-demo
			    paths:
			    - teams/project1/frontend/prod.yaml
			    - teams/project1/frontend/prod-tf

			- name: project1-frontend-prod-platform-src
			  type: git
			  source:
			    uri: https://github.com/daniellavoie/infra-as-code-demo
			    private_key: ((git.ssh-private-key))
			    branch: v2
			    paths:
			    - teams/project1/frontend/prod.yaml
			    - teams/project1/frontend/prod-tf
			- name: teams
			  type: teams-notification
			  source:
			    url: ((teams.webhookUrl))


			jobs:
			- name: project1-backend-dev-terraform-apply
			  plan:
			  - in_parallel:
			    - get: project1-backend-dev-platform-src
			      trigger: true
			    - get: ci-src
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-apply
			    file: ci-src/.concourse/tasks/terraform/terraform-apply.yaml
			    input_mapping:
			      src: project1-backend-dev-platform-src
			    params:
			      PLATFORM_MANIFEST_PATH: teams/project1/backend/dev.yaml
			      TARGET: project1-backend-dev
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states
			      TERRAFORM_EXTENSIONS_DIRECTORY: teams/project1/backend/dev-tf
			  on_success: *teams_job_success
			  on_failure: *teams_job_failed

			- name: project1-backend-dev-terraform-destroy
			  plan:
			  - in_parallel:
			    - get: project1-backend-dev-platform-src
			    - get: ci-src
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-destroy
			    file: ci-src/.concourse/tasks/terraform/terraform-destroy.yaml
			    input_mapping:
			      src: project1-backend-dev-platform-src
			    params:
			      PLATFORM_MANIFEST_PATH: teams/project1/backend/dev.yaml
			      TARGET: project1-backend-dev
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states
			      TERRAFORM_EXTENSIONS_DIRECTORY: teams/project1/backend/dev-tf
			  on_success: *teams_job_success
			  on_failure: *teams_job_failed

			- name: project1-backend-dev-terraform-plan
			  plan:
			  - in_parallel:
			    - get: project1-backend-dev-platform-pr
			      trigger: true
			    - get: ci-src
			  - in_parallel:
			    - put: project1-backend-dev-platform-pr
			      params:
			        path: project1-backend-dev-platform-pr
			        status: pending
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-plan
			    file: ci-src/.concourse/tasks/terraform/terraform-plan.yaml
			    input_mapping:
			      src: project1-backend-dev-platform-pr
			    params:
			      PLATFORM_MANIFEST_PATH: teams/project1/backend/dev.yaml
			      TARGET: project1-backend-dev
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states
			      TERRAFORM_EXTENSIONS_DIRECTORY: teams/project1/backend/dev-tf
			  - put: project1-backend-dev-platform-pr
			    params:
			      comment_file: terraform-out/terraform.md
			      path: project1-backend-dev-platform-pr
			      status: success
			  on_success: *teams_job_success
			  on_failure:
			    do:
			    - put: project1-backend-dev-platform-pr
			      params:
			        path: project1-backend-dev-infra-pr
			        status: failure
			    - <<: *teams_job_failed
			- name: project1-backend-prod-terraform-apply
			  plan:
			  - in_parallel:
			    - get: project1-backend-prod-platform-src
			      trigger: true
			    - get: ci-src
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-apply
			    file: ci-src/.concourse/tasks/terraform/terraform-apply.yaml
			    input_mapping:
			      src: project1-backend-prod-platform-src
			    params:
			      PLATFORM_MANIFEST_PATH: teams/project1/backend/prod.yaml
			      TARGET: project1-backend-prod
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states
			      TERRAFORM_EXTENSIONS_DIRECTORY: teams/project1/backend/prod-tf
			  on_success: *teams_job_success
			  on_failure: *teams_job_failed

			- name: project1-backend-prod-terraform-destroy
			  plan:
			  - in_parallel:
			    - get: project1-backend-prod-platform-src
			    - get: ci-src
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-destroy
			    file: ci-src/.concourse/tasks/terraform/terraform-destroy.yaml
			    input_mapping:
			      src: project1-backend-prod-platform-src
			    params:
			      PLATFORM_MANIFEST_PATH: teams/project1/backend/prod.yaml
			      TARGET: project1-backend-prod
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states
			      TERRAFORM_EXTENSIONS_DIRECTORY: teams/project1/backend/prod-tf
			  on_success: *teams_job_success
			  on_failure: *teams_job_failed

			- name: project1-backend-prod-terraform-plan
			  plan:
			  - in_parallel:
			    - get: project1-backend-prod-platform-pr
			      trigger: true
			    - get: ci-src
			  - in_parallel:
			    - put: project1-backend-prod-platform-pr
			      params:
			        path: project1-backend-prod-platform-pr
			        status: pending
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-plan
			    file: ci-src/.concourse/tasks/terraform/terraform-plan.yaml
			    input_mapping:
			      src: project1-backend-prod-platform-pr
			    params:
			      PLATFORM_MANIFEST_PATH: teams/project1/backend/prod.yaml
			      TARGET: project1-backend-prod
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states
			      TERRAFORM_EXTENSIONS_DIRECTORY: teams/project1/backend/prod-tf
			  - put: project1-backend-prod-platform-pr
			    params:
			      comment_file: terraform-out/terraform.md
			      path: project1-backend-prod-platform-pr
			      status: success
			  on_success: *teams_job_success
			  on_failure:
			    do:
			    - put: project1-backend-prod-platform-pr
			      params:
			        path: project1-backend-prod-infra-pr
			        status: failure
			    - <<: *teams_job_failed
			- name: project1-frontend-dev-terraform-apply
			  plan:
			  - in_parallel:
			    - get: project1-frontend-dev-platform-src
			      trigger: true
			    - get: ci-src
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-apply
			    file: ci-src/.concourse/tasks/terraform/terraform-apply.yaml
			    input_mapping:
			      src: project1-frontend-dev-platform-src
			    params:
			      PLATFORM_MANIFEST_PATH: teams/project1/frontend/dev.yaml
			      TARGET: project1-frontend-dev
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states
			      TERRAFORM_EXTENSIONS_DIRECTORY: teams/project1/frontend/dev-tf
			  on_success: *teams_job_success
			  on_failure: *teams_job_failed

			- name: project1-frontend-dev-terraform-destroy
			  plan:
			  - in_parallel:
			    - get: project1-frontend-dev-platform-src
			    - get: ci-src
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-destroy
			    file: ci-src/.concourse/tasks/terraform/terraform-destroy.yaml
			    input_mapping:
			      src: project1-frontend-dev-platform-src
			    params:
			      PLATFORM_MANIFEST_PATH: teams/project1/frontend/dev.yaml
			      TARGET: project1-frontend-dev
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states
			      TERRAFORM_EXTENSIONS_DIRECTORY: teams/project1/frontend/dev-tf
			  on_success: *teams_job_success
			  on_failure: *teams_job_failed

			- name: project1-frontend-dev-terraform-plan
			  plan:
			  - in_parallel:
			    - get: project1-frontend-dev-platform-pr
			      trigger: true
			    - get: ci-src
			  - in_parallel:
			    - put: project1-frontend-dev-platform-pr
			      params:
			        path: project1-frontend-dev-platform-pr
			        status: pending
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-plan
			    file: ci-src/.concourse/tasks/terraform/terraform-plan.yaml
			    input_mapping:
			      src: project1-frontend-dev-platform-pr
			    params:
			      PLATFORM_MANIFEST_PATH: teams/project1/frontend/dev.yaml
			      TARGET: project1-frontend-dev
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states
			      TERRAFORM_EXTENSIONS_DIRECTORY: teams/project1/frontend/dev-tf
			  - put: project1-frontend-dev-platform-pr
			    params:
			      comment_file: terraform-out/terraform.md
			      path: project1-frontend-dev-platform-pr
			      status: success
			  on_success: *teams_job_success
			  on_failure:
			    do:
			    - put: project1-frontend-dev-platform-pr
			      params:
			        path: project1-frontend-dev-infra-pr
			        status: failure
			    - <<: *teams_job_failed
			- name: project1-frontend-prod-terraform-apply
			  plan:
			  - in_parallel:
			    - get: project1-frontend-prod-platform-src
			      trigger: true
			    - get: ci-src
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-apply
			    file: ci-src/.concourse/tasks/terraform/terraform-apply.yaml
			    input_mapping:
			      src: project1-frontend-prod-platform-src
			    params:
			      PLATFORM_MANIFEST_PATH: teams/project1/frontend/prod.yaml
			      TARGET: project1-frontend-prod
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states
			      TERRAFORM_EXTENSIONS_DIRECTORY: teams/project1/frontend/prod-tf
			  on_success: *teams_job_success
			  on_failure: *teams_job_failed

			- name: project1-frontend-prod-terraform-destroy
			  plan:
			  - in_parallel:
			    - get: project1-frontend-prod-platform-src
			    - get: ci-src
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-destroy
			    file: ci-src/.concourse/tasks/terraform/terraform-destroy.yaml
			    input_mapping:
			      src: project1-frontend-prod-platform-src
			    params:
			      PLATFORM_MANIFEST_PATH: teams/project1/frontend/prod.yaml
			      TARGET: project1-frontend-prod
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states
			      TERRAFORM_EXTENSIONS_DIRECTORY: teams/project1/frontend/prod-tf
			  on_success: *teams_job_success
			  on_failure: *teams_job_failed

			- name: project1-frontend-prod-terraform-plan
			  plan:
			  - in_parallel:
			    - get: project1-frontend-prod-platform-pr
			      trigger: true
			    - get: ci-src
			  - in_parallel:
			    - put: project1-frontend-prod-platform-pr
			      params:
			        path: project1-frontend-prod-platform-pr
			        status: pending
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-plan
			    file: ci-src/.concourse/tasks/terraform/terraform-plan.yaml
			    input_mapping:
			      src: project1-frontend-prod-platform-pr
			    params:
			      PLATFORM_MANIFEST_PATH: teams/project1/frontend/prod.yaml
			      TARGET: project1-frontend-prod
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states
			      TERRAFORM_EXTENSIONS_DIRECTORY: teams/project1/frontend/prod-tf
			  - put: project1-frontend-prod-platform-pr
			    params:
			      comment_file: terraform-out/terraform.md
			      path: project1-frontend-prod-platform-pr
			      status: success
			  on_success: *teams_job_success
			  on_failure:
			    do:
			    - put: project1-frontend-prod-platform-pr
			      params:
			        path: project1-frontend-prod-infra-pr
			        status: failure
			    - <<: *teams_job_failed

			groups:
			- name: project1-backend-dev
			  jobs:
			  - project1-backend-dev-terraform-apply
			  - project1-backend-dev-terraform-destroy
			  - project1-backend-dev-terraform-plan
			- name: project1-backend-prod
			  jobs:
			  - project1-backend-prod-terraform-apply
			  - project1-backend-prod-terraform-destroy
			  - project1-backend-prod-terraform-plan
			- name: project1-frontend-dev
			  jobs:
			  - project1-frontend-dev-terraform-apply
			  - project1-frontend-dev-terraform-destroy
			  - project1-frontend-dev-terraform-plan
			- name: project1-frontend-prod
			  jobs:
			  - project1-frontend-prod-terraform-apply
			  - project1-frontend-prod-terraform-destroy
			  - project1-frontend-prod-terraform-plan""";
}
