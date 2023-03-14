#!/bin/bash

if [ -z "${TRIGGER_NAME}" ]; then
  echo "env variable TRIGGER_NAME is undefined"
  exit 1
fi

if [ -z "${PROJECT}" ]; then
  echo "env variable PROJECT is undefined"
  exit 1
fi

if [ -z "${REGION}" ]; then
  echo "env variable REGION is undefined"
  exit 1
fi

ARGS="--sha=$(cat src/.git/ref)"

if [ $? -ne 0 ]; then
  echo "Failed to compute source code revision to run trigger
  exit 1
fi

if [ ! -z "IMPERSONATE_SERVICE_ACCOUNT" ]; then
  ARGS="$ARGS --impersonate-service-account=${IMPERSONATE_SERVICE_ACCOUNT}"
fi

gcloud builds triggers run \
    --project ${PROJECT} \
    --region $REGION \
    $TRIGGER_NAME \
    $ARGS