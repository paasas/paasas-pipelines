#!/bin/sh

if [ -z "${PLATFORM_MANIFEST_PATH}" ]; then
  echo "Env variable PLATFORM_MANIFEST_PATH is undefined"
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
  echo "Env variable PLATFORM_MANIFEST_PATH is undefined"
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

if [ -z "$TERRAFORM_EXTENSIONS_DIRECTORY" ]; then
  echo "Env variable TERRAFORM_EXTENSIONS_DIRECTORY is undefined"
  exit 1
fi

WORKDIR=$(pwd)

TERRAFORM_BASELINE=$(yq '.infraBaseline' src/$PLATFORM_MANIFEST_PATH)

if [ "${TERRAFORM_BASELINE}" == "null" ]; then
  echo "failed to extra infra baseline from src/$ PLATFORM_MANIFEST_PATH}"
  
  exit 1
fi

GCP_PROJECT_ID=$(yq '.terraformVars.project_id' src/$PLATFORM_MANIFEST_PATH)

if [ "${GCP_PROJECT_ID}" == "null" ]; then
  echo "failed to extra gcp project id from src/$ PLATFORM_MANIFEST_PATH}"
  
  exit 1
fi

read -r -d '' PROVIDER_TF << EOM
provider "google" {
  project = "${GCP_PROJECT_ID}"
}
EOM

mkdir tf && \
  cp terraform-${TERRAFORM_BASELINE}-src/terraform/infra/${TERRAFORM_BASELINE}/* tf/

if [ -d "${WORKDIR}/src/${TERRAFORM_EXTENSIONS_DIRECTORY}" ]; then
  cp ${WORKDIR}/src/${TERRAFORM_EXTENSIONS_DIRECTORY}/* tf/

  if [ $? -ne 0 ]; then
    echo "failed to copy terraform extensions from '${TERRAFORM_EXTENSIONS_DIRECTORY}' to 'tf' directory"
    exit 1
  fi
fi

cd tf && \
  echo "${PROVIDER_TF}" > gcp.tf && \
  ln -fs ${WORKDIR}/.terraform .terraform && \
  terraform init \
    -backend-config="bucket=${TERRAFORM_BACKEND_GCS_BUCKET}" \
    -backend-config="prefix=${TARGET}"
  
if [ $? -ne 0 ]; then
  exit 1
fi

yq -o=json -I=0 '.terraformVars' ${WORKDIR}/src/${PLATFORM_MANIFEST_PATH} > ${WORKDIR}/tf/tfvars.json && \
  set -o pipefail && \
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
