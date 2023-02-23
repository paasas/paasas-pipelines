#!/bin/bash

if [ -z "${MANIFEST_PATH}" ]; then
  echo "env variable MANIFEST_PATH is undefined"
  exit 1
fi


if [ -z "${PIPELINES_CLOUDRUN_GOOGLECREDENTIALSJSON}" ]; then
  echo "env variable PIPELINES_CLOUDRUN_GOOGLECREDENTIALSJSON is required"
  exit 1
fi
  
if [ ${AUTH_RESULT} -ne 0 ]; then
  exit 1
fi

PIPELINES_CONCOURSE_DEPLOYMENTPATHPREFIX=$MANIFEST_PATH && \
  java -jar /opt/paasas-pipelines/paasas-pipelines.jar \
    update-google-deployment \
    src/$MANIFEST_PATH