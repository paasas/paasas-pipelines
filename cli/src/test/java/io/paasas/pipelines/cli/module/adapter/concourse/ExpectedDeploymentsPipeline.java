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
			- name: metadata
			  type: docker-image
			  source:
			    repository: olhtbr/metadata-resource
			    tag: 2.0.1
			resources:
			- name: ci-src
			  type: git
			  source:
			    uri: git@github.com:paasas/paasas-pipelines-scripts.git
			    private_key: ((git.ssh-private-key))
			    branch: main
			    paths:
			    - .concourse
			- name: teams
			  type: teams-notification
			  source:
			    url: ((teams.webhookUrl))
			- name: metadata
			  type: metadata
			- name: manifest-src
			  type: git
			  source:
			    uri: git@github.com:daniellavoie/deployment-as-code-demo.git
			    private_key: ((git.ssh-private-key))
			    branch: main
			    paths:
			    - {{manifest-path}}
			- name: demo-webapp-image
			  type: registry-image
			  source:
			    password: ((googleCredentials))
			    repository: gcr.io/cloudrun/container/hello
			    tag: latest
			    username: _json_key
			- name: demo-webapp-tests-src
			  type: git
			  source:
			    uri: git@github.com:teleport-java-client/my-cloud-run-tests.git
			    private_key: ((git.ssh-private-key))
			    paths:
			    - cloud-run-tests
			- name: demo-webapp-test-reports-src
			  type: git
			  source:
			    uri: git@github.com:teleport-java-client/my-cloud-run-tests.git
			    private_key: ((git.ssh-private-key))
			    branch: gh-pages
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
			    paths:
			    - dags-path
			    tag_filter: 0.1.0
			- name: composer-1-variables-src
			  type: git
			  source:
			    uri: git@github.com:daniellavoie/deployment-as-code-demo.git
			    private_key: ((git.ssh-private-key))
			    branch: main
			    paths:
			    - {{manifest-dir}}/dev-composer-variables/composer-1.json
			- name: api-to-gcs-ingestion-image
			  type: registry-image
			  source:
			    password: ((googleCredentials))
			    repository: gcr.io/prj-iapw-cicd-c-cicd-vld4/dataflow/iawealth-api-to-gcs-ingestion
			    tag: v1.0.0
			    username: _json_key
			- name: gcs-to-bq-cob-image
			  type: registry-image
			  source:
			    password: ((googleCredentials))
			    repository: gcr.io/prj-iapw-cicd-c-cicd-vld4/dataflow/iawealth-gcs-to-bq-cob
			    tag: v1.0.0
			    username: _json_key
			- name: gcs-to-bq-csv-ingestion-image
			  type: registry-image
			  source:
			    password: ((googleCredentials))
			    repository: gcr.io/prj-iapw-cicd-c-cicd-vld4/dataflow/iawealth-gcs-to-bq-csv-ingestion
			    tag: v1.0.0
			    username: _json_key
			- name: gcs-to-bq-json-ingestion-image
			  type: registry-image
			  source:
			    password: ((googleCredentials))
			    repository: gcr.io/prj-iapw-cicd-c-cicd-vld4/dataflow/iawealth-gcs-to-bq-json-ingestion
			    tag: v1.0.0
			    username: _json_key
			- name: gcs-to-bq-mdm-image
			  type: registry-image
			  source:
			    password: ((googleCredentials))
			    repository: gcr.io/prj-iapw-cicd-c-cicd-vld4/dataflow/iawealth-gcs-to-bq-mdm
			    tag: v1.0.0
			    username: _json_key
			- name: firebase-src
			  type: git
			  source:
			    uri: git@github.com:teleport-java-client/paas-moe-le-cloud.git
			    private_key: ((git.ssh-private-key))
			    paths:
			    - firebase-app
			    tag_filter: my-tag
			- name: firebase-app-tests-src
			  type: git
			  source:
			    uri: git@github.com:teleport-java-client/my-tests.git
			    private_key: ((git.ssh-private-key))
			    paths:
			    - firebase-app-tests
			- name: firebase-app-test-reports-src
			  type: git
			  source:
			    uri: git@github.com:teleport-java-client/my-tests.git
			    private_key: ((git.ssh-private-key))
			    branch: gh-pages
			jobs:
			- name: update-cloud-run
			  plan:
			  - in_parallel:
			    - get: ci-src
			    - get: manifest-src
			      trigger: true
			    - get: demo-webapp-image
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
			- name: test-demo-webapp
			  plan:
			  - in_parallel:
			    - get: ci-src
			    - put: metadata
			    - get: manifest-src
			      passed:
			      - update-cloud-run
			      trigger: true
			    - get: demo-webapp-tests-src
			      trigger: true
			    - get: demo-webapp-test-reports-src
			  - task: test-demo-webapp
			    file: ci-src/.concourse/tasks/maven-test/maven-test.yaml
			    params:
			      APP_ID: demo-webapp
			      GIT_PRIVATE_KEY: ((git.ssh-private-key))
			      GIT_USER_EMAIL: dlavoie@live.ca
			      GIT_USER_NAME: daniellavoie
			      GOOGLE_IMPERSONATE_SERVICE_ACCOUNT: terraform@control-plane-377914.iam.gserviceaccount.com
			      GOOGLE_PROJECT_ID: control-plane-377914
			      MANIFEST_PATH: {{manifest-dir}}/dev.yaml
			      MVN_REPOSITORY_PASSWORD: ((github.userAccessToken))
			      MVN_REPOSITORY_USERNAME: daniellavoie
			      PIPELINES_GCP_IMPERSONATESERVICEACCOUNT: terraform@control-plane-377914.iam.gserviceaccount.com
			    input_mapping:
			      src: demo-webapp-tests-src
			      test-reports-src: demo-webapp-test-reports-src
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
			- name: build-flex-template-api-to-gcs-ingestion
			  plan:
			  - in_parallel:
			    - get: ci-src
			    - get: api-to-gcs-ingestion-image
			      trigger: true
			    - get: composer-1-dags-src
			      passed:
			      - update-composer-dags-composer-1
			  - task: build-flex-template
			    file: ci-src/.concourse/tasks/composer-update-flex-templates/composer-update-flex-templates.yaml
			    params:
			      COMPOSER_FLEX_TEMPLATES_IMAGE: gcr.io/prj-iapw-cicd-c-cicd-vld4/dataflow/iawealth-api-to-gcs-ingestion
			      COMPOSER_FLEX_TEMPLATES_METADATA: cloud-composer/dags/raw/df-docker-metadata/api-to-gcs-ingestion/metadata.json
			      COMPOSER_FLEX_TEMPLATES_TARGET_BUCKET: composer-1-bucket
			      COMPOSER_FLEX_TEMPLATES_TARGET_PATH: flex-templates/iawealth-api-to-gcs-ingestion
			      COMPOSER_FLEX_TEMPLATES_VERSION: v1.0.0
			      GOOGLE_IMPERSONATE_SERVICE_ACCOUNT: terraform@control-plane-377914.iam.gserviceaccount.com
			    input_mapping:
			      dags-src: composer-1-dags-src
			- name: build-flex-template-gcs-to-bq-cob
			  plan:
			  - in_parallel:
			    - get: ci-src
			    - get: gcs-to-bq-cob-image
			      trigger: true
			    - get: composer-1-dags-src
			      passed:
			      - update-composer-dags-composer-1
			  - task: build-flex-template
			    file: ci-src/.concourse/tasks/composer-update-flex-templates/composer-update-flex-templates.yaml
			    params:
			      COMPOSER_FLEX_TEMPLATES_IMAGE: gcr.io/prj-iapw-cicd-c-cicd-vld4/dataflow/iawealth-gcs-to-bq-cob
			      COMPOSER_FLEX_TEMPLATES_METADATA: cloud-composer/dags/raw/df-docker-metadata/gcs-to-bq-cob/metadata.json
			      COMPOSER_FLEX_TEMPLATES_TARGET_BUCKET: composer-1-bucket
			      COMPOSER_FLEX_TEMPLATES_TARGET_PATH: flex-templates/iawealth-gcs-to-bq-cob
			      COMPOSER_FLEX_TEMPLATES_VERSION: v1.0.0
			      GOOGLE_IMPERSONATE_SERVICE_ACCOUNT: terraform@control-plane-377914.iam.gserviceaccount.com
			    input_mapping:
			      dags-src: composer-1-dags-src
			- name: build-flex-template-gcs-to-bq-csv-ingestion
			  plan:
			  - in_parallel:
			    - get: ci-src
			    - get: gcs-to-bq-csv-ingestion-image
			      trigger: true
			    - get: composer-1-dags-src
			      passed:
			      - update-composer-dags-composer-1
			  - task: build-flex-template
			    file: ci-src/.concourse/tasks/composer-update-flex-templates/composer-update-flex-templates.yaml
			    params:
			      COMPOSER_FLEX_TEMPLATES_IMAGE: gcr.io/prj-iapw-cicd-c-cicd-vld4/dataflow/iawealth-gcs-to-bq-csv-ingestion
			      COMPOSER_FLEX_TEMPLATES_METADATA: cloud-composer/dags/raw/df-docker-metadata/gcs-to-bq-csv-ingestion/metadata.json
			      COMPOSER_FLEX_TEMPLATES_TARGET_BUCKET: composer-1-bucket
			      COMPOSER_FLEX_TEMPLATES_TARGET_PATH: flex-templates/iawealth-gcs-to-bq-csv-ingestion
			      COMPOSER_FLEX_TEMPLATES_VERSION: v1.0.0
			      GOOGLE_IMPERSONATE_SERVICE_ACCOUNT: terraform@control-plane-377914.iam.gserviceaccount.com
			    input_mapping:
			      dags-src: composer-1-dags-src
			- name: build-flex-template-gcs-to-bq-json-ingestion
			  plan:
			  - in_parallel:
			    - get: ci-src
			    - get: gcs-to-bq-json-ingestion-image
			      trigger: true
			    - get: composer-1-dags-src
			      passed:
			      - update-composer-dags-composer-1
			  - task: build-flex-template
			    file: ci-src/.concourse/tasks/composer-update-flex-templates/composer-update-flex-templates.yaml
			    params:
			      COMPOSER_FLEX_TEMPLATES_IMAGE: gcr.io/prj-iapw-cicd-c-cicd-vld4/dataflow/iawealth-gcs-to-bq-json-ingestion
			      COMPOSER_FLEX_TEMPLATES_METADATA: cloud-composer/dags/raw/df-docker-metadata/gcs-to-bq-json-ingestion/metadata.json
			      COMPOSER_FLEX_TEMPLATES_TARGET_BUCKET: composer-1-bucket
			      COMPOSER_FLEX_TEMPLATES_TARGET_PATH: flex-templates/iawealth-gcs-to-bq-json-ingestion
			      COMPOSER_FLEX_TEMPLATES_VERSION: v1.0.0
			      GOOGLE_IMPERSONATE_SERVICE_ACCOUNT: terraform@control-plane-377914.iam.gserviceaccount.com
			    input_mapping:
			      dags-src: composer-1-dags-src
			- name: build-flex-template-gcs-to-bq-mdm
			  plan:
			  - in_parallel:
			    - get: ci-src
			    - get: gcs-to-bq-mdm-image
			      trigger: true
			    - get: composer-1-dags-src
			      passed:
			      - update-composer-dags-composer-1
			  - task: build-flex-template
			    file: ci-src/.concourse/tasks/composer-update-flex-templates/composer-update-flex-templates.yaml
			    params:
			      COMPOSER_FLEX_TEMPLATES_IMAGE: gcr.io/prj-iapw-cicd-c-cicd-vld4/dataflow/iawealth-gcs-to-bq-mdm
			      COMPOSER_FLEX_TEMPLATES_METADATA: cloud-composer/dags/raw/df-docker-metadata/gcs-to-bq-mdm/metadata.json
			      COMPOSER_FLEX_TEMPLATES_TARGET_BUCKET: composer-1-bucket
			      COMPOSER_FLEX_TEMPLATES_TARGET_PATH: flex-templates/iawealth-gcs-to-bq-mdm
			      COMPOSER_FLEX_TEMPLATES_VERSION: v1.0.0
			      GOOGLE_IMPERSONATE_SERVICE_ACCOUNT: terraform@control-plane-377914.iam.gserviceaccount.com
			    input_mapping:
			      dags-src: composer-1-dags-src
			- name: deploy-firebase
			  plan:
			  - in_parallel:
			    - get: ci-src
			    - get: manifest-src
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
			- name: test-firebase-app
			  plan:
			  - in_parallel:
			    - get: ci-src
			    - put: metadata
			    - get: manifest-src
			      passed:
			      - deploy-firebase
			      trigger: true
			    - get: firebase-app-tests-src
			      trigger: true
			    - get: firebase-app-test-reports-src
			  - task: test-firebase-app
			    file: ci-src/.concourse/tasks/maven-test/maven-test.yaml
			    params:
			      APP_ID: firebase-app
			      GIT_PRIVATE_KEY: ((git.ssh-private-key))
			      GIT_USER_EMAIL: dlavoie@live.ca
			      GIT_USER_NAME: daniellavoie
			      GOOGLE_IMPERSONATE_SERVICE_ACCOUNT: terraform@control-plane-377914.iam.gserviceaccount.com
			      GOOGLE_PROJECT_ID: control-plane-377914
			      MANIFEST_PATH: {{manifest-dir}}/dev.yaml
			      MVN_REPOSITORY_PASSWORD: ((github.userAccessToken))
			      MVN_REPOSITORY_USERNAME: daniellavoie
			      PIPELINES_GCP_IMPERSONATESERVICEACCOUNT: terraform@control-plane-377914.iam.gserviceaccount.com
			    input_mapping:
			      src: firebase-app-tests-src
			      test-reports-src: firebase-app-test-reports-src
			      """;
}