#!/bin/bash

if [ -z "${GCP_PROJECT_ID}" ]; then
  echo "env variable GCP_PROJECT_ID is undefined"
  exit 1
fi

if [  "${GOOGLE_IMPERSONATE_SERVICE_ACCOUNT}" != "" ]; then
  GCLOUD_FLAGS="--impersonate-service-account=$GOOGLE_IMPERSONATE_SERVICE_ACCOUNT"
fi

mkdir -p /root/.config/gcloud

read -r -d '' FIREBASERC << EOM
{
  "projects": {
    "default": "${GCP_PROJECT_ID}"
  }
}
EOM


echo "$GOOGLE_CREDENTIALS" > /root/.config/gcloud/application_default_credentials.json && \
  set -x && \
  gcloud auth activate-service-account ${GCLOUD_FLAGS} --key-file=/root/.config/gcloud/application_default_credentials.json && \
  pushd src/${FIREBASE_APP_PATH} && \
  echo "$FIREBASERC" > .firebaserc && \
  firebase \
      --token $(gcloud auth print-access-token $GCLOUD_FLAGS) \
    deploy \
      --only hosting