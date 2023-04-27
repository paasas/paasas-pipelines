#!/bin/bash

if [ -z "${COMPOSER_DAGS_BUCKET_NAME}" ]; then
  echo "env variable COMPOSER_DAGS_BUCKET_NAME is undefined"
  exit 1
fi

if [  "${GOOGLE_IMPERSONATE_SERVICE_ACCOUNT}" != "" ]; then
  GS_UTIL_FLAGS="-i $GOOGLE_IMPERSONATE_SERVICE_ACCOUNT"
  GCLOUD_FLAGS="--impersonate-service-account $GOOGLE_IMPERSONATE_SERVICE_ACCOUNT"
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
  set -x && \
  gsutil \
    $GS_UTIL_FLAGS \
    cp \
    composer-variables-src/$COMPOSER_VARIABLES_PATH \
    gs://$COMPOSER_DAGS_BUCKET_NAME/composer-variables.json && \
  EXTERNAL_IP=$(dig @resolver4.opendns.com myip.opendns.com +short) && \
  CONTAINER_CLUSTER_FULLNAME=$(gcloud composer environments describe $COMPOSER_ENVIRONMENT_NAME --location=$COMPOSER_LOCATION --project=${COMPOSER_PROJECT} $GCLOUD_FLAGS --format="value(config.gkeCluster)") &&
  CONTAINER_CLUSTER_NAME=$(basename $CONTAINER_CLUSTER_FULLNAME) && \
  CONTAINER_CLUSTER_LOCATION=$(dirname $CONTAINER_CLUSTER_FULLNAME | xargs dirname | xargs basename) && \
  gcloud \
    container \
    clusters \
    update \
    $CONTAINER_CLUSTER_NAME \
    --location=$CONTAINER_CLUSTER_LOCATION \
    --project=${COMPOSER_PROJECT} \
    --enable-master-authorized-networks \
    --master-authorized-networks \
    $EXTERNAL_IP/32 \
    $GCLOUD_FLAGS

if [ $? -ne 0 ]; then 
  rm /root/.config/gcloud/application_default_credentials.json
  
  exit 1
if

gcloud \
  composer \
  environments \
  run $COMPOSER_ENVIRONMENT_NAME \
  --project=${COMPOSER_PROJECT} \
  --location=${COMPOSER_LOCATION} \
  $GCLOUD_FLAGS \
  variables \
    import -- /home/airflow/gcs/composer-variables.json

EXIT_CODE=$?

gcloud \
  container \
  clusters \
  update \
  $CONTAINER_CLUSTER_NAME \
  --project=${COMPOSER_PROJECT} \
  --location=$CONTAINER_CLUSTER_LOCATION \
  --no-enable-master-authorized-networks \
  $GCLOUD_FLAGS

rm /root/.config/gcloud/application_default_credentials.json

exit $EXIT_CODE