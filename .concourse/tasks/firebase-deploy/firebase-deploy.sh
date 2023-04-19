#!/bin/bash

if [ -z "${GCP_PROJECT_ID} ]; then
  echo "env variable GCP_PROJECT_ID is undefined"
  exit 1
fi

if [ -z "${FIREBASE_CONFIG} ]; then
  echo "env variable FIREBASE_CONFIG is undefined"
  exit 1
fi

if [  "${GOOGLE_IMPERSONATE_SERVICE_ACCOUNT}" != "" ]; then
  GCLOUD_FLAGS="-i $GOOGLE_IMPERSONATE_SERVICE_ACCOUNT"
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
  gcloud auth activate-service-account ${GCLOUD_FLAGS} --key-file=/root/.config/gcloud/application_default_credentials.json && \
  pushd src/${FIREBASE_APP_PATH} && \
  echo "$FIREBASE_CONFIG" > firebase.json && \
  echo "$FIREBASERC" > .firebaserc && \
  firebase deploy --only hosting