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
			    uri: https://github.com/paasas/platform-repository
			    private_key: ((git.ssh-private-key))
			    branch: v2
			    paths:
			    - terraform/infra/lts
			- name: terraform-next-src
			  type: git
			  source:
			    uri: https://github.com/paasas/platform-repository
			    private_key: ((git.ssh-private-key))
			    branch: v2
			    paths:
			    - terraform/infra/next
			- name: project1-frontend-dev.yaml-platform-pr
			  type: pull-request
			  source:
			    access_token: ((github.accessToken))
			    repository: paasas/platform-repository
			    paths:
			    - project1/frontend/dev.yaml

			- name: project1-frontend-dev.yaml-platform-src
			  type: git
			  source:
			    uri: v2
			    private_key: ((git.ssh-private-key))
			    branch: https://github.com/paasas/deployment-repository
			    paths:
			    - project1/frontend/dev.yaml
			- name: project1-frontend-prod.yaml-platform-pr
			  type: pull-request
			  source:
			    access_token: ((github.accessToken))
			    repository: paasas/platform-repository
			    paths:
			    - project1/frontend/prod.yaml

			- name: project1-frontend-prod.yaml-platform-src
			  type: git
			  source:
			    uri: v2
			    private_key: ((git.ssh-private-key))
			    branch: https://github.com/paasas/deployment-repository
			    paths:
			    - project1/frontend/prod.yaml
			- name: project1-backend-dev.yaml-platform-pr
			  type: pull-request
			  source:
			    access_token: ((github.accessToken))
			    repository: paasas/platform-repository
			    paths:
			    - project1/backend/dev.yaml

			- name: project1-backend-dev.yaml-platform-src
			  type: git
			  source:
			    uri: v2
			    private_key: ((git.ssh-private-key))
			    branch: https://github.com/paasas/deployment-repository
			    paths:
			    - project1/backend/dev.yaml
			- name: project1-backend-prod.yaml-platform-pr
			  type: pull-request
			  source:
			    access_token: ((github.accessToken))
			    repository: paasas/platform-repository
			    paths:
			    - project1/backend/prod.yaml

			- name: project1-backend-prod.yaml-platform-src
			  type: git
			  source:
			    uri: v2
			    private_key: ((git.ssh-private-key))
			    branch: https://github.com/paasas/deployment-repository
			    paths:
			    - project1/backend/prod.yaml

			jobs:
			- name: project1-frontend-dev.yaml-terraform-apply
			  plan:
			  - in_parallel:
			    - get: project1-frontend-dev.yaml-platform-src
			      trigger: true
			    - get: ci-src
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-apply
			    file: ci-src/.concourse/tasks/terraform-apply/terraform-apply.yml
			    input_mapping:
			      src: src-staging
			    params:
			      PLATFORM_MANIFEST_PATH: project1/frontend/dev.yaml

			- name: project1-frontend-dev.yaml-terraform-plan
			  plan:
			  - in_parallel:
			    - get: project1-frontend-dev.yaml-platform-pr
			      trigger: true
			    - get: ci-src
			  - in_parallel:
			    - put: project1-frontend-dev.yaml-platform-pr
			      params:
			        path: project1-frontend-dev.yaml-platform-pr
			        status: pending
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-plan
			    file: ci-src/.concourse/tasks/terraform-plan/terraform-plan.yml
			    input_mapping:
			      src: project1-frontend-dev.yaml-platform-pr
			    params:
			      PLATFORM_MANIFEST_PATH: project1/frontend/dev.yaml
			  - put: project1-frontend-dev.yaml-platform-pr
			    params:
			      comment_file: terraform-out/plan.md
			      path: project1-frontend-dev.yaml-platform-pr
			      status: success
			  on_failure:
			    do:
			    - put: project1-frontend-dev.yaml-platform-pr
			      params:
			        path: project1-frontend-dev.yaml-infra-pr
			        status: failure
			- name: project1-frontend-prod.yaml-terraform-apply
			  plan:
			  - in_parallel:
			    - get: project1-frontend-prod.yaml-platform-src
			      trigger: true
			    - get: ci-src
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-apply
			    file: ci-src/.concourse/tasks/terraform-apply/terraform-apply.yml
			    input_mapping:
			      src: src-staging
			    params:
			      PLATFORM_MANIFEST_PATH: project1/frontend/prod.yaml

			- name: project1-frontend-prod.yaml-terraform-plan
			  plan:
			  - in_parallel:
			    - get: project1-frontend-prod.yaml-platform-pr
			      trigger: true
			    - get: ci-src
			  - in_parallel:
			    - put: project1-frontend-prod.yaml-platform-pr
			      params:
			        path: project1-frontend-prod.yaml-platform-pr
			        status: pending
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-plan
			    file: ci-src/.concourse/tasks/terraform-plan/terraform-plan.yml
			    input_mapping:
			      src: project1-frontend-prod.yaml-platform-pr
			    params:
			      PLATFORM_MANIFEST_PATH: project1/frontend/prod.yaml
			  - put: project1-frontend-prod.yaml-platform-pr
			    params:
			      comment_file: terraform-out/plan.md
			      path: project1-frontend-prod.yaml-platform-pr
			      status: success
			  on_failure:
			    do:
			    - put: project1-frontend-prod.yaml-platform-pr
			      params:
			        path: project1-frontend-prod.yaml-infra-pr
			        status: failure
			- name: project1-backend-dev.yaml-terraform-apply
			  plan:
			  - in_parallel:
			    - get: project1-backend-dev.yaml-platform-src
			      trigger: true
			    - get: ci-src
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-apply
			    file: ci-src/.concourse/tasks/terraform-apply/terraform-apply.yml
			    input_mapping:
			      src: src-staging
			    params:
			      PLATFORM_MANIFEST_PATH: project1/backend/dev.yaml

			- name: project1-backend-dev.yaml-terraform-plan
			  plan:
			  - in_parallel:
			    - get: project1-backend-dev.yaml-platform-pr
			      trigger: true
			    - get: ci-src
			  - in_parallel:
			    - put: project1-backend-dev.yaml-platform-pr
			      params:
			        path: project1-backend-dev.yaml-platform-pr
			        status: pending
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-plan
			    file: ci-src/.concourse/tasks/terraform-plan/terraform-plan.yml
			    input_mapping:
			      src: project1-backend-dev.yaml-platform-pr
			    params:
			      PLATFORM_MANIFEST_PATH: project1/backend/dev.yaml
			  - put: project1-backend-dev.yaml-platform-pr
			    params:
			      comment_file: terraform-out/plan.md
			      path: project1-backend-dev.yaml-platform-pr
			      status: success
			  on_failure:
			    do:
			    - put: project1-backend-dev.yaml-platform-pr
			      params:
			        path: project1-backend-dev.yaml-infra-pr
			        status: failure
			- name: project1-backend-prod.yaml-terraform-apply
			  plan:
			  - in_parallel:
			    - get: project1-backend-prod.yaml-platform-src
			      trigger: true
			    - get: ci-src
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-apply
			    file: ci-src/.concourse/tasks/terraform-apply/terraform-apply.yml
			    input_mapping:
			      src: src-staging
			    params:
			      PLATFORM_MANIFEST_PATH: project1/backend/prod.yaml

			- name: project1-backend-prod.yaml-terraform-plan
			  plan:
			  - in_parallel:
			    - get: project1-backend-prod.yaml-platform-pr
			      trigger: true
			    - get: ci-src
			  - in_parallel:
			    - put: project1-backend-prod.yaml-platform-pr
			      params:
			        path: project1-backend-prod.yaml-platform-pr
			        status: pending
			    - get: terraform-lts-src
			    - get: terraform-next-src
			  - task: terraform-plan
			    file: ci-src/.concourse/tasks/terraform-plan/terraform-plan.yml
			    input_mapping:
			      src: project1-backend-prod.yaml-platform-pr
			    params:
			      PLATFORM_MANIFEST_PATH: project1/backend/prod.yaml
			  - put: project1-backend-prod.yaml-platform-pr
			    params:
			      comment_file: terraform-out/plan.md
			      path: project1-backend-prod.yaml-platform-pr
			      status: success
			  on_failure:
			    do:
			    - put: project1-backend-prod.yaml-platform-pr
			      params:
			        path: project1-backend-prod.yaml-infra-pr
			        status: failure

			groups:
			- name: project1-frontend-dev.yaml
			  jobs:
			  - project1-frontend-dev.yaml-terraform-plan
			  - project1-frontend-dev.yaml-terraform-apply

			- name: project1-frontend-prod.yaml
			  jobs:
			  - project1-frontend-prod.yaml-terraform-plan
			  - project1-frontend-prod.yaml-terraform-apply

			- name: project1-backend-dev.yaml
			  jobs:
			  - project1-backend-dev.yaml-terraform-plan
			  - project1-backend-dev.yaml-terraform-apply

			- name: project1-backend-prod.yaml
			  jobs:
			  - project1-backend-prod.yaml-terraform-plan
			  - project1-backend-prod.yaml-terraform-apply
			""";
}
