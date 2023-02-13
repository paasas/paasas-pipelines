#!/bin/bash

pushd \
  ./mvnw -Pnative native:compile && \
  ./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout > ../version/version && \
  popd && \
  mv src/concourse/target/pipelines-concourse build/pipelines-concourse && \
  