package io.paasas.pipelines.cli.module.adapter.concourse;

public abstract class ExpectedPlatformsPipeline {
	public static final String PIPELINE = """
			---
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
			    uri: git@github.com:paasas/paasas-pipelines-scripts.git
			    private_key: ((git.ssh-private-key))
			    branch: main
			    paths:
			    - .concourse
			- name: terraform-lts-src
			  type: git
			  source:
			    uri: git@github.com:daniellavoie/infra-as-code-demo
			    private_key: ((git.ssh-private-key))
			    branch: main
			    paths:
			    - terraform/infra/lts
			- name: terraform-next-src
			  type: git
			  source:
			    uri: git@github.com:daniellavoie/infra-as-code-demo
			    private_key: ((git.ssh-private-key))
			    branch: main
			    paths:
			    - terraform/infra/next
			- name: teams
			  type: teams-notification
			  source:
			    url: ((teams.webhookUrl))
			- name: project1-backend-dev-platform-pr
			  type: pull-request
			  source:
			    access_token: ((github.userAccessToken))
			    repository: daniellavoie/deployment-as-code-demo
			    paths:
			    - teams/project1/backend/dev.yaml
			    - teams/project1/backend/dev-tf
			- name: project1-backend-dev-platform-src
			  type: git
			  source:
			    uri: git@github.com:daniellavoie/infra-as-code-demo.git
			    private_key: ((git.ssh-private-key))
			    branch: v2
			    paths:
			    - teams/project1/backend/dev.yaml
			    - teams/project1/backend/dev-tf
			- name: project1-backend-dev-deployment-src
			  type: git
			  source:
			    uri: git@github.com:daniellavoie/deployment-as-code-demo.git
			    private_key: ((git.ssh-private-key))
			    branch: main
			    paths:
			    - teams/project1/backend/dev.yaml
			- name: project1-backend-prod-platform-pr
			  type: pull-request
			  source:
			    access_token: ((github.userAccessToken))
			    repository: daniellavoie/deployment-as-code-demo
			    paths:
			    - teams/project1/backend/prod.yaml
			    - teams/project1/backend/prod-tf
			- name: project1-backend-prod-platform-src
			  type: git
			  source:
			    uri: git@github.com:daniellavoie/infra-as-code-demo.git
			    private_key: ((git.ssh-private-key))
			    branch: v2
			    paths:
			    - teams/project1/backend/prod.yaml
			    - teams/project1/backend/prod-tf
			- name: project1-backend-prod-deployment-src
			  type: git
			  source:
			    uri: git@github.com:daniellavoie/deployment-as-code-demo.git
			    private_key: ((git.ssh-private-key))
			    branch: main
			    paths:
			    - teams/project1/backend/prod.yaml
			- name: project1-frontend-dev-platform-pr
			  type: pull-request
			  source:
			    access_token: ((github.userAccessToken))
			    repository: daniellavoie/deployment-as-code-demo
			    paths:
			    - teams/project1/frontend/dev.yaml
			    - teams/project1/frontend/dev-tf
			- name: project1-frontend-dev-platform-src
			  type: git
			  source:
			    uri: git@github.com:daniellavoie/infra-as-code-demo.git
			    private_key: ((git.ssh-private-key))
			    branch: v2
			    paths:
			    - teams/project1/frontend/dev.yaml
			    - teams/project1/frontend/dev-tf
			- name: project1-frontend-dev-deployment-src
			  type: git
			  source:
			    uri: git@github.com:daniellavoie/deployment-as-code-demo.git
			    private_key: ((git.ssh-private-key))
			    branch: main
			    paths:
			    - teams/project1/frontend/dev.yaml
			- name: project1-frontend-prod-platform-pr
			  type: pull-request
			  source:
			    access_token: ((github.userAccessToken))
			    repository: daniellavoie/deployment-as-code-demo
			    paths:
			    - teams/project1/frontend/prod.yaml
			    - teams/project1/frontend/prod-tf
			- name: project1-frontend-prod-platform-src
			  type: git
			  source:
			    uri: git@github.com:daniellavoie/infra-as-code-demo.git
			    private_key: ((git.ssh-private-key))
			    branch: v2
			    paths:
			    - teams/project1/frontend/prod.yaml
			    - teams/project1/frontend/prod-tf
			- name: project1-frontend-prod-deployment-src
			  type: git
			  source:
			    uri: git@github.com:daniellavoie/deployment-as-code-demo.git
			    private_key: ((git.ssh-private-key))
			    branch: main
			    paths:
			    - teams/project1/frontend/prod.yaml
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
			    file: ci-src/.concourse/tasks/terraform-platform/terraform-platform-apply.yaml
			    params:
			      GOOGLE_IMPERSONATE_SERVICE_ACCOUNT: service-account@yo.com
			      PLATFORM_MANIFEST_PATH: teams/project1/backend/dev.yaml
			      TARGET: project1-backend-dev
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states
			      TERRAFORM_EXTENSIONS_DIRECTORY: teams/project1/backend/dev-tf
			    input_mapping:
			      src: project1-backend-dev-platform-src
			  on_success:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME completed successfully
			  on_failure:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME failed
			- name: project1-backend-dev-terraform-destroy
			  plan:
			  - in_parallel:
			    - get: project1-backend-dev-platform-src
			    - get: ci-src
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-destroy
			    file: ci-src/.concourse/tasks/terraform-platform/terraform-platform-destroy.yaml
			    params:
			      GOOGLE_IMPERSONATE_SERVICE_ACCOUNT: service-account@yo.com
			      PLATFORM_MANIFEST_PATH: teams/project1/backend/dev.yaml
			      TARGET: project1-backend-dev
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states
			      TERRAFORM_EXTENSIONS_DIRECTORY: teams/project1/backend/dev-tf
			    input_mapping:
			      src: project1-backend-dev-platform-src
			  on_success:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME completed successfully
			  on_failure:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME failed
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
			    file: ci-src/.concourse/tasks/terraform-platform/terraform-platform-plan.yaml
			    params:
			      GOOGLE_IMPERSONATE_SERVICE_ACCOUNT: service-account@yo.com
			      PLATFORM_MANIFEST_PATH: teams/project1/backend/dev.yaml
			      TARGET: project1-backend-dev
			      TERRAFORM_BACKEND_DIRECTORY: terraform/platforms/
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states
			      TERRAFORM_BACKEND_PREFIX: terraform/platforms/
			      TERRAFORM_EXTENSIONS_DIRECTORY: teams/project1/backend/dev-tf
			    input_mapping:
			      src: project1-backend-dev-platform-pr
			  - put: project1-backend-dev-platform-pr
			    params:
			      comment_file: terraform-out/terraform.md
			      path: project1-backend-dev-platform-pr
			      status: success
			  on_success:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME completed successfully
			  on_failure:
			    do:
			    - put: project1-backend-dev-platform-pr
			      params:
			        path: project1-backend-dev-platform-pr
			        status: failure
			    - put: teams
			      params:
			        actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			        text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME failed
			- name: project1-backend-dev-update-deployment-pipeline
			  plan:
			  - in_parallel:
			    - get: project1-backend-dev-deployment-src
			      trigger: true
			    - get: ci-src
			  - task: update-deployment-pipeline
			    file: ci-src/.concourse/tasks/deployment/update-deployment-pipeline.yaml
			    params:
			      MANIFEST_PATH: teams/project1/backend/dev.yaml
			      PIPELINES_CONCOURSE_CISRCURI: git@github.com:paasas/paasas-pipelines-scripts.git
			      PIPELINES_CONCOURSE_DEPLOYMENTPATHPREFIX: teams/
			      PIPELINES_CONCOURSE_DEPLOYMENTSRCBRANCH: main
			      PIPELINES_CONCOURSE_DEPLOYMENTSRCURI: git@github.com:daniellavoie/deployment-as-code-demo.git
			      PIPELINES_CONCOURSE_DEPLOYMENTTERRAFORMBACKENDPREFIX: terraform/deployments/
			      PIPELINES_CONCOURSE_GCRCREDENTIALSJSONSECRETNAME: googleCredentials
			      PIPELINES_CONCOURSE_GITHUBEMAIL: dlavoie@live.ca
			      PIPELINES_CONCOURSE_GITHUBREPOSITORY: daniellavoie/deployment-as-code-demo
			      PIPELINES_CONCOURSE_GITHUBUSERNAME: daniellavoie
			      PIPELINES_CONCOURSE_PIPELINESSERVER: http://localhost:8080
			      PIPELINES_CONCOURSE_PIPELINESSERVERPASSWORD: ((pipelines-server.security-ci-password))
			      PIPELINES_CONCOURSE_PIPELINESSERVERUSERNAME: ci
			      PIPELINES_CONCOURSE_PLATFORMPATHPREFIX: teams/
			      PIPELINES_CONCOURSE_PLATFORMSRCBRANCH: v2
			      PIPELINES_CONCOURSE_PLATFORMSRCURI: git@github.com:daniellavoie/infra-as-code-demo.git
			      PIPELINES_CONCOURSE_PLATFORMTERRAFORMBACKENDPREFIX: terraform/platforms/
			      PIPELINES_CONCOURSE_TERRAFORMBACKENDGCSBUCKET: terraform-states
			      PIPELINES_CONCOURSE_TERRAFORMSRCBRANCH: main
			      PIPELINES_CONCOURSE_TERRAFORMSRCURI: git@github.com:daniellavoie/infra-as-code-demo
			      PIPELINES_GCP_IMPERSONATESERVICEACCOUNT: service-account@yo.com
			      TARGET: project1-backend-dev
			    input_mapping:
			      src: project1-backend-dev-deployment-src
			  - set_pipeline: project1-backend-dev-deployment
			    file: pipelines/pipelines.yaml
			  on_success:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME completed successfully
			  on_failure:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME failed
			- name: project1-backend-prod-terraform-apply
			  plan:
			  - in_parallel:
			    - get: project1-backend-prod-platform-src
			      trigger: true
			    - get: ci-src
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-apply
			    file: ci-src/.concourse/tasks/terraform-platform/terraform-platform-apply.yaml
			    params:
			      GOOGLE_IMPERSONATE_SERVICE_ACCOUNT: service-account@yo.com
			      PLATFORM_MANIFEST_PATH: teams/project1/backend/prod.yaml
			      TARGET: project1-backend-prod
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states
			      TERRAFORM_EXTENSIONS_DIRECTORY: teams/project1/backend/prod-tf
			    input_mapping:
			      src: project1-backend-prod-platform-src
			  on_success:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME completed successfully
			  on_failure:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME failed
			- name: project1-backend-prod-terraform-destroy
			  plan:
			  - in_parallel:
			    - get: project1-backend-prod-platform-src
			    - get: ci-src
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-destroy
			    file: ci-src/.concourse/tasks/terraform-platform/terraform-platform-destroy.yaml
			    params:
			      GOOGLE_IMPERSONATE_SERVICE_ACCOUNT: service-account@yo.com
			      PLATFORM_MANIFEST_PATH: teams/project1/backend/prod.yaml
			      TARGET: project1-backend-prod
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states
			      TERRAFORM_EXTENSIONS_DIRECTORY: teams/project1/backend/prod-tf
			    input_mapping:
			      src: project1-backend-prod-platform-src
			  on_success:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME completed successfully
			  on_failure:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME failed
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
			    file: ci-src/.concourse/tasks/terraform-platform/terraform-platform-plan.yaml
			    params:
			      GOOGLE_IMPERSONATE_SERVICE_ACCOUNT: service-account@yo.com
			      PLATFORM_MANIFEST_PATH: teams/project1/backend/prod.yaml
			      TARGET: project1-backend-prod
			      TERRAFORM_BACKEND_DIRECTORY: terraform/platforms/
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states
			      TERRAFORM_BACKEND_PREFIX: terraform/platforms/
			      TERRAFORM_EXTENSIONS_DIRECTORY: teams/project1/backend/prod-tf
			    input_mapping:
			      src: project1-backend-prod-platform-pr
			  - put: project1-backend-prod-platform-pr
			    params:
			      comment_file: terraform-out/terraform.md
			      path: project1-backend-prod-platform-pr
			      status: success
			  on_success:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME completed successfully
			  on_failure:
			    do:
			    - put: project1-backend-prod-platform-pr
			      params:
			        path: project1-backend-prod-platform-pr
			        status: failure
			    - put: teams
			      params:
			        actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			        text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME failed
			- name: project1-backend-prod-update-deployment-pipeline
			  plan:
			  - in_parallel:
			    - get: project1-backend-prod-deployment-src
			      trigger: true
			    - get: ci-src
			  - task: update-deployment-pipeline
			    file: ci-src/.concourse/tasks/deployment/update-deployment-pipeline.yaml
			    params:
			      MANIFEST_PATH: teams/project1/backend/prod.yaml
			      PIPELINES_CONCOURSE_CISRCURI: git@github.com:paasas/paasas-pipelines-scripts.git
			      PIPELINES_CONCOURSE_DEPLOYMENTPATHPREFIX: teams/
			      PIPELINES_CONCOURSE_DEPLOYMENTSRCBRANCH: main
			      PIPELINES_CONCOURSE_DEPLOYMENTSRCURI: git@github.com:daniellavoie/deployment-as-code-demo.git
			      PIPELINES_CONCOURSE_DEPLOYMENTTERRAFORMBACKENDPREFIX: terraform/deployments/
			      PIPELINES_CONCOURSE_GCRCREDENTIALSJSONSECRETNAME: googleCredentials
			      PIPELINES_CONCOURSE_GITHUBEMAIL: dlavoie@live.ca
			      PIPELINES_CONCOURSE_GITHUBREPOSITORY: daniellavoie/deployment-as-code-demo
			      PIPELINES_CONCOURSE_GITHUBUSERNAME: daniellavoie
			      PIPELINES_CONCOURSE_PIPELINESSERVER: http://localhost:8080
			      PIPELINES_CONCOURSE_PIPELINESSERVERPASSWORD: ((pipelines-server.security-ci-password))
			      PIPELINES_CONCOURSE_PIPELINESSERVERUSERNAME: ci
			      PIPELINES_CONCOURSE_PLATFORMPATHPREFIX: teams/
			      PIPELINES_CONCOURSE_PLATFORMSRCBRANCH: v2
			      PIPELINES_CONCOURSE_PLATFORMSRCURI: git@github.com:daniellavoie/infra-as-code-demo.git
			      PIPELINES_CONCOURSE_PLATFORMTERRAFORMBACKENDPREFIX: terraform/platforms/
			      PIPELINES_CONCOURSE_TERRAFORMBACKENDGCSBUCKET: terraform-states
			      PIPELINES_CONCOURSE_TERRAFORMSRCBRANCH: main
			      PIPELINES_CONCOURSE_TERRAFORMSRCURI: git@github.com:daniellavoie/infra-as-code-demo
			      PIPELINES_GCP_IMPERSONATESERVICEACCOUNT: service-account@yo.com
			      TARGET: project1-backend-prod
			    input_mapping:
			      src: project1-backend-prod-deployment-src
			  - set_pipeline: project1-backend-prod-deployment
			    file: pipelines/pipelines.yaml
			  on_success:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME completed successfully
			  on_failure:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME failed
			- name: project1-frontend-dev-terraform-apply
			  plan:
			  - in_parallel:
			    - get: project1-frontend-dev-platform-src
			      trigger: true
			    - get: ci-src
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-apply
			    file: ci-src/.concourse/tasks/terraform-platform/terraform-platform-apply.yaml
			    params:
			      GOOGLE_IMPERSONATE_SERVICE_ACCOUNT: service-account@yo.com
			      PLATFORM_MANIFEST_PATH: teams/project1/frontend/dev.yaml
			      TARGET: project1-frontend-dev
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states
			      TERRAFORM_EXTENSIONS_DIRECTORY: teams/project1/frontend/dev-tf
			    input_mapping:
			      src: project1-frontend-dev-platform-src
			  on_success:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME completed successfully
			  on_failure:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME failed
			- name: project1-frontend-dev-terraform-destroy
			  plan:
			  - in_parallel:
			    - get: project1-frontend-dev-platform-src
			    - get: ci-src
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-destroy
			    file: ci-src/.concourse/tasks/terraform-platform/terraform-platform-destroy.yaml
			    params:
			      GOOGLE_IMPERSONATE_SERVICE_ACCOUNT: service-account@yo.com
			      PLATFORM_MANIFEST_PATH: teams/project1/frontend/dev.yaml
			      TARGET: project1-frontend-dev
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states
			      TERRAFORM_EXTENSIONS_DIRECTORY: teams/project1/frontend/dev-tf
			    input_mapping:
			      src: project1-frontend-dev-platform-src
			  on_success:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME completed successfully
			  on_failure:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME failed
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
			    file: ci-src/.concourse/tasks/terraform-platform/terraform-platform-plan.yaml
			    params:
			      GOOGLE_IMPERSONATE_SERVICE_ACCOUNT: service-account@yo.com
			      PLATFORM_MANIFEST_PATH: teams/project1/frontend/dev.yaml
			      TARGET: project1-frontend-dev
			      TERRAFORM_BACKEND_DIRECTORY: terraform/platforms/
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states
			      TERRAFORM_BACKEND_PREFIX: terraform/platforms/
			      TERRAFORM_EXTENSIONS_DIRECTORY: teams/project1/frontend/dev-tf
			    input_mapping:
			      src: project1-frontend-dev-platform-pr
			  - put: project1-frontend-dev-platform-pr
			    params:
			      comment_file: terraform-out/terraform.md
			      path: project1-frontend-dev-platform-pr
			      status: success
			  on_success:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME completed successfully
			  on_failure:
			    do:
			    - put: project1-frontend-dev-platform-pr
			      params:
			        path: project1-frontend-dev-platform-pr
			        status: failure
			    - put: teams
			      params:
			        actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			        text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME failed
			- name: project1-frontend-dev-update-deployment-pipeline
			  plan:
			  - in_parallel:
			    - get: project1-frontend-dev-deployment-src
			      trigger: true
			    - get: ci-src
			  - task: update-deployment-pipeline
			    file: ci-src/.concourse/tasks/deployment/update-deployment-pipeline.yaml
			    params:
			      MANIFEST_PATH: teams/project1/frontend/dev.yaml
			      PIPELINES_CONCOURSE_CISRCURI: git@github.com:paasas/paasas-pipelines-scripts.git
			      PIPELINES_CONCOURSE_DEPLOYMENTPATHPREFIX: teams/
			      PIPELINES_CONCOURSE_DEPLOYMENTSRCBRANCH: main
			      PIPELINES_CONCOURSE_DEPLOYMENTSRCURI: git@github.com:daniellavoie/deployment-as-code-demo.git
			      PIPELINES_CONCOURSE_DEPLOYMENTTERRAFORMBACKENDPREFIX: terraform/deployments/
			      PIPELINES_CONCOURSE_GCRCREDENTIALSJSONSECRETNAME: googleCredentials
			      PIPELINES_CONCOURSE_GITHUBEMAIL: dlavoie@live.ca
			      PIPELINES_CONCOURSE_GITHUBREPOSITORY: daniellavoie/deployment-as-code-demo
			      PIPELINES_CONCOURSE_GITHUBUSERNAME: daniellavoie
			      PIPELINES_CONCOURSE_PIPELINESSERVER: http://localhost:8080
			      PIPELINES_CONCOURSE_PIPELINESSERVERPASSWORD: ((pipelines-server.security-ci-password))
			      PIPELINES_CONCOURSE_PIPELINESSERVERUSERNAME: ci
			      PIPELINES_CONCOURSE_PLATFORMPATHPREFIX: teams/
			      PIPELINES_CONCOURSE_PLATFORMSRCBRANCH: v2
			      PIPELINES_CONCOURSE_PLATFORMSRCURI: git@github.com:daniellavoie/infra-as-code-demo.git
			      PIPELINES_CONCOURSE_PLATFORMTERRAFORMBACKENDPREFIX: terraform/platforms/
			      PIPELINES_CONCOURSE_TERRAFORMBACKENDGCSBUCKET: terraform-states
			      PIPELINES_CONCOURSE_TERRAFORMSRCBRANCH: main
			      PIPELINES_CONCOURSE_TERRAFORMSRCURI: git@github.com:daniellavoie/infra-as-code-demo
			      PIPELINES_GCP_IMPERSONATESERVICEACCOUNT: service-account@yo.com
			      TARGET: project1-frontend-dev
			    input_mapping:
			      src: project1-frontend-dev-deployment-src
			  - set_pipeline: project1-frontend-dev-deployment
			    file: pipelines/pipelines.yaml
			  on_success:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME completed successfully
			  on_failure:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME failed
			- name: project1-frontend-prod-terraform-apply
			  plan:
			  - in_parallel:
			    - get: project1-frontend-prod-platform-src
			      trigger: true
			    - get: ci-src
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-apply
			    file: ci-src/.concourse/tasks/terraform-platform/terraform-platform-apply.yaml
			    params:
			      GOOGLE_IMPERSONATE_SERVICE_ACCOUNT: service-account@yo.com
			      PLATFORM_MANIFEST_PATH: teams/project1/frontend/prod.yaml
			      TARGET: project1-frontend-prod
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states
			      TERRAFORM_EXTENSIONS_DIRECTORY: teams/project1/frontend/prod-tf
			    input_mapping:
			      src: project1-frontend-prod-platform-src
			  on_success:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME completed successfully
			  on_failure:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME failed
			- name: project1-frontend-prod-terraform-destroy
			  plan:
			  - in_parallel:
			    - get: project1-frontend-prod-platform-src
			    - get: ci-src
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-destroy
			    file: ci-src/.concourse/tasks/terraform-platform/terraform-platform-destroy.yaml
			    params:
			      GOOGLE_IMPERSONATE_SERVICE_ACCOUNT: service-account@yo.com
			      PLATFORM_MANIFEST_PATH: teams/project1/frontend/prod.yaml
			      TARGET: project1-frontend-prod
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states
			      TERRAFORM_EXTENSIONS_DIRECTORY: teams/project1/frontend/prod-tf
			    input_mapping:
			      src: project1-frontend-prod-platform-src
			  on_success:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME completed successfully
			  on_failure:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME failed
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
			    file: ci-src/.concourse/tasks/terraform-platform/terraform-platform-plan.yaml
			    params:
			      GOOGLE_IMPERSONATE_SERVICE_ACCOUNT: service-account@yo.com
			      PLATFORM_MANIFEST_PATH: teams/project1/frontend/prod.yaml
			      TARGET: project1-frontend-prod
			      TERRAFORM_BACKEND_DIRECTORY: terraform/platforms/
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states
			      TERRAFORM_BACKEND_PREFIX: terraform/platforms/
			      TERRAFORM_EXTENSIONS_DIRECTORY: teams/project1/frontend/prod-tf
			    input_mapping:
			      src: project1-frontend-prod-platform-pr
			  - put: project1-frontend-prod-platform-pr
			    params:
			      comment_file: terraform-out/terraform.md
			      path: project1-frontend-prod-platform-pr
			      status: success
			  on_success:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME completed successfully
			  on_failure:
			    do:
			    - put: project1-frontend-prod-platform-pr
			      params:
			        path: project1-frontend-prod-platform-pr
			        status: failure
			    - put: teams
			      params:
			        actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			        text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME failed
			- name: project1-frontend-prod-update-deployment-pipeline
			  plan:
			  - in_parallel:
			    - get: project1-frontend-prod-deployment-src
			      trigger: true
			    - get: ci-src
			  - task: update-deployment-pipeline
			    file: ci-src/.concourse/tasks/deployment/update-deployment-pipeline.yaml
			    params:
			      MANIFEST_PATH: teams/project1/frontend/prod.yaml
			      PIPELINES_CONCOURSE_CISRCURI: git@github.com:paasas/paasas-pipelines-scripts.git
			      PIPELINES_CONCOURSE_DEPLOYMENTPATHPREFIX: teams/
			      PIPELINES_CONCOURSE_DEPLOYMENTSRCBRANCH: main
			      PIPELINES_CONCOURSE_DEPLOYMENTSRCURI: git@github.com:daniellavoie/deployment-as-code-demo.git
			      PIPELINES_CONCOURSE_DEPLOYMENTTERRAFORMBACKENDPREFIX: terraform/deployments/
			      PIPELINES_CONCOURSE_GCRCREDENTIALSJSONSECRETNAME: googleCredentials
			      PIPELINES_CONCOURSE_GITHUBEMAIL: dlavoie@live.ca
			      PIPELINES_CONCOURSE_GITHUBREPOSITORY: daniellavoie/deployment-as-code-demo
			      PIPELINES_CONCOURSE_GITHUBUSERNAME: daniellavoie
			      PIPELINES_CONCOURSE_PIPELINESSERVER: http://localhost:8080
			      PIPELINES_CONCOURSE_PIPELINESSERVERPASSWORD: ((pipelines-server.security-ci-password))
			      PIPELINES_CONCOURSE_PIPELINESSERVERUSERNAME: ci
			      PIPELINES_CONCOURSE_PLATFORMPATHPREFIX: teams/
			      PIPELINES_CONCOURSE_PLATFORMSRCBRANCH: v2
			      PIPELINES_CONCOURSE_PLATFORMSRCURI: git@github.com:daniellavoie/infra-as-code-demo.git
			      PIPELINES_CONCOURSE_PLATFORMTERRAFORMBACKENDPREFIX: terraform/platforms/
			      PIPELINES_CONCOURSE_TERRAFORMBACKENDGCSBUCKET: terraform-states
			      PIPELINES_CONCOURSE_TERRAFORMSRCBRANCH: main
			      PIPELINES_CONCOURSE_TERRAFORMSRCURI: git@github.com:daniellavoie/infra-as-code-demo
			      PIPELINES_GCP_IMPERSONATESERVICEACCOUNT: service-account@yo.com
			      TARGET: project1-frontend-prod
			    input_mapping:
			      src: project1-frontend-prod-deployment-src
			  - set_pipeline: project1-frontend-prod-deployment
			    file: pipelines/pipelines.yaml
			  on_success:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME completed successfully
			  on_failure:
			    put: teams
			    params:
			      actionTarget: $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME
			      text: Job $ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME failed
			groups:
			- name: project1-backend-dev
			  jobs:
			  - project1-backend-dev-terraform-apply
			  - project1-backend-dev-terraform-destroy
			  - project1-backend-dev-terraform-plan
			  - project1-backend-dev-update-deployment-pipeline
			- name: project1-backend-prod
			  jobs:
			  - project1-backend-prod-terraform-apply
			  - project1-backend-prod-terraform-destroy
			  - project1-backend-prod-terraform-plan
			  - project1-backend-prod-update-deployment-pipeline
			- name: project1-frontend-dev
			  jobs:
			  - project1-frontend-dev-terraform-apply
			  - project1-frontend-dev-terraform-destroy
			  - project1-frontend-dev-terraform-plan
			  - project1-frontend-dev-update-deployment-pipeline
			- name: project1-frontend-prod
			  jobs:
			  - project1-frontend-prod-terraform-apply
			  - project1-frontend-prod-terraform-destroy
			  - project1-frontend-prod-terraform-plan
			  - project1-frontend-prod-update-deployment-pipeline
			""";
}
