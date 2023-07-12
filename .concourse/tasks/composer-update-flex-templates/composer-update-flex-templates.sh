#!/bin/bash

if [ -z "${COMPOSER_FLEX_TEMPLATES_IMAGE}" ]; then
  echo "env variable COMPOSER_FLEX_TEMPLATES_IMAGE is undefined"
  exit 1
fi

if [ -z "${COMPOSER_FLEX_TEMPLATES_TARGET_BUCKET}" ]; then
  echo "env variable COMPOSER_FLEX_TEMPLATES_TARGET_BUCKET is undefined"
  exit 1
fi

if [ -z "${COMPOSER_FLEX_TEMPLATES_TARGET_PATH}" ]; then
  echo "env variable COMPOSER_FLEX_TEMPLATES_TARGET_PATH is undefined"
  exit 1
fi

if [ -z "${COMPOSER_FLEX_TEMPLATES_VERSION}" ]; then
  echo "env variable COMPOSER_FLEX_TEMPLATES_VERSION is undefined"
  exit 1
fi

if [ -z "${COMPOSER_FLEX_TEMPLATES_METADATA}" ]; then
  echo "env variable COMPOSER_FLEX_TEMPLATES_TARGET_BUCKET is undefined"
  exit 1
fi

if [  "${GOOGLE_IMPERSONATE_SERVICE_ACCOUNT}" != "" ]; then
  GS_UTIL_FLAGS="-i $GOOGLE_IMPERSONATE_SERVICE_ACCOUNT"
fi

mkdir -p /root/.config/gcloud

echo "$GOOGLE_CREDENTIALS" > /root/.config/gcloud/application_default_credentials.json && \
  gcloud auth activate-service-account --key-file=/root/.config/gcloud/application_default_credentials.json && \
  gcloud dataflow flex-template build \
    "gs://${COMPOSER_FLEX_TEMPLATES_TARGET_BUCKET}/${COMPOSER_FLEX_TEMPLATES_TARGET_PATH}/${COMPOSER_FLEX_TEMPLATES_VERSION}/dataflow-ingestion.json" \
    --image "${COMPOSER_FLEX_TEMPLATES_IMAGE}" \
    --sdk-language JAVA \
    --metadata-file dags-src/${COMPOSER_FLEX_TEMPLATES_METADATA}

EXIT_CODE=$?

rm /root/.config/gcloud/application_default_credentials.json

exit $EXIT_CODE