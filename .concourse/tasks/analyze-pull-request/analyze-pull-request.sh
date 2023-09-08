#!/bin/bash

if [ -z "$GCP_PROJECT_ID: " ]; then
  echo "Env variable GCP_PROJECT_ID is undefined" 
  exit 1
fi

if [ -z "$PIPELINES_SERVER" ]; then
  echo "Env variable PIPELINES_SERVER is undefined"
  exit 1
fi

if [ -z "$PIPELINES_SERVER_USERNAME" ]; then
  echo "Env variable PIPELINES_SERVER_USERNAME is undefined"
  exit 1
fi

if [ -z "$PIPELINES_SERVER_PASSWORD" ]; then
  echo "Env variable PIPELINES_SERVER_PASSWORD is undefined"
  exit 1
fi

if [ -z "$GITHUB_REPOSITORY" ]; then
  echo "Env variable GITHUB_REPOSITORY is undefined"
  exit 1
fi

if [ -z "$MANIFEST_PATH" ]; then
  echo "Env variable MANIFEST_PATH is undefined"
  exit 1
fi

PULL_REQUEST_NUMBER=$(cat pr/.git/resource/pr) && \
  COMMIT=$(cat pr/.git/resource/author) && \
  COMMIT_AUTHOR=$(cat pr/.git/resource/author_email) && \
  MANIFEST_BASE64=$(cat manifest-src/$MANIFEST_PATH | base64 -w 0 -)

if [ $? -ne 0 ]; then
  exit 1
fi

read -r -d '' REQUEST_BODY << EOM
{
  "commit": "$COMMIT",
  "commitAuthor": "$COMMIT_AUTHOR",
  "manifestBase64": "$MANIFEST_BASE64",
  "project": "$GCP_PROJECT_ID",
  "pullRequestNumber": "$PULL_REQUEST_NUMBER",
  "repository": "$GITHUB_REPOSITORY"
}
EOM

echo "Requesting refresh with request body:"
echo "$REQUEST_BODY"

curl $PIPELINES_SERVER/api/ci/pull-request-analysis \
  -X POST \
  -H "Content-Type: application/json" \
  -u $PIPELINES_SERVER_USERNAME:$PIPELINES_SERVER_PASSWORD \
  -d "$REQUEST_BODY" \
  --fail-with-body