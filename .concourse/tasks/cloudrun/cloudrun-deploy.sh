#!/bin/bash

if [ -z "${MANIFEST_PATH}" ]; then
  echo "env variable MANIFEST_PATH is undefined"
  exit 1
fi

if [ -z "${PIPELINES_GCP_CREDENTIALSJSON}" ]; then
  echo "env variable PIPELINES_GCP_CREDENTIALSJSON is required"
  exit 1
fi

if [ ! -z "$PIPELINES_SERVER" ]; then
  if [ -z "$PIPELINES_SERVER_USERNAME" ]; then
    echo "Env variable PIPELINES_SERVER_USERNAME is undefined"
    exit 1
  fi
  
  if [ -z "$PIPELINES_SERVER_PASSWORD" ]; then
    echo "Env variable PIPELINES_SERVER_PASSWORD is undefined"
    exit 1
  fi
fi

export PIPELINES_CONCOURSE_DEPLOYMENTPATHPREFIX=$MANIFEST_PATH && \
  export PIPELINES_CONCOURSE_PIPELINESSERVER=$PIPELINES_SERVER && \
  export PIPELINES_CONCOURSE_PIPELINESSERVERUSERNAME=$PIPELINES_SERVER_USERNAME && \
  export PIPELINES_CONCOURSE_PIPELINESSERVERPASSWORD=$PIPELINES_SERVER_PASSWORD && \
  export PIPELINES_CONCOURSE_JOBINFO_BUILD=$(/bin/cat build-metadata/build-name) && \
  export PIPELINES_CONCOURSE_JOBINFO_PIPELINE=$(/bin/cat build-metadata/build-pipeline-name) && \
  export PIPELINES_CONCOURSE_JOBINFO_TEAM=$(/bin/cat build-metadata/build-team-name) && \
  export PIPELINES_CONCOURSE_JOBINFO_JOB=$(/bin/cat build-metadata/build-job-name) && \
  export PIPELINES_CONCOURSE_JOBINFO_URL=$(/bin/cat build-metadata/atc-external-url) && \
  java -jar /opt/paasas-pipelines/paasas-pipelines-cli.jar \
    update-google-deployment \
    src/$MANIFEST_PATH