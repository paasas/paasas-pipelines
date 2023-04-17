#!/bin/bash

if [ -z "${COMPOSER_DAGS_BUCKET_NAME}" ]; then
  echo "env variable COMPOSER_DAGS_BUCKET_NAME is undefined"
  exit 1
fi

if [ -z "${COMPOSER_DAGS_BUCKET_PATH}" ]; then
  echo "env variable COMPOSER_DAGS_BUCKET_PATH is undefined"
  exit 1
fi

if [  "${GOOGLE_IMPERSONATE_SERVICE_ACCOUNT}" != "" ]; then
  GS_UTIL_FLAGS="-i $GOOGLE_IMPERSONATE_SERVICE_ACCOUNT"
fi

mkdir -p /root/.config/gcloud

echo "$GOOGLE_CREDENTIALS" > /root/.config/gcloud/application_default_credentials.json && \
  gcloud auth activate-service-account --key-file=/root/.config/gcloud/application_default_credentials.json && \
  gsutil $GS_UTIL_FLAGS rsync dags-src/$COMPOSER_DAGS_PATH gs://$COMPOSER_DAGS_BUCKET_NAME/$COMPOSER_DAGS_BUCKET_PATH

EXIT_CODE=$?

rm /root/.config/gcloud/application_default_credentials.json

exit $EXIT_CODE