#!/bin/bash

set -x

export JAVA_HOME=/root/.sdkman/candidates/java/current

pushd src && \
  ./mvnw install && \
  ./mvnw -f concourse/pom.xml -Pnative native:compile && \
  ./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout > ../version/version && \
  popd && \
  mv src/concourse/target/pipelines-concourse build/pipelines-concourse