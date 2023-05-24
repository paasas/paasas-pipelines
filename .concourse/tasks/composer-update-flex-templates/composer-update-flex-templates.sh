#!/bin/bash

if [ -z "${COMPOSER_FLEX_TEMPLATES_TARGET_BUCKET}" ]; then
  echo "env variable COMPOSER_FLEX_TEMPLATES_TARGET_BUCKET is undefined"
  exit 1
fi

if [  "${GOOGLE_IMPERSONATE_SERVICE_ACCOUNT}" != "" ]; then
  GS_UTIL_FLAGS="-i $GOOGLE_IMPERSONATE_SERVICE_ACCOUNT"
fi

mkdir -p /root/.config/gcloud

echo "$GOOGLE_CREDENTIALS" > /root/.config/gcloud/application_default_credentials.json && \
  gcloud auth activate-service-account --key-file=/root/.config/gcloud/application_default_credentials.json && \
  gsutil \
    $GS_UTIL_FLAGS \
    rsync \
    -d \
    -r \
    flex-templates-src/$COMPOSER_FLEX_TEMPLATES_SOURCE_PATH_PREFIX \
    gs://$COMPOSER_DAGS_BUCKET_NAME/$COMPOSER_FLEX_TEMPLATES_TARGET_PATH_PREFIX

EXIT_CODE=$?

rm /root/.config/gcloud/application_default_credentials.json

exit $EXIT_CODE