#!/bin/bash

if [ -z "${MANIFEST_PATH}" ]; then
  echo "env variable MANIFEST_PATH is undefined"
  exit 1
fi


if [ -z "${GOOGLE_CREDENTIALS_JSON}" ]; then
  echo "env variable GOOGLE_CREDENTIALS_JSON is required"
  exit 1
fi
  
if [ -z "${GOOGLE_PROJECT}" ]; then
  echo "env variable GOOGLE_PROJECT is required"
  exit 1
fi
  
if [ -z "${GOOGLE_SERVICE_ACCOUNT_EMAIL}" ]; then
  echo "env variable GOOGLE_SERVICE_ACCOUNT_EMAIL is required"
  exit 1
fi

echo "$GOOGLE_CREDENTIALS_JSON" > google-credentials.json && \
  gcloud auth activate-service-account ${GOOGLE_SERVICE_ACCOUNT_EMAIL} \
      --key-file=google-credentials.json \
      --project=${GOOGLE_PROJECT}

AUTH_RESULT=$?

rm google-credentials.json

if [ ${AUTH_RESULT} -ne 0 ]; then
  exit 1
fi



gcloud run deploy my-backend --image=us-docker.pkg.dev/project/image
  --env-vars-file=