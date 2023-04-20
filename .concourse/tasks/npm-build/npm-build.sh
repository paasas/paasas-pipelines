#!/bin/bash

if [ -z "${NPM_COMMAND}" ]; then
  echo "env variable NPM_COMMAND is undefined"
  exit 1
fi

pushd src/${NPM_PATH} && \
  echo "$NPM_ENV" > .env && \
  npm install ${NPM_INSTALL_ARGS} && \
  npx ${NPM_COMMAND}