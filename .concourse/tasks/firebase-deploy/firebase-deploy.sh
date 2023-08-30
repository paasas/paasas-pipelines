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

if [ -z "${MANIFEST_PATH}" ]; then
  echo "Env variable MANIFEST_PATH is undefined"
  exit 1
fi

if [  "${GOOGLE_IMPERSONATE_SERVICE_ACCOUNT}" != "" ]; then
  GCLOUD_FLAGS="--impersonate-service-account=$GOOGLE_IMPERSONATE_SERVICE_ACCOUNT"
fi

WORKDIR=$(pwd)

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
  gcloud auth activate-service-account ${GCLOUD_FLAGS} --key-file=/root/.config/gcloud/application_default_credentials.json && \
  pushd src/${FIREBASE_APP_PATH} && \
  echo "$FIREBASERC" > .firebaserc && \
  firebase \
      --token $(gcloud auth print-access-token $GCLOUD_FLAGS) \
    deploy \
      --only hosting

if [ $? -ne 0 ]; then
  exit 1
fi

set -x

if [ ! -z "$PIPELINES_SERVER" ]; then
  cd ${WORKDIR}/src && \
  COMMIT=$(git rev-parse HEAD) && \
  TAG=$(/usr/local/bin/yq -o=json -I=0 ".firebaseApp.git.tag" ${WORKDIR}/manifest-src/${MANIFEST_PATH}) && \
  PATH=$(/usr/local/bin/yq -o=json -I=0 ".firebaseApp.git.path" ${WORKDIR}/manifest-src/${MANIFEST_PATH}) && \
  NPM=$(/usr/local/bin/yq -o=json -I=0 ".firebaseApp.npm" ${WORKDIR}/manifest-src/${MANIFEST_PATH}) && \
  OPTIONAL_TAG="" && \
  OPTIONAL_PATH=""
  
  if [ $? -ne 0 ]; then
    exit 1
  fi
  
  if [ "$TAG" != "null" ]; then
    OPTIONAL_TAG="\n,    \"tag\": ${TAG}"
  fi
  
  if [ "$PATH" != "null" ]; then
    OPTIONAL_PATH=", \"path\": $PATH"
  fi
  
  BUILD=$(/bin/cat ${WORKDIR}/build-metadata/build-name) && \
  JOB=$(/bin/cat ${WORKDIR}/build-metadata/build-job-name) && \
  PIPELINE=$(/bin/cat ${WORKDIR}/build-metadata/build-pipeline-name) && \
  TEAM=$(/bin/cat ${WORKDIR}/build-metadata/build-team-name) && \
  URL=$(/bin/cat ${WORKDIR}/build-metadata/atc-external-url) && \
  COMMIT_AUTHOR=$(/bin/cat ${WORKDIR}/src/.git/committer) && \
  read -r -d '' FIREBASE_APP_DEPLOYMENT << EOM
{
  "jobInfo": {
    "build": "$BUILD",
    "job": "$JOB",
    "pipeline": "$PIPELINE",
    "projectId": "$GCP_PROJECT_ID",
    "team": "$TEAM",
    "url": "$URL"
  },
  "gitRevision": {
    "commit": "${COMMIT}",
    "commitAuthor": "$COMMIT_AUTHOR",
    "repository": "${GITHUB_REPOSITORY}"${OPTIONAL_TAG}${OPTIONAL_PATH}
  },
  "npm": $NPM
}
EOM

  if [ -z "$FIREBASE_APP_DEPLOYMENT" ]; then
    exit 1
  fi
  
  /usr/bin/curl -X POST $PIPELINES_SERVER/api/ci/deployment/firebase \
    --fail \
    -H "Content-Type: application/json" \
    -u $PIPELINES_SERVER_USERNAME:$PIPELINES_SERVER_PASSWORD \
    -d "$FIREBASE_APP_DEPLOYMENT"
fi