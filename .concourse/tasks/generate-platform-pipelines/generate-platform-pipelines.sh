#!/bin/sh

if [ -z "${TARGETS_DIRECTORY}" ]; then
  echo "env variable TARGETS_DIRECTORY is undefined"
  exit 1
fi

paasas-pipelines \
    generate-pipeline \
    platforms-src/$TARGETS_DIRECTORY \
    pipelines/pipelines.yaml && \
  echo "Generated pipeline:"
  echo ""
  echo "$(cat pipelines/pipelines.yaml)"