#!/bin/sh

if [ -z "${PLATFORM_MANIFEST_PATH}" ]; then
  echo "Env variable PLATFORM_MANIFEST_PATH is undefined"
  exit 1
fi

set -x

WORKDIR=$(pwd)

TERRAFORM_BASELINE=$(yq '.infraVersion' platform-manifest/$PLATFORM_MANIFEST_PATH) && \
  mkdir tf && \
  cp terraform-${TERRAFORM_BASELINE}-src/terraform/infra/${TERRAFORM_BASELINE}/* tf/
  cd tf/ && \
  ln -fs ${WORKDIR}/.terraform .terraform && \
  terraform init
  
if [ $? -ne 0 ]; then
  exit 1
fi

yq -o=json -I=0 '.terraformfVars' ${WORKDIR}/platform-manifest/${PLATFORM_MANIFEST_PATH} > ${WORKDIR}/tfVars.json && \
  terraform apply \
    --input=false \
    -var-file=${WORKDIR}/tfvars.json \
    -auto-approve && \
  terraform show \
    -json > ${WORKDIR}/terraform-state/state.json