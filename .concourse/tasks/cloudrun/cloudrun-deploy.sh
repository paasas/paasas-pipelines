#!/bin/bash

if [ -z "${MANIFEST_PATH}" ]; then
  echo "env variable MANIFEST_PATH is undefined"
  exit 1
fi

if [ -z "${PIPELINES_GCP_CREDENTIALSJSON}" ]; then
  echo "env variable PIPELINES_GCP_CREDENTIALSJSON is required"
  exit 1
fi

PIPELINES_CONCOURSE_DEPLOYMENTPATHPREFIX=$MANIFEST_PATH && \
  java -jar /opt/paasas-pipelines/paasas-pipelines.jar \
    update-google-deployment \
    src/$MANIFEST_PATH