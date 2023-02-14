#!/bin/sh

VERSION=$(cat paasas-pipelines-release/version)

read -r -d '' BUILD_ARGS << EOM
{
  "VERSION":  "${VERSION}"
}
EOM

cp build/pipelines-concourse paasas-pipelines-image-src/.concourse/images/paasas-pipelines/paasas-pipelines

echo "$BUILD_ARGS" > build-args/build-args.json