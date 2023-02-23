#!/bin/sh

if [ -z "${TARGETS_DIRECTORY}" ]; then
  echo "env variable TARGETS_DIRECTORY is undefined"
  exit 1
fi

if [ -z "${TERRAFORM_BACKEND_GCS_BUCKET}" ]; then
  echo "env variable TERRAFORM_BACKEND_GCS_BUCKET is undefined"
  exit 1
fi

PIPELINES_CONCOURSE_TERRAFORMBACKENDGCSBUCKET="${TERRAFORM_BACKEND_GCS_BUCKET}" \
  PIPELINES_CONCOURSE_TERRAFORMSRCBRANCH=main \
  pipelines.concourse.platform-path-prefix=teams/$TARGETS_DIRECTORY \
  set -x && \
  java -jar /opt/paasas-pipelines/paasas-pipelines.jar \
    generate-pipeline \
    platforms-src/$TARGETS_DIRECTORY \
    pipelines/pipelines.yaml && \
  set +x && \
  echo "Generated pipeline:"
  echo ""
  echo "$(cat pipelines/pipelines.yaml)"