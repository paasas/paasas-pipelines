#!/bin/bash

if [ -z "${MVN_REPOSITORY_USERNAME}" ]; then
  echo "env variable MVN_REPOSITORY_USERNAME is undefined"
  exit 1
fi

if [ -z  "${GIT_USER_NAME}" ]; then
  echo "env variable GIT_USER_NAME is undefined"
  exit 1
fi

if [ -z  "${GIT_USER_EMAIL}" ]; then
  echo "env variable GIT_USER_EMAIL is undefined"
  exit 1
fi

if [ -z  "${GIT_PRIVATE_KEY}" ]; then
  echo "env variable GIT_PRIVATE_KEY is undefined"
  exit 1
fi

if [ -z  "${GOOGLE_PROJECT_ID}" ]; then
  echo "env variable GOOGLE_PROJECT_ID is undefined"
  exit 1
fi

if [ -z  "${TEST_REPORTS_GIT_BRANCH}" ]; then
  echo "env variable TEST_REPORTS_GIT_BRANCH is undefined"
  exit 1
fi

if [ -z "${TEST_URL}" ]; then
  echo "env variable TEST_URL is undefined"
  exit 1
fi

export M2_HOME=~/.m2

mkdir -p ${M2_HOME}

if [ $? -ne 0 ]; then
  exit 1
fi

export BUILD_NUMBER="$(cat metadata/build_name)"
export PROJECT_ID="${GOOGLE_PROJECT_ID}"

if [ -z "$BUILD_NUMBER" ]; then
  exit 1
fi

pushd test-reports-src && \
  git config --global user.name "${GIT_USER_NAME}" && \
  git config --global user.email "${GIT_USER_EMAIL}" && \
  mkdir ~/.ssh && \
  ssh-keyscan github.com >> ~/.ssh/known_hosts && \
  echo "${GIT_PRIVATE_KEY}" > ~/.ssh/id_rsa && \
  chmod 600 ~/.ssh/id_rsa && \
  git update-ref refs/heads/${TEST_REPORTS_GIT_BRANCH} HEAD
  git checkout ${TEST_REPORTS_GIT_BRANCH} && \
  git pull --ff-only
  
if [ $? -ne 0 ]; then
    exit 1
fi

set -x

mkdir -p $GOOGLE_PROJECT_ID
  
cp -R $GOOGLE_PROJECT_ID/* ../src/src/test/resources/reports/consolidated/ && \
  popd && \
  pushd src && \
  rm -rf ~/.m2 && \
  ln -fs $(pwd)/m2 ~/.m2 && \
  cat > $(pwd)/m2/settings.xml <<EOF
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                          https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>github</id>
      <username>${MVN_REPOSITORY_USERNAME}</username>
      <password>${MVN_REPOSITORY_PASSWORD}</password>
    </server>
  </servers>
</settings>
EOF

if [ $? -ne 0 ]; then
    exit 1
fi

./mvnw -U test

TEST_RESULT=$?

popd && \
  pushd test-reports-src
  
if [ $? -ne 0 ]; then
  exit 1
fi

if [ ! -z "$ENV_VARIABLES_SECRET_MANAGER_KEY_NAME" ]; then
  mkdir -p /root/.config/gcloud

  echo "$GOOGLE_CREDENTIALS" > /root/.config/gcloud/application_default_credentials.json && \
    gcloud auth activate-service-account --key-file=/root/.config/gcloud/application_default_credentials.json && \
    VARIABLES_JSON=$(gcloud beta secrets versions access --project ${GOOGLE_PROJECT_ID} --secret ENV_VARIABLES_SECRET_MANAGER_KEY_NAME latest) && \
    for KEY in $(echo "${VARIABLES_JSON}" | jq 'keys[]' -r); do
      echo "Exporting env variable $KEY"
      export $KEY="$(echo $VARIABLES_JSON | jq --arg key $KEY '.[$key]' -r)"
    done
fi

ENV_VARIABLES_JSON='{"VAR1": "TOTO", "VAR2": "TATA"}' && \
  for KEY in $(echo "${sample}" | jq 'keys[]' -r); do
    export $KEY="$(echo $sample | jq --arg key $KEY '.[$key]' -r)"
  done

cp -R ../src/src/test/resources/reports/consolidated/* $GOOGLE_PROJECT_ID/ && \
  git add --all && \
  git commit -m "chore: update test reports" && \
  git push --set-upstream origin $TEST_REPORTS_GIT_BRANCH && \
  exit $TEST_RESULT