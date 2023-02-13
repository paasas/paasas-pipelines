#!/bin/bash

set -x

pushd src && \
  ./mvnw -Pnative native:compile && \
  ./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout > ../version/version && \
  popd && \
  mv src/concourse/target/pipelines-concourse build/pipelines-concourse