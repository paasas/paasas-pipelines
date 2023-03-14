#!/bin/sh

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

java -jar /opt/paasas-pipelines/paasas-pipelines.jar \
    generate-pipeline \
    src/$PIPELINES_CONCOURSE_PLATFORMPATHPREFIX \
    pipelines/pipelines.yaml && \
  echo "Generated pipeline:"
  echo ""
  echo "$(cat pipelines/pipelines.yaml)"