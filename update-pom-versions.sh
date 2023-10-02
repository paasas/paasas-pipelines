#!/bin/bash

VERSION=$1

if [ -z "$VERSION" ]; then
  echo "argument VERSION is undefined"
  exit 1
fi

./mvnw versions:set -DnewVersion=$VERSION