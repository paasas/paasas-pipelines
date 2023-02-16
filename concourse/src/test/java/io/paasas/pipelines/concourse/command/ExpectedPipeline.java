package io.paasas.pipelines.concourse.command;

public abstract class ExpectedPipeline {
	public static final String PIPELINE = """
			resource_types:
			- name: pull-request
			  type: docker-image
			  source:
			    repository: teliaoss/github-pr-resource

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

			- name: project1-backend-dev-platform-src
			  type: git
			  source:
			    uri: https://github.com/daniellavoie/infra-as-code-demo
			    private_key: ((git.ssh-private-key))
			    branch: v2
			    paths:
			    - teams/project1/backend/dev.yaml
			- name: project1-backend-prod-platform-pr
			  type: pull-request
			  source:
			    access_token: ((github.accessToken))
			    repository: daniellavoie/infra-as-code-demo
			    paths:
			    - teams/project1/backend/prod.yaml

			- name: project1-backend-prod-platform-src
			  type: git
			  source:
			    uri: https://github.com/daniellavoie/infra-as-code-demo
			    private_key: ((git.ssh-private-key))
			    branch: v2
			    paths:
			    - teams/project1/backend/prod.yaml
			- name: project1-frontend-dev-platform-pr
			  type: pull-request
			  source:
			    access_token: ((github.accessToken))
			    repository: daniellavoie/infra-as-code-demo
			    paths:
			    - teams/project1/frontend/dev.yaml

			- name: project1-frontend-dev-platform-src
			  type: git
			  source:
			    uri: https://github.com/daniellavoie/infra-as-code-demo
			    private_key: ((git.ssh-private-key))
			    branch: v2
			    paths:
			    - teams/project1/frontend/dev.yaml
			- name: project1-frontend-prod-platform-pr
			  type: pull-request
			  source:
			    access_token: ((github.accessToken))
			    repository: daniellavoie/infra-as-code-demo
			    paths:
			    - teams/project1/frontend/prod.yaml

			- name: project1-frontend-prod-platform-src
			  type: git
			  source:
			    uri: https://github.com/daniellavoie/infra-as-code-demo
			    private_key: ((git.ssh-private-key))
			    branch: v2
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
			    file: ci-src/.concourse/tasks/terraform/terraform-apply.yaml
			    input_mapping:
			      src: project1-backend-dev-platform-src
			    params:
			      PLATFORM_MANIFEST_PATH: teams/project1/backend/dev.yaml
			      TARGET: project1-backend-dev
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states

			- name: project1-backend-dev-terraform-destroy
			  plan:
			  - in_parallel:
			    - get: project1-backend-dev-platform-src
			    - get: ci-src
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-apply
			    file: ci-src/.concourse/tasks/terraform/terraform-destroy.yaml
			    input_mapping:
			      src: project1-backend-dev-platform-src
			    params:
			      PLATFORM_MANIFEST_PATH: teams/project1/backend/dev.yaml
			      TARGET: project1-backend-dev
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states

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
			  - put: project1-backend-dev-platform-pr
			    params:
			      comment_file: terraform-out/terraform.md
			      path: project1-backend-dev-platform-pr
			      status: success
			  on_failure:
			    do:
			    - put: project1-backend-dev-platform-pr
			      params:
			        path: project1-backend-dev-infra-pr
			        status: failure
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

			- name: project1-backend-prod-terraform-destroy
			  plan:
			  - in_parallel:
			    - get: project1-backend-prod-platform-src
			    - get: ci-src
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-apply
			    file: ci-src/.concourse/tasks/terraform/terraform-destroy.yaml
			    input_mapping:
			      src: project1-backend-prod-platform-src
			    params:
			      PLATFORM_MANIFEST_PATH: teams/project1/backend/prod.yaml
			      TARGET: project1-backend-prod
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states

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
			  - put: project1-backend-prod-platform-pr
			    params:
			      comment_file: terraform-out/terraform.md
			      path: project1-backend-prod-platform-pr
			      status: success
			  on_failure:
			    do:
			    - put: project1-backend-prod-platform-pr
			      params:
			        path: project1-backend-prod-infra-pr
			        status: failure
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

			- name: project1-frontend-dev-terraform-destroy
			  plan:
			  - in_parallel:
			    - get: project1-frontend-dev-platform-src
			    - get: ci-src
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-apply
			    file: ci-src/.concourse/tasks/terraform/terraform-destroy.yaml
			    input_mapping:
			      src: project1-frontend-dev-platform-src
			    params:
			      PLATFORM_MANIFEST_PATH: teams/project1/frontend/dev.yaml
			      TARGET: project1-frontend-dev
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states

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
			  - put: project1-frontend-dev-platform-pr
			    params:
			      comment_file: terraform-out/terraform.md
			      path: project1-frontend-dev-platform-pr
			      status: success
			  on_failure:
			    do:
			    - put: project1-frontend-dev-platform-pr
			      params:
			        path: project1-frontend-dev-infra-pr
			        status: failure
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

			- name: project1-frontend-prod-terraform-destroy
			  plan:
			  - in_parallel:
			    - get: project1-frontend-prod-platform-src
			    - get: ci-src
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-apply
			    file: ci-src/.concourse/tasks/terraform/terraform-destroy.yaml
			    input_mapping:
			      src: project1-frontend-prod-platform-src
			    params:
			      PLATFORM_MANIFEST_PATH: teams/project1/frontend/prod.yaml
			      TARGET: project1-frontend-prod
			      TERRAFORM_BACKEND_GCS_BUCKET: terraform-states

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
			  - put: project1-frontend-prod-platform-pr
			    params:
			      comment_file: terraform-out/terraform.md
			      path: project1-frontend-prod-platform-pr
			      status: success
			  on_failure:
			    do:
			    - put: project1-frontend-prod-platform-pr
			      params:
			        path: project1-frontend-prod-infra-pr
			        status: failure

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
			  - project1-frontend-prod-terraform-plan
			""";
}
