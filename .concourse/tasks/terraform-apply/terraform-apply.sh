#!/bin/sh

if [ -z "${PLATFORM_MANIFEST_PATH}" ]; then
  echo "Env variable PLATFORM_MANIFEST_PATH is undefined"
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

if [ -z "${GCP_PROJECT_ID}" ]; then
  echo "Env variable GCP_PROJECT_ID is undefined"
  exit 1
fi

WORKDIR=$(pwd)

TERRAFORM_BASELINE=$(yq '.infraBaseline' src/$PLATFORM_MANIFEST_PATH)

if [ "${TERRAFORM_BASELINE}" == "null" ]; then
  echo "failed to extra infra baseline from src/$ PLATFORM_MANIFEST_PATH}"
  
  exit 1
fi

read -r -d '' PROVIDER_TF << EOM
provider "google" {
  project = "${GCP_PROJECT_ID}"
}
EOM

if [ $? -ne 0 ]; then
  exit 1
fi

mkdir tf && \
  cp terraform-${TERRAFORM_BASELINE}-src/terraform/infra/${TERRAFORM_BASELINE}/* tf/
  cd tf/ && \
  echo "${PROVIDER_TF}" > gcp.tf && \
  ln -fs ${WORKDIR}/.terraform .terraform && \
  terraform init \
    -backend-config="bucket=${TERRAFORM_BACKEND_GCS_BUCKET}" \
    -backend-config="bucket=${GCP_PROJECT_ID}"
  
if [ $? -ne 0 ]; then
  exit 1
fi

yq -o=json -I=0 '.terraformVars' ${WORKDIR}/src/${PLATFORM_MANIFEST_PATH} > ${WORKDIR}/tf/tfvars.json && \
  terraform apply \
    --input=false \
    -var-file=${WORKDIR}/tf/tfvars.json \
    -auto-approve && \
  terraform show \
    -json > ${WORKDIR}/terraform-state/state.json