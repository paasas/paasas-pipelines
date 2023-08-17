#!/bin/bash

set -x

export M2_HOME=~/.m2

mkdir -p ${M2_HOME}

pushd src && \
  rm -rf ~/.m2 && \
  ln -fs $(pwd)/m2 ~/.m2 && \
  ./mvnw install && \
  ./mvnw -f server/pom.xml jib:build \
    -Ddocker-registry.username=${DOCKER_REGISTRY_USERNAME} \
    -Ddocker-registry.password=${DOCKER_REGISTRY_PASSWORD}
  ./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout > ../version/version && \
  popd && \
  mv src/cli/target/paasas-pipelines-cli-*.jar build/paasas-pipelines-cli.jar