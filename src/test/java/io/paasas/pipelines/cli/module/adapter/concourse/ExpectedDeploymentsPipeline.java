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
			- name: manifest-src
			  type: git
			  source:
			    uri: git@github.com:daniellavoie/deployment-as-code-demo.git
			    private_key: ((git.ssh-private-key))
			    branch: main
			    paths:
			    - {{manifest-path}}
			- name: terraform-dataset-1-src
			  type: git
			  source:
			    uri: git@github.com:teleport-java-client/paas-moe-le-cloud.git
			    private_key: ((git.ssh-private-key))
			    paths:
			    - dataset-1
			    tag_filter: v0.10.0
			- name: terraform-dataset-2-src
			  type: git
			  source:
			    uri: git@github.com:teleport-java-client/paas-moe-le-cloud.git
			    private_key: ((git.ssh-private-key))
			    branch: my-branch
			    paths:
			    - dataset-2
			- name: composer-1-dags-src
			  type: git
			  source:
			    uri: git@github.com:teleport-java-client/paas-moe-le-cloud.git
			    private_key: ((git.ssh-private-key))
			    branch: dags-branch
			    paths:
			    - dags-path
			- name: composer-1-variables-src
			  type: git
			  source:
			    uri: git@github.com:daniellavoie/deployment-as-code-demo.git
			    private_key: ((git.ssh-private-key))
			    branch: main
			    paths:
			    - {{manifest-dir}}/dev-composer-variables/composer-1.json
			- name: firebase-src
			  type: git
			  source:
			    uri: git@github.com:teleport-java-client/paas-moe-le-cloud.git
			    private_key: ((git.ssh-private-key))
			    branch: firebase-app
			    paths:
			    - firebase-app
			jobs:
			- name: update-cloud-run
			  plan:
			  - in_parallel:
			    - get: ci-src
			    - get: manifest-src
			      trigger: true
			  - task: update-cloud-run
			    file: ci-src/.concourse/tasks/cloudrun/cloudrun-deploy.yaml
			    params:
			      MANIFEST_PATH: {{manifest-path}}
			      PIPELINES_GCP_IMPERSONATESERVICEACCOUNT: terraform@control-plane-377914.iam.gserviceaccount.com
			    input_mapping:
			      src: manifest-src
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
			- name: terraform-apply-dataset-1
			  plan:
			  - in_parallel:
			    - get: ci-src
			    - get: manifest-src
			      trigger: true
			    - get: terraform-dataset-1-src
			      trigger: true
			  - task: terraform-apply
			    file: ci-src/.concourse/tasks/terraform-deployment/terraform-deployment-apply.yaml
			    params:
			      GCP_PROJECT_ID: control-plane-377914
			      GOOGLE_IMPERSONATE_SERVICE_ACCOUNT: terraform@control-plane-377914.iam.gserviceaccount.com
			      MANIFEST_PATH: {{manifest-path}}
			      TERRAFORM_BACKEND_GCS_BUCKET: control-plane-377914
			      TERRAFORM_DIRECTORY: dataset-1
			      TERRAFORM_GROUP_NAME: dataset-1
			      TERRAFORM_PREFIX: project1-backend-dev-dataset-1
			    input_mapping:
			      src: terraform-dataset-1-src
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
			- name: terraform-apply-dataset-2
			  plan:
			  - in_parallel:
			    - get: ci-src
			    - get: manifest-src
			      trigger: true
			    - get: terraform-dataset-2-src
			      trigger: true
			  - task: terraform-apply
			    file: ci-src/.concourse/tasks/terraform-deployment/terraform-deployment-apply.yaml
			    params:
			      GCP_PROJECT_ID: control-plane-377914
			      GOOGLE_IMPERSONATE_SERVICE_ACCOUNT: terraform@control-plane-377914.iam.gserviceaccount.com
			      MANIFEST_PATH: {{manifest-path}}
			      TERRAFORM_BACKEND_GCS_BUCKET: control-plane-377914
			      TERRAFORM_DIRECTORY: dataset-2
			      TERRAFORM_GROUP_NAME: dataset-2
			      TERRAFORM_PREFIX: project1-backend-dev-dataset-2
			    input_mapping:
			      src: terraform-dataset-2-src
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
			- name: update-composer-dags-composer-1
			  plan:
			  - in_parallel:
			    - get: ci-src
			    - get: composer-1-dags-src
			      trigger: true
			  - task: update-dags
			    file: ci-src/.concourse/tasks/composer-update-dags/composer-update-dags.yaml
			    params:
			      COMPOSER_DAGS_BUCKET_NAME: composer-1-bucket
			      COMPOSER_DAGS_BUCKET_PATH: dags
			      COMPOSER_DAGS_PATH: dags-path
			      GOOGLE_IMPERSONATE_SERVICE_ACCOUNT: terraform@control-plane-377914.iam.gserviceaccount.com
			    input_mapping:
			      dags-src: composer-1-dags-src
			- name: update-composer-variables-composer-1
			  plan:
			  - in_parallel:
			    - get: ci-src
			    - get: composer-1-variables-src
			      trigger: true
			  - task: update-variables
			    file: ci-src/.concourse/tasks/composer-update-variables/composer-update-variables.yaml
			    params:
			      COMPOSER_DAGS_BUCKET_NAME: composer-1-bucket
			      COMPOSER_DAGS_BUCKET_PATH: dags
			      COMPOSER_ENVIRONMENT_NAME: composer-1
			      COMPOSER_LOCATION: us-east1
			      COMPOSER_PROJECT: control-plane-377914
			      COMPOSER_VARIABLES_PATH: {{manifest-dir}}/dev-composer-variables/composer-1.json
			      GOOGLE_IMPERSONATE_SERVICE_ACCOUNT: terraform@control-plane-377914.iam.gserviceaccount.com
			    input_mapping:
			      composer-variables-src: composer-1-variables-src
			- name: deploy-firebase
			  plan:
			  - in_parallel:
			    - get: ci-src
			    - get: firebase-src
			      trigger: true
			  - task: npm-build
			    file: ci-src/.concourse/tasks/npm-build/npm-build.yaml
			    params:
			      NPM_COMMAND: env-cmd -f .env npm run build
			      NPM_ENV: |
			        my-test-env: my-test-value
			      NPM_INSTALL_ARGS: --legacy-peer-deps
			      NPM_PATH: firebase-app
			    input_mapping:
			      src: firebase-src
			    output_mapping:
			      src: firebase-src
			  - task: firebase-deploy
			    file: ci-src/.concourse/tasks/firebase-deploy/firebase-deploy.yaml
			    params:
			      FIREBASE_APP_PATH: firebase-app
			      FIREBASE_CONFIG: |
			        {
			          "hosting": {
			            "headers": [{
			              "source": "*",
			              "headers": [{
			                "key": "Access-Control-Allow-Origin",
			                "value": "*"
			              }]
			            }]
			          }
			        }
			      GCP_PROJECT_ID: control-plane-377914
			      GOOGLE_IMPERSONATE_SERVICE_ACCOUNT: terraform@control-plane-377914.iam.gserviceaccount.com
			    input_mapping:
			      src: firebase-src
			    output_mapping:
			      src: firebase-src
			      """;
}
