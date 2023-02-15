#!/bin/sh

if [ -z "${TARGETS_DIRECTORY}" ]; then
  echo "env variable TARGETS_DIRECTORY is undefined"
  exit 1
fi

if [ -z "${TERRAFORM_BACKEND_GCS_BUCKET}" ]; then
  echo "env variable TERRAFORM_BACKEND_GCS_BUCKET is undefined"
  exit 1
fi 

PIPELINES_TERRAFORMBACKENDGCSBUCKET=${TERRAFORM_BACKEND_GCS_BUCKET} \
  paasas-pipelines \
    generate-pipeline \
    platforms-src/$TARGETS_DIRECTORY \
    pipelines/pipelines.yaml && \
  echo "Generated pipeline:"
  echo ""
  echo "$(cat pipelines/pipelines.yaml)"