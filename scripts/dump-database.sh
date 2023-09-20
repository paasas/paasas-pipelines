#!/bin/bash

kubectl exec pipelines-server-postgresql-0 -- bash -c 'echo $POSTGRES_POSTGRES_PASSWORD | pg_dump -U postgres --column-inserts --data-only --exclude-table-data=flyway_schema_history pipelines-server'

