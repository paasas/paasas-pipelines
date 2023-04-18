#!/bin/sh

if [ -z "${GCP_PROJECT_ID}" ]; then
  echo "Env variable GCP_PROJECT_ID is undefined"
  exit 1
fi

if [ -z "${MANIFEST_PATH}" ]; then
  echo "Env variable MANIFEST_PATH is undefined"
  exit 1
fi

if [ -z "${TERRAFORM_COMMAND}" ]; then
  echo "Env variable TERRAFORM_COMMAND is undefined"
  exit 1
fi

if [ -z "${TERRAFORM_FLAGS}" ]; then
  echo "Env variable TERRAFORM_FLAGS is undefined"
  exit 1
fi

if [ -z "${TERRAFORM_GROUP_NAME}" ]; then
  echo "Env variable TERRAFORM_GROUP_NAME is undefined"
  exit 1
fi

if [ -z "${TERRAFORM_BACKEND_GCS_BUCKET}" ]; then
  echo "Env variable TERRAFORM_BACKEND_GCS_BUCKET is undefined"
  exit 1
fi

if [ -z "${GOOGLE_CREDENTIALS}" ]; then
  echo "Env variable GOOGLE_CREDENTIALS is undefined"
  exit 1
fi

if [ -z "${TERRAFORM_PREFIX}" ]; then
  echo "Env variable TERRAFORM_PREFIX is undefined"
  exit 1
fi

WORKDIR=$(pwd)

read -r -d '' PROVIDER_TF << EOM
provider "google" {
  project = "${GCP_PROJECT_ID}"
}

terraform {
  backend "gcs" {}
}
EOM

cd src/${TERRAFORM_DIRECTORY} && \
  echo "${PROVIDER_TF}" > gcp.tf && \
  ln -fs ${WORKDIR}/.terraform .terraform && \
  terraform init \
    -backend-config="bucket=${TERRAFORM_BACKEND_GCS_BUCKET}" \
    -backend-config="prefix=${TERRAFORM_PREFIX}"
  
if [ $? -ne 0 ]; then
  exit 1
fi

yq -o=json -I=0 ".terraform[]|select(.name|select(.==\"${TERRAFORM_GROUP_NAME}\")).vars" ${WORKDIR}/manifest-src/${MANIFEST_PATH} > tfvars.json && \
  set -o pipefail && \
  terraform ${TERRAFORM_COMMAND} \
    ${TERRAFORM_FLAGS} \
    -var-file=tfvars.json | tee ${WORKDIR}/terraform-out/terraform.log

TF_COMMAND_ERROR=$?

set +o pipefail

terraform show \
    -json > ${WORKDIR}/terraform-state/state.json && \
  echo '```' > ${WORKDIR}/terraform-out/terraform.md && \
  cat ${WORKDIR}/terraform-out/terraform.log >> ${WORKDIR}/terraform-out/terraform.md && \
  echo '```' >> ${WORKDIR}/terraform-out/terraform.md
  
ERROR=$?

if [ $TF_COMMAND_ERROR -ne 0 ]; then
  exit $TF_COMMAND_ERROR
fi

exit $ERROR