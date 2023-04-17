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

set -x
 
gsutil \
  $GS_UTIL_FLAGS \
  rsync dags-src/$COMPOSER_DAGS_PATH gs://$COMPOSER_DAGS_BUCKET_NAME/$COMPOSER_DAGS_BUCKET_PATH