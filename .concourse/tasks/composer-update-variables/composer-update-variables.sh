#!/bin/bash

if [ -z "${COMPOSER_DAGS_BUCKET_NAME}" ]; then
  echo "env variable COMPOSER_DAGS_BUCKET_NAME is undefined"
  exit 1
fi

if [  "${GOOGLE_IMPERSONATE_SERVICE_ACCOUNT}" != "" ]; then
  GS_UTIL_FLAGS="-i $GOOGLE_IMPERSONATE_SERVICE_ACCOUNT"
fi

if [ -z "${COMPOSER_ENVIRONMENT_NAME}" ]; then
  echo "env variable COMPOSER_ENVIRONMENT_NAME is undefined"
  exit 1
fi

if [ -z "${COMPOSER_LOCATION}" ]; then
  echo "env variable COMPOSER_LOCATION is undefined"
  exit 1
fi

if [ -z "${COMPOSER_PROJECT}" ]; then
  echo "env variable COMPOSER_PROJECT is undefined"
  exit 1
fi

if [ -z "${COMPOSER_VARIABLES_PATH}" ]; then
  echo "env variable COMPOSER_VARIABLES_PATH is undefined"
  exit 1
fi

mkdir -p /root/.config/gcloud

echo "$GOOGLE_CREDENTIALS" > /root/.config/gcloud/application_default_credentials.json && \
  gcloud auth activate-service-account --key-file=/root/.config/gcloud/application_default_credentials.json && \
  gsutil \
    $GS_UTIL_FLAGS \
    cp \
    composer-variables-src/$COMPOSER_VARIABLES_PATH \
    gs://$COMPOSER_DAGS_BUCKET_NAME/composer-variables.json && \
  gcloud \
    composer \
    environments \
    run $COMPOSER_ENVIRONMENT_NAME \
    --project=${COMPOSER_PROJECT} \
    --location=${COMPOSER_LOCATION} \
    variables \
      import -- /home/airflow/gcs/composer-variables.json

EXIT_CODE=$?

rm /root/.config/gcloud/application_default_credentials.json

exit $EXIT_CODE