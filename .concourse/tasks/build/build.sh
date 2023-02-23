#!/bin/bash

set -x

export JAVA_HOME=/root/.sdkman/candidates/java/current

pushd src && \
  ./mvnw install && \
  ./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout > ../version/version && \
  popd && \
  mv src/target/paasas-pipelines-*.jar build/paasas-pipelines.jar