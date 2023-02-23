#!/bin/bash

set -x

export M2_HOME=~/.m2

mkdir -p ${M2_HOME}

export JAVA_HOME=/root/.sdkman/candidates/java/current

pushd src && \
  rm -rf ~/.m2 && \
  ln -fs $(pwd)/m2 ~/.m2 && \
  ./mvnw install && \
  ./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout > ../version/version && \
  popd && \
  mv src/target/paasas-pipelines-*.jar build/paasas-pipelines.jar