#!/bin/sh

TERRAFORM_VERSION=$(cat terraform-release/version)

read -r -d '' BUILD_ARGS << EOM
{
  "TERRAFORM_VERSION":  "${TERRAFORM_VERSION}"
}
EOM

echo "$BUILD_ARGS" > build-args/build-args.json