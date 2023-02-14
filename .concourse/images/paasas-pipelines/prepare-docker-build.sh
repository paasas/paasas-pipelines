#!/bin/sh

VERSION=$(cat version/version)

read -r -d '' BUILD_ARGS << EOM
{
  "VERSION":  "${VERSION}"
}
EOM

cp build/pipelines-concourse src/.concourse/images/paasas-pipelines/paasas-pipelines

echo "$BUILD_ARGS" > build-args/build-args.json