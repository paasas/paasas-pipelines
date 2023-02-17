#!/bin/bash

set -x

export JAVA_HOME=/root/.sdkman/candidates/java/current

pushd src && \
  ./mvnw -Pnative native:compile && \
  ./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout > ../version/version && \
  popd && \
  mv src/target/paasas-pipelines build/paasas-pipelines