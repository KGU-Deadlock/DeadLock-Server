#!/bin/bash
# Loads a pre-baked dataset SQL dump on first-time Postgres volume initialization.
# No-op when DATASET or DATASET_PG_FILE is empty, or the dump file is not found.
# Each Postgres service sets its own DATASET_PG_FILE (e.g. postgres-user.sql).
set -e

if [ -z "$DATASET" ]; then
  echo "[02-load-dataset] DATASET not set — skipping."
  exit 0
fi

if [ -z "$DATASET_PG_FILE" ]; then
  echo "[02-load-dataset] DATASET_PG_FILE not set — skipping."
  exit 0
fi

DUMP="/perf-datasets/${DATASET}/${DATASET_PG_FILE}"

if [ ! -f "$DUMP" ]; then
  echo "[02-load-dataset] Dump not found at ${DUMP} — skipping."
  exit 0
fi

echo "[02-load-dataset] Loading dataset '${DATASET}' (${DATASET_PG_FILE}) into '${POSTGRES_DB}' ..."
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -f "$DUMP"
echo "[02-load-dataset] Dataset '${DATASET}' (${DATASET_PG_FILE}) loaded successfully."
