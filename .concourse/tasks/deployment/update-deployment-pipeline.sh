#!/bin/sh

if [ -z "${MANIFEST_PATH}" ]; then
  echo "env variable MANIFEST_PATH is undefined"
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

if [ -z "${PIPELINES_GCP_IMPERSONATESERVICEACCOUNT}" ]; then
  echo "env PIPELINES_GCP_IMPERSONATESERVICEACCOUNT is undefined"
  exit 1
fi

cd src && \
  java -jar /opt/paasas-pipelines/paasas-pipelines.jar \
    generate-deployment-pipeline \
    $TARGET \
    ${MANIFEST_PATH} \
    ../pipelines/pipelines.yaml && \
  echo "Generated pipeline:"
  echo ""
  echo "$(cat ../pipelines/pipelines.yaml)"