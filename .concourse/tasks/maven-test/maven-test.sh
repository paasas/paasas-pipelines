#!/bin/bash

if [ -z "${MVN_REPOSITORY_USERNAME}" ]; then
  echo "env variable MVN_REPOSITORY_USERNAME is undefined"
  exit 1
fi

export M2_HOME=~/.m2

mkdir -p ${M2_HOME}

if [ $? -ne 0 ]; then
    exit 1
fi

pushd src && \
  rm -rf ~/.m2 && \
  ln -fs $(pwd)/m2 ~/.m2 && \
  cat > $(pwd)/m2/settings.xml <<EOF
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                          https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>github</id>
      <username>${MVN_REPOSITORY_USERNAME}</username>
      <password>${MVN_REPOSITORY_PASSWORD}</password>
    </server>
  </servers>
</settings>
EOF

if [ $? -ne 0 ]; then
    exit 1
fi

./mvnw -U test