#!/bin/sh

if [ -z "${PIPELINES_CONCOURSE_DEPLOYMENT_MANIFEST_FILE}" ]; then
  echo "env variable PIPELINES_CONCOURSE_DEPLOYMENT_MANIFEST_FILE is undefined"
  exit 1
fi

if [ -z "${PIPELINES_CONCOURSE_PLATFORMPATHPREFIX}" ]; then
  echo "env variable PIPELINES_CONCOURSE_PLATFORMPATHPREFIX is undefined"
  exit 1
fi

if [ -z "${PIPELINES_CONCOURSE_PLATFORMPATHPREFIX}" ]; then
  echo "env variable PIPELINES_CONCOURSE_PLATFORMPATHPREFIX is undefined"
  exit 1
fi

if [ -z "${PIPELINES_CONCOURSE_TERRAFORMSRCBRANCH}" ]; then
  echo "env variable PIPELINES_CONCOURSE_TERRAFORMSRCBRANCH is undefined"
  exit 1
fi

if [ -z "${PIPELINES_CONCOURSE_TERRAFORMBACKENDGCSBUCKET}" ]; then
  echo "env variable PIPELINES_CONCOURSE_TERRAFORMBACKENDGCSBUCKET is undefined"
  exit 1
fi

if [ -z "${PIPELINES_CONCOURSE_GCP_IMPERSONATE_SERVICE_ACCOUNT}" ]; then
  echo "env PIPELINES_CONCOURSE_GCP_IMPERSONATE_SERVICE_ACCOUNT is undefined"
  exit 1
fi

if [ -z "${PIPELINES_CONCOURSE_GCP_PROJECT}" ]; then
  echo "env PIPELINES_CONCOURSE_GCP_PROJECT is undefined"
  exit 1
fi

if [ -z "${PIPELINES_CONCOURSE_GCP_REGION}" ]; then
  echo "env PIPELINES_CONCOURSE_GCP_REGION is undefined"
  exit 1
fi

java -jar /opt/paasas-pipelines/paasas-pipelines.jar \
    generate-deployment-pipeline \
    $TARGET \
    src/$PIPELINES_CONCOURSE_DEPLOYMENT_MANIFEST_FILE \
    pipelines/pipelines.yaml && \
  echo "Generated pipeline:"
  echo ""
  echo "$(cat pipelines/pipelines.yaml)"