#!/bin/bash

if [ -z "${DOCKERHUB_USERNAME}" ]; then
  echo "env variable DOCKERHUB_USERNAME is undefined"
  exit 1
fi

if [ -z "${DOCKERHUB_PASSWORD}" ]; then
  echo "env variable DOCKERHUB_PASSWORD is undefined"
  exit 1
fi

./mvnw -f server/pom.xml jib:build \
    -Ddocker-registry.username=${DOCKERHUB_USERNAME} \
    -Ddocker-registry.password=${DOCKERHUB_PASSWORD}