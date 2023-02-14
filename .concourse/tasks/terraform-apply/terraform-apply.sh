#!/bin/sh

if [ -z "${PLATFORM_MANIFEST_PATH}" ]; then
  echo "Env variable PLATFORM_MANIFEST_PATH is undefined"
  exit 1
fi

set -x

WORKDIR=$(pwd)

TERRAFORM_BASELINE=$(yq '.infraBaseline' src/$PLATFORM_MANIFEST_PATH)

if [ "${TERRAFORM_BASELINE}" == "null" ]; then
  echo "failed to extra infra baseline from src/$ PLATFORM_MANIFEST_PATH}"
  
  exit 1
fi

mkdir tf && \
  cp terraform-${TERRAFORM_BASELINE}-src/terraform/infra/${TERRAFORM_BASELINE}/* tf/
  cd tf/ && \
  ln -fs ${WORKDIR}/.terraform .terraform && \
  terraform init
  
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