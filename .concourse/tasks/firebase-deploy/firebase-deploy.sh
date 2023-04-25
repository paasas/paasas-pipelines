#!/bin/bash

if [ -z "${FIREBASE_APP_PATH}" ]; then
  FIREBASE_JSON_FILE=src/firebase.json
else
  FIREBASE_JSON_FILE=src/${FIREBASE_APP_PATH}/firebase.json
fi

if [ ! -f "${FIREBASE_JSON_FILE}" ]; then
  echo "expected file ${FIREBASE_JSON_FILE}"
  exit 1
fi

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

if [ ! -z "${FIREBASE_CONFIG}" ]; then
  echo "${FIREBASE_CONFIG}" > /tmp/firebase.json &&
    jq -s '.[0] * .[1]' ${FIREBASE_JSON_FILE} /tmp/firebase.json > ${FIREBASE_JSON_FILE} &&
    echo "Generated firebase config at ${FIREBASE_JSON_FILE}:"
    echo "$(cat  ${FIREBASE_JSON_FILE})"
fi

echo "$GOOGLE_CREDENTIALS" > /root/.config/gcloud/application_default_credentials.json && \
  set -x && \
  gcloud auth activate-service-account ${GCLOUD_FLAGS} --key-file=/root/.config/gcloud/application_default_credentials.json && \
  pushd src/${FIREBASE_APP_PATH} && \
  echo "$FIREBASERC" > .firebaserc && \
  firebase \
      --token $(gcloud auth print-access-token $GCLOUD_FLAGS) \
    deploy \
      --only hosting