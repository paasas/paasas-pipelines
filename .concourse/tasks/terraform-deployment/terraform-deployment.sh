#!/bin/sh

if [ -z "${GCP_PROJECT_ID}" ]; then
  echo "Env variable GCP_PROJECT_ID is undefined"
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

if [ -z "${TERRAFORM_BACKEND_GCS_BUCKET}" ]; then
  echo "Env variable TERRAFORM_BACKEND_GCS_BUCKET is undefined"
  exit 1
fi

if [ -z "${GOOGLE_CREDENTIALS}" ]; then
  echo "Env variable GOOGLE_CREDENTIALS is undefined"
  exit 1
fi

if [ -z "${TARGET}" ]; then
  echo "Env variable TARGET is undefined"
  exit 1
fi

WORKDIR=$(pwd)

read -r -d '' PROVIDER_TF << EOM
provider "google" {
  project = "${GCP_PROJECT_ID}"
}
EOM

cd src/${TERRAFORM_DIRECTORY} && \
  echo "${PROVIDER_TF}" > gcp.tf && \
  ln -fs ${WORKDIR}/.terraform .terraform && \
  terraform init \
    -backend-config="bucket=${TERRAFORM_BACKEND_GCS_BUCKET}" \
    -backend-config="prefix=${TARGET}"
  
if [ $? -ne 0 ]; then
  exit 1
fi

terraform ${TERRAFORM_COMMAND} \
  ${TERRAFORM_FLAGS} \
  -var-file=${WORKDIR}/tf/tfvars.json | tee ${WORKDIR}/terraform-out/terraform.log

ERROR=$?

set +o pipefail

if [ "${TERRAFORM_COMMAND}" == "destroy" ]; then
  # Destroy requires to run twice
  set -o pipefail && \
    terraform ${TERRAFORM_COMMAND} \
      ${TERRAFORM_FLAGS} \
      -var-file=${WORKDIR}/tf/tfvars.json | tee -a ${WORKDIR}/terraform-out/terraform.log
  
  ERROR=$?
  
  set +o pipefail
fi

terraform show \
    -json > ${WORKDIR}/terraform-state/state.json && \
  echo '```' > ${WORKDIR}/terraform-out/terraform.md && \
  cat ${WORKDIR}/terraform-out/terraform.log >> ${WORKDIR}/terraform-out/terraform.md && \
  echo '```' >> ${WORKDIR}/terraform-out/terraform.md && \
  exit $ERROR
