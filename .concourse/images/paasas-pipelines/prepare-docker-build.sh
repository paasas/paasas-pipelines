#!/bin/sh

VERSION=$(cat paasas-pipelines-release/version)

read -r -d '' BUILD_ARGS << EOM
{
  "VERSION":  "${VERSION}"
}
EOM

cp paasas-pipelines-release/concourse-pipelines paasas-pipelines-image-src/.concourse/images/paasas-pipelines

echo "$BUILD_ARGS" > build-args/build-args.json