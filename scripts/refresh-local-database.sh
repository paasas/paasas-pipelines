#!/bin/bash

DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd ) && \
  PGPASSWORD=pipelines-server psql -h localhost -p 5432 -U pipelines-server pipelines-server < <($DIR/dump-database.sh)