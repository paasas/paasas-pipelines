#!/bin/sh

if [ -z "${TARGETS_DIRECTORY}" ]; then
  echo "env variable TARGETS_DIRECTORY is undefined"
  exit 1
fi

set -x

paasas-pipelines \
    generate-pipeline \
    platforms-src/$TARGETS_DIRECTORY \
    pipelines/pipelines.yaml && \
  set +x && \
  echo "Generated pipeline:"
  echo ""
  echo "$(cat pipelines/pipelines.yaml)"